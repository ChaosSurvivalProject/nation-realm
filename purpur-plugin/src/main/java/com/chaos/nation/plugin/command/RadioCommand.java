package com.chaos.nation.plugin.command;

import com.chaos.nation.plugin.service.RadioJoinPrompt;
import com.chaos.nation.protocol.ErrorCode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /radio 命令（一期骨架）：复述客户端安装提示（带冷却）。
 *
 * <p>未安装 Fabric 客户端时，电台与语音不可用——这里只说明情况并返回 CLIENT_REQUIRED 语义的提示， 不断线、不踢出。真正的调谐/订阅/发射由协议处理器在二期实现。
 */
public final class RadioCommand implements CommandExecutor {
  private final RadioJoinPrompt prompt;
  private final boolean radioEnabled;

  public RadioCommand(RadioJoinPrompt prompt, boolean radioEnabled) {
    this.prompt = prompt;
    this.radioEnabled = radioEnabled;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("该命令只能由玩家执行。");
      return true;
    }
    if (!radioEnabled) {
      player.sendMessage("§c电台功能当前未启用。");
      return true;
    }
    // 复述安装提示（受冷却限制，防刷屏）。文案已写明"可选安装、不影响进服"。
    prompt.onCommand(player);
    return true;
  }

  /** 供网络层在无 RADIO 能力位客户端请求电台操作时复用：统一 CLIENT_REQUIRED 提示。 */
  public static String clientRequiredMessage() {
    return "§e" + ErrorCode.RADIO_CLIENT_REQUIRED.message();
  }
}
