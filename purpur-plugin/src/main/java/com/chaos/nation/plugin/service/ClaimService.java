package com.chaos.nation.plugin.service;

import com.chaos.nation.core.territory.TerritoryClaimService;
import com.chaos.nation.protocol.MessageHeader;
import com.chaos.nation.protocol.MessageType;
import com.chaos.nation.protocol.packets.DataPackets.ChunkClaim;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * 服务端占领编排：以单个区块为最小单位。
 *
 * <p>职责边界：
 *
 * <ul>
 *   <li>把 core 的 {@link TerritoryClaimService} 领域结果转换为协议包与持久化记录。
 *   <li>主线程只做内存领域操作（快、无 IO）；落库与广播由异步 flush 批处理完成。
 *   <li>变更去重 + 版本化：同一区块的高版本包可覆盖低版本包，避免乱序导致地图回退。
 * </ul>
 *
 * <p>不依赖 Bukkit 类型，便于单元测试；插件主类负责把请求 ID、玩家 UUID 等上下文传入。
 */
public final class ClaimService {
  /** 待落库/广播的一次单区块变更。 */
  public record PendingClaim(
      int chunkX,
      int chunkZ,
      int nationId,
      long regionVersion,
      long claimedAtUtc,
      String claimerUuid) {}

  private final TerritoryClaimService territory;
  private final Queue<PendingClaim> pending = new ConcurrentLinkedQueue<>();
  private final int batchSize;
  private final Consumer<List<PendingClaim>> sink;

  public ClaimService(
      TerritoryClaimService territory, int batchSize, Consumer<List<PendingClaim>> sink) {
    this.territory = territory;
    this.batchSize = batchSize;
    this.sink = sink;
  }

  /** 领域服务只读视图（测试/命令查询归属）。 */
  public TerritoryClaimService territory() {
    return territory;
  }

  /** 占领单个区块。成功变更时入队一条待落库记录，并返回可下发的协议包；幂等时返回 null。 */
  public ChunkClaim claim(
      int chunkX, int chunkZ, int nationId, String claimerUuid, long requestId) {
    TerritoryClaimService.ClaimOutcome outcome = territory.claim(chunkX, chunkZ, nationId);
    if (!outcome.changed()) return null;
    enqueue(outcome.change(), claimerUuid);
    return toPacket(outcome.change(), requestId);
  }

  /** 放弃单个区块。 */
  public ChunkClaim release(int chunkX, int chunkZ, String claimerUuid, long requestId) {
    TerritoryClaimService.ClaimOutcome outcome = territory.release(chunkX, chunkZ);
    if (!outcome.changed()) return null;
    enqueue(outcome.change(), claimerUuid);
    return toPacket(outcome.change(), requestId);
  }

  private void enqueue(TerritoryClaimService.ChunkChange change, String claimerUuid) {
    pending.add(
        new PendingClaim(
            change.chunkX(),
            change.chunkZ(),
            change.nationId(),
            change.regionVersion(),
            System.currentTimeMillis(),
            claimerUuid));
  }

  private static ChunkClaim toPacket(TerritoryClaimService.ChunkChange change, long requestId) {
    MessageHeader h = MessageHeader.of(MessageType.CHUNK_CLAIM, requestId, change.regionVersion());
    long claimAt = change.nationId() == 0 ? 0L : System.currentTimeMillis();
    return new ChunkClaim(
        h, change.chunkX(), change.chunkZ(), change.nationId(), change.regionVersion(), claimAt);
  }

  /** 异步周期调用：拉取一批待落库记录交给 sink；空返回 0。 */
  public int flush() {
    List<PendingClaim> batch = new ArrayList<>(batchSize);
    PendingClaim c;
    while (batch.size() < batchSize && (c = pending.poll()) != null) {
      batch.add(c);
    }
    if (batch.isEmpty()) return 0;
    sink.accept(batch);
    return batch.size();
  }

  public int pending() {
    return pending.size();
  }
}
