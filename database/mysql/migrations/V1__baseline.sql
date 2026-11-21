-- NationRealm 基线 schema，MySQL 8 / InnoDB / utf8mb4
-- 金额一律使用 BIGINT 最小货币单位；时间一律 UTC（BIGINT 毫秒或 TIMESTAMP）；
-- 从第一天写入 server_id / 实体版本 / last_write_server / 幂等键 / 事件序号。

CREATE TABLE IF NOT EXISTS schema_migrations (
  version VARCHAR(32) NOT NULL PRIMARY KEY,
  applied_at_utc BIGINT NOT NULL,
  checksum VARCHAR(64) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS players (
  uuid CHAR(36) NOT NULL PRIMARY KEY,
  last_name VARCHAR(64) NOT NULL,
  first_seen_utc BIGINT NOT NULL,
  last_seen_utc BIGINT NOT NULL,
  last_server_id VARCHAR(64) NOT NULL DEFAULT '',
  INDEX idx_players_last_seen (last_seen_utc)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS nations (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  abbreviation VARCHAR(8) NOT NULL,
  capital_chunk_x INT NOT NULL DEFAULT 0,
  capital_chunk_z INT NOT NULL DEFAULT 0,
  capital_set BOOLEAN NOT NULL DEFAULT FALSE,
  color_rgb INT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0,
  server_id VARCHAR(64) NOT NULL,
  last_write_server VARCHAR(64) NOT NULL,
  created_at_utc BIGINT NOT NULL,
  UNIQUE KEY uk_nations_name (name),
  UNIQUE KEY uk_nations_abbr (abbreviation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS nation_members (
  nation_id BIGINT NOT NULL,
  player_uuid CHAR(36) NOT NULL,
  role VARCHAR(32) NOT NULL DEFAULT 'MEMBER',
  joined_utc BIGINT NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (nation_id, player_uuid),
  KEY idx_members_player (player_uuid),
  CONSTRAINT fk_members_nation FOREIGN KEY (nation_id) REFERENCES nations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS offices (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(64) NOT NULL UNIQUE,
  title VARCHAR(128) NOT NULL,
  political BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS office_grants (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nation_id BIGINT NOT NULL,
  player_uuid CHAR(36) NOT NULL,
  office_id INT NOT NULL,
  granted_by CHAR(36) NOT NULL,
  granted_utc BIGINT NOT NULL,
  expires_utc BIGINT NOT NULL,
  revoked_utc BIGINT NULL,
  KEY idx_grants_active (nation_id, player_uuid, expires_utc),
  CONSTRAINT fk_grants_nation FOREIGN KEY (nation_id) REFERENCES nations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS regions (
  region_x INT NOT NULL,
  region_z INT NOT NULL,
  server_id VARCHAR(64) NOT NULL,
  bitmap BLOB NOT NULL COMMENT '稀疏位图：探索/所有权',
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (region_x, region_z)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS explored_chunks (
  player_uuid CHAR(36) NOT NULL,
  chunk_x INT NOT NULL,
  chunk_z INT NOT NULL,
  first_explored_utc BIGINT NOT NULL,
  source VARCHAR(32) NOT NULL DEFAULT 'move',
  PRIMARY KEY (player_uuid, chunk_x, chunk_z),
  KEY idx_explored_chunk (chunk_x, chunk_z)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS territory_claims (
  chunk_x INT NOT NULL,
  chunk_z INT NOT NULL,
  nation_id BIGINT NOT NULL,
  claimed_utc BIGINT NOT NULL,
  claimer_uuid CHAR(36) NOT NULL,
  source VARCHAR(32) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (chunk_x, chunk_z),
  KEY idx_claims_nation (nation_id),
  CONSTRAINT fk_claims_nation FOREIGN KEY (nation_id) REFERENCES nations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS buildings (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nation_id BIGINT NOT NULL,
  template_id VARCHAR(64) NOT NULL,
  chunk_x INT NOT NULL,
  chunk_z INT NOT NULL,
  stage VARCHAR(32) NOT NULL,
  progress_permille INT NOT NULL DEFAULT 0,
  health INT NOT NULL DEFAULT 100,
  version BIGINT NOT NULL DEFAULT 0,
  KEY idx_buildings_nation (nation_id),
  CONSTRAINT fk_buildings_nation FOREIGN KEY (nation_id) REFERENCES nations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS construction_orders (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nation_id BIGINT NOT NULL,
  template_id VARCHAR(64) NOT NULL,
  chunk_x INT NOT NULL,
  chunk_z INT NOT NULL,
  state VARCHAR(32) NOT NULL,
  started_utc BIGINT NOT NULL,
  finish_utc BIGINT NOT NULL,
  cost_units BIGINT NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL UNIQUE,
  KEY idx_orders_nation (nation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS accounts (
  id VARCHAR(64) NOT NULL PRIMARY KEY,
  nation_id BIGINT NULL,
  type VARCHAR(32) NOT NULL,
  currency VARCHAR(16) NOT NULL DEFAULT 'CNY_MINOR',
  version BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS journal_entries (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  idempotency_key VARCHAR(128) NOT NULL UNIQUE,
  reason VARCHAR(255) NOT NULL,
  created_utc BIGINT NOT NULL,
  server_id VARCHAR(64) NOT NULL,
  INDEX idx_journal_created (created_utc)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS journal_lines (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  entry_id BIGINT NOT NULL,
  account_id VARCHAR(64) NOT NULL,
  amount_units BIGINT NOT NULL COMMENT '正=借 负=贷，借贷必相等',
  memo VARCHAR(255) NOT NULL DEFAULT '',
  KEY idx_lines_entry (entry_id),
  CONSTRAINT fk_lines_entry FOREIGN KEY (entry_id) REFERENCES journal_entries(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tax_policies (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nation_id BIGINT NOT NULL,
  rate_permille INT NOT NULL,
  period VARCHAR(32) NOT NULL,
  effective_utc BIGINT NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  KEY idx_tax_nation (nation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS laws (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nation_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  state VARCHAR(32) NOT NULL,
  current_version INT NOT NULL DEFAULT 1,
  KEY idx_laws_nation (nation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS law_versions (
  law_id BIGINT NOT NULL,
  version INT NOT NULL,
  structured_rules JSON NOT NULL COMMENT '结构化条件与操作白名单，禁止自由文本直接变代码',
  enacted_utc BIGINT NOT NULL,
  PRIMARY KEY (law_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS diplomatic_relations (
  nation_a BIGINT NOT NULL,
  nation_b BIGINT NOT NULL,
  relation VARCHAR(32) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (nation_a, nation_b)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS treaties (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nation_a BIGINT NOT NULL,
  nation_b BIGINT NOT NULL,
  version INT NOT NULL DEFAULT 1,
  terms_hash VARCHAR(128) NOT NULL,
  effective_utc BIGINT NOT NULL,
  expires_utc BIGINT NOT NULL,
  confirmed_a BOOLEAN NOT NULL DEFAULT FALSE,
  confirmed_b BOOLEAN NOT NULL DEFAULT FALSE,
  violated BOOLEAN NOT NULL DEFAULT FALSE,
  KEY idx_treaties_parties (nation_a, nation_b)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS military_units (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nation_id BIGINT NOT NULL,
  template_id VARCHAR(64) NOT NULL,
  chunk_x INT NOT NULL,
  chunk_z INT NOT NULL,
  morale INT NOT NULL DEFAULT 100,
  supply_units INT NOT NULL DEFAULT 0,
  soldier_count INT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0,
  KEY idx_units_nation (nation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS unit_orders (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  unit_id BIGINT NOT NULL,
  order_type VARCHAR(32) NOT NULL,
  target_x INT NOT NULL,
  target_z INT NOT NULL,
  eta_utc BIGINT NOT NULL,
  state VARCHAR(32) NOT NULL,
  KEY idx_orders_unit (unit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS npcs (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nation_id BIGINT NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  ai_visible BOOLEAN NOT NULL DEFAULT TRUE COMMENT '永久 AI 标记，禁止隐藏',
  profession VARCHAR(64) NOT NULL,
  personality VARCHAR(128) NOT NULL DEFAULT '',
  level INT NOT NULL DEFAULT 1,
  xp BIGINT NOT NULL DEFAULT 0,
  health INT NOT NULL DEFAULT 100,
  food INT NOT NULL DEFAULT 100,
  stamina INT NOT NULL DEFAULT 100,
  chunk_x INT NOT NULL,
  chunk_z INT NOT NULL,
  state VARCHAR(32) NOT NULL DEFAULT 'IDLE',
  owner_uuid CHAR(36) NULL,
  version BIGINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_npc_name_per_nation (nation_id, display_name),
  KEY idx_npcs_nation (nation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS npc_memories (
  npc_id BIGINT NOT NULL,
  summary TEXT NOT NULL COMMENT '只存必要摘要，禁止完整聊天/密钥/敏感信息',
  updated_utc BIGINT NOT NULL,
  PRIMARY KEY (npc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_requests (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  npc_id BIGINT NOT NULL,
  request_id VARCHAR(64) NOT NULL,
  model_key VARCHAR(64) NOT NULL,
  prompt_hash VARCHAR(128) NOT NULL,
  intent_json JSON NULL,
  status VARCHAR(32) NOT NULL,
  cost_units BIGINT NOT NULL DEFAULT 0,
  created_utc BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS outbox_events (
  seq BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  aggregate_type VARCHAR(64) NOT NULL,
  aggregate_id VARCHAR(64) NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  payload_json JSON NOT NULL,
  server_id VARCHAR(64) NOT NULL,
  created_utc BIGINT NOT NULL,
  published BOOLEAN NOT NULL DEFAULT FALSE,
  INDEX idx_outbox_unpublished (published, seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS idempotency_keys (
  idempotency_key VARCHAR(128) NOT NULL PRIMARY KEY,
  scope VARCHAR(64) NOT NULL,
  result_ref VARCHAR(128) NOT NULL,
  created_utc BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
