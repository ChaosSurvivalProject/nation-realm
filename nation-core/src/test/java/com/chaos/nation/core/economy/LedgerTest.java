package com.chaos.nation.core.economy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaos.nation.core.domain.Money;
import com.chaos.nation.core.economy.Ledger.Entry;
import java.util.List;
import org.junit.jupiter.api.Test;

class LedgerTest {
  @Test
  void doubleEntry_transferKeepsConservation_andUpdatesBalances() {
    Ledger ledger = new Ledger();
    ledger.post(
        "tx-1",
        "成员向国库缴税",
        List.of(
            new Entry("wallet-1", Money.of(-500), "缴税支出"),
            new Entry("treasury", Money.of(500), "税收收入")),
        1000L);

    assertThat(ledger.validateConservation()).isTrue();
    assertThat(ledger.balanceOf("wallet-1")).isEqualTo(Money.of(-500));
    assertThat(ledger.balanceOf("treasury")).isEqualTo(Money.of(500));
  }

  @Test
  void unbalancedEntry_isRejected() {
    Ledger ledger = new Ledger();
    assertThatThrownBy(
            () ->
                ledger.post(
                    "bad",
                    "错误交易",
                    List.of(new Entry("a", Money.of(100), ""), new Entry("b", Money.of(-50), "")),
                    1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("借贷不平衡");
    assertThat(ledger.journals()).isEmpty();
  }

  @Test
  void duplicateIdempotencyKey_doesNotDoubleCharge() {
    Ledger ledger = new Ledger();
    List<Entry> tx =
        List.of(
            new Entry("wallet", Money.of(-1000), "建都"),
            new Entry("treasury", Money.of(1000), "建都收入"));
    long first = ledger.post("order-42", "建造物资中心", tx, 1L);
    long second = ledger.post("order-42", "建造物资中心", tx, 1L);
    assertThat(second).isEqualTo(first);
    assertThat(ledger.journals()).hasSize(1);
    assertThat(ledger.balanceOf("wallet")).isEqualTo(Money.of(-1000));
  }

  @Test
  void money_usesIntegerMinorUnits_noFloatRounding() {
    assertThat(Money.ofYuan(12, 34).units()).isEqualTo(1234);
    assertThat(Money.ofYuan(12, 34).plus(Money.ofYuan(0, 66))).isEqualTo(Money.ofYuan(13, 0));
  }
}
