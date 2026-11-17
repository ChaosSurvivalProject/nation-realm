package com.chaos.nation.plugin;

import org.bukkit.Server;

/** Purpur 专有能力检测：通过反射探测，存在则用，不存在则降级。 不直接 import 仅在特定构建存在的内部类，避免编译/运行时脆弱。 */
public final class ProtocolCapability {
  private boolean purpurDetected;
  private boolean tickRegionApi;

  public void detect(Server server) {
    try {
      purpurDetected = Class.forName("org.purpurmc.purpur.PurpurConfig") != null;
    } catch (ClassNotFoundException ignored) {
      purpurDetected = false;
    }
    // 专有 region/tick API 属于可选能力，缺失时走标准 Bukkit 调度。
    try {
      Class.forName("org.purpurmc.purpur.event.PlayerSetExperienceWithLevelEvent");
      tickRegionApi = true;
    } catch (ClassNotFoundException ignored) {
      tickRegionApi = false;
    }
  }

  public boolean purpurDetected() {
    return purpurDetected;
  }

  public boolean tickRegionApi() {
    return tickRegionApi;
  }

  public String summary() {
    return "purpur="
        + purpurDetected
        + ",tickRegionApi="
        + tickRegionApi
        + (purpurDetected ? "" : " (降级到标准 Bukkit API)");
  }
}
