package com.chaos.nation.core.gdp;

import com.chaos.nation.core.domain.Money;

/** GDP 是周期生产价值指标（只读汇总），不是国库余额。 禁止把 GDP 直接扣减——此处只提供计算，不提供任何“扣款”操作。 */
public final class GdpCalculator {
  public record Production(long nationId, String sector, Money value, long periodIndex) {}

  /** 计算某国在某周期的生产总值。 */
  public Money quarterlyGdp(long nationId, long periodIndex, Iterable<Production> records) {
    long total = 0;
    for (Production p : records) {
      if (p.nationId() == nationId && p.periodIndex() == periodIndex) {
        total += p.value().units();
      }
    }
    return Money.of(total);
  }
}
