package com.chaos.nation.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Fabric 客户端入口：仅客户端侧，打开地图 GUI，无规则决定权。 */
public final class NationRealmClient implements ClientModInitializer {
    private final MapDataStore store = new MapDataStore();
    private final ClientNetworkHandler network = new ClientNetworkHandler(store);
    public static final KeyBinding OPEN_MAP = new KeyBinding(
            "key.nationrealm.open_map", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, "category.nationrealm");

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(OPEN_MAP);
        network.register();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> store.setStatus("已断开"));
        System.out.println("[NationRealm] 客户端地图模组已加载（客户端侧）");
    }

    public MapDataStore store() { return store; }
    public ClientNetworkHandler network() { return network; }
}
