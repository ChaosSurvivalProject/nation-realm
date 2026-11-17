package com.chaos.nation.core.npc;

import java.util.Objects;

/** NPC 确定性行为状态机：AI 不可用（超时/熔断）时按此树继续工作/吃饭/休息/防守/返回， 不会集体原地静止。所有转移由服务端验证，不依赖模型返回。 */
public final class NpcStateMachine {
  public enum State {
    IDLE,
    WORKING,
    EATING,
    RESTING,
    DEFENDING,
    RETURNING,
    DEAD
  }

  public record NpcContext(
      State state,
      int hunger,
      int stamina,
      int health,
      boolean underAttack,
      boolean hasWork,
      boolean farFromHome) {}

  public record Decision(State next, String reason) {}

  /** AI 关闭时的确定性决策：优先级 死亡 > 受攻击防守 > 饥饿进食 > 体力耗尽休息 > 远离返回 > 有工作工作 > 空闲。 */
  public Decision decide(NpcContext ctx) {
    Objects.requireNonNull(ctx);
    if (ctx.health() <= 0) return new Decision(State.DEAD, "生命耗尽");
    if (ctx.underAttack()) return new Decision(State.DEFENDING, "受到攻击，进入防守");
    if (ctx.hunger() < 20) return new Decision(State.EATING, "饥饿，进食");
    if (ctx.stamina() < 15) return new Decision(State.RESTING, "体力不足，休息");
    if (ctx.farFromHome()) return new Decision(State.RETURNING, "远离驻地，返回");
    if (ctx.hasWork()) return new Decision(State.WORKING, "有工作任务");
    return new Decision(State.IDLE, "无事，空闲");
  }

  /** 判断是否允许由 AI 意图切换到某状态：模型只能建议，不能越过生存约束。 */
  public boolean allowAiSuggestion(NpcContext ctx, State suggested) {
    // 死亡不可逆；受攻击时强制防守；饥饿/体力危险时不允许模型覆盖为工作
    if (ctx.health() <= 0) return false;
    if (ctx.underAttack() && suggested != State.DEFENDING) return false;
    if (ctx.hunger() < 20 && (suggested == State.WORKING || suggested == State.IDLE)) return false;
    if (ctx.stamina() < 15 && suggested == State.WORKING) return false;
    return true;
  }
}
