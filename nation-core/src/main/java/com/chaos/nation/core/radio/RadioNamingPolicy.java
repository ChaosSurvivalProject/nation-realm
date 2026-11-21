package com.chaos.nation.core.radio;

import java.util.Locale;
import java.util.Set;

/**
 * 呼号 / 频段命名合规校验：类比"现实国家/地区重名拒绝"（见 NationNameRegistry）。
 *
 * <p>禁用词表仅做名称层面的合规拦截：真实国家/地区名、紧急呼救词不允许作为自定义呼号。 清单可由运营扩充；本类只做技术拦截，不代表政治立场。
 */
public final class RadioNamingPolicy {
  public record Result(boolean allowed, String reason) {}

  /** 不允许用作自定义呼号的词（中文 + 英文，大小写不敏感）。 */
  private static final Set<String> FORBIDDEN =
      Set.of(
          // 现实国家/地区（示例，运营可扩充）
          "中国",
          "美国",
          "日本",
          "德国",
          "法国",
          "英国",
          "俄罗斯",
          "china",
          "usa",
          "united states",
          "japan",
          "germany",
          "france",
          "uk",
          "russia",
          // 紧急 / 遇险呼号，防止冒充
          "mayday",
          "sos",
          "distress",
          "emergency",
          "police",
          "112",
          "911");

  private RadioNamingPolicy() {}

  /** 校验自定义呼号；null/空白拒绝，超长拒绝，禁用词拒绝。 */
  public static Result validateCallsign(String raw) {
    if (raw == null || raw.isBlank()) {
      return new Result(false, "呼号不能为空");
    }
    String normalized = normalize(raw);
    if (normalized.length() > 24) {
      return new Result(false, "呼号过长（最多 24 字符）");
    }
    if (FORBIDDEN.contains(normalized)) {
      return new Result(false, "该呼号为现实国家/地区或紧急呼号，为符合相关规定，不可使用");
    }
    return new Result(true, "ok");
  }

  /** 归一化：去首尾空白、折叠中间空白、转小写、全角字母数字转半角。 */
  static String normalize(String raw) {
    StringBuilder sb = new StringBuilder(raw.length());
    for (char c : raw.trim().toCharArray()) {
      char mapped = c;
      if (c >= 0xFF01 && c <= 0xFF5E) mapped = (char) (c - 0xFEE0); // 全角 -> 半角
      if (mapped == '\u3000') mapped = ' '; // 全角空格
      if (Character.isWhitespace(mapped)) {
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') sb.append(' ');
      } else {
        sb.append(mapped);
      }
    }
    return sb.toString().toLowerCase(Locale.ROOT);
  }
}
