package com.chaos.nation.core.radio;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaos.nation.core.radio.LinkBudget.Config;
import com.chaos.nation.core.radio.LinkBudget.TerrainSample;
import com.chaos.nation.core.radio.SignalReport.Quality;
import org.junit.jupiter.api.Test;

class LinkBudgetTest {
  private static final Config CFG = Config.defaults();

  private static Transmitter tower() {
    return new Transmitter(
        100,
        1,
        "world",
        0,
        0,
        64.0,
        20_000,
        Frequency.ofMhz(433.0),
        7,
        0,
        Transmitter.Status.ONLINE);
  }

  @Test
  void sameInputs_produceIdenticalReport_determinismProperty() {
    TerrainSample terrain = new TerrainSample(2, 3.5, 12345L);
    SignalReport a = LinkBudget.compute(10, 20, tower(), CFG, terrain);
    SignalReport b = LinkBudget.compute(10, 20, tower(), CFG, terrain);
    assertThat(a).isEqualTo(b); // record 值相等 = 全字段确定
  }

  @Test
  void repeatedAcrossManySeeds_isDeterministic() {
    for (long seed = 0; seed < 200; seed++) {
      TerrainSample t = new TerrainSample((int) (seed % 5), seed * 0.1, seed);
      SignalReport first = LinkBudget.compute(7, 9, tower(), CFG, t);
      SignalReport second = LinkBudget.compute(7, 9, tower(), CFG, t);
      assertThat(first).isEqualTo(second);
    }
  }

  @Test
  void quality_degradesWithDistance() {
    TerrainSample clear = new TerrainSample(0, 0, 0L);
    SignalReport near = LinkBudget.compute(1, 0, tower(), CFG, clear);
    SignalReport mid = LinkBudget.compute(60, 0, tower(), CFG, clear);
    SignalReport far = LinkBudget.compute(200, 0, tower(), CFG, clear);
    assertThat(near.quality().ordinal()).isLessThanOrEqualTo(mid.quality().ordinal());
    assertThat(mid.quality().ordinal()).isLessThanOrEqualTo(far.quality().ordinal());
    assertThat(near.distanceBlocks()).isLessThan(mid.distanceBlocks());
    assertThat(mid.distanceBlocks()).isLessThan(far.distanceBlocks());
  }

  @Test
  void opaqueChunks_reduceSignal() {
    TerrainSample clear = new TerrainSample(0, 0, 0L);
    TerrainSample blocked = new TerrainSample(8, 0, 0L);
    SignalReport a = LinkBudget.compute(30, 0, tower(), CFG, clear);
    SignalReport b = LinkBudget.compute(30, 0, tower(), CFG, blocked);
    assertThat(b.receivedDbm()).isLessThan(a.receivedDbm());
    assertThat(b.terrainTag()).isEqualTo("OCCLUDED");
  }

  @Test
  void beyondDesignRadius_isMuted() {
    TerrainSample clear = new TerrainSample(0, 0, 0L);
    // 设计半径 256 格；300 格应静默。
    SignalReport out = LinkBudget.compute(300, 0, tower(), CFG, clear);
    assertThat(out.quality()).isEqualTo(Quality.MUTED);
    assertThat(out.audible()).isFalse();
  }

  @Test
  void qualityMapping_thresholdsAreOrdered() {
    assertThat(Quality.fromSnr(30)).isEqualTo(Quality.CLEAR);
    assertThat(Quality.fromSnr(15)).isEqualTo(Quality.HISSY);
    assertThat(Quality.fromSnr(5)).isEqualTo(Quality.BROKEN);
    assertThat(Quality.fromSnr(0)).isEqualTo(Quality.MUTED);
  }
}
