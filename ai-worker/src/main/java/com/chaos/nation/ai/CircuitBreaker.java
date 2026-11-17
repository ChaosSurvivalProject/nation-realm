package com.chaos.nation.ai;

/** 熔断器：连续失败达到阈值后打开，冷却后半开；避免对失联 AI 反复重试刷日志。 */
public final class CircuitBreaker {
  private final int failureThreshold;
  private final long cooldownMs;
  private int consecutiveFailures;
  private long openedAtMs;
  private boolean halfOpen;

  public CircuitBreaker(int failureThreshold, long cooldownMs) {
    this.failureThreshold = failureThreshold;
    this.cooldownMs = cooldownMs;
  }

  public synchronized boolean isOpen() {
    if (!halfOpen && consecutiveFailures >= failureThreshold) {
      long now = System.currentTimeMillis();
      if (now - openedAtMs > cooldownMs) {
        halfOpen = true; // 半开：放一次探测请求
        return false;
      }
      return true;
    }
    return false;
  }

  public synchronized void recordSuccess() {
    consecutiveFailures = 0;
    halfOpen = false;
  }

  public synchronized void recordFailure() {
    consecutiveFailures++;
    if (consecutiveFailures >= failureThreshold) {
      openedAtMs = System.currentTimeMillis();
      halfOpen = false;
    }
  }
}
