package com.chaos.nation.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChunkClaimStoreTest {
  @Test
  void applySingleChunk_thenQueryOwner() {
    ChunkClaimStore store = new ChunkClaimStore();
    assertThat(store.ownerAt(5, 9)).isZero(); // 未记录 = 无主
    store.apply(5, 9, 7, 1L, 1000L);
    assertThat(store.ownerAt(5, 9)).isEqualTo(7);
    assertThat(store.versionAt(5, 9)).isEqualTo(1L);
    assertThat(store.size()).isEqualTo(1);
  }

  @Test
  void staleVersion_isRejected_preventsMapRollback() {
    ChunkClaimStore store = new ChunkClaimStore();
    store.apply(1, 1, 3, 10L, 1000L);
    // 乱序旧包（版本 9）必须被丢弃，不能把国家 3 覆盖成他国
    assertThat(store.apply(1, 1, 9, 9L, 900L)).isFalse();
    assertThat(store.ownerAt(1, 1)).isEqualTo(3);
    // 重复包也不产生变更
    assertThat(store.apply(1, 1, 3, 10L, 1000L)).isFalse();
  }

  @Test
  void higherVersionTransfer_overwritesOwner() {
    ChunkClaimStore store = new ChunkClaimStore();
    store.apply(2, 2, 3, 1L, 1000L);
    assertThat(store.apply(2, 2, 8, 2L, 1100L)).isTrue(); // 转让，版本更高
    assertThat(store.ownerAt(2, 2)).isEqualTo(8);
  }

  @Test
  void releaseWithZeroOwner_isApplied() {
    ChunkClaimStore store = new ChunkClaimStore();
    store.apply(4, 4, 6, 1L, 1000L);
    store.apply(4, 4, 0, 2L, 0L); // nationId=0 = 放弃
    assertThat(store.ownerAt(4, 4)).isZero();
    assertThat(store.versionAt(4, 4)).isEqualTo(2L);
  }

  @Test
  void negativeCoordinates_areDistinctChunks() {
    ChunkClaimStore store = new ChunkClaimStore();
    store.apply(-1, -1, 1, 1L, 1000L);
    store.apply(0, 0, 2, 1L, 1000L);
    assertThat(store.ownerAt(-1, -1)).isEqualTo(1);
    assertThat(store.ownerAt(0, 0)).isEqualTo(2);
    assertThat(store.size()).isEqualTo(2);
  }
}
