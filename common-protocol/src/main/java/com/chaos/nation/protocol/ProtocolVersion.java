package com.chaos.nation.protocol;

import java.util.Objects;

/** 协议版本三元组：主版本 / 次版本 / 模组版本。 主版本不同 -> 不兼容；主版本相同、次版本不同 -> 尽力兼容（未知字段跳过）。 */
public final class ProtocolVersion {
  private final int major;
  private final int minor;
  private final String modVersion;
  private final String serverBuild;

  public ProtocolVersion(int major, int minor, String modVersion, String serverBuild) {
    this.major = major;
    this.minor = minor;
    this.modVersion = Objects.requireNonNull(modVersion, "modVersion");
    this.serverBuild = serverBuild == null ? "" : serverBuild;
  }

  public int major() {
    return major;
  }

  public int minor() {
    return minor;
  }

  public String modVersion() {
    return modVersion;
  }

  public String serverBuild() {
    return serverBuild;
  }

  /** 主版本必须完全一致，否则拒绝启用高级界面。 */
  public boolean compatibleWith(ProtocolVersion other) {
    return other != null && this.major == other.major;
  }

  /** 次版本更高的一方可以安全地向更低一方发送（未知字段会被跳过）。 */
  public boolean canSendTo(ProtocolVersion peer) {
    return compatibleWith(peer) && this.minor <= peer.minor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ProtocolVersion)) return false;
    ProtocolVersion that = (ProtocolVersion) o;
    return major == that.major
        && minor == that.minor
        && modVersion.equals(that.modVersion)
        && serverBuild.equals(that.serverBuild);
  }

  @Override
  public int hashCode() {
    return Objects.hash(major, minor, modVersion, serverBuild);
  }

  @Override
  public String toString() {
    return "ProtocolVersion{major="
        + major
        + ", minor="
        + minor
        + ", mod='"
        + modVersion
        + "', build='"
        + serverBuild
        + "'}";
  }
}
