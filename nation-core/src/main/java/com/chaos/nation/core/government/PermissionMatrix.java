package com.chaos.nation.core.government;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** 官职权限矩阵：官职只能授予真人玩家；NPC 只能担任执行岗（无政治权力）。 授权带期限，过期自动失效。 */
public final class PermissionMatrix {
  public enum Perm {
    MANAGE_MEMBERS,
    GRANT_OFFICE,
    DECLARE_WAR,
    SIGN_TREATY,
    SPEND_TREASURY,
    PROPOSE_LAW,
    VOTE_LAW,
    COMMAND_ARMY,
    BUILD,
    TAX,
    PARDON
  }

  /** 政治权力职位——仅真人玩家可持有。 */
  public enum PoliticalOffice {
    PRESIDENT(EnumSet.allOf(Perm.class)),
    FINANCE_MINISTER(EnumSet.of(Perm.SPEND_TREASURY, Perm.TAX)),
    FOREIGN_MINISTER(EnumSet.of(Perm.DECLARE_WAR, Perm.SIGN_TREATY)),
    DEFENSE_MINISTER(EnumSet.of(Perm.COMMAND_ARMY, Perm.BUILD)),
    LEGISLATOR(EnumSet.of(Perm.PROPOSE_LAW, Perm.VOTE_LAW)),
    JUDGE(EnumSet.of(Perm.PARDON));

    private final Set<Perm> perms;

    PoliticalOffice(Set<Perm> perms) {
      this.perms = perms;
    }

    public Set<Perm> permissions() {
      return EnumSet.copyOf(perms);
    }
  }

  /** 执行岗——NPC 可担任，只有行动权无政治权。 */
  public enum ExecutiveRole {
    WORKER(EnumSet.of(Perm.BUILD)),
    SOLDIER(EnumSet.of(Perm.COMMAND_ARMY)),
    CLERK(EnumSet.of(Perm.BUILD));

    private final Set<Perm> perms;

    ExecutiveRole(Set<Perm> perms) {
      this.perms = perms;
    }

    public Set<Perm> permissions() {
      return EnumSet.copyOf(perms);
    }
  }

  private final Map<String, Grant> grants = new HashMap<>();

  public record Grant(String playerUuid, Perm perm, long expiresAtUtc, boolean npc) {}

  /** 授予权限；NPC 永远不能被授予政治权力 Perm，只能授予执行岗的有限行动权。 */
  public void grant(String playerUuid, Perm perm, long expiresAtUtc, boolean npc) {
    if (npc && isPoliticalPower(perm)) {
      throw new IllegalArgumentException("NPC 不能拥有政治权力: " + perm);
    }
    grants.put(playerUuid + ":" + perm, new Grant(playerUuid, perm, expiresAtUtc, npc));
  }

  private boolean isPoliticalPower(Perm perm) {
    // 政治权力 = 除纯执行动作外的权限
    return switch (perm) {
      case BUILD -> false;
      default -> true;
    };
  }

  /** 检查权限：未授予、已过期、被撤销都拒绝。 */
  public boolean has(String playerUuid, Perm perm, long nowUtc) {
    Grant g = grants.get(playerUuid + ":" + perm);
    return g != null && nowUtc < g.expiresAtUtc();
  }

  public void revoke(String playerUuid, Perm perm) {
    grants.remove(playerUuid + ":" + perm);
  }
}
