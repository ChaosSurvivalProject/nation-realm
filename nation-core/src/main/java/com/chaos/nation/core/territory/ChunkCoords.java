package com.chaos.nation.core.territory;

/**
 * 全局区块坐标与 region 局部坐标的换算。
 *
 * <p>世界以 {@value ChunkBitmap#REGION_SIZE}×{@value ChunkBitmap#REGION_SIZE} 区块为一个 region 管理，
 * 但占领的最小单位始终是<strong>单个区块</strong>。负坐标必须用向下取整（floorDiv/floorMod）， 否则 x=-1 会被错误算进相邻 region。
 */
public final class ChunkCoords {
  private ChunkCoords() {}

  /** 区块全局坐标 → region 坐标（向下取整，支持负数）。 */
  public static int regionOf(long chunk) {
    return (int) Math.floorDiv(chunk, (long) ChunkBitmap.REGION_SIZE);
  }

  /** 区块全局坐标 → region 内局部坐标 [0, 31]（支持负数）。 */
  public static int localOf(long chunk) {
    return (int) Math.floorMod(chunk, (long) ChunkBitmap.REGION_SIZE);
  }

  /** 把两个 int 区块坐标打包成一个 long，作为 map key，避免字符串拼接开销。 */
  public static long key(int chunkX, int chunkZ) {
    return (((long) chunkX) << 32) ^ (chunkZ & 0xFFFFFFFFL);
  }

  public static int chunkX(long key) {
    return (int) (key >> 32);
  }

  public static int chunkZ(long key) {
    return (int) key;
  }

  /** 玩家世界坐标（方块）→ 区块坐标：每 16 方块一个区块，向下取整支持负坐标。 */
  public static int chunkOfBlock(double blockCoord) {
    return (int) Math.floor(blockCoord / 16.0D);
  }
}
