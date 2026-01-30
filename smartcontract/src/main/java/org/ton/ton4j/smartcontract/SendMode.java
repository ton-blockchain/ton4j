package org.ton.ton4j.smartcontract;

public enum SendMode {
  CARRY_ALL_REMAINING_BALANCE_AND_DESTROY(160), // 128 + 32
  CARRY_ALL_REMAINING_INCOMING_VALUE_AND_SEND_BOUNCE(80), // 64 + 16
  CARRY_ALL_REMAINING_BALANCE(128),
  CARRY_ALL_REMAINING_INCOMING_VALUE(64),
  DESTROY_ACCOUNT_IF_ZERO(32),
  SEND_BOUNCE_IF_ACTION_FAIL(16),
  PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS(3),
  IGNORE_ERRORS(2),
  PAY_GAS_SEPARATELY(1),
  DEFAULT_MODE(0);

  private final int value;

  SendMode(final int newValue) {
    value = newValue;
  }

  public int getValue() {
    return value;
  }

  public static SendMode valueOfInt(int value) {
    switch (value) {
      case 0:
        return DEFAULT_MODE;
      case 1:
        return PAY_GAS_SEPARATELY;
      case 2:
        return IGNORE_ERRORS;
      case 3:
        return PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS;
      case 16:
        return SEND_BOUNCE_IF_ACTION_FAIL;
      case 32:
        return DESTROY_ACCOUNT_IF_ZERO;
      case 64:
        return CARRY_ALL_REMAINING_INCOMING_VALUE;
      case 80:
        return CARRY_ALL_REMAINING_INCOMING_VALUE_AND_SEND_BOUNCE;
      case 128:
        return CARRY_ALL_REMAINING_BALANCE;
      case 160:
        return CARRY_ALL_REMAINING_BALANCE_AND_DESTROY;
    }
    throw new IllegalArgumentException();
  }

  public int getIndex() {
    return ordinal();
  }
}
