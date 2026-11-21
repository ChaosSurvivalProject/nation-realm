package com.chaos.nation.core.radio;

/**
 * 发射会话：短时票据绑定的会话领域表示。
 *
 * <p>票据由 voice-api 签发（playerId + stationId + sessionId + 源 IP + HMAC），
 * 服务端验签后建立会话；一期只建模结构与过期判定，不实现音频转发（二期 voice-server）。
 */
public record TransmissionSession(
    String sessionId,
    String playerId,
    long stationId,
    int chunkX,
    int chunkZ,
    long startedAtUtc,
    long expiresAtUtc) {

  public boolean expired(long nowUtc) {
    return nowUtc >= expiresAtUtc;
  }
}
