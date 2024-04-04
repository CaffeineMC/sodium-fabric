package net.caffeinemc.mods.sodium.client.fabric;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.render.frapi.SodiumRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.packs.PackType;

public class SodiumFabricMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModContainer mod = FabricLoader.getInstance()
                .getModContainer("sodium")
                .orElseThrow(NullPointerException::new);

        SodiumClientMod.onInitialization(mod.getMetadata().getVersion().getFriendlyString());

        FlawlessFrames.onClientInitialization();

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(SpriteFinderCache.ReloadListener.INSTANCE);
        RendererAccess.INSTANCE.registerRenderer(SodiumRenderer.INSTANCE);
    }
}
