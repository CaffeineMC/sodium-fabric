package net.caffeinemc.mods.sodium.fabric.texture;

import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.util.Collection;
import java.util.List;

/**
 * Caches {@link SpriteFinder}s for maximum efficiency. They must be refreshed after each resource reload.
 *
 * <p><b>This class should not be used during a resource reload</b>, as returned SpriteFinders may be null or outdated.
 */
public class SpriteFinderCache {
    private static SpriteFinder blockAtlasSpriteFinder;

    public static SpriteFinder forBlockAtlas() {
        return blockAtlasSpriteFinder;
    }

<<<<<<<< HEAD:common/src/main/java/net/caffeinemc/mods/sodium/client/render/frapi/SpriteFinderCache.java
    public static class ReloadListener implements SimpleSynchronousResourceReloadListener {
========
    public static class ReloadListener implements ResourceManagerReloadListener, IdentifiableResourceReloadListener {
>>>>>>>> ml:fabric/src/main/java/net/caffeinemc/mods/sodium/fabric/texture/SpriteFinderCache.java
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("sodium", "sprite_finder_cache");
        public static final List<ResourceLocation> DEPENDENCIES = List.of(ResourceReloadListenerKeys.MODELS);
        public static final ReloadListener INSTANCE = new ReloadListener();

        private ReloadListener() {
        }

        // BakedModelManager#getAtlas only returns correct results after the BakedModelManager is done reloading
        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            ModelManager modelManager = Minecraft.getInstance().getModelManager();
            blockAtlasSpriteFinder = SpriteFinder.get(modelManager.getAtlas(TextureAtlas.LOCATION_BLOCKS));
        }

        @Override
        public ResourceLocation getFabricId() {
            return ID;
        }

        @Override
        public Collection<ResourceLocation> getFabricDependencies() {
            return DEPENDENCIES;
        }
    }
}
