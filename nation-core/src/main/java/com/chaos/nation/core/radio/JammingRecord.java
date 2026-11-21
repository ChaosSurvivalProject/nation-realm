package com.chaos.nation.core.radio;

/**
 * 干扰记录（三期骨架，仅建模，不实现）。
 *
 * <p>干扰 / 俘获 / 窃听开关默认关闭；INTERCEPT 永久关闭。 一期不实现干扰行为，仅保留记录结构与幂等键。
 */
public record JammingRecord(
    long jammerTransmitterId,
    long victimTransmitterId,
    long startedAtUtc,
    long endsAtUtc,
    String idempotencyKey) {}
