package com.chaos.nation.core.territory;

/**
 * 区域领土位图：一个 region 覆盖 32x32 区块，用稀疏 long[] 位图存储所有权是否存在， 并用一个 int[] 存归属国家 ID。禁止每区块一个臃肿对象。未探索区块
 * owner=0 显示“未知”。
 */
public final class ChunkBitmap {
  public static final int REGION_SIZE = 32;
  private static final int TOTAL = REGION_SIZE * REGION_SIZE;
  private final int regionX;
  private final int regionZ;
  private final long[] explored = new long[(TOTAL + 63) / 64];
  private final int[] owner = new int[TOTAL]; // 0 = 未知 / 未主张
  private final long[] claimedAtUtc = new long[TOTAL];

  public ChunkBitmap(int regionX, int regionZ) {
    this.regionX = regionX;
    this.regionZ = regionZ;
  }

  public int regionX() {
    return regionX;
  }

  public int regionZ() {
    return regionZ;
  }

  private static int index(int localX, int localZ) {
    if (localX < 0 || localX >= REGION_SIZE || localZ < 0 || localZ >= REGION_SIZE) {
      throw new IllegalArgumentException("local chunk out of region");
    }
    return localZ * REGION_SIZE + localX;
  }

  public void markExplored(int localX, int localZ, long atUtc) {
    int i = index(localX, localZ);
    explored[i >>> 6] |= 1L << (i & 63);
    claimedAtUtc[i] = Math.max(claimedAtUtc[i], atUtc);
  }

  public boolean isExplored(int localX, int localZ) {
    int i = index(localX, localZ);
    return (explored[i >>> 6] & (1L << (i & 63))) != 0;
  }

  public void claim(int localX, int localZ, int nationId) {
    if (nationId <= 0) throw new IllegalArgumentException("nationId must be positive");
    int i = index(localX, localZ);
    owner[i] = nationId;
  }

  /** 只有未主张或同国可重复主张；返回是否发生变更（用于增量版本）。 */
  public boolean claimIfEmpty(int localX, int localZ, int nationId) {
    int i = index(localX, localZ);
    if (owner[i] == 0) {
      owner[i] = nationId;
      return true;
    }
    return false;
  }

  public int ownerOf(int localX, int localZ) {
    return owner[index(localX, localZ)];
  }

  /** 内存占用：位图 + owner + 时间，固定大小，不随区块数量增长对象数。 */
  public int estimatedBytes() {
    return explored.length * 8 + owner.length * 4 + claimedAtUtc.length * 8;
  }
}
