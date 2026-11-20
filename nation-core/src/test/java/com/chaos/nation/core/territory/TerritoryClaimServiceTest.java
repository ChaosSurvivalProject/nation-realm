package com.chaos.nation.core.territory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class TerritoryClaimServiceTest {
  @Test
  void claimSingleChunk_isIdempotentAndVersioned() {
    AtomicLong clock = new AtomicLong(1000L);
    TerritoryClaimService svc = new TerritoryClaimService(clock::get);

    TerritoryClaimService.ClaimOutcome first = svc.claim(5, 9, 7);
    assertThat(first.result()).isEqualTo(TerritoryClaimService.ClaimResult.CLAIMED);
    assertThat(first.change().nationId()).isEqualTo(7);
    assertThat(first.change().chunkX()).isEqualTo(5);
    assertThat(first.change().chunkZ()).isEqualTo(9);
    assertThat(first.change().regionVersion()).isEqualTo(1L);
    assertThat(svc.ownerOf(5, 9)).isEqualTo(7);

    // 同国重复占领：幂等，无变更，版本不变
    TerritoryClaimService.ClaimOutcome again = svc.claim(5, 9, 7);
    assertThat(again.result()).isEqualTo(TerritoryClaimService.ClaimResult.UNCHANGED);
    assertThat(again.changed()).isFalse();
    assertThat(svc.versionOf(5, 9)).isEqualTo(1L);
  }

  @Test
  void claimNeighborChunk_doesNotAffectOtherChunk() {
    TerritoryClaimService svc = new TerritoryClaimService();
    svc.claim(0, 0, 1);
    svc.claim(1, 0, 2); // 同一 region，不同区块
    assertThat(svc.ownerOf(0, 0)).isEqualTo(1);
    assertThat(svc.ownerOf(1, 0)).isEqualTo(2);
    // 同 region 共享版本计数：两次变更 → 版本 2
    assertThat(svc.versionOf(0, 0)).isEqualTo(2L);
  }

  @Test
  void transferFromOtherNation_isReportedAsTransferred() {
    TerritoryClaimService svc = new TerritoryClaimService();
    svc.claim(3, 3, 1);
    TerritoryClaimService.ClaimOutcome take = svc.claim(3, 3, 2);
    assertThat(take.result()).isEqualTo(TerritoryClaimService.ClaimResult.TRANSFERRED);
    assertThat(take.change().nationId()).isEqualTo(2);
    assertThat(svc.ownerOf(3, 3)).isEqualTo(2);
  }

  @Test
  void claimIfEmpty_neverOverwrites() {
    TerritoryClaimService svc = new TerritoryClaimService();
    svc.claim(8, 8, 1);
    TerritoryClaimService.ClaimOutcome outcome = svc.claimIfEmpty(8, 8, 2);
    assertThat(outcome.result()).isEqualTo(TerritoryClaimService.ClaimResult.UNCHANGED);
    assertThat(svc.ownerOf(8, 8)).isEqualTo(1);
  }

  @Test
  void releaseSingleChunk_emitsZeroOwnerChange() {
    TerritoryClaimService svc = new TerritoryClaimService();
    svc.claim(2, 2, 1);
    TerritoryClaimService.ClaimOutcome release = svc.release(2, 2);
    assertThat(release.result()).isEqualTo(TerritoryClaimService.ClaimResult.RELEASED);
    assertThat(release.change().nationId()).isZero();
    assertThat(svc.ownerOf(2, 2)).isZero();
    assertThat(svc.release(2, 2).result())
        .isEqualTo(TerritoryClaimService.ClaimResult.NOTHING_TO_RELEASE);
  }

  @Test
  void negativeCoordinates_workAcrossRegionBoundary() {
    TerritoryClaimService svc = new TerritoryClaimService();
    svc.claim(-1, -1, 9); // region (-1,-1), local (31,31)
    svc.claim(0, 0, 8); // region (0,0), local (0,0)
    assertThat(svc.ownerOf(-1, -1)).isEqualTo(9);
    assertThat(svc.ownerOf(0, 0)).isEqualTo(8);
    assertThat(svc.versionOf(-1, -1)).isEqualTo(1L);
    assertThat(svc.versionOf(0, 0)).isEqualTo(1L);
    assertThat(svc.loadedRegions()).isEqualTo(2);
  }

  @Test
  void claimAll_returnsOnlyActualChanges() {
    TerritoryClaimService svc = new TerritoryClaimService();
    svc.claim(1, 1, 5); // 先占了
    List<int[]> batch = List.of(new int[] {1, 1}, new int[] {2, 2}, new int[] {3, 3});
    List<TerritoryClaimService.ChunkChange> changes = svc.claimAll(batch, 6);
    // 1,1 从国家5转让给6（变更），2,2 与 3,3 新占领，共 3 个变更。
    assertThat(changes).hasSize(3);
  }

  @Test
  void invalidNationId_isRejected() {
    TerritoryClaimService svc = new TerritoryClaimService();
    assertThatThrownBy(() -> svc.claim(0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> svc.claim(0, 0, -1)).isInstanceOf(IllegalArgumentException.class);
  }
}
