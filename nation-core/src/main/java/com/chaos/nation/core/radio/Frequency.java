package com.chaos.nation.core.radio;

import java.util.Objects;

/**
 * 无线频率：内部以<strong>整数 kHz</strong>存储（避免浮点误差），展示时换算为 MHz。
 *
 * <p>这是游戏内拟真：频段编号模仿真实无线电划分，但仅用于游戏，UI 必须标注
 * "虚构频段，仅供游戏"。不得把它当作真实无线电发射参数。
 */
public final class Frequency {
  private final int khz;

  private Frequency(int khz) {
    this.khz = khz;
  }

  public static Frequency ofKhz(int khz) {
    if (khz <= 0) throw new IllegalArgumentException("frequency kHz must be positive");
    return new Frequency(khz);
  }

  /** 由 MHz 构造，四舍五入到最近 kHz（如 145.5 -> 145500 kHz）。 */
  public static Frequency ofMhz(double mhz) {
    if (mhz <= 0 || Double.isNaN(mhz) || Double.isInfinite(mhz)) {
      throw new IllegalArgumentException("invalid MHz");
    }
    long khz = Math.round(mhz * 1000.0D);
    if (khz > Integer.MAX_VALUE) throw new IllegalArgumentException("MHz out of range");
    return new Frequency((int) khz);
  }

  public int khz() {
    return khz;
  }

  /** 显示为 MHz，固定三位小数（如 "145.500"）。 */
  public String displayMhz() {
    return String.format("%d.%03d", khz / 1000, khz % 1000);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Frequency && ((Frequency) o).khz == khz;
  }

  @Override
  public int hashCode() {
    return Objects.hash(khz);
  }

  @Override
  public String toString() {
    return displayMhz() + " MHz";
  }
}
