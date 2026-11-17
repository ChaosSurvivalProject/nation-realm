package com.chaos.nation.client;

import java.util.HashMap;
import java.util.Map;

/**
 * 地图着色：三色邻接着色，冲突时加第四色；叠加纹理/图案支持色盲玩家。
 * 纯 Java，可在无 Minecraft 环境下单元测试。
 */
public final class MapRenderer {
    public enum ViewMode { TWO_D, THREE_D }

    /** 返回区块的显示颜色（RGB）。unknown 用中性灰，不能用颜色单独区分归属。 */
    public int colorFor(int ownerNationId, boolean explored, boolean colorBlind, Map<Long,Integer> nationColors) {
        if (!explored || ownerNationId == 0) return 0x4A4A4A; // 未知区域中性灰
        int base = nationColors.getOrDefault((long) ownerNationId, 0x888888);
        return colorBlind ? shiftForColorBlind(base, ownerNationId) : base;
    }

    /**
     * 色盲模式：除颜色外叠加按国家 ID 派生的亮度偏移与图案编号，
     * 保证不只靠色相区分。返回图案编号（0-3）供绘制层叠加纹理。
     */
    public int patternFor(int ownerNationId, boolean colorBlind) {
        if (ownerNationId == 0) return 0;
        return colorBlind ? Math.abs(ownerNationId) % 4 : 0;
    }

    private int shiftForColorBlind(int rgb, int nationId) {
        // 在色盲模式下用明度而非仅色相区分，实际绘制再叠加 patternFor 的图案。
        int delta = (Math.abs(nationId) % 4) * 0x10;
        return clamp(rgb + delta);
    }

    private int clamp(int v) {
        return Math.max(0, Math.min(0xFFFFFF, v));
    }

    /** 三色着色：回溯相邻区块，若前三色都被占用则用第四色。 */
    public int colorWithNeighborConstraint(int[] neighborColors, Map<Long,Integer> nationColors, int myNationId) {
        boolean[] used = new boolean[5];
        for (int c : neighborColors) {
            for (int i = 1; i <= 3; i++) {
                if (c == palette(i)) used[i] = true;
            }
        }
        for (int i = 1; i <= 3; i++) {
            if (!used[i]) return palette(i);
        }
        return palette(4); // 数学上三色不总能满足，冲突时第四色 + 边界/纹理区分
    }

    private int palette(int i) {
        return switch (i) {
            case 1 -> 0xE06C6C;
            case 2 -> 0x6CA8E0;
            case 3 -> 0x8FD46C;
            default -> 0xD4C86C;
        };
    }
}
