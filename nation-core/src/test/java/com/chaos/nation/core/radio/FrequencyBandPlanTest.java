package com.chaos.nation.core.radio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FrequencyBandPlanTest {
  @Test
  void frequency_storesKhzAndDisplaysMhz() {
    Frequency f = Frequency.ofMhz(145.5);
    assertThat(f.khz()).isEqualTo(145_500);
    assertThat(f.displayMhz()).isEqualTo("145.500");
    assertThat(Frequency.ofKhz(118_000).displayMhz()).isEqualTo("118.000");
  }

  @Test
  void frequency_rejectsInvalid() {
    assertThatThrownBy(() -> Frequency.ofKhz(0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Frequency.ofMhz(-1)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void bandOf_findsCorrectBandOrNull() {
    assertThat(BandPlan.bandOf(Frequency.ofMhz(121.5))).isEqualTo(BandPlan.VHF_AIR);
    assertThat(BandPlan.bandOf(Frequency.ofMhz(433.0))).isEqualTo(BandPlan.UHF_PUBLIC);
    assertThat(BandPlan.bandOf(Frequency.ofMhz(7.1))).isEqualTo(BandPlan.SHORTWAVE);
    assertThat(BandPlan.bandOf(Frequency.ofMhz(300.0))).isNull(); // 不在任何规划段
  }

  @Test
  void rasterAlignment_checksChannelSpacing() {
    // VHF_AIR 从 118000 起、间隔 25kHz：118025 在栅格，118010 不在。
    assertThat(BandPlan.VHF_AIR.onRaster(Frequency.ofKhz(118_025))).isTrue();
    assertThat(BandPlan.VHF_AIR.onRaster(Frequency.ofKhz(118_010))).isFalse();
  }

  @Test
  void subToneAllowed_handlesZeroAndKnownAndUnknown() {
    assertThat(BandPlan.subToneAllowed(BandPlan.VHF_AIR, 0)).isTrue(); // 不启用亚音
    assertThat(BandPlan.subToneAllowed(BandPlan.VHF_AIR, 1)).isTrue(); // 67.0 Hz
    assertThat(BandPlan.subToneAllowed(BandPlan.VHF_AIR, 999)).isFalse();
  }

  @Test
  void stationAndTransmitter_validateInvariants() {
    Station st =
        new Station(1, "NATION-1", Frequency.ofMhz(433.0), 0, StationScope.FACTION, 7, true, 1);
    assertThat(st.isPublic()).isFalse();
    assertThatThrownBy(
            () -> new Station(0, "x", Frequency.ofMhz(433.0), 0, StationScope.PUBLIC, 0, true, 1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void subscriptionIdempotencyKey_isStable() {
    RadioSubscription s = new RadioSubscription("player-uuid", 5, 2, 123L, true);
    assertThat(s.idempotencyKey()).isEqualTo("player-uuid:5:2");
  }

  @Test
  void transmissionSession_expiry() {
    TransmissionSession s = new TransmissionSession("sid", "p", 1, 0, 0, 1000L, 1030L);
    assertThat(s.expired(1029L)).isFalse();
    assertThat(s.expired(1030L)).isTrue(); // 30s 票据
  }
}
