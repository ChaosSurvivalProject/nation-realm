package com.chaos.nation.plugin;

import com.chaos.nation.core.territory.TerritoryClaimService;
import com.chaos.nation.plugin.command.NationCommand;
import com.chaos.nation.plugin.config.PluginConfig;
import com.chaos.nation.plugin.service.ClaimService;
import com.chaos.nation.plugin.service.ExplorationService;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** 插件生命周期：配置校验、异步任务、网络协议注册、安全关闭。 */
public final class NationPlugin extends JavaPlugin {
  private PluginConfig config;
  private ExplorationService exploration;
  private ClaimService claims;
  private BukkitTask flushTask;
  private final ProtocolCapability capability = new ProtocolCapability();

  @Override
  public void onEnable() {
    try {
      config = new PluginConfig(getConfig());
    } catch (IllegalArgumentException e) {
      getLogger().log(Level.SEVERE, "配置校验失败，插件安全关闭: " + e.getMessage());
      getServer().getPluginManager().disablePlugin(this);
      return;
    }
    saveConfig();

    // 单区块占领服务：内存领域操作同步完成，落库/广播异步批处理。
    claims =
        new ClaimService(
            new TerritoryClaimService(),
            config.db().asyncBatchSize(),
            batch -> {
              // 真实实现由 MySQL 仓储 + Netty 广播适配；这里仅记录日志，避免主线程阻塞。
              getLogger().fine("异步落库区块占领批次: " + batch.size());
            });

    // 注册国家主命令（含名称合规校验与单区块占领/查询）。
    var nationCommand = getCommand("nation");
    if (nationCommand != null) {
      nationCommand.setExecutor(new NationCommand(claims));
    }

    exploration =
        new ExplorationService(
            config.db().asyncBatchSize(),
            batch -> {
              // 真实实现由 MySQL 仓储适配；这里仅记录日志，避免主线程阻塞。
              getLogger().fine("异步落库区块探索批次: " + batch.size());
            });

    // 异步批量落库；数据库失败由适配层退避重试，不循环刷日志。
    flushTask =
        getServer()
            .getScheduler()
            .runTaskTimerAsynchronously(
                this,
                () -> {
                  try {
                    exploration.flush();
                    claims.flush();
                  } catch (Exception e) {
                    getLogger().log(Level.WARNING, "异步落库失败，退避重试: " + e.getMessage());
                  }
                },
                20L,
                config.db().asyncFlushMs() / 50L);

    // Purpur 专有能力检测并降级。
    capability.detect(getServer());
    getLogger()
        .info(
            "[NationRealm] 启用完成 serverId="
                + config.server().serverId()
                + " 协议能力="
                + capability.summary());
  }

  @Override
  public void onDisable() {
    if (flushTask != null) flushTask.cancel();
    if (exploration != null) exploration.flush();
    if (claims != null) claims.flush();
    getLogger().info("[NationRealm] 已安全关闭，任务与连接已释放");
  }

  public PluginConfig config() {
    return config;
  }

  public ExplorationService exploration() {
    return exploration;
  }

  public ClaimService claims() {
    return claims;
  }
}
