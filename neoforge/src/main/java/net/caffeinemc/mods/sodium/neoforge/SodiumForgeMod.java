package net.caffeinemc.mods.sodium.neoforge;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gui.SodiumOptionsGUI;
import net.caffeinemc.mods.sodium.client.render.frapi.SodiumRenderer;
import net.caffeinemc.mods.sodium.client.render.texture.SpriteFinderCache;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod("sodium")
public class SodiumForgeMod {
    public SodiumForgeMod(IEventBus bus, ModContainer modContainer) {
        SodiumClientMod.onInitialization(ModList.get().getModContainerById("sodium").orElseThrow().getModInfo().getVersion().toString());
        bus.addListener(this::onResourceReload);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (minecraft, screen) -> SodiumOptionsGUI.createScreen(screen));
        RendererAccess.INSTANCE.registerRenderer(SodiumRenderer.INSTANCE);
    }

    public void onResourceReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(SpriteFinderCache.ReloadListener.INSTANCE);
    }
}