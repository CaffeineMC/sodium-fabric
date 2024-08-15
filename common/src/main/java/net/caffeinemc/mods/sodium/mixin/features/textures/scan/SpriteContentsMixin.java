package net.caffeinemc.mods.sodium.mixin.features.textures.scan;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.NativeImage;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.SpriteContentsExtension;
import net.caffeinemc.mods.sodium.client.util.NativeImageHelper;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.util.FastColor;
import org.lwjgl.system.MemoryUtil;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SpriteContents.class)
public class SpriteContentsMixin implements SpriteContentsExtension {
    @Mutable
    @Shadow
    @Final
    private NativeImage originalImage;

    @Unique
    public boolean sodium$hasTransparentPixels = false;

    @Unique
    public boolean sodium$hasTranslucentPixels = false;

    @WrapOperation(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/texture/SpriteContents;originalImage:Lcom/mojang/blaze3d/platform/NativeImage;", opcode = Opcodes.PUTFIELD))
    private void sodium$beforeGenerateMipLevels(SpriteContents instance, NativeImage nativeImage, Operation<Void> original) {
        sodium$scanSpriteContents(nativeImage);

        original.call(instance, nativeImage);
    }

    @Unique
    private void sodium$scanSpriteContents(NativeImage nativeImage) {
        final long ppPixel = NativeImageHelper.getPointerRGBA(nativeImage);
        final int pixelCount = nativeImage.getHeight() * nativeImage.getWidth();

        for (int pixelIndex = 0; pixelIndex < pixelCount; pixelIndex++) {
            long pPixel = ppPixel + (pixelIndex * 4L);

            int color = MemoryUtil.memGetInt(pPixel);
            int alpha = FastColor.ABGR32.alpha(color);

            // track if this image has transparent or even translucent pixels
            if (alpha == 0) {
                this.sodium$hasTransparentPixels = true;
            } else if (alpha < 255) {
                this.sodium$hasTranslucentPixels = true;
            }
        }

        this.sodium$hasTransparentPixels |= this.sodium$hasTranslucentPixels;
    }

    @Override
    public boolean sodium$hasTransparentPixels() {
        return this.sodium$hasTransparentPixels;
    }

    @Override
    public boolean sodium$hasTranslucentPixels() {
        return this.sodium$hasTranslucentPixels;
    }
}
