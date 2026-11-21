package com.chaos.nation.core.radio;

import static com.chaos.nation.core.diplomacy.Diplomacy.Relation;
import static org.assertj.core.api.Assertions.assertThat;

import com.chaos.nation.core.radio.RadioAccessPolicy.Action;
import com.chaos.nation.core.radio.RadioAccessPolicy.Context;
import com.chaos.nation.core.radio.RadioAccessPolicy.Reason;
import org.junit.jupiter.api.Test;

class RadioAccessPolicyTest {
  private static Context ctx(
      StationScope scope,
      long owner,
      long actor,
      Relation relation,
      boolean listen,
      boolean transmit,
      boolean npc) {
    return new Context(
        scope, owner, actor, relation, listen, transmit, true, true, true, npc, false);
  }

  @Test
  void publicListen_alwaysAllowed_evenForStatelessAndEnemy() {
    Context stateless =
        ctx(StationScope.PUBLIC, 0, 0, Relation.UNKNOWN, false, false, false);
    assertThat(RadioAccessPolicy.evaluate(stateless, Action.LISTEN).allowed()).isTrue();
    Context enemy = ctx(StationScope.PUBLIC, 7, 8, Relation.WAR, false, false, false);
    assertThat(RadioAccessPolicy.evaluate(enemy, Action.LISTEN).allowed()).isTrue();
  }

  @Test
  void factionListen_sameNationAllowed_strangersDenied() {
    Context same =
        ctx(StationScope.FACTION, 7, 7, Relation.UNKNOWN, true, true, false);
    assertThat(RadioAccessPolicy.evaluate(same, Action.LISTEN).allowed()).isTrue();

    Context hostile =
        ctx(StationScope.FACTION, 7, 8, Relation.HOSTILE, true, true, false);
    assertThat(RadioAccessPolicy.evaluate(hostile, Action.LISTEN).reason())
        .isEqualTo(Reason.HOSTILE);

    Context neutral =
        ctx(StationScope.FACTION, 7, 8, Relation.NEUTRAL, true, true, false);
    assertThat(RadioAccessPolicy.evaluate(neutral, Action.LISTEN).allowed()).isFalse();
  }

  @Test
  void ally_canListenButCannotTransmit() {
    Context ally = ctx(StationScope.FACTION, 7, 8, Relation.ALLY, true, true, false);
    assertThat(RadioAccessPolicy.evaluate(ally, Action.LISTEN).allowed()).isTrue();
    assertThat(RadioAccessPolicy.evaluate(ally, Action.TRANSMIT).allowed()).isFalse();
    assertThat(RadioAccessPolicy.evaluate(ally, Action.TRANSMIT).reason())
        .isEqualTo(Reason.SCOPE_DENIED);
  }

  @Test
  void sameNation_canTransmit_onFactionStation() {
    Context same =
        ctx(StationScope.FACTION, 7, 7, Relation.UNKNOWN, true, true, false);
    assertThat(RadioAccessPolicy.evaluate(same, Action.TRANSMIT).allowed()).isTrue();
  }

  @Test
  void npc_neverTransmitsOrBuildsTower() {
    Context npc = ctx(StationScope.FACTION, 7, 7, Relation.UNKNOWN, true, true, true);
    assertThat(RadioAccessPolicy.evaluate(npc, Action.TRANSMIT).reason())
        .isEqualTo(Reason.NPC_DENIED);
    assertThat(RadioAccessPolicy.evaluate(npc, Action.BUILD_TOWER).reason())
        .isEqualTo(Reason.NPC_DENIED);
    // NPC 听公共台不受限（恒放行）
    assertThat(RadioAccessPolicy.evaluate(npc, Action.LISTEN).reason())
        .isEqualTo(Reason.ALLOW);
  }

  @Test
  void permissionBits_areEnforced() {
    Context noPerm = ctx(StationScope.PUBLIC, 7, 7, Relation.UNKNOWN, false, false, false);
    assertThat(RadioAccessPolicy.evaluate(noPerm, Action.TRANSMIT).reason())
        .isEqualTo(Reason.NO_PERMISSION);

    // TUNE / BUILD_TOWER 权限位单独构造（ctx 辅助方法默认这些位为 true）。
    Context noPrivs =
        new Context(
            StationScope.PUBLIC, 7, 7, Relation.UNKNOWN, false, false, false, false, false, false, false);
    assertThat(RadioAccessPolicy.evaluate(noPrivs, Action.TUNE).reason())
        .isEqualTo(Reason.NO_PERMISSION);
    assertThat(RadioAccessPolicy.evaluate(noPrivs, Action.BUILD_TOWER).reason())
        .isEqualTo(Reason.NO_PERMISSION);
  }

  @Test
  void roleSquad_requiresSameRoleSubtype() {
    Context sameRole =
        new Context(
            StationScope.ROLE_SQUAD, 7, 7, Relation.UNKNOWN, true, true, true, true, true, false, true);
    assertThat(RadioAccessPolicy.evaluate(sameRole, Action.LISTEN).allowed()).isTrue();
    Context diffRole =
        new Context(
            StationScope.ROLE_SQUAD, 7, 7, Relation.UNKNOWN, true, true, true, true, true, false, false);
    assertThat(RadioAccessPolicy.evaluate(diffRole, Action.LISTEN).reason())
        .isEqualTo(Reason.SCOPE_DENIED);
  }
}
