package com.chaos.nation.protocol;

/** 所有网络消息的根接口。每个包都有公共头与二进制负载。 */
public interface Packet {
  MessageHeader header();

  /** 将来未知字段以扩展块结尾，旧版本可跳过。 */
  default byte[] extensions() {
    return new byte[0];
  }
}
