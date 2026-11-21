package com.chaos.nation.core.radio;

import java.util.List;
import java.util.Optional;

/** 电台仓储接口：实现留在 purpur-plugin 的 JDBC 适配层，领域层不绑定 JDBC。 */
public final class RadioRepositories {
  private RadioRepositories() {}

  public interface StationRepository {
    Optional<Station> findById(long stationId);

    List<Station> allOperational();

    void save(Station station); // 幂等：stationId + version
  }

  public interface TransmitterRepository {
    Optional<Transmitter> findById(long transmitterId);

    List<Transmitter> byStation(long stationId);

    void save(Transmitter transmitter); // 幂等：transmitterId + revision
  }

  public interface SubscriptionRepository {
    Optional<RadioSubscription> find(String playerId, long stationId, long epoch);

    void save(RadioSubscription subscription); // 幂等：playerId+stationId+epoch
  }
}
