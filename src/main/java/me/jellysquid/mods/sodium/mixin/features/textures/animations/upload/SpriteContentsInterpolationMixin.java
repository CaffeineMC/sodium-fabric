package me.jellysquid.mods.sodium.mixin.features.textures.animations.upload;

import me.jellysquid.mods.sodium.client.util.NativeImageHelper;
import me.jellysquid.mods.sodium.mixin.features.textures.SpriteContentsInvoker;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteContents;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SpriteContents.Interpolation.class)
public class SpriteContentsInterpolationMixin {
    @Shadow
    @Final
    private NativeImage[] images;

    @Unique
    private SpriteContents parent;

    @Unique
    private static final int STRIDE = 4;

    /**
     * @author IMS
     * @reason Replace fragile Shadow
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    public void assignParent(SpriteContents parent, CallbackInfo ci) {
        this.parent = parent;
    }

    /**
     * @author JellySquid
     * @reason Drastic optimizations
     */
    @Overwrite
    void apply(int x, int y, SpriteContents.AnimatorImpl arg) {
        SpriteContents.Animation animation = ((SpriteContentsAnimatorImplAccessor) arg).getAnimation();
        SpriteContentsAnimationAccessor animation2 = (SpriteContentsAnimationAccessor) ((SpriteContentsAnimatorImplAccessor) arg).getAnimation();
        List<SpriteContents.AnimationFrame> frames = ((SpriteContentsAnimationAccessor) animation).getFrames();
        SpriteContentsAnimatorImplAccessor accessor = (SpriteContentsAnimatorImplAccessor) arg;
        SpriteContentsAnimationFrameAccessor animationFrame = (SpriteContentsAnimationFrameAccessor) frames.get(accessor.getFrameIndex());

        int curIndex = animationFrame.getIndex();
        int nextIndex = ((SpriteContentsAnimationFrameAccessor) animation2.getFrames().get((accessor.getFrameIndex() + 1) % frames.size())).getIndex();

        if (curIndex == nextIndex) {
            return;
        }

        // The mix factor between the current and next frame
        float mix = 1.0F - (float) accessor.getFrameTicks() / (float) animationFrame.getTime();

        for (int layer = 0; layer < this.images.length; layer++) {
            int width = this.parent.getWidth() >> layer;
            int height = this.parent.getHeight() >> layer;

            int curX = ((curIndex % animation2.getFrameCount()) * width);
            int curY = ((curIndex / animation2.getFrameCount()) * height);

            int nextX = ((nextIndex % animation2.getFrameCount()) * width);
            int nextY = ((nextIndex / animation2.getFrameCount()) * height);

            NativeImage src = ((SpriteContentsAccessor) this.parent).getImages()[layer];
            NativeImage dst = this.images[layer];

            long ppSrcPixel = NativeImageHelper.getPointerRGBA(src);
            long ppDstPixel = NativeImageHelper.getPointerRGBA(dst);

            // Pointers to the pixel array for the current and next frame
            long pRgba1 = ppSrcPixel + (curX + (long) curY * src.getWidth() * STRIDE);
            long pRgba2 = ppSrcPixel + (nextX + (long) nextY * src.getWidth() * STRIDE);

            for (int pixelIndex = 0, pixelCount = width * height; pixelIndex < pixelCount; pixelIndex++) {
                int rgba1 = MemoryUtil.memGetInt(pRgba1);
                int rgba2 = MemoryUtil.memGetInt(pRgba2);

                // Mix the RGB components and truncate the A component
                int mixedRgb = ColorMixer.mix(rgba1, rgba2, mix) & 0x00FFFFFF;

                // Take the A component from the source pixel
                int alpha = rgba1 & 0xFF000000;

                // Update the pixel within the interpolated frame using the combined RGB and A components
                MemoryUtil.memPutInt(ppDstPixel, mixedRgb | alpha);

                pRgba1 += STRIDE;
                pRgba2 += STRIDE;

                ppDstPixel += STRIDE;
            }
        }

        ((SpriteContentsInvoker) this.parent).invokeUpload(x, y, 0, 0, this.images);
    }
}
