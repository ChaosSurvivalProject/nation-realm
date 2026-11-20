package com.chaos.nation.core.territory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * 领土占领服务：<strong>以单个区块为最小单位</strong>进行占领、放弃与查询。
 *
 * <p>设计要点：
 *
 * <ul>
 *   <li>按 region 懒加载 {@link ChunkBitmap}，避免启动时分配全世界；每个 region 固定约 20KB 内存。
 *   <li>每次成功变更递增 region 版本，并产出 {@link ChunkChange}，供服务端下发增量到客户端。
 *   <li>幂等：相同归属的重复占领返回 no-op，不递增版本、不产生变更。
 *   <li>时钟通过 {@link LongSupplier} 注入，便于测试确定性。
 * </ul>
 *
 * <p>本类不依赖 Bukkit / 协议 / 数据库，纯领域逻辑，可单测。
 */
public final class TerritoryClaimService {
  /** 单个区块的一次归属变更。 */
  public record ChunkChange(
      int chunkX,
      int chunkZ,
      int nationId, // 0 = 放弃主张
      long regionVersion) {}

  public enum ClaimResult {
    CLAIMED, // 新占领
    TRANSFERRED, // 从他国转让
    UNCHANGED, // 幂等：已属该国
    RELEASED, // 放弃成功
    NOTHING_TO_RELEASE // 本就无主张
  }

  private final Map<Long, ChunkBitmap> regions = new ConcurrentHashMap<>();
  private final Map<Long, Long> versions = new ConcurrentHashMap<>();
  private final LongSupplier clock;

  public TerritoryClaimService(LongSupplier clock) {
    this.clock = clock;
  }

  public TerritoryClaimService() {
    this(System::currentTimeMillis);
  }

  private static long regionKey(int regionX, int regionZ) {
    return ChunkCoords.key(regionX, regionZ);
  }

  private ChunkBitmap region(int chunkX, int chunkZ) {
    int rx = ChunkCoords.regionOf(chunkX);
    int rz = ChunkCoords.regionOf(chunkZ);
    long key = regionKey(rx, rz);
    return regions.computeIfAbsent(key, k -> new ChunkBitmap(rx, rz));
  }

  private long bumpVersion(int chunkX, int chunkZ) {
    int rx = ChunkCoords.regionOf(chunkX);
    int rz = ChunkCoords.regionOf(chunkZ);
    long key = regionKey(rx, rz);
    return versions.merge(key, 1L, Long::sum);
  }

  public long versionOf(int chunkX, int chunkZ) {
    return versions.getOrDefault(
        regionKey(ChunkCoords.regionOf(chunkX), ChunkCoords.regionOf(chunkZ)), 0L);
  }

  /** 查询单个区块归属；0 = 未主张。 */
  public int ownerOf(int chunkX, int chunkZ) {
    ChunkBitmap region = region(chunkX, chunkZ);
    return region.ownerOf(ChunkCoords.localOf(chunkX), ChunkCoords.localOf(chunkZ));
  }

  /**
   * 占领单个区块。若属他国则转让（覆盖）；若已属该国则幂等不变。
   *
   * @return 结构化结果，调用方据此决定是否落库/下发
   */
  public ClaimOutcome claim(int chunkX, int chunkZ, int nationId) {
    if (nationId <= 0) throw new IllegalArgumentException("nationId must be positive");
    ChunkBitmap region = region(chunkX, chunkZ);
    int lx = ChunkCoords.localOf(chunkX);
    int lz = ChunkCoords.localOf(chunkZ);
    int previous = region.ownerOf(lx, lz);
    if (previous == nationId) {
      return new ClaimOutcome(ClaimResult.UNCHANGED, null);
    }
    boolean changed = region.claim(lx, lz, nationId, clock.getAsLong());
    if (!changed) {
      return new ClaimOutcome(ClaimResult.UNCHANGED, null);
    }
    long v = bumpVersion(chunkX, chunkZ);
    ClaimResult kind = previous == 0 ? ClaimResult.CLAIMED : ClaimResult.TRANSFERRED;
    return new ClaimOutcome(kind, new ChunkChange(chunkX, chunkZ, nationId, v));
  }

  /** 仅占领未被主张的区块；已主张返回 UNCHANGED，不覆盖他国。 */
  public ClaimOutcome claimIfEmpty(int chunkX, int chunkZ, int nationId) {
    if (ownerOf(chunkX, chunkZ) != 0) {
      return new ClaimOutcome(ClaimResult.UNCHANGED, null);
    }
    return claim(chunkX, chunkZ, nationId);
  }

  /** 放弃单个区块主张。 */
  public ClaimOutcome release(int chunkX, int chunkZ) {
    ChunkBitmap region = region(chunkX, chunkZ);
    int lx = ChunkCoords.localOf(chunkX);
    int lz = ChunkCoords.localOf(chunkZ);
    if (!region.release(lx, lz)) {
      return new ClaimOutcome(ClaimResult.NOTHING_TO_RELEASE, null);
    }
    long v = bumpVersion(chunkX, chunkZ);
    return new ClaimOutcome(ClaimResult.RELEASED, new ChunkChange(chunkX, chunkZ, 0, v));
  }

  /** 已加载 region 数量，运维/监控用。 */
  public int loadedRegions() {
    return regions.size();
  }

  public record ClaimOutcome(ClaimResult result, ChunkChange change) {
    public boolean changed() {
      return change != null;
    }
  }

  /** 批量占领，返回所有实际变更（按调用顺序）。 */
  public List<ChunkChange> claimAll(List<int[]> chunks, int nationId) {
    List<ChunkChange> changes = new ArrayList<>();
    for (int[] c : chunks) {
      ClaimOutcome outcome = claim(c[0], c[1], nationId);
      if (outcome.changed()) changes.add(outcome.change());
    }
    return changes;
  }
}
