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

    b.claim(5, 9, 7);
    assertThat(b.ownerOf(5, 9)).isEqualTo(7);
    assertThat(b.claimIfEmpty(5, 9, 8)).isFalse(); // 已被主张，不能覆盖
    assertThat(b.ownerOf(5, 9)).isEqualTo(7);
  }

  @Test
  void memoryIsFixedPerRegion_notPerChunk() {
    ChunkBitmap b = new ChunkBitmap(1, 2);
    int before = b.estimatedBytes();
    for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++) b.markExplored(x, z, 1L);
    assertThat(b.estimatedBytes()).isEqualTo(before);
    // 固定约 12.6KB（位图 512B + owner 4KB + 时间 8KB），不随区块数增长对象数量。
    assertThat(before).isLessThan(20_000);
  }
}
