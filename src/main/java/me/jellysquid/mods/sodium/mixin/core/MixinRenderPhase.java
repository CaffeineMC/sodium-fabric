package me.jellysquid.mods.sodium.mixin.core;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.interop.vanilla.mixin.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.Shader;
import net.minecraft.util.Identifier;
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

@Mixin(RenderPhase.class)
public class MixinRenderPhase {
    @Mixin(RenderPhase.TextureBase.class)
    public static class MixinTextureBase implements ShaderTextureProvider {
        @Override
        public List<ShaderTexture> getTextures() {
            return List.of();
        }
    }

    @Mixin(RenderPhase.Textures.class)
    public static class MixinTextures implements ShaderTextureProvider {
        private List<Triple<Identifier, Boolean, Boolean>> textures;

        @Inject(method = "<init>", at = @At("RETURN"))
        private void init(ImmutableList<Triple<Identifier, Boolean, Boolean>> textures, CallbackInfo ci) {
            this.textures = textures;
        }

        @Override
        public List<ShaderTexture> getTextures() {
            var textureManager = MinecraftClient.getInstance().getTextureManager();
            var list = new ArrayList<ShaderTexture>();

            for (int target = 0; target < this.textures.size(); target++) {
                var params = this.textures.get(target);

                IntSupplier texture = () -> textureManager.getTexture(params.getLeft()).getGlId();
                var blur = params.getMiddle();
                var mipmap = params.getRight();

                list.add(new ShaderTexture(target, texture, new ShaderTextureParameters(blur, mipmap)));
            }

            return list;
        }
    }

    @Mixin(RenderPhase.Texture.class)
    public static class MixinTexture implements ShaderTextureProvider {
        @Shadow
        @Final
        private Optional<Identifier> id;

        @Shadow
        @Final
        private boolean blur;

        @Shadow
        @Final
        private boolean mipmap;

        @Override
        public List<ShaderTexture> getTextures() {
            var textureManager = MinecraftClient.getInstance().getTextureManager();
            IntSupplier texture = () -> textureManager.getTexture(this.id.get()).getGlId();

            return List.of(new ShaderTexture(0, texture, new ShaderTextureParameters(this.blur, this.mipmap)));
        }
    }

    @Mixin(RenderPhase.Overlay.class)
    public static class MixinOverlay implements ShaderTextureProvider {
        private static int getOverlayTextureId() {
            return MinecraftClient.getInstance().gameRenderer.getOverlayTexture().texture.getGlId();
        }

        @Override
        public List<ShaderTexture> getTextures() {
            return List.of(new ShaderTexture(
                    1, MixinOverlay::getOverlayTextureId, new ShaderTextureParameters(true, false)
            ));
        }
    }

    @Mixin(RenderPhase.Lightmap.class)
    public static class MixinLightmap implements ShaderTextureProvider {

        private static int getLightmapTextureId() {
            MinecraftClient client = MinecraftClient.getInstance();
            LightmapTextureManagerAccessor lightmapTextureManager =
                    ((LightmapTextureManagerAccessor) client.gameRenderer.getLightmapTextureManager());

            return lightmapTextureManager.getTexture().getGlId();
        }

        @Override
        public List<ShaderTexture> getTextures() {
            return List.of(new ShaderTexture(
                    2, MixinLightmap::getLightmapTextureId, new ShaderTextureParameters(true, false)
            ));
        }
    }

    @Mixin(RenderPhase.Shader.class)
    public static class MixinShader implements RenderPhaseShaderAccess {
        @Shadow
        @Final
        private Optional<Supplier<Shader>> supplier;

        @Override
        public Supplier<Shader> getShader() {
            return this.supplier.orElse(null);
        }
    }
}
