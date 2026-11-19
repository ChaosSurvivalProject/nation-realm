package com.chaos.nation.core.government;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaos.nation.protocol.ErrorCode;
import org.junit.jupiter.api.Test;

class NationNameRegistryTest {
  @Test
  void realWorldChineseName_isRejectedWithLawReason() {
    NationNameRegistry.Result r = NationNameRegistry.validate("中国");
    assertThat(r.allowed()).isFalse();
    assertThat(r.errorCode()).isEqualTo(ErrorCode.NAME_RESTRICTED);
    assertThat(r.reason()).contains("法律法规");
  }

  @Test
  void realWorldEnglishName_caseAndSpaceVariants_areRejected() {
    assertThat(NationNameRegistry.validate("United   States").allowed()).isFalse();
    assertThat(NationNameRegistry.validate("uSa").allowed()).isFalse();
    assertThat(NationNameRegistry.isReserved("  germany ")).isTrue();
    assertThat(NationNameRegistry.isReserved("JaPan")).isTrue();
  }

  @Test
  void fictionalName_isAllowed() {
    assertThat(NationNameRegistry.validate("艾泽拉斯").allowed()).isTrue();
    assertThat(NationNameRegistry.validate("NationRealm").allowed()).isTrue();
    assertThat(NationNameRegistry.validate("银河共和国").allowed()).isTrue();
  }

  @Test
  void emptyOrTooLong_isInvalidArgument() {
    assertThat(NationNameRegistry.validate("  ").errorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
    assertThat(NationNameRegistry.validate("x".repeat(49)).errorCode())
        .isEqualTo(ErrorCode.INVALID_ARGUMENT);
  }
}
