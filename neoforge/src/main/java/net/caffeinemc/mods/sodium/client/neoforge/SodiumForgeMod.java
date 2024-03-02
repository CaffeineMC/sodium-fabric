package net.caffeinemc.mods.sodium.client.neoforge;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

@Mod("sodium")
public class SodiumForgeMod {
    public SodiumForgeMod(IEventBus bus) {
        SodiumClientMod.onInitialization(ModList.get().getModContainerById("sodium").get().getModInfo().getVersion().toString());
        bus.addListener(this::onResourceReload);
    }

    public void onResourceReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(SpriteFinderCache.ReloadListener.INSTANCE);
    }
}