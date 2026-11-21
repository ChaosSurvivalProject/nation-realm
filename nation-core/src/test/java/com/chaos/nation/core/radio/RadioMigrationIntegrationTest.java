package com.chaos.nation.core.radio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * 迁移集成测试：直连 CI 的 MySQL service，校验 V1 + V2 迁移真实建表。
 *
 * <p>标记 {@code @Tag("integration")}，仅由 CI 的 database job（有 Docker）运行； 本地 {@code ./gradlew test}
 * 排除该标签。环境变量由 database.yml 注入： {@code NATION_DB_HOST / NATION_DB_USER / NATION_DB_PASS /
 * NATION_DB_NAME}。
 */
@Tag("integration")
class RadioMigrationIntegrationTest {
  private static String env(String key, String fallback) {
    String v = System.getenv(key);
    return v == null || v.isBlank() ? fallback : v;
  }

  private Connection connect() throws SQLException {
    String host = env("NATION_DB_HOST", "");
    // 无 DB 环境（本地误跑 integrationTest）时跳过，不伪造通过。
    assumeTrue(!host.isBlank(), "NATION_DB_HOST 未设置，跳过集成测试");
    String user = env("NATION_DB_USER", "root");
    String pass = env("NATION_DB_PASS", "");
    String db = env("NATION_DB_NAME", "nation_realm_test");
    String url =
        "jdbc:mysql://" + host + ":3306/" + db + "?useSSL=false&allowPublicKeyRetrieval=true";
    return DriverManager.getConnection(url, user, pass);
  }

  @Test
  void baselineTablesExist_afterV1() throws SQLException {
    try (Connection c = connect();
        Statement st = c.createStatement()) {
      assertTable(st, "schema_migrations");
      assertTable(st, "players");
      assertTable(st, "nations");
      assertTable(st, "territory_claims");
      assertTable(st, "explored_chunks");
      assertTable(st, "outbox_events");
      assertTable(st, "idempotency_keys");
    }
  }

  @Test
  void radioTablesExist_afterV2() throws SQLException {
    try (Connection c = connect();
        Statement st = c.createStatement()) {
      assertTable(st, "radio_stations");
      assertTable(st, "radio_transmitters");
      assertTable(st, "radio_subscriptions");
      assertTable(st, "radio_transmissions");
      assertTable(st, "radio_coverage_cells");
    }
  }

  @Test
  void currencyColumnIsWideEnough() throws SQLException {
    // 回归：currency VARCHAR(8) DEFAULT 'CNY_MINOR' 曾导致 ERROR 1067 迁移中止。
    try (Connection c = connect();
        Statement st = c.createStatement();
        ResultSet rs =
            st.executeQuery(
                "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS "
                    + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='accounts' "
                    + "AND COLUMN_NAME='currency'")) {
      assumeTrue(rs.next(), "accounts.currency 列不存在，跳过");
      assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(9);
    }
  }

  @Test
  void migrationVersionRecorded() throws SQLException {
    try (Connection c = connect();
        Statement st = c.createStatement();
        ResultSet rs =
            st.executeQuery("SELECT COUNT(*) FROM schema_migrations WHERE version IN ('1','2')")) {
      assumeTrue(rs.next(), "schema_migrations 查询无结果");
      assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(1);
    }
  }

  private static void assertTable(Statement st, String table) throws SQLException {
    try (ResultSet rs =
        st.executeQuery(
            "SELECT COUNT(*) FROM information_schema.TABLES "
                + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='"
                + table
                + "'")) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getInt(1)).as("table " + table + " should exist").isEqualTo(1);
    }
  }
}
