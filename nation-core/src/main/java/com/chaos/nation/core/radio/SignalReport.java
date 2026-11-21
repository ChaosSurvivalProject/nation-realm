package com.chaos.nation.core.radio;

/** 信号报告：LinkBudget 的可序列化输出，供协议下发与地图热力图使用。 */
public record SignalReport(
    int listenerChunkX,
    int listenerChunkZ,
    long transmitterId,
    double receivedDbm,
    double snrDb,
    double distanceBlocks,
    Quality quality,
    String terrainTag) {

  /** 四档信号质量：距离越远信号越差，依次退化。 */
  public enum Quality {
    CLEAR, // 清晰
    HISSY, // 有嘶声
    BROKEN, // 断裂断续
    MUTED; // 完全静默

    /** 由信纳比阈值映射；阈值为游戏化建议值，需实测标定。 */
    public static Quality fromSnr(double snrDb) {
      if (snrDb >= 20.0) return CLEAR;
      if (snrDb >= 10.0) return HISSY;
      if (snrDb >= 3.0) return BROKEN;
      return MUTED;
    }
  }

  public SignalReport {
    if (Double.isNaN(receivedDbm) || Double.isNaN(snrDb)) {
      throw new IllegalArgumentException("signal values must be finite");
    }
  }

  public boolean audible() {
    return quality != Quality.MUTED;
  }
}
