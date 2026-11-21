package com.chaos.nation.core.radio;

/**
 * 链路预算：纯函数，计算"收听者 ↔ 发射源(塔/手持台)"的无线电链路质量。
 *
 * <p><strong>两套距离严格分开</strong>：本类只算<strong>电台</strong>链路（收听者↔发射源），
 * 不用于近场语音（说话玩家↔周围玩家）。近场语音是二期客户端音频栈，另用距离+朝向+遮挡模型。
 *
 * <p>模型 = 标准 FSPL 自由空间损耗 + 游戏化额外损耗（不透明区块遮挡、环境损耗、确定性慢衰落），
 * 把有效通信距离压进设计半径，避免几百格内全图清晰。
 *
 * <p>所有参数集中在 {@link Config}，<strong>需实测标定</strong>——不是已验证数字，不得当真实物理值引用。
 * 输入相同则输出完全相同（含地形采样由调用方以 {@link TerrainSample} 显式传入，不在函数内读世界）。
 */
public final class LinkBudget {
  /** 地形采样：由服务端按塔→收听者路径采样后传入，保证纯函数。 */
  public record TerrainSample(int opaqueChunks, double avgHeightDelta, long seed) {}

  /** 集中可配参数；默认值均为游戏化建议，<strong>需实测标定</strong>。 */
  public record Config(
      double txPowerDbm,
      double txAntennaGainDbi,
      double rxAntennaGainDbi,
      double frequencyMhz,
      double environmentLossDb,
      double lossPerOpaqueChunkDb,
      double slowFadeAmplitudeDb,
      double designRadiusBlocks) {

    public static Config defaults() {
      return new Config(
          20.0, // 发射功率 dBm（建议值，需实测标定）
          2.0, // 发射天线增益 dBi
          0.0, // 接收天线增益 dBi
          433.0, // 频率 MHz（与 BandPlan.UHF_PUBLIC 一致）
          12.0, // 环境基础损耗 dB
          6.0, // 每个不透明区块额外损耗 dB
          3.0, // 确定性慢衰落幅度 dB
          256.0); // 设计半径（格），需实测标定
    }
  }

  private LinkBudget() {}

  /**
   * 计算单条电台链路的信号报告。
   *
   * @param listenerChunkX 收听者区块 X
   * @param listenerChunkZ 收听者区块 Z
   * @param transmitter 发射源（含区块坐标）
   * @param cfg 链路参数
   * @param terrain 路径地形采样
   */
  public static SignalReport compute(
      int listenerChunkX,
      int listenerChunkZ,
      Transmitter transmitter,
      Config cfg,
      TerrainSample terrain) {

    double dx = listenerChunkX - transmitter.chunkX();
    double dz = listenerChunkZ - transmitter.chunkZ();
    double distance = Math.sqrt(dx * dx + dz * dz); // 单位：区块

    // 标准 FSPL：32.44 + 20log10(f_MHz) + 20log10(d_km)。
    // 这里 d 以区块计，按"每区块 = 16m"折算 km（游戏化换算，需实测标定）。
    double distanceKm = Math.max(distance * 16.0 / 1000.0, 0.001);
    double fspl =
        32.44
            + 20.0 * Math.log10(Math.max(cfg.frequencyMhz(), 1.0))
            + 20.0 * Math.log10(distanceKm);

    // 游戏化额外损耗：不透明区块遮挡 + 环境 + 确定性慢衰落。
    double occlusion = terrain.opaqueChunks() * cfg.lossPerOpaqueChunkDb();
    double slowFade = deterministicSlowFade(terrain.seed(), distance, cfg.slowFadeAmplitudeDb());
    double totalLoss = fspl + occlusion + cfg.environmentLossDb() + slowFade;

    double receivedDbm =
        cfg.txPowerDbm() + cfg.txAntennaGainDbi() + cfg.rxAntennaGainDbi() - totalLoss;

    // 信纳比简化模型：以接收功率相对噪声底(-100 dBm)表示，游戏化，需实测标定。
    final double noiseFloorDbm = -100.0;
    double snrDb = receivedDbm - noiseFloorDbm;

    // 超出设计半径直接静默（游戏化硬边界，需实测标定）。
    if (distance > cfg.designRadiusBlocks()) {
      snrDb = Double.MIN_NORMAL;
    }

    SignalReport.Quality quality = SignalReport.Quality.fromSnr(snrDb);
    String terrainTag = terrain.opaqueChunks() == 0 ? "CLEAR_PATH" : "OCCLUDED";

    return new SignalReport(
        listenerChunkX,
        listenerChunkZ,
        transmitter.transmitterId(),
        receivedDbm,
        snrDb,
        distance,
        quality,
        terrainTag);
  }

  /**
   * 确定性慢衰落：用种子与距离做无状态哈希抖动，保证同输入同输出（可重现，便于属性测试）。
   */
  static double deterministicSlowFade(long seed, double distance, double amplitudeDb) {
    long v = seed ^ Double.doubleToLongBits(distance);
    v ^= v >>> 33;
    v *= 0xff51afd7ed558ccdL;
    v ^= v >>> 33;
    // 映射 [-1,1]，再乘幅度。
    double unit = ((v & 0xFFFFFFFFL) / (double) 0xFFFFFFFFL) * 2.0 - 1.0;
    return unit * amplitudeDb;
  }
}
