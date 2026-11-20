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
  private final long[] exploredAtUtc = new long[TOTAL];
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

  /** 仅标记探索，不触碰占领时间：探索 ≠ 占领。 */
  public void markExplored(int localX, int localZ, long atUtc) {
    int i = index(localX, localZ);
    explored[i >>> 6] |= 1L << (i & 63);
    exploredAtUtc[i] = Math.max(exploredAtUtc[i], atUtc);
  }

  public boolean isExplored(int localX, int localZ) {
    int i = index(localX, localZ);
    return (explored[i >>> 6] & (1L << (i & 63))) != 0;
  }

  public long exploredAtUtc(int localX, int localZ) {
    return exploredAtUtc[index(localX, localZ)];
  }

  /**
   * 以单个区块为最小单位占领：覆盖写入并记录占领时间。
   *
   * @return true 表示归属发生变化（版本需递增并下发增量）
   */
  public boolean claim(int localX, int localZ, int nationId, long atUtc) {
    if (nationId <= 0) throw new IllegalArgumentException("nationId must be positive");
    int i = index(localX, localZ);
    if (owner[i] == nationId) return false;
    owner[i] = nationId;
    claimedAtUtc[i] = atUtc;
    return true;
  }

  /** 兼容旧调用：以当前时间占领。 */
  public boolean claim(int localX, int localZ, int nationId) {
    return claim(localX, localZ, nationId, System.currentTimeMillis());
  }

  /** 只有未主张区块可占领；返回是否发生变更（用于增量版本）。 */
  public boolean claimIfEmpty(int localX, int localZ, int nationId, long atUtc) {
    if (nationId <= 0) throw new IllegalArgumentException("nationId must be positive");
    int i = index(localX, localZ);
    if (owner[i] == 0) {
      owner[i] = nationId;
      claimedAtUtc[i] = atUtc;
      return true;
    }
    return false;
  }

  public boolean claimIfEmpty(int localX, int localZ, int nationId) {
    return claimIfEmpty(localX, localZ, nationId, System.currentTimeMillis());
  }

  /** 以单个区块为最小单位放弃主张；未主张时无变更。 */
  public boolean release(int localX, int localZ) {
    int i = index(localX, localZ);
    if (owner[i] == 0) return false;
    owner[i] = 0;
    claimedAtUtc[i] = 0L;
    return true;
  }

  public int ownerOf(int localX, int localZ) {
    return owner[index(localX, localZ)];
  }

  public long claimedAtUtc(int localX, int localZ) {
    return claimedAtUtc[index(localX, localZ)];
  }

  /** 内存占用：位图 + owner + 探索时间 + 占领时间，固定大小，不随区块数量增长对象数。 */
  public int estimatedBytes() {
    return explored.length * 8
        + owner.length * 4
        + exploredAtUtc.length * 8
        + claimedAtUtc.length * 8;
  }
}
