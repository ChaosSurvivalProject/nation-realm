package com.chaos.nation.plugin.command;

import com.chaos.nation.core.government.NationNameRegistry;
import com.chaos.nation.protocol.ErrorCode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** 国家主命令：/nation create <名称> 走合规校验；其他子命令为后续阶段预留入口。 */
public final class NationCommand implements CommandExecutor {
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("该命令只能由玩家执行。");
      return true;
    }
    if (args.length < 2 || !args[0].equalsIgnoreCase("create")) {
      sender.sendMessage("用法: /nation create <国家名称>");
      return true;
    }

    String requested = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
    NationNameRegistry.Result result = NationNameRegistry.validate(requested);
    if (!result.allowed()) {
      if (result.errorCode() == ErrorCode.NAME_RESTRICTED) {
        sender.sendMessage("§c" + result.reason());
      } else {
        sender.sendMessage("§c注册失败: " + result.reason());
      }
      return true;
    }

    // 通过合规校验后的实际建国逻辑（成员/首都/账本）由后续纵向切片接入。
    sender.sendMessage("§a名称「" + requested + "」通过校验，可以注册。");
    return true;
  }
}
