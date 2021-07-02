package me.jellysquid.mods.sodium.mixin.features.texture_updates;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Sprite.Interpolation.class)
public class MixinSpriteInterpolated {
    @Shadow
    @Final
    private NativeImage[] images;

    @Shadow(aliases = "field_21757")
    private Sprite parent;

    private static final int COLOR_MAX_VALUE = 256;
    private static final int STRIDE = 4;

    private static final long MASK1 = 0x00FF00FF;
    private static final long MASK2 = 0xFF00FF00;

    /**
     * @author JellySquid
     * @reason Drastic optimizations
     */
    @Overwrite
    void apply(Sprite.Animation animation) {
        Sprite.AnimationFrame animationFrame = animation.frames.get(animation.frameIndex);

        int curIndex = animationFrame.index;
        int nextIndex = animation.frames.get((animation.frameIndex + 1) % animation.frames.size()).index;

        if (curIndex == nextIndex) {
            return;
        }

        float delta = 1.0F - (float) animation.frameTicks / (float) animationFrame.time;

        int f2 = (int) (COLOR_MAX_VALUE * delta);
        int f1 = COLOR_MAX_VALUE - f2;

        for (int layer = 0; layer < this.images.length; layer++) {
            int width = this.parent.width >> layer;
            int height = this.parent.height >> layer;

            int curX = ((curIndex % animation.frameCount) * width);
            int curY = ((curIndex / animation.frameCount) * height);

            int nextX = ((nextIndex % animation.frameCount) * width);
            int nextY = ((nextIndex / animation.frameCount) * height);

            NativeImage src = this.parent.images[layer];
            NativeImage dst = this.images[layer];

            // Source pointers
            long s1p = src.pointer + (curX + (curY * src.getWidth()) * STRIDE);
            long s2p = src.pointer + (nextX + (nextY * src.getWidth()) * STRIDE);

            // Destination pointers
            long dp = dst.pointer;

            int pixelCount = width * height;

            for (int i = 0; i < pixelCount; i++) {
                // Source colors
                long c1 = Integer.toUnsignedLong(MemoryUtil.memGetInt(s1p));
                long c2 = Integer.toUnsignedLong(MemoryUtil.memGetInt(s2p));

                long color = (((((c2 & MASK1) * f1) + ((c1 & MASK1) * f2)) >> 8) & MASK1) |
                        (((((c2 & MASK2) * f1) + ((c1 & MASK2) * f2)) >> 8) & MASK2);

                MemoryUtil.memPutInt(dp, (int) color);

                s1p += STRIDE;
                s2p += STRIDE;
                dp += STRIDE;
            }
        }

        this.parent.upload(0, 0, this.images);
    }

}
