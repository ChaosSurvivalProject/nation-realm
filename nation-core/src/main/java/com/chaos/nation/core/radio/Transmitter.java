package com.chaos.nation.core.radio;

/**
 * 发射源：绑定 (worldId, chunkX, chunkZ)，覆盖区由塔位置 + 功率 + 频段 + 高度图实时算出。
 *
 * <p>功率、高度等为游戏化参数，<strong>需实测标定</strong>。塔所在区块易主即 CAPTURED 并按占领方 外交关系重算收听权（三期接入占领事件，一期仅建模字段）。
 */
public record Transmitter(
    long transmitterId,
    long stationId,
    String worldId,
    int chunkX,
    int chunkZ,
    double heightY,
    int powerWatts, // 游戏化功率，需实测标定
    Frequency frequency,
    long ownerNationId,
    long revision, // 幂等：transmitterId + revision
    Status status) {

  public enum Status {
    ONLINE,
    OFFLINE,
    CAPTURED,
    JAMMED
  }

  public Transmitter {
    if (transmitterId <= 0) throw new IllegalArgumentException("transmitterId must be positive");
    if (powerWatts < 0) throw new IllegalArgumentException("power must be >= 0");
  }
}
