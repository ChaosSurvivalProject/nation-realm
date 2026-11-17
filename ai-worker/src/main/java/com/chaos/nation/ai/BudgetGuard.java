package com.chaos.nation.ai;

import java.util.concurrent.atomic.AtomicLong;

/** 预算护栏：每国/每服务器每分钟请求数、每日成本上限、队列上限。 达到上限后停止外呼，NPC 自动回落到确定性行为树。 */
public final class BudgetGuard {
  private final int perMinuteLimit;
  private final long dailyCostLimitUnits;
  private final AtomicLong minuteWindowStart = new AtomicLong(0);
  private final AtomicLong minuteCount = new AtomicLong(0);
  private final AtomicLong dailyCostUnits = new AtomicLong(0);
  private final AtomicLong dayStart = new AtomicLong(0);

  public BudgetGuard(int perMinuteLimit, long dailyCostLimitUnits) {
    this.perMinuteLimit = perMinuteLimit;
    this.dailyCostLimitUnits = dailyCostLimitUnits;
  }

  private void rollWindows(long nowSec) {
    long minute = nowSec / 60;
    if (minuteWindowStart.get() != minute) {
      minuteWindowStart.set(minute);
      minuteCount.set(0);
    }
    long day = nowSec / 86400;
    if (dayStart.get() != day) {
      dayStart.set(day);
      dailyCostUnits.set(0);
    }
  }

  /** 是否允许发起一次外呼。 */
  public synchronized boolean allow(long nowEpochSec, long estimatedCostUnits) {
    rollWindows(nowEpochSec);
    if (minuteCount.get() >= perMinuteLimit) return false;
    if (dailyCostUnits.get() + estimatedCostUnits > dailyCostLimitUnits) return false;
    return true;
  }

  /** 记录一次实际调用，累计请求数与成本。 */
  public synchronized void record(long nowEpochSec, long actualCostUnits) {
    rollWindows(nowEpochSec);
    minuteCount.incrementAndGet();
    dailyCostUnits.addAndGet(actualCostUnits);
  }

  public long dailyCostUnits() {
    return dailyCostUnits.get();
  }
}
