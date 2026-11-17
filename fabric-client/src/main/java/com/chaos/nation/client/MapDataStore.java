package com.chaos.nation.client;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端地图数据存储：持有服务器下发的快照/差分，按版本去重。
 * 数据只在快照/差分到达时更新，渲染层只读，不每帧解析全部数据。
 */
public final class MapDataStore {
    /** 区域快照的最小接口，屏蔽具体网络 record。 */
    public interface RegionSnapshotLike {
        int regionX();
        int regionZ();
        long version();
        int[] owners();
    }

    private final Map<Long, RegionSnapshotLike> regions = new HashMap<>();
    private String status = "未连接";
    private boolean colorBlindMode;
    private boolean reducedMotion;
    private int zoom = 1;

    public void setStatus(String status) { this.status = status; }
    public String status() { return status; }

    public long versionOf(int regionX, int regionZ) {
        RegionSnapshotLike r = regions.get(key(regionX, regionZ));
        return r == null ? -1 : r.version();
    }

    public void applySnapshot(RegionSnapshotLike snapshot) {
        regions.put(key(snapshot.regionX(), snapshot.regionZ()), snapshot);
    }

    public RegionSnapshotLike region(int regionX, int regionZ) {
        return regions.get(key(regionX, regionZ));
    }

    public int loadedRegionCount() { return regions.size(); }

    private static long key(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xFFFFFFFFL);
    }

    public boolean colorBlindMode() { return colorBlindMode; }
    public void colorBlindMode(boolean v) { this.colorBlindMode = v; }
    public boolean reducedMotion() { return reducedMotion; }
    public void reducedMotion(boolean v) { this.reducedMotion = v; }
    public int zoom() { return zoom; }
    public void zoom(int v) { this.zoom = Math.max(1, Math.min(5, v)); }
}
