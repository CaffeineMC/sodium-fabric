package me.jellysquid.mods.sodium.api;

import me.jellysquid.mods.sodium.api.world.ISodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.render.WorldRenderer;

public class SodiumApiImpl implements SodiumApi {
    public static void install() {
        SodiumApi.Impl.instance = new SodiumApiImpl();
    }

    public ISodiumWorldRenderer getWorldRenderer(WorldRenderer worldRenderer) {
        return SodiumWorldRenderer.getInstance().getApiImpl();
    }
}
