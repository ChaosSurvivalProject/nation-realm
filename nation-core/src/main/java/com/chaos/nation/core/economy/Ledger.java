package com.chaos.nation.core.economy;

import com.chaos.nation.core.domain.Money;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** 复式记账账本：每笔交易至少两条分录，借方总额必须等于贷方总额。 经济守恒通过 {@link #validateConservation()} 自动测试；不允许直接改余额。 */
public final class Ledger {
  /** 账户类型：国库、成员钱包、国家基金等。 */
  public enum AccountType {
    TREASURY,
    MEMBER_WALLET,
    NATION_FUND,
    EXTERNAL
  }

  public record Account(String id, AccountType type, String ownerId) {
    public Account {
      Objects.requireNonNull(id);
      Objects.requireNonNull(type);
    }
  }

  /** 一条分录：正数为借，负数为贷（以最小货币单位表示，借贷必相等）。 */
  public record Entry(String accountId, Money amount, String memo) {
    public Entry {
      Objects.requireNonNull(accountId);
      Objects.requireNonNull(amount);
    }
  }

  public record Journal(
      long id, String idempotencyKey, String reason, List<Entry> entries, long createdAtUtc) {
    public Journal {
      entries = List.copyOf(entries);
    }
  }

  private final List<Journal> journals = new ArrayList<>();
  private long nextId = 1;

  /** 记一笔复式交易；借贷不平衡或幂等键重复会被拒绝。 */
  public synchronized long post(
      String idempotencyKey, String reason, List<Entry> entries, long createdAtUtc) {
    if (entries == null || entries.size() < 2) {
      throw new IllegalArgumentException("复式交易至少需要两条分录");
    }
    long debit = 0, credit = 0;
    for (Entry e : entries) {
      if (e.amount().units() >= 0) debit += e.amount().units();
      else credit -= e.amount().units();
    }
    if (debit != credit) {
      throw new IllegalArgumentException("借贷不平衡: debit=" + debit + " credit=" + credit);
    }
    if (hasIdempotencyKey(idempotencyKey)) {
      return findByKey(idempotencyKey).id(); // 幂等：重复请求返回原交易，不重复扣款
    }
    long id = nextId++;
    journals.add(new Journal(id, idempotencyKey, reason, entries, createdAtUtc));
    return id;
  }

  private boolean hasIdempotencyKey(String key) {
    return key != null && findByKey(key) != null;
  }

  private Journal findByKey(String key) {
    for (Journal j : journals) {
      if (key.equals(j.idempotencyKey())) return j;
    }
    return null;
  }

  /** 计算某账户当前余额（所有分录之和）。 */
  public synchronized Money balanceOf(String accountId) {
    long sum = 0;
    for (Journal j : journals) {
      for (Entry e : j.entries()) {
        if (e.accountId().equals(accountId)) sum += e.amount().units();
      }
    }
    return Money.of(sum);
  }

  /** 全账本任意时刻借贷必然守恒（总额为 0）。 */
  public boolean validateConservation() {
    long total = 0;
    for (Journal j : journals) {
      for (Entry e : j.entries()) total += e.amount().units();
    }
    return total == 0;
  }

  public List<Journal> journals() {
    return Collections.unmodifiableList(journals);
  }
}
