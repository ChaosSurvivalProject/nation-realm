package com.chaos.nation.core.territory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChunkCoordsTest {
  @Test
  void positiveCoordinates_mapToRegionAndLocal() {
    assertThat(ChunkCoords.regionOf(0)).isZero();
    assertThat(ChunkCoords.localOf(0)).isZero();
    assertThat(ChunkCoords.regionOf(33)).isEqualTo(1);
    assertThat(ChunkCoords.localOf(33)).isEqualTo(1);
    assertThat(ChunkCoords.regionOf(31)).isZero();
    assertThat(ChunkCoords.localOf(31)).isEqualTo(31);
  }

  @Test
  void negativeCoordinates_floorDivToCorrectRegion() {
    // 关键：-1 属于 region -1 的 local 31，而不是 region 0。
    assertThat(ChunkCoords.regionOf(-1)).isEqualTo(-1);
    assertThat(ChunkCoords.localOf(-1)).isEqualTo(31);
    assertThat(ChunkCoords.regionOf(-32)).isEqualTo(-1);
    assertThat(ChunkCoords.localOf(-32)).isZero();
    assertThat(ChunkCoords.regionOf(-33)).isEqualTo(-2);
    assertThat(ChunkCoords.localOf(-33)).isEqualTo(31);
  }

  @Test
  void keyPackRoundTrip_handlesNegatives() {
    long key = ChunkCoords.key(-123, 456);
    assertThat(ChunkCoords.chunkX(key)).isEqualTo(-123);
    assertThat(ChunkCoords.chunkZ(key)).isEqualTo(456);
    assertThat(ChunkCoords.key(Integer.MIN_VALUE, Integer.MAX_VALUE))
        .isNotEqualTo(ChunkCoords.key(Integer.MAX_VALUE, Integer.MIN_VALUE));
  }

  @Test
  void blockToChunk_usesFloorDivision() {
    assertThat(ChunkCoords.chunkOfBlock(15.9)).isZero();
    assertThat(ChunkCoords.chunkOfBlock(16.0)).isEqualTo(1);
    assertThat(ChunkCoords.chunkOfBlock(-0.1)).isEqualTo(-1);
    assertThat(ChunkCoords.chunkOfBlock(-16.1)).isEqualTo(-2);
  }
}
