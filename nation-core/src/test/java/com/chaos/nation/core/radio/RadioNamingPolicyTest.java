package com.chaos.nation.core.radio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RadioNamingPolicyTest {
  @Test
  void fictionalCallsign_isAllowed() {
    assertThat(RadioNamingPolicy.validateCallsign("艾泽拉斯-1").allowed()).isTrue();
    assertThat(RadioNamingPolicy.validateCallsign("GRYPHON-7").allowed()).isTrue();
  }

  @Test
  void realWorldName_isRejectedWithReason() {
    RadioNamingPolicy.Result r = RadioNamingPolicy.validateCallsign("China");
    assertThat(r.allowed()).isFalse();
    assertThat(r.reason()).contains("现实国家");
  }

  @Test
  void emergencyCallsign_isRejected() {
    assertThat(RadioNamingPolicy.validateCallsign("MAYDAY").allowed()).isFalse();
    assertThat(RadioNamingPolicy.validateCallsign("sos").allowed()).isFalse();
  }

  @Test
  void caseSpaceAndFullWidthVariants_areNormalizedAndRejected() {
    assertThat(RadioNamingPolicy.validateCallsign("  united   states ").allowed()).isFalse();
    // 全角 U S A -> 半角 usa
    assertThat(RadioNamingPolicy.validateCallsign("ＵＳＡ").allowed()).isFalse();
  }

  @Test
  void emptyAndTooLong_areRejected() {
    assertThat(RadioNamingPolicy.validateCallsign("  ").allowed()).isFalse();
    assertThat(RadioNamingPolicy.validateCallsign("a".repeat(25)).allowed()).isFalse();
  }
}
