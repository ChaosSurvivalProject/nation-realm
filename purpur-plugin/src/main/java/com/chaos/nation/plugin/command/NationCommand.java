package com.chaos.nation.plugin.command;

import com.chaos.nation.core.government.NationNameRegistry;
import com.chaos.nation.core.territory.ChunkCoords;
import com.chaos.nation.plugin.service.ClaimService;
import com.chaos.nation.protocol.ErrorCode;
import com.chaos.nation.protocol.packets.DataPackets.ChunkClaim;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 国家主命令。
 *
 * <ul>
 *   <li>{@code /nation create <名称>} —— 走名称合规校验。
 *   <li>{@code /nation claim} —— 占领玩家<strong>所在区块</strong>（最小单位=单区块）。
 *   <li>{@code /nation release} —— 放弃玩家所在区块。
 *   <li>{@code /nation info} —— 查询玩家所在区块归属与版本。
 * </ul>
 *
 * <p>占领请求需要玩家给出国家 ID（阶段切片：{@code /nation claim <nationId>}）， 真实成员校验由服务端权威完成，客户端不能越权指定他国。
 */
public final class NationCommand implements CommandExecutor {
  private final ClaimService claims;

  public NationCommand(ClaimService claims) {
    this.claims = claims;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("该命令只能由玩家执行。");
      return true;
    }
    if (args.length == 0) {
      sendUsage(sender);
      return true;
    }

    return switch (args[0].toLowerCase()) {
      case "create" -> handleCreate(sender, args);
      case "claim" -> handleClaim(player, args);
      case "release" -> handleRelease(player);
      case "info" -> handleInfo(player);
      default -> {
        sendUsage(sender);
        yield true;
      }
    };
  }

  private boolean handleCreate(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage("用法: /nation create <国家名称>");
      return true;
    }
    String requested = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
    NationNameRegistry.Result result = NationNameRegistry.validate(requested);
    if (!result.allowed()) {
      sender.sendMessage("§c" + result.reason());
      return true;
    }
    sender.sendMessage("§a名称「" + requested + "」通过校验，可以注册。");
    return true;
  }

  private boolean handleClaim(Player player, String[] args) {
    if (!player.hasPermission("nationrealm.use")) {
      player.sendMessage("§c" + ErrorCode.NO_PERMISSION.message());
      return true;
    }
    if (args.length < 2) {
      player.sendMessage("用法: /nation claim <nationId>（占领你脚下的区块）");
      return true;
    }
    int nationId;
    try {
      nationId = Integer.parseInt(args[1]);
    } catch (NumberFormatException e) {
      player.sendMessage("§c" + ErrorCode.INVALID_ARGUMENT.message() + "：nationId 必须是整数");
      return true;
    }

    int chunkX = ChunkCoords.chunkOfBlock(player.getLocation().getBlockX());
    int chunkZ = ChunkCoords.chunkOfBlock(player.getLocation().getBlockZ());
    ChunkClaim packet = claims.claim(chunkX, chunkZ, nationId, player.getUniqueId().toString(), 0L);
    if (packet == null) {
      player.sendMessage("§e该区块已归属于国家 " + claims.territory().ownerOf(chunkX, chunkZ));
      return true;
    }
    player.sendMessage(
        "§a已占领区块 ("
            + chunkX
            + ", "
            + chunkZ
            + ") 给国家 "
            + nationId
            + "，region 版本 "
            + packet.regionVersion());
    return true;
  }

  private boolean handleRelease(Player player) {
    int chunkX = ChunkCoords.chunkOfBlock(player.getLocation().getBlockX());
    int chunkZ = ChunkCoords.chunkOfBlock(player.getLocation().getBlockZ());
    ChunkClaim packet = claims.release(chunkX, chunkZ, player.getUniqueId().toString(), 0L);
    if (packet == null) {
      player.sendMessage("§e该区块本就无主张。");
      return true;
    }
    player.sendMessage("§a已放弃区块 (" + chunkX + ", " + chunkZ + ") 的主张。");
    return true;
  }

  private boolean handleInfo(Player player) {
    int chunkX = ChunkCoords.chunkOfBlock(player.getLocation().getBlockX());
    int chunkZ = ChunkCoords.chunkOfBlock(player.getLocation().getBlockZ());
    int owner = claims.territory().ownerOf(chunkX, chunkZ);
    long version = claims.territory().versionOf(chunkX, chunkZ);
    String ownerText = owner == 0 ? "无主" : ("国家 " + owner);
    player.sendMessage(
        "§7当前区块 (" + chunkX + ", " + chunkZ + ")：归属 " + ownerText + "，region 版本 " + version);
    return true;
  }

  private static void sendUsage(CommandSender sender) {
    sender.sendMessage("用法: /nation create|claim|release|info");
  }
}
