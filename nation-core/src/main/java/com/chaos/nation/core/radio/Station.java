package com.chaos.nation.core.radio;

/** 电台：逻辑收听点。PUBLIC 台 nationId 可为 0；己方台绑定 nationId。 */
public record Station(
    long stationId,
    String callsign,
    Frequency frequency,
    int subToneId,
    StationScope scope,
    long ownerNationId, // 0 = 运营方/公共
    boolean operational,
    long version) {

  public Station {
    if (stationId <= 0) throw new IllegalArgumentException("stationId must be positive");
    if (callsign == null || callsign.isBlank()) {
      throw new IllegalArgumentException("callsign required");
    }
  }

  /** 是否为公共台。 */
  public boolean isPublic() {
    return scope == StationScope.PUBLIC;
  }
}
