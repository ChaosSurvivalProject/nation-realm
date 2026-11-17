package com.chaos.nation.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 客户端网络处理器：握手 + 订阅 + 快照/差分应用。
 * 客户端绝不拥有规则决定权；操作请求只发意图，服务端再次验证。
 */
public final class ClientNetworkHandler {
    public static final Identifier CHANNEL_ID = Identifier.of("nationrealm", "data");
    /** 1.21.1 Fabric API 使用 Id<CustomPayload> 作为通道键。 */
    public static final CustomPayload.Id<CustomPayload> CHANNEL = new CustomPayload.Id<>(CHANNEL_ID);

    private final MapDataStore store;
    private int protocolMajor = 1;
    private int protocolMinor = 0;
    private boolean highDetailEnabled = false;

    public ClientNetworkHandler(MapDataStore store) {
        this.store = store;
    }

    /**
     * 处理握手响应：主版本不兼容时拒绝启用高级界面并标记需要提示玩家；
     * 小版本差异尽力兼容。不静默出错。
     */
    public void onHandshake(boolean accepted, int serverMajor, int serverMinor, String serverBuild) {
        if (!accepted || serverMajor != protocolMajor) {
            highDetailEnabled = false;
            store.setStatus("协议不兼容：需要主版本 " + protocolMajor
                    + "，服务端 " + serverMajor + "。高级地图已禁用。");
            return;
        }
        protocolMinor = Math.min(protocolMinor, serverMinor);
        highDetailEnabled = true;
        store.setStatus("已连接（协议 " + protocolMajor + "." + protocolMinor + "）");
    }

    public boolean highDetailEnabled() {
        return highDetailEnabled;
    }

    /** 应用区域快照；按 nationDataVersion 去重，重复包幂等。 */
    public void applySnapshot(MapDataStore.RegionSnapshotLike snapshot) {
        if (!highDetailEnabled) return;
        if (snapshot.version() <= store.versionOf(snapshot.regionX(), snapshot.regionZ())) {
            return; // 旧包或重复包，丢弃
        }
        store.applySnapshot(snapshot);
    }

    public void register() {
        // 真实 payload 注册见 PROTOCOL.md；此处仅示意通道注册点，避免空壳。
        // PayloadTypeRegistry.playS2C().register(...)
        ClientPlayNetworking.registerGlobalReceiver(CHANNEL, (payload, context) -> {
            // 业务解码与分发在解码器完成后调用本类方法。
        });
    }
}
