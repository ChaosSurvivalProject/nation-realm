package com.chaos.nation.core.diplomacy;

import java.util.EnumMap;
import java.util.Map;

/** 外交关系状态机。状态转移受到约束，不允许随意从未知跳到战争。 条约有版本、期限、双方确认；违约记录进入审计。 */
public final class Diplomacy {
  public enum Relation {
    UNKNOWN,
    CONTACT,
    NEUTRAL,
    FRIENDLY,
    ALLY,
    TRADE,
    CEASEFIRE,
    HOSTILE,
    WAR;

    /** 合法转移表：未列出的转移被拒绝。 */
    private static final Map<Relation, EnumSetView> ALLOWED = build();

    public boolean canTransitionTo(Relation target) {
      EnumSetView set = ALLOWED.get(this);
      return set != null && set.contains(target);
    }

    private static Map<Relation, EnumSetView> build() {
      Map<Relation, EnumSetView> m = new EnumMap<>(Relation.class);
      m.put(UNKNOWN, new EnumSetView(CONTACT));
      m.put(CONTACT, new EnumSetView(NEUTRAL, TRADE, HOSTILE));
      m.put(NEUTRAL, new EnumSetView(FRIENDLY, TRADE, HOSTILE, CEASEFIRE));
      m.put(FRIENDLY, new EnumSetView(ALLY, TRADE, NEUTRAL));
      m.put(ALLY, new EnumSetView(FRIENDLY, TRADE, NEUTRAL));
      m.put(TRADE, new EnumSetView(NEUTRAL, FRIENDLY, HOSTILE));
      m.put(HOSTILE, new EnumSetView(WAR, CEASEFIRE, NEUTRAL));
      m.put(WAR, new EnumSetView(CEASEFIRE));
      m.put(CEASEFIRE, new EnumSetView(NEUTRAL, HOSTILE, WAR));
      return m;
    }

    /** 轻量不可变枚举集合，避免核心模块依赖 Guava 等。 */
    static final class EnumSetView {
      private final Relation[] values;

      EnumSetView(Relation... values) {
        this.values = values;
      }

      boolean contains(Relation r) {
        for (Relation v : values) if (v == r) return true;
        return false;
      }
    }
  }

  public record Treaty(
      long id,
      long nationA,
      long nationB,
      int version,
      String termsHash,
      long effectiveFromUtc,
      long expiresAtUtc,
      boolean confirmedA,
      boolean confirmedB,
      boolean violated) {}

  private final Map<Long, Relation> relations = new java.util.HashMap<>();

  /** key = 两国 ID 有序对编码。 */
  private static long key(long a, long b) {
    return a < b ? (a << 32) ^ b : (b << 32) ^ a;
  }

  public Relation relation(long nationA, long nationB) {
    return relations.getOrDefault(key(nationA, nationB), Relation.UNKNOWN);
  }

  /** 状态转移；非法转移抛异常，保证外交不会出现自相矛盾状态。 */
  public Relation transition(long nationA, long nationB, Relation target) {
    Relation current = relation(nationA, nationB);
    if (!current.canTransitionTo(target)) {
      throw new IllegalStateException("非法外交状态转移: " + current + " -> " + target);
    }
    relations.put(key(nationA, nationB), target);
    return target;
  }

  /** 条约需双方确认且未过期才生效。 */
  public boolean isTreatyActive(Treaty t, long nowUtc) {
    return t.confirmedA()
        && t.confirmedB()
        && nowUtc >= t.effectiveFromUtc()
        && nowUtc < t.expiresAtUtc()
        && !t.violated();
  }
}
