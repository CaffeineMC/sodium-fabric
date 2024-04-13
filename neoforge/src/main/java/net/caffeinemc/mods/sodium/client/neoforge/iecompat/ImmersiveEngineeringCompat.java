package net.caffeinemc.mods.sodium.client.neoforge.iecompat;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.core.SectionPos;
import net.neoforged.fml.loading.FMLLoader;

public class ImmersiveEngineeringCompat {
    public static boolean isLoaded = FMLLoader.getLoadingModList().getModFileById("immersiveengineering") != null;

    static {
        if (isLoaded) {
            System.out.println("[Sodium] Found Immersive Engineering.");
        }
    }

    public static void renderConnectionsInSection(ChunkBuildBuffers buffers, LevelSlice worldSlice, SectionPos position) {
        if (isLoaded) {
            SodiumConnectionRenderer.renderConnectionsInSection(buffers, worldSlice, position);
        }
    }

    public static boolean sectionNeedsRendering(SectionPos position) {
        return SodiumConnectionRenderer.sectionNeedsRendering(position);
    }
}
