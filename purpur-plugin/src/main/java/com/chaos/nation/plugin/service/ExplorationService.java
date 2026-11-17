package com.chaos.nation.plugin.service;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** 区块探索服务：玩家移动按区块坐标批量记录，去重后异步落库。 不加载区块、不生成地形；主线程只入队，异步线程批量拉取。 */
public final class ExplorationService {
  public record ChunkVisit(String playerUuid, int chunkX, int chunkZ, long atUtc) {}

  private final int batchSize;
  private final Queue<ChunkVisit> queue = new ConcurrentLinkedQueue<>();
  private final Set<String> seen = ConcurrentHashMap.newKeySet();
  private final BatchSink sink;

  /** 落库接口，由数据库适配器实现（异步线程调用）。 */
  public interface BatchSink {
    void persist(java.util.List<ChunkVisit> batch);
  }

  public ExplorationService(int batchSize, BatchSink sink) {
    this.batchSize = batchSize;
    this.sink = sink;
  }

  /** 主线程调用：入队；对 (player, chunk) 去重，避免重复落库。 */
  public void record(String playerUuid, int chunkX, int chunkZ, long atUtc) {
    String key = playerUuid + ":" + chunkX + ":" + chunkZ;
    if (seen.add(key)) {
      queue.add(new ChunkVisit(playerUuid, chunkX, chunkZ, atUtc));
    }
  }

  /** 异步线程周期性调用：拉取一批落库；队列空返回 0。 */
  public int flush() {
    java.util.List<ChunkVisit> batch = new java.util.ArrayList<>(batchSize);
    ChunkVisit v;
    while (batch.size() < batchSize && (v = queue.poll()) != null) {
      batch.add(v);
    }
    if (batch.isEmpty()) return 0;
    sink.persist(batch);
    // 落库成功后从去重集合移除，允许将来再次探索（跨会话重新记录）
    for (ChunkVisit item : batch) {
      seen.remove(item.playerUuid() + ":" + item.chunkX() + ":" + item.chunkZ());
    }
    return batch.size();
  }

  public int pending() {
    return queue.size();
  }
}
