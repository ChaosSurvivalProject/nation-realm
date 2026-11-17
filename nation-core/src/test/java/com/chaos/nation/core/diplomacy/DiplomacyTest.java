package com.chaos.nation.core.diplomacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaos.nation.core.diplomacy.Diplomacy.Relation;
import org.junit.jupiter.api.Test;

class DiplomacyTest {
  @Test
  void illegalJump_isRejected() {
    Diplomacy d = new Diplomacy();
    assertThatThrownBy(() -> d.transition(1, 2, Relation.WAR))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void validPathToWar_works_andIsSymmetric() {
    Diplomacy d = new Diplomacy();
    d.transition(1, 2, Relation.CONTACT);
    d.transition(1, 2, Relation.NEUTRAL);
    d.transition(1, 2, Relation.HOSTILE);
    d.transition(1, 2, Relation.WAR);
    assertThat(d.relation(1, 2)).isEqualTo(Relation.WAR);
    assertThat(d.relation(2, 1)).isEqualTo(Relation.WAR); // key 有序对，双向一致
  }
}
