package com.chaos.nation.plugin.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExplorationServiceTest {
  @Test
  void duplicatesAreDeduped_beforePersist() {
    List<ExplorationService.ChunkVisit> persisted = new ArrayList<>();
    ExplorationService svc = new ExplorationService(512, persisted::addAll);
    svc.record("uuid-1", 3, 4, 100L);
    svc.record("uuid-1", 3, 4, 101L); // 重复区块
    svc.record("uuid-1", 5, 6, 102L);
    assertThat(svc.pending()).isEqualTo(2);
    assertThat(svc.flush()).isEqualTo(2);
    assertThat(persisted).hasSize(2);
    assertThat(svc.pending()).isZero();
  }

  @Test
  void flushRespectsBatchSize_andAllowsReexplorationAfterFlush() {
    List<ExplorationService.ChunkVisit> persisted = new ArrayList<>();
    ExplorationService svc = new ExplorationService(2, persisted::addAll);
    for (int i = 0; i < 5; i++) svc.record("u", i, 0, i);
    assertThat(svc.flush()).isEqualTo(2);
    assertThat(svc.flush()).isEqualTo(2);
    assertThat(svc.flush()).isEqualTo(1);
    assertThat(svc.flush()).isZero();
    // 落库后可再次记录同一区块（新会话）
    svc.record("u", 0, 0, 999L);
    assertThat(svc.pending()).isEqualTo(1);
  }
}
