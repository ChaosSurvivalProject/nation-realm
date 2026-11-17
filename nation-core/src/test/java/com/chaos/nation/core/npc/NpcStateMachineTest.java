package com.chaos.nation.core.npc;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaos.nation.core.npc.NpcStateMachine.NpcContext;
import com.chaos.nation.core.npc.NpcStateMachine.State;
import org.junit.jupiter.api.Test;

class NpcStateMachineTest {
  private final NpcStateMachine sm = new NpcStateMachine();

  @Test
  void aiOffline_priorityOrder_isDeterministic() {
    // 受攻击优先于饥饿
    NpcContext underAttack = new NpcContext(State.IDLE, 10, 50, 100, true, true, false);
    assertThat(sm.decide(underAttack).next()).isEqualTo(State.DEFENDING);

    NpcContext hungry = new NpcContext(State.WORKING, 10, 50, 100, false, true, false);
    assertThat(sm.decide(hungry).next()).isEqualTo(State.EATING);

    NpcContext working = new NpcContext(State.IDLE, 80, 80, 100, false, true, false);
    assertThat(sm.decide(working).next()).isEqualTo(State.WORKING);
  }

  @Test
  void aiSuggestion_cannotOverrideSurvivalRules() {
    NpcContext hungry = new NpcContext(State.IDLE, 10, 80, 100, false, true, false);
    // 模型想让饥饿 NPC 去工作 -> 拒绝
    assertThat(sm.allowAiSuggestion(hungry, State.WORKING)).isFalse();
    // 模型建议进食 -> 允许
    assertThat(sm.allowAiSuggestion(hungry, State.EATING)).isTrue();

    NpcContext attacked = new NpcContext(State.IDLE, 80, 80, 100, true, false, false);
    assertThat(sm.allowAiSuggestion(attacked, State.IDLE)).isFalse();
  }
}
