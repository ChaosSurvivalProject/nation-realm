package com.chaos.nation.core.territory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChunkBitmapTest {
  @Test
  void explorationAndClaim_areSparseAndCorrect() {
    ChunkBitmap b = new ChunkBitmap(0, 0);
    assertThat(b.isExplored(5, 9)).isFalse();
    b.markExplored(5, 9, 1000L);
    assertThat(b.isExplored(5, 9)).isTrue();
    assertThat(b.ownerOf(5, 9)).isZero(); // 探索 != 主张；未主张显示未知

    // 探索时间 与 占领时间 必须分离：探索过但占领时间为 0。
    assertThat(b.claimedAtUtc(5, 9)).isZero();
    assertThat(b.exploredAtUtc(5, 9)).isEqualTo(1000L);

    assertThat(b.claim(5, 9, 7, 2000L)).isTrue(); // 归属变化 → true
    assertThat(b.ownerOf(5, 9)).isEqualTo(7);
    assertThat(b.claimedAtUtc(5, 9)).isEqualTo(2000L);
    assertThat(b.claim(5, 9, 7, 3000L)).isFalse(); // 幂等 → false，时间不回退
    assertThat(b.claimedAtUtc(5, 9)).isEqualTo(2000L);
    assertThat(b.claimIfEmpty(5, 9, 8)).isFalse(); // 已被主张，不能覆盖
    assertThat(b.ownerOf(5, 9)).isEqualTo(7);

    assertThat(b.release(5, 9)).isTrue(); // 单个区块释放
    assertThat(b.ownerOf(5, 9)).isZero();
    assertThat(b.claimedAtUtc(5, 9)).isZero();
    assertThat(b.isExplored(5, 9)).isTrue(); // 释放不影响探索标记
  }

  @Test
  void memoryIsFixedPerRegion_notPerChunk() {
    ChunkBitmap b = new ChunkBitmap(1, 2);
    int before = b.estimatedBytes();
    for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++) b.markExplored(x, z, 1L);
    assertThat(b.estimatedBytes()).isEqualTo(before);
    // 固定约 20.5KB（位图 512B + owner 4KB + 探索时间 8KB + 占领时间 8KB），
    // 不随区块数增长对象数量：仍是每 region 固定开销。
    assertThat(before).isLessThan(24_000);
  }
}
