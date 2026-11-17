package com.chaos.nation.core.domain;

import java.util.Objects;

/** 金额：定点整数最小货币单位（long），绝不使用浮点数。 1 单位 = 1 分（最小货币单位），展示层再除以 100。 */
public final class Money {
  public static final Money ZERO = new Money(0);
  private final long units;

  private Money(long units) {
    this.units = units;
  }

  public static Money of(long units) {
    return new Money(units);
  }

  /** 由“元 + 分”构造，避免小数误差。 */
  public static Money ofYuan(long yuan, int cents) {
    if (cents < 0 || cents >= 100) throw new IllegalArgumentException("cents out of range");
    return new Money(Math.multiplyExact(yuan, 100) + cents);
  }

  public long units() {
    return units;
  }

  public Money plus(Money other) {
    return new Money(Math.addExact(units, other.units));
  }

  public Money minus(Money other) {
    return new Money(Math.subtractExact(units, other.units));
  }

  public boolean isNegative() {
    return units < 0;
  }

  public boolean isZero() {
    return units == 0;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Money && ((Money) o).units == units;
  }

  @Override
  public int hashCode() {
    return Objects.hash(units);
  }

  @Override
  public String toString() {
    return String.format("%d.%02d", units / 100, Math.abs(units % 100));
  }
}
