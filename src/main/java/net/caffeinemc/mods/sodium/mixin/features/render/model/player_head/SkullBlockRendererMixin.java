package net.caffeinemc.mods.sodium.mixin.features.render.model.player_head;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SkullBlock;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

import java.time.Duration;
import java.util.Map;

@Mixin(SkullBlockRenderer.class)
public class SkullBlockRendererMixin {

    @Shadow
    @Final
    private static Map<SkullBlock.Type, ResourceLocation> SKIN_BY_TYPE;
    @Unique
    private static final Map<SkullBlock.Type, RenderType> SKULLS_RENDER_TYPE = Util.make(Maps.newHashMap(), (hashMap) -> {
        for (SkullBlock.Type type : SKIN_BY_TYPE.keySet()) {
            hashMap.put(type, RenderType.entityCutoutNoCullZOffset(SKIN_BY_TYPE.get(type)));
        }
    });

    // Use the PropertyMap as the key instead of the UUID since there can be many skins with the same UUID,
    // for example, a head with a previous skin and the current one from the same player.
    // Instead, in the PropertyMap, the skin's URL is stored.
    @Unique
    private static final Cache<PropertyMap, RenderType> RENDER_TYPE_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMillis(500L)).build();

    /**
     * @author Zailer43
     * @reason Adds cache to the RenderType as it is CPU expensive
     * and minecraft gets it all the time inside the render method
     */
    @Overwrite
    public static RenderType getRenderType(SkullBlock.Type type, @Nullable GameProfile profile) {
        if (type == SkullBlock.Types.PLAYER && profile != null) {
            PropertyMap profileProperties = profile.getProperties();
            RenderType renderType = RENDER_TYPE_CACHE.getIfPresent(profileProperties);
            if (renderType != null) {
                return renderType;
            }

            SkinManager skinManager = Minecraft.getInstance().getSkinManager();
            ResourceLocation skin = skinManager.getInsecureSkin(profile).texture();
            renderType = RenderType.entityTranslucent(skin);

            // before the skin is loaded a default skin is used, we do not want to cache that
            if (skin != DefaultPlayerSkin.get(profile).texture()) {
                RENDER_TYPE_CACHE.put(profileProperties, renderType);
            }

            return renderType;
        }

        return SKULLS_RENDER_TYPE.get(type);
    }

}
