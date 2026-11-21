package com.chaos.nation.core.radio;

import java.util.List;

/**
 * 频段规划（band plan）：定义可用频段范围、频道间隔与亚音（CTCSS 式逻辑隔离）。
 *
 * <p>频段编号模仿真实无线电划分，但<strong>纯属游戏内拟真</strong>，UI 必须标注
 * "虚构频段，仅供游戏"。频道间隔、半径等参数集中于此，<strong>需实测标定</strong>，
 * 不作为已验证数字。一期 UI 只给预设频道与简化半径条（假设 D）。
 */
public final class BandPlan {
  /** 亚音使用整数存储（单位 0.1 Hz，如 1000 = 100.0 Hz），避免浮点比较。 */
  public record SubTone(int id, String label, int deciHz) {}

  /** 一个频段的规划参数。 */
  public record Band(
      String id,
      String label,
      int minKhz,
      int maxKhz,
      int channelSpacingKhz,
      List<SubTone> allowedSubTones) {

    public boolean contains(Frequency f) {
      return f.khz() >= minKhz && f.khz() <= maxKhz;
    }

    /** 频率是否落在该频段的频道栅格上（相对 minKhz 对齐）。 */
    public boolean onRaster(Frequency f) {
      int offset = f.khz() - minKhz;
      return offset >= 0 && offset % channelSpacingKhz == 0;
    }
  }

  /** 常用亚音表（游戏拟真，值为建议示例，需实测标定其隔离效果）。 */
  public static final List<SubTone> STANDARD_SUB_TONES =
      List.of(
          new SubTone(1, "67.0", 670),
          new SubTone(2, "100.0", 1000),
          new SubTone(3, "123.0", 1230),
          new SubTone(4, "156.7", 1567),
          new SubTone(5, "192.8", 1928));

  /**
   * 默认游戏频段：编号贴近真实业余/公共频段，但仅为拟真。 一期只暴露这三个预设频段（假设 D）。
   */
  public static final Band VHF_AIR =
      new Band("VHF_AIR", "VHF 航空段(虚构)", 118_000, 136_000, 25, STANDARD_SUB_TONES);

  public static final Band UHF_PUBLIC =
      new Band("UHF_PUBLIC", "UHF 公共段(虚构)", 400_000, 470_000, 25, STANDARD_SUB_TONES);

  public static final Band SHORTWAVE =
      new Band("SHORTWAVE", "短波段(虚构)", 3_500, 30_000, 10, STANDARD_SUB_TONES);

  public static final List<Band> DEFAULT_BANDS = List.of(VHF_AIR, UHF_PUBLIC, SHORTWAVE);

  private BandPlan() {}

  /** 找到频率所属频段；不属于任何规划频段返回 null。 */
  public static Band bandOf(Frequency f) {
    for (Band b : DEFAULT_BANDS) {
      if (b.contains(f)) return b;
    }
    return null;
  }

  /** 亚音 ID 是否在给定频段的允许集合内；id<=0 表示不使用亚音，视为合法。 */
  public static boolean subToneAllowed(Band band, int subToneId) {
    if (subToneId <= 0) return true; // 0 = 不启用亚音
    for (SubTone s : band.allowedSubTones()) {
      if (s.id() == subToneId) return true;
    }
    return false;
  }
}
