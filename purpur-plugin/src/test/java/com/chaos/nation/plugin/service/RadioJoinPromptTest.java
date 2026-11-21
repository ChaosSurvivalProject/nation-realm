package com.chaos.nation.plugin.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaos.nation.plugin.config.PluginConfig.RadioSettings;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class RadioJoinPromptTest {
  private static RadioSettings settings(String url) {
    return new RadioSettings(true, url, false, "FACTION", 256, 12, 6, 30, 600);
  }

  @Test
  void message_withUrl_isClickableAndExplainsOptional() {
    RadioJoinPrompt prompt =
        new RadioJoinPrompt(settings("https://example.org/releases/nation-realm-fabric"));
    Component msg = prompt.buildMessage();
    String flat = plain(msg);
    assertThat(flat).contains("可选安装");
    assertThat(flat).contains("不影响进服");
    assertThat(flat).contains("下载页");
    // 含 click event（open_url）
    assertThat(hasClickEvent(msg)).isTrue();
  }

  @Test
  void message_withoutUrl_hasNoLinkAndMarksPending() {
    RadioJoinPrompt prompt = new RadioJoinPrompt(settings(""));
    Component msg = prompt.buildMessage();
    String flat = plain(msg);
    assertThat(flat).contains("待运营补齐");
    assertThat(flat).doesNotContain("http");
    assertThat(hasClickEvent(msg)).isFalse();
  }

  // ---- 测试辅助：递归展开组件文本与 click event（不依赖 Minecraft 服务端）----

  private static String plain(Component c) {
    StringBuilder sb = new StringBuilder();
    collect(c, sb, new boolean[1]);
    return sb.toString();
  }

  private static void collect(Component c, StringBuilder sb, boolean[] hasClick) {
    if (c.clickEvent() != null) hasClick[0] = true;
    if (c instanceof net.kyori.adventure.text.TextComponent tc) {
      sb.append(tc.content());
    }
    for (Component child : c.children()) collect(child, sb, hasClick);
  }

  private static boolean hasClickEvent(Component c) {
    boolean[] flag = new boolean[1];
    collect(c, new StringBuilder(), flag);
    return flag[0];
  }
}
