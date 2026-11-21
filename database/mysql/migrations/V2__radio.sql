-- 电台系统迁移（一期）：控制面元数据。音频帧永不入库，媒体面为独立 UDP。
-- DDL 可重复执行（IF NOT EXISTS）；幂等键与 Outbox 表对齐。
-- 迁移版本：2。执行顺序在 V1__baseline.sql 之后。

CREATE TABLE IF NOT EXISTS radio_stations (
  station_id BIGINT NOT NULL PRIMARY KEY,
  callsign VARCHAR(24) NOT NULL,
  frequency_khz INT NOT NULL,
  sub_tone_id INT NOT NULL DEFAULT 0,
  scope VARCHAR(16) NOT NULL,
  owner_nation_id BIGINT NOT NULL DEFAULT 0,
  operational BOOLEAN NOT NULL DEFAULT TRUE,
  version BIGINT NOT NULL DEFAULT 0,
  KEY idx_stations_freq (frequency_khz),
  KEY idx_stations_owner (owner_nation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS radio_transmitters (
  transmitter_id BIGINT NOT NULL PRIMARY KEY,
  station_id BIGINT NOT NULL,
  world_id VARCHAR(64) NOT NULL,
  chunk_x INT NOT NULL,
  chunk_z INT NOT NULL,
  height_y DOUBLE NOT NULL DEFAULT 0,
  power_watts INT NOT NULL DEFAULT 0,
  frequency_khz INT NOT NULL,
  owner_nation_id BIGINT NOT NULL DEFAULT 0,
  revision BIGINT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'ONLINE',
  UNIQUE KEY uq_tx_revision (transmitter_id, revision),
  KEY idx_tx_station (station_id),
  KEY idx_tx_chunk (world_id, chunk_x, chunk_z),
  CONSTRAINT fk_tx_station FOREIGN KEY (station_id) REFERENCES radio_stations(station_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS radio_subscriptions (
  player_id VARCHAR(64) NOT NULL,
  station_id BIGINT NOT NULL,
  epoch BIGINT NOT NULL,
  subscribed_utc BIGINT NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (player_id, station_id, epoch),
  KEY idx_sub_station (station_id),
  CONSTRAINT fk_sub_station FOREIGN KEY (station_id) REFERENCES radio_stations(station_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 发射会话：短时票据绑定；票据 HMAC 校验在 voice-api，DB 只存会话事实。
CREATE TABLE IF NOT EXISTS radio_transmissions (
  session_id VARCHAR(64) NOT NULL PRIMARY KEY,
  player_id VARCHAR(64) NOT NULL,
  station_id BIGINT NOT NULL,
  chunk_x INT NOT NULL,
  chunk_z INT NOT NULL,
  started_utc BIGINT NOT NULL,
  expires_utc BIGINT NOT NULL,
  KEY idx_txn_player (player_id),
  KEY idx_txn_expiry (expires_utc),
  CONSTRAINT fk_txn_station FOREIGN KEY (station_id) REFERENCES radio_stations(station_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 覆盖热力：以单区块为单位的信号档位缓存（由单区块增量通道更新，不每 tick 全量重发）。
CREATE TABLE IF NOT EXISTS radio_coverage_cells (
  transmitter_id BIGINT NOT NULL,
  chunk_x INT NOT NULL,
  chunk_z INT NOT NULL,
  quality VARCHAR(8) NOT NULL,
  snr_permille INT NOT NULL DEFAULT 0,
  revision BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (transmitter_id, chunk_x, chunk_z),
  KEY idx_coverage_chunk (chunk_x, chunk_z),
  CONSTRAINT fk_coverage_tx FOREIGN KEY (transmitter_id) REFERENCES radio_transmitters(transmitter_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 幂等键：订阅(player+station+epoch)、塔(transmitterId+revision)等复用统一幂等表，
-- 与 V1 的 idempotency_keys 对齐（复合键编码为 scope:key 写入 idempotency_key）。
INSERT IGNORE INTO schema_migrations (version, applied_at_utc, checksum)
VALUES ('2', UNIX_TIMESTAMP() * 1000, 'radio-phase-1');
