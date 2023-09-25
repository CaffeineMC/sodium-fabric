package me.jellysquid.mods.sodium.client.render.particle;

import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkNibbleArray;
import org.jetbrains.annotations.NotNull;

public class ParticleRenderView {
    private static final int CACHE_SIZE = 2048;

    private final Long2ReferenceLinkedOpenHashMap<LightArrays> lightCache = new Long2ReferenceLinkedOpenHashMap<>(CACHE_SIZE);
    private final World world;

    public ParticleRenderView(World world) {
        this.world = world;
    }

    public int getBrightness(int x, int y, int z) {
        var arrays = this.getCachedLightArrays(x >> 4, y >> 4, z >> 4);

        int skyLight = getLightValue(arrays.skyLight, x, y, z);
        int blockLight = getLightValue(arrays.blockLight, x, y, z);

        return LightmapTextureManager.pack(blockLight, skyLight);
    }

    private static int getLightValue(ChunkNibbleArray array, int x, int y, int z) {
        return array == null ? 0 : array.get(x & 15, y & 15, z & 15);
    }

    private LightArrays getCachedLightArrays(int x, int y, int z) {
        var position = ChunkSectionPos.asLong(x, y, z);

        LightArrays entry = this.lightCache.get(position);

        if (entry == null) {
            entry = this.fetchLightArrays(x, y, z, position);
        }

        return entry;
    }

    private ParticleRenderView.@NotNull LightArrays fetchLightArrays(int x, int y, int z, long packed) {
        LightArrays arrays = LightArrays.load(this.world, ChunkSectionPos.from(x, y, z));

        if (this.lightCache.size() >= CACHE_SIZE) {
            this.lightCache.removeLast();
        }

        this.lightCache.putAndMoveToFirst(packed, arrays);

        return arrays;
    }

    public void resetCache() {
        this.lightCache.clear();
    }

    private static class LightArrays {
        private final ChunkNibbleArray blockLight;
        private final ChunkNibbleArray skyLight;

        private LightArrays(ChunkNibbleArray blockLight, ChunkNibbleArray skyLight) {
            this.blockLight = blockLight;
            this.skyLight = skyLight;
        }

        public static LightArrays load(World world, ChunkSectionPos pos) {
            var lightingProvider = world.getLightingProvider();

            var blockLight = lightingProvider.get(LightType.BLOCK)
                    .getLightSection(pos);

            var skyLight = lightingProvider.get(LightType.SKY)
                    .getLightSection(pos);

            return new LightArrays(blockLight, skyLight);
        }
    }
}
