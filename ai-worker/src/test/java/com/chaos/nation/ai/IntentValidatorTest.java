package com.chaos.nation.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IntentValidatorTest {
  private final IntentValidator validator = new IntentValidator();

  @Test
  void validMoveIntent_isAccepted() {
    IntentValidator.Result r =
        validator.validate("{\"action\":\"move\",\"target_chunk\":[1,2],\"priority\":3}");
    assertThat(r).isInstanceOf(IntentValidator.Result.Valid.class);
    IntentValidator.Result.Valid v = (IntentValidator.Result.Valid) r;
    assertThat(v.intent().action).isEqualTo(Intent.Action.MOVE);
  }

  @Test
  void forbiddenField_isRejected() {
    IntentValidator.Result r = validator.validate("{\"action\":\"move\",\"op\":true}");
    assertThat(r).isInstanceOf(IntentValidator.Result.Rejected.class);
  }

  @Test
  void unknownAction_isRejected() {
    IntentValidator.Result r = validator.validate("{\"action\":\"destroy_everything\"}");
    assertThat(r).isInstanceOf(IntentValidator.Result.Rejected.class);
  }

  @Test
  void malformedJson_isRejected() {
    assertThat(validator.validate("{not json")).isInstanceOf(IntentValidator.Result.Rejected.class);
  }

  @Test
  void budgetGuard_stopsAfterLimit_andResetsDaily() {
    BudgetGuard guard = new BudgetGuard(2, 1000);
    long now = 1_700_000_000L;
    assertThat(guard.allow(now, 10)).isTrue();
    guard.record(now, 10);
    assertThat(guard.allow(now, 10)).isTrue();
    guard.record(now, 10);
    assertThat(guard.allow(now, 10)).isFalse(); // 达到每分钟上限
    // 跨分钟窗口重置
    assertThat(guard.allow(now + 60, 10)).isTrue();
    // 超出每日成本上限
    assertThat(guard.allow(now + 120, 2000)).isFalse();
  }

  @Test
  void circuitBreaker_opensAfterFailures() {
    CircuitBreaker cb = new CircuitBreaker(3, 10_000);
    cb.recordFailure();
    cb.recordFailure();
    assertThat(cb.isOpen()).isFalse();
    cb.recordFailure();
    assertThat(cb.isOpen()).isTrue();
  }
}
