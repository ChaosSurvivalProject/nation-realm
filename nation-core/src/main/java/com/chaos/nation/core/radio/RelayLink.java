package com.chaos.nation.core.radio;

/**
 * 中继链路（三期骨架，仅建模，不实现）。
 *
 * <p>跨服中继要求 radio.crossServer.enabled=true（默认关闭，等阶段 7 多服集成演练完成）。 同一频段在 proxy 全局只允许一个 owner server
 * 发射，其余服为接收端。
 */
public record RelayLink(
    long fromTransmitterId,
    long toTransmitterId,
    String ownerServerId, // proxy 全局该频段的唯一发射服
    int linkQualityPermille,
    boolean active) {}
