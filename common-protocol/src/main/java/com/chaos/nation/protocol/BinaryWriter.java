package com.chaos.nation.protocol;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** 确定性二进制写入器：小端 + 变长整数。 每条字符串、字节数组都写长度前缀，读取端据此跳过未知字段。 */
public final class BinaryWriter {
  private final List<Byte> out = new ArrayList<>();

  public BinaryWriter writeByte(int value) {
    out.add((byte) (value & 0xFF));
    return this;
  }

  public BinaryWriter writeBoolean(boolean value) {
    return writeByte(value ? 1 : 0);
  }

  public BinaryWriter writeInt(int value) {
    writeByte(value);
    writeByte(value >>> 8);
    writeByte(value >>> 16);
    writeByte(value >>> 24);
    return this;
  }

  public BinaryWriter writeLong(long value) {
    for (int i = 0; i < 8; i++) {
      writeByte((int) (value >>> (i * 8)));
    }
    return this;
  }

  /** VarInt（ ZigZag 前的原始无符号 LEB128），用于小整数与枚举。 */
  public BinaryWriter writeVarInt(int value) {
    int v = value;
    while ((v & ~0x7F) != 0) {
      writeByte((v & 0x7F) | 0x80);
      v >>>= 7;
    }
    writeByte(v);
    return this;
  }

  public BinaryWriter writeString(String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    if (bytes.length > ProtocolConstants.MAX_STRING_LENGTH) {
      throw new IllegalArgumentException("string too long: " + bytes.length);
    }
    writeVarInt(bytes.length);
    for (byte b : bytes) writeByte(b);
    return this;
  }

  public BinaryWriter writeBytes(byte[] value) {
    writeVarInt(value.length);
    for (byte b : value) writeByte(b);
    return this;
  }

  public byte[] toByteArray() {
    byte[] result = new byte[out.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = out.get(i);
    }
    return result;
  }

  public int size() {
    return out.size();
  }
}
