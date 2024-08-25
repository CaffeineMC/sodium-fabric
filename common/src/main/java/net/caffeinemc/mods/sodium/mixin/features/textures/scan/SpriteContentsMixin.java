package net.caffeinemc.mods.sodium.mixin.features.textures.scan;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.NativeImage;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.SpriteContentsExtension;
import net.caffeinemc.mods.sodium.client.util.NativeImageHelper;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.lwjgl.system.MemoryUtil;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

/**
 * This mixin scans a {@link SpriteContents} for transparent and translucent pixels. This information is later used during mesh generation to reassign the render pass to either cutout if the sprite has no translucent pixels or solid if it doesn't even have any transparent pixels.
 *
 * @author douira
 */
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

    /*
     * Uses a WrapOperation here since Inject doesn't work on 1.20.1 forge.
     */
    @WrapOperation(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/texture/SpriteContents;originalImage:Lcom/mojang/blaze3d/platform/NativeImage;", opcode = Opcodes.PUTFIELD))
    private void sodium$beforeGenerateMipLevels(SpriteContents instance, NativeImage nativeImage, Operation<Void> original) {
        scanSpriteContents(nativeImage);

        original.call(instance, nativeImage);
    }

    @Unique
    private void scanSpriteContents(NativeImage nativeImage) {
        final long ppPixel = NativeImageHelper.getPointerRGBA(nativeImage);
        final int pixelCount = nativeImage.getHeight() * nativeImage.getWidth();

        for (int pixelIndex = 0; pixelIndex < pixelCount; pixelIndex++) {
            int color = MemoryUtil.memGetInt(ppPixel + (pixelIndex * 4L));
            int alpha = ColorABGR.unpackAlpha(color);

            // 25 is used as the threshold since the alpha cutoff is 0.1
            if (alpha <= 25) { // 0.1 * 255
                this.sodium$hasTransparentPixels = true;
            } else if (alpha < 255) {
                this.sodium$hasTranslucentPixels = true;
            }
        }

        // the image contains transparency also if there are translucent pixels,
        // since translucent pixels prevent a downgrade to the opaque render pass just as transparent pixels do
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
