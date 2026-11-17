package com.chaos.nation.protocol;

import java.nio.charset.StandardCharsets;

/** 与 BinaryWriter 对称的读取器；对长度做上限校验，未知扩展字段可跳过而不崩溃。 */
public final class BinaryReader {
  private final byte[] data;
  private int index;

  public BinaryReader(byte[] data) {
    this.data = data;
  }

  public int remaining() {
    return data.length - index;
  }

  private void require(int n) {
    if (n < 0 || remaining() < n) {
      throw new MalformedPacketException("unexpected end of packet");
    }
  }

  public int readByte() {
    require(1);
    return data[index++] & 0xFF;
  }

  public boolean readBoolean() {
    return readByte() != 0;
  }

  public int readInt() {
    require(4);
    int v =
        (data[index] & 0xFF)
            | ((data[index + 1] & 0xFF) << 8)
            | ((data[index + 2] & 0xFF) << 16)
            | ((data[index + 3] & 0xFF) << 24);
    index += 4;
    return v;
  }

  public long readLong() {
    require(8);
    long v = 0;
    for (int i = 0; i < 8; i++) {
      v |= ((long) (data[index + i] & 0xFF)) << (i * 8);
    }
    index += 8;
    return v;
  }

  public int readVarInt() {
    int result = 0;
    int shift = 0;
    while (true) {
      int b = readByte();
      result |= (b & 0x7F) << shift;
      if ((b & 0x80) == 0) return result;
      shift += 7;
      if (shift >= 32) throw new MalformedPacketException("varint overflow");
    }
  }

  public String readString() {
    int length = readVarInt();
    if (length < 0 || length > ProtocolConstants.MAX_STRING_LENGTH) {
      throw new MalformedPacketException("invalid string length: " + length);
    }
    require(length);
    String s = new String(data, index, length, StandardCharsets.UTF_8);
    index += length;
    return s;
  }

  public byte[] readBytes() {
    int length = readVarInt();
    if (length < 0 || length > ProtocolConstants.MAX_PACKET_BYTES) {
      throw new MalformedPacketException("invalid bytes length: " + length);
    }
    require(length);
    byte[] bytes = new byte[length];
    System.arraycopy(data, index, bytes, 0, length);
    index += length;
    return bytes;
  }

  /** 未知字段：按长度跳过，实现“小版本未知字段跳过”。 */
  public void skipUnknown() {
    int length = readVarInt();
    if (length < 0 || length > remaining()) {
      throw new MalformedPacketException("invalid unknown field length");
    }
    index += length;
  }
}
