package com.chaos.nation.plugin.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaos.nation.core.territory.TerritoryClaimService;
import com.chaos.nation.protocol.packets.DataPackets.ChunkClaim;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClaimServiceTest {
  @Test
  void claimSingleChunk_emitsPacketAndQueuesPersistence() {
    List<ClaimService.PendingClaim> persisted = new ArrayList<>();
    ClaimService svc =
        new ClaimService(new TerritoryClaimService(), 512, batch -> persisted.addAll(batch));

    ChunkClaim packet = svc.claim(4, 5, 3, "player-1", 77L);

    assertThat(packet).isNotNull();
    assertThat(packet.chunkX()).isEqualTo(4);
    assertThat(packet.chunkZ()).isEqualTo(5);
    assertThat(packet.nationId()).isEqualTo(3);
    assertThat(packet.header().nationDataVersion()).isEqualTo(1L);
    assertThat(svc.pending()).isEqualTo(1);

    assertThat(svc.flush()).isEqualTo(1);
    assertThat(persisted).hasSize(1);
    assertThat(persisted.get(0).nationId()).isEqualTo(3);
    assertThat(persisted.get(0).claimerUuid()).isEqualTo("player-1");
  }

  @Test
  void idempotentClaim_emitsNoPacketAndNoPersistence() {
    ClaimService svc = new ClaimService(new TerritoryClaimService(), 512, batch -> {});
    assertThat(svc.claim(1, 1, 2, "p", 1L)).isNotNull();
    assertThat(svc.claim(1, 1, 2, "p", 2L)).isNull(); // 幂等
    assertThat(svc.pending()).isEqualTo(1); // 只有第一次入队
  }

  @Test
  void release_emitsZeroOwnerPacket() {
    ClaimService svc = new ClaimService(new TerritoryClaimService(), 512, batch -> {});
    svc.claim(9, 9, 6, "p", 1L);
    ChunkClaim release = svc.release(9, 9, "p", 2L);
    assertThat(release).isNotNull();
    assertThat(release.nationId()).isZero();
    assertThat(release.regionVersion()).isEqualTo(2L);
    assertThat(svc.territory().ownerOf(9, 9)).isZero();
  }

  @Test
  void flushRespectsBatchSize() {
    ClaimService svc = new ClaimService(new TerritoryClaimService(), 2, batch -> {});
    for (int i = 0; i < 5; i++) svc.claim(i, 0, 1, "p", i);
    assertThat(svc.pending()).isEqualTo(5);
    assertThat(svc.flush()).isEqualTo(2);
    assertThat(svc.flush()).isEqualTo(2);
    assertThat(svc.flush()).isEqualTo(1);
    assertThat(svc.flush()).isZero();
  }
}
