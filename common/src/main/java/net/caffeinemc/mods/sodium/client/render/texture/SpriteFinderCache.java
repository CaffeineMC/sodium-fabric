package net.caffeinemc.mods.sodium.client.render.texture;

import net.caffeinemc.mods.sodium.mixin.core.render.texture.TextureAtlasAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * Caches {@link BlockSpriteFinder}s for maximum efficiency. They must be refreshed after each resource reload.
 *
 * <p><b>This class should not be used during a resource reload</b>, as returned SpriteFinders may be null or outdated.
 */
public class SpriteFinderCache {
    private static BlockSpriteFinder blockAtlasSpriteFinder;

    public static BlockSpriteFinder forBlockAtlas() {
        return blockAtlasSpriteFinder;
    }

    public static class ReloadListener implements ResourceManagerReloadListener {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("sodium", "sprite_finder_cache");
        public static final ReloadListener INSTANCE = new ReloadListener();

        private ReloadListener() {
        }

        // BakedModelManager#getAtlas only returns correct results after the BakedModelManager is done reloading
        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            ModelManager modelManager = Minecraft.getInstance().getModelManager();
            TextureAtlas atlas = modelManager.getAtlas(TextureAtlas.LOCATION_BLOCKS);
            blockAtlasSpriteFinder = new BlockSpriteFinder(((TextureAtlasAccessor) atlas).getTexturesByName(), atlas);
        }
    }
}
