package me.jellysquid.mods.sodium.mixin.core;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.interop.vanilla.mixin.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Triple;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

@Mixin(RenderStateShard.class)
public class MixinRenderPhase {
    @Mixin(RenderStateShard.EmptyTextureStateShard.class)
    public static class MixinTextureBase implements ShaderTextureProvider {
        @Override
        public List<ShaderTexture> getTextures() {
            return List.of();
        }
    }

    @Mixin(RenderStateShard.MultiTextureStateShard.class)
    public static class MixinTextures implements ShaderTextureProvider {
        private List<Triple<ResourceLocation, Boolean, Boolean>> textures;

        @Inject(method = "<init>", at = @At("RETURN"))
        private void init(ImmutableList<Triple<ResourceLocation, Boolean, Boolean>> textures, CallbackInfo ci) {
            this.textures = textures;
        }

        @Override
        public List<ShaderTexture> getTextures() {
            var textureManager = Minecraft.getInstance().getTextureManager();
            var list = new ArrayList<ShaderTexture>();

            for (int target = 0; target < this.textures.size(); target++) {
                var params = this.textures.get(target);

                IntSupplier texture = () -> textureManager.getTexture(params.getLeft()).getId();
                var blur = params.getMiddle();
                var mipmap = params.getRight();

                list.add(new ShaderTexture(target, texture, new ShaderTextureParameters(blur, mipmap)));
            }

            return list;
        }
    }

    @Mixin(RenderStateShard.TextureStateShard.class)
    public static class MixinTexture implements ShaderTextureProvider {
        @Shadow
        @Final
        private Optional<ResourceLocation> texture;

        @Shadow
        @Final
        private boolean blur;

        @Shadow
        @Final
        private boolean mipmap;

        @Override
        public List<ShaderTexture> getTextures() {
            var textureManager = Minecraft.getInstance().getTextureManager();
            IntSupplier texture = () -> textureManager.getTexture(this.texture.get()).getId();

            return List.of(new ShaderTexture(0, texture, new ShaderTextureParameters(this.blur, this.mipmap)));
        }
    }

    @Mixin(RenderStateShard.OverlayStateShard.class)
    public static class MixinOverlay implements ShaderTextureProvider {
        private static int getOverlayTextureId() {
            return Minecraft.getInstance().gameRenderer.overlayTexture().texture.getId();
        }

        @Override
        public List<ShaderTexture> getTextures() {
            return List.of(new ShaderTexture(
                    1, MixinOverlay::getOverlayTextureId, new ShaderTextureParameters(true, false)
            ));
        }
    }

    @Mixin(RenderStateShard.LightmapStateShard.class)
    public static class MixinLightmap implements ShaderTextureProvider {

        private static int getLightmapTextureId() {
            Minecraft client = Minecraft.getInstance();
            LightmapTextureManagerAccessor lightmapTextureManager =
                    ((LightmapTextureManagerAccessor) client.gameRenderer.lightTexture());

            return lightmapTextureManager.getTexture().getId();
        }

        @Override
        public List<ShaderTexture> getTextures() {
            return List.of(new ShaderTexture(
                    2, MixinLightmap::getLightmapTextureId, new ShaderTextureParameters(true, false)
            ));
        }
    }

    @Mixin(RenderStateShard.ShaderStateShard.class)
    public static class MixinShader implements RenderPhaseShaderAccess {
        @Shadow
        @Final
        private Optional<Supplier<ShaderInstance>> shader;

        @Override
        public Supplier<ShaderInstance> getShader() {
            return this.shader.orElse(null);
        }
    }
}
