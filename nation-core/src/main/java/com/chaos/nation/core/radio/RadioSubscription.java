package com.chaos.nation.core.radio;

/** 订阅：以 playerId + stationId + epoch 作为幂等键（对齐 Outbox/幂等表约定）。 */
public record RadioSubscription(
    String playerId, long stationId, long epoch, long subscribedAtUtc, boolean active) {

  public String idempotencyKey() {
    return playerId + ":" + stationId + ":" + epoch;
  }
}
