package com.chaos.nation.core.radio;

/** 电台收听范围档位（一期实现枚举与策略，不实现具体音频）。 */
public enum StationScope {
  /** 公共台：敌我、无国家玩家都可收听。 */
  PUBLIC,
  /** 己方台：仅本国 / 本阵营成员可听（默认档）。 */
  FACTION,
  /** 按角色 / 小队细分（二期实现，一期仅保留枚举）。 */
  ROLE_SQUAD,
  /**
   * 字面仅本人可听。
   *
   * <p><strong>歧义说明</strong>：SELF 字面含义是"仅发射者本人"，与 FACTION/ROLE_SQUAD 的群体语义不同。 本实现保留该档供配置，但不把它当成"己方台"的默认；运营若配置为 SELF， 收到的效果就是只有本人，需明确告知玩家，避免误以为同国成员能听到。
   */
  SELF
}
