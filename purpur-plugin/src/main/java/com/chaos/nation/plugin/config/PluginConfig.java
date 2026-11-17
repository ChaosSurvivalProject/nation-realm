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

  private final DbSettings db;
  private final ServerSettings server;
  private final MapSettings map;
  private final NpcSettings npc;

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
}
