package com.chaos.nation.protocol;

/** 格式错误的封包——由长度/枚举/范围校验失败抛出，绝不传播为玩家可见堆栈。 */
public class MalformedPacketException extends RuntimeException {
  public MalformedPacketException(String message) {
    super(message);
  }
}
