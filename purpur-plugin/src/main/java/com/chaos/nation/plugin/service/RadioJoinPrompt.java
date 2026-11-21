package com.chaos.nation.plugin.service;

import com.chaos.nation.plugin.config.PluginConfig.RadioSettings;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * 电台客户端安装提示：进服给一次可点击提示（文本组件 click event 打开下载 URL），/radio 可复述。
 *
 * <p>严格遵守裁决 3：文案显式写明"可选安装、不影响进服"；不踢出、不强制下载、不锁入口； 带冷却防刷屏。未安装客户端时电台与语音不可用（返回
 * RADIO_CLIENT_REQUIRED），但正常玩法不受影响。
 *
 * <p>裁决 4：下载链接无落点时（clientDownloadUrl 为空=待运营补齐），只发文案不发链接， 绝不编造/占位/猜测 URL。
 */
public final class RadioJoinPrompt {
  private final RadioSettings settings;
  private final long cooldownMs;
  private final Map<String, Long> lastPromptMs = new HashMap<>();

  public RadioJoinPrompt(RadioSettings settings) {
    this.settings = settings;
    this.cooldownMs = settings.promptCooldownSeconds() * 1000L;
  }

  /** 玩家进服时调用：受冷却限制，首次必发。 */
  public void onJoin(Player player) {
    if (!settings.enabled()) return;
    sendIfAllowed(player, false);
  }

  /** 玩家执行 /radio 时调用：同样受冷却限制，防刷屏。 */
  public void onCommand(Player player) {
    if (!settings.enabled()) return;
    sendIfAllowed(player, true);
  }

  private void sendIfAllowed(Player player, boolean fromCommand) {
    String key = player.getUniqueId().toString();
    long now = System.currentTimeMillis();
    Long last = lastPromptMs.get(key);
    if (last != null && now - last < cooldownMs) {
      if (fromCommand) {
        player.sendMessage(Component.text("电台提示过于频繁，请稍后再试。", NamedTextColor.GRAY));
      }
      return;
    }
    lastPromptMs.put(key, now);
    player.sendMessage(buildMessage());
  }

  /** 构造提示文本组件（可单测的纯逻辑拆在 buildMessage）。 */
  Component buildMessage() {
    Component base =
        Component.text("[NationRealm 电台] ", NamedTextColor.GOLD)
            .append(
                Component.text(
                    "电台与近场语音为可选安装功能，需要 Fabric 客户端；" + "不安装不影响进服，国家/领土/命令/经济/地图玩法完全不受影响。",
                    NamedTextColor.YELLOW));

    String url = settings.clientDownloadUrl() == null ? "" : settings.clientDownloadUrl().trim();
    if (!url.isEmpty()) {
      base = base.append(Component.newline());
      Component link =
          Component.text("点此打开客户端下载页（含版本兼容说明与 SHA-256 校验值）", NamedTextColor.AQUA)
              .clickEvent(ClickEvent.openUrl(url));
      base = base.append(link);
    } else {
      // 下载链接待运营补齐：只发文案，明确标注，绝不编造 URL。
      base =
          base.append(Component.newline())
              .append(Component.text("下载入口待运营补齐，请关注官方公告；本提示不含任何临时链接。", NamedTextColor.GRAY));
    }
    return base;
  }

  /** 清理离线玩家冷却记录，避免长期在线服内存累积。 */
  public void forget(Player player) {
    lastPromptMs.remove(player.getUniqueId().toString());
  }
}
