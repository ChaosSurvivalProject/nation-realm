package com.chaos.nation.plugin.config;

import org.bukkit.configuration.file.FileConfiguration;

/** 配置热校验：非法值使插件安全关闭而不是带病运行。 */
public final class PluginConfig {
  public record DbSettings(
      String host,
      int port,
      String name,
      String user,
      String password,
      int poolMax,
      int asyncBatchSize,
      int asyncFlushMs) {}

  public record ServerSettings(String serverId, boolean nationServer) {}

  public record MapSettings(int refreshIntervalMs, int maxPacketBytes) {}

  public record NpcSettings(int maxPerNation, String aiModelKey, long dailyCostLimitUnits) {}

  /**
   * 电台设置（一期骨架）。
   *
   * <ul>
   *   <li>{@code clientDownloadUrl} 默认空 = <strong>待运营补齐</strong>：必须先建 Releases/资源中心下载项
   *       再把真实 URL 填进来。严禁编造、占位或猜测 URL；为空时提示骨架只发文案不发链接。
   *   <li>距离/增益/半径为游戏化参数，<strong>需实测标定</strong>，非已验证数字。
   *   <li>{@code crossServerEnabled} 默认 false，等阶段 7 多服集成演练完成才开放。
   * </ul>
   */
  public record RadioSettings(
      boolean enabled,
      String clientDownloadUrl,
      boolean crossServerEnabled,
      String defaultFactionScope,
      double designRadiusBlocks,
      double environmentLossDb,
      double lossPerOpaqueChunkDb,
      int transmitSessionSeconds,
      int promptCooldownSeconds) {}

  private final DbSettings db;
  private final ServerSettings server;
  private final MapSettings map;
  private final NpcSettings npc;
  private final RadioSettings radio;

  public PluginConfig(FileConfiguration c) {
    this.db =
        new DbSettings(
            env("NATION_DB_HOST", c.getString("database.host", "localhost")),
            c.getInt("database.port", 3306),
            c.getString("database.name", "nation_realm"),
            env("NATION_DB_USER", c.getString("database.user", "nation")),
            env("NATION_DB_PASS", c.getString("database.password", "")),
            c.getInt("database.pool-max", 16),
            c.getInt("database.async-batch-size", 512),
            c.getInt("database.async-flush-ms", 1000));
    this.server =
        new ServerSettings(
            c.getString("server.server-id", "nation-1"),
            c.getBoolean("server.nation-server", true));
    this.map =
        new MapSettings(
            c.getInt("map.refresh-interval-ms", 60000), c.getInt("map.max-packet-bytes", 1048576));
    this.npc =
        new NpcSettings(
            c.getInt("npc.max-per-nation", 80),
            c.getString("npc.ai-model-key", "deepseek-flash-4.1"),
            c.getLong("npc.daily-cost-limit-units", 100000));
    this.radio =
        new RadioSettings(
            c.getBoolean("radio.enabled", true),
            // 默认空：下载链接无落点（Releases 为空），待运营补齐；绝不编造 URL。
            c.getString("radio.client-download-url", ""),
            c.getBoolean("radio.cross-server.enabled", false), // 默认关闭，等阶段 7
            c.getString("radio.default-faction-scope", "FACTION"),
            c.getDouble("radio.design-radius-blocks", 256.0), // 需实测标定
            c.getDouble("radio.environment-loss-db", 12.0), // 需实测标定
            c.getDouble("radio.loss-per-opaque-chunk-db", 6.0), // 需实测标定
            c.getInt("radio.transmit-session-seconds", 30), // 短时票据建议 30s
            c.getInt("radio.prompt-cooldown-seconds", 600)); // 进服提示防刷屏冷却
    validate();
  }

  private static String env(String key, String fallback) {
    String v = System.getenv(key);
    return (v == null || v.isBlank()) ? fallback : v;
  }

  /** 校验失败抛 IllegalArgumentException，插件 onEnable 捕获后安全关闭。 */
  private void validate() {
    if (db.port() <= 0 || db.port() > 65535) throw new IllegalArgumentException("database.port 非法");
    if (db.poolMax() < 1) throw new IllegalArgumentException("database.pool-max 必须 >= 1");
    if (db.asyncBatchSize() < 1 || db.asyncFlushMs() < 50)
      throw new IllegalArgumentException("异步批处理参数非法");
    if (npc.maxPerNation() < 1 || npc.maxPerNation() > 80)
      throw new IllegalArgumentException("npc.max-per-nation 必须在 1..80（服务端硬上限 80）");
    if (map.refreshIntervalMs() < 1000) throw new IllegalArgumentException("地图刷新周期过短");
    // 电台一期骨架校验：跨服默认关；参数需为正；票据/冷却须为正。
    if (radio.crossServerEnabled()) {
      // 允许显式打开，但阶段 7 未演练前默认关闭；此处不强制拒绝，仅提示由运维负责。
    }
    if (radio.designRadiusBlocks() <= 0
        || radio.environmentLossDb() < 0
        || radio.lossPerOpaqueChunkDb() < 0) {
      throw new IllegalArgumentException("radio 链路参数非法");
    }
    if (radio.transmitSessionSeconds() <= 0 || radio.promptCooldownSeconds() <= 0) {
      throw new IllegalArgumentException("radio 票据/冷却秒数必须为正");
    }
    // 下载链接为空是允许的（待运营补齐），但不接受占位符/猜测串。
    if (!radio.clientDownloadUrl().isBlank()) {
      String u = radio.clientDownloadUrl().trim().toLowerCase();
      if (!u.startsWith("https://") && !u.startsWith("http://")) {
        throw new IllegalArgumentException("radio.client-download-url 必须是 http(s) URL");
      }
      if (u.contains("example.com") || u.contains("todo") || u.contains("placeholder")) {
        throw new IllegalArgumentException("radio.client-download-url 不得为占位链接");
      }
    }
  }

  public DbSettings db() {
    return db;
  }

  public ServerSettings server() {
    return server;
  }

  public MapSettings map() {
    return map;
  }

  public NpcSettings npc() {
    return npc;
  }

  public RadioSettings radio() {
    return radio;
  }
}
