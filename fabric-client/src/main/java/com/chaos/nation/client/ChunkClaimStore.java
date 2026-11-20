package com.chaos.nation.client;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端单区块占领视图：以<strong>一个区块</strong>为最小单位保存归属。
 *
 * <p>与 region 快照解耦：占领增量（CHUNK_CLAIM）可直接落到单区块，不必等整块 region 快照。
 * 每个区块记录单调版本，乱序到达的旧包会被丢弃，避免地图归属回退。纯 Java，可单测。
 */
public final class ChunkClaimStore {
  private record Entry(int nationId, long regionVersion, long claimedAtUtc) {}

  private final Map<Long, Entry> chunks = new HashMap<>();

  /**
   * 应用单区块占领变更。
   *
   * @return true 表示成功应用（新版本）；false 表示旧包/重复包被丢弃
   */
  public boolean apply(int chunkX, int chunkZ, int nationId, long regionVersion, long claimedAtUtc) {
    long key = key(chunkX, chunkZ);
    Entry prev = chunks.get(key);
    if (prev != null && regionVersion <= prev.regionVersion()) {
      return false; // 旧包或重复包，幂等丢弃
    }
    chunks.put(key, new Entry(nationId, regionVersion, claimedAtUtc));
    return true;
  }

  /** 查询单个区块归属；未记录返回 0（无主/未知）。 */
  public int ownerAt(int chunkX, int chunkZ) {
    Entry e = chunks.get(key(chunkX, chunkZ));
    return e == null ? 0 : e.nationId();
  }

  public long versionAt(int chunkX, int chunkZ) {
    Entry e = chunks.get(key(chunkX, chunkZ));
    return e == null ? -1L : e.regionVersion();
  }

  public int size() {
    return chunks.size();
  }

  /** 与 ChunkCoords.key 保持一致的打包方式（客户端不引入 core 依赖）。 */
  private static long key(int chunkX, int chunkZ) {
    return (((long) chunkX) << 32) ^ (chunkZ & 0xFFFFFFFFL);
  }
}
