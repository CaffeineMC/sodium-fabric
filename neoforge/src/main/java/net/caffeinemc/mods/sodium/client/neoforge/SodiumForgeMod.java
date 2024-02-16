package net.caffeinemc.mods.sodium.client.neoforge;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.minecraft.server.packs.PackType;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;

@Mod("sodium")
public class SodiumForgeMod {
    public SodiumForgeMod() {
        SodiumClientMod.onInitialization(ModList.get().getModContainerById("sodium").get().getModInfo().getVersion().toString());

        //ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(SpriteFinderCache.ReloadListener.INSTANCE);
    }
}
