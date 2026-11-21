package com.chaos.nation.core.radio;

import com.chaos.nation.core.diplomacy.Diplomacy.Relation;
import java.util.Objects;

/**
 * 电台访问判定：<strong>集中在一个纯函数</strong>，命令处理器与协议处理器都调用它，避免两处规则漂移。
 *
 * <p>裁决（来自已定方案与一期假设）：
 *
 * <ul>
 *   <li>PUBLIC 恒放行（含无国家玩家、敌对玩家）；
 *   <li>FACTION 仅同 nationId；盟国(ALLY/FRIENDLY)可听不可发；中立/交战默认不可听；
 *   <li>NPC 永不拥有发射与建塔权；
 *   <li>不新建平行权限体系，权限位来自 {@code PermissionMatrix.Perm}；
 *   <li>窃听/干扰/俘获默认关闭、INTERCEPT 永久关闭——这些是运营配置门，策略只校验权限位。
 * </ul>
 */
public final class RadioAccessPolicy {
  public enum Action {
    LISTEN,
    TRANSMIT,
    TUNE,
    BUILD_TOWER,
    JAM
  }

  public enum Reason {
    ALLOW,
    NO_PERMISSION,
    NOT_MEMBER,
    HOSTILE,
    NPC_DENIED,
    SCOPE_DENIED
  }

  public record Context(
      StationScope scope,
      long ownerNationId,
      long actorNationId, // 0 = 无国家玩家
      Relation relation, // actor 国 -> owner 国；无国家时为 UNKNOWN
      boolean hasListen,
      boolean hasTransmit,
      boolean hasTune,
      boolean hasBuildTower,
      boolean hasJam,
      boolean npc,
      boolean sameRoleOrSquad // ROLE_SQUAD 档二期使用，一期传 false
      ) {
    public Context {
      Objects.requireNonNull(scope);
      Objects.requireNonNull(relation);
    }
  }

  public record Result(boolean allowed, Reason reason) {
    static Result allow() {
      return new Result(true, Reason.ALLOW);
    }

    static Result deny(Reason reason) {
      return new Result(false, reason);
    }
  }

  private RadioAccessPolicy() {}

  /** 统一判定入口。 */
  public static Result evaluate(Context c, Action action) {
    // NPC 永无发射与建塔权（干扰同样禁止，防 NPC 借 AI 越权）。
    if (c.npc()
        && (action == Action.TRANSMIT
            || action == Action.BUILD_TOWER
            || action == Action.JAM)) {
      return Result.deny(Reason.NPC_DENIED);
    }

    return switch (action) {
      case LISTEN -> evaluateListen(c);
      case TRANSMIT -> evaluateTransmit(c);
      case TUNE -> c.hasTune() ? Result.allow() : Result.deny(Reason.NO_PERMISSION);
      case BUILD_TOWER ->
          c.hasBuildTower() ? Result.allow() : Result.deny(Reason.NO_PERMISSION);
      case JAM -> c.hasJam() ? Result.allow() : Result.deny(Reason.NO_PERMISSION);
    };
  }

  private static Result evaluateListen(Context c) {
    // PUBLIC 恒放行：敌我、无国家玩家都可收听。
    if (c.scope() == StationScope.PUBLIC) return Result.allow();

    boolean sameNation = c.actorNationId() != 0 && c.actorNationId() == c.ownerNationId();
    if (sameNation) {
      if (c.scope() == StationScope.ROLE_SQUAD && !c.sameRoleOrSquad()) {
        return Result.deny(Reason.SCOPE_DENIED);
      }
      return c.hasListen() ? Result.allow() : Result.deny(Reason.NO_PERMISSION);
    }

    // 盟国/友好：可听不可发（发射在 evaluateTransmit 中按 NationScope 拒绝）。
    if (c.relation() == Relation.ALLY || c.relation() == Relation.FRIENDLY) {
      return c.hasListen() ? Result.allow() : Result.deny(Reason.NO_PERMISSION);
    }

    // 中立 / 交战 / 未知：默认不可听。
    return Result.deny(
        c.relation() == Relation.HOSTILE || c.relation() == Relation.WAR
            ? Reason.HOSTILE
            : Reason.NOT_MEMBER);
  }

  private static Result evaluateTransmit(Context c) {
    if (!c.hasTransmit()) return Result.deny(Reason.NO_PERMISSION);

    boolean sameNation = c.actorNationId() != 0 && c.actorNationId() == c.ownerNationId();

    // PUBLIC 台发射：仅运营方与授权官职（hasTransmit 已体现授权）。
    if (c.scope() == StationScope.PUBLIC) {
      return Result.allow();
    }

    // 己方台发射：必须本国成员；盟国只可听不可发。
    if (!sameNation) {
      return Result.deny(
          c.relation() == Relation.ALLY || c.relation() == Relation.FRIENDLY
              ? Reason.SCOPE_DENIED
              : Reason.NOT_MEMBER);
    }
    if (c.scope() == StationScope.ROLE_SQUAD && !c.sameRoleOrSquad()) {
      return Result.deny(Reason.SCOPE_DENIED);
    }
    return Result.allow();
  }
}
