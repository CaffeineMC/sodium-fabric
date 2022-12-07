package me.jellysquid.mods.sodium.mixin.features.texture_updates;

import me.jellysquid.mods.sodium.client.util.color.ColorMixer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteContents;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SpriteContents.Interpolation.class)
public class MixinSpriteInterpolated {
    @Shadow
    @Final
    private NativeImage[] images;

    @Unique
    private SpriteContents parent;

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
        SpriteContents.Animation animation = ((SpriteInfoAnimationAccessor) arg).getAnimation();
        AnimationAccessor animation2 = (AnimationAccessor) ((SpriteInfoAnimationAccessor) arg).getAnimation();
        List<SpriteContents.AnimationFrame> frames = ((AnimationAccessor) animation).getFrames();
        SpriteInfoAnimationAccessor accessor = (SpriteInfoAnimationAccessor) arg;
        AnimationFrameAccessor animationFrame = (AnimationFrameAccessor) frames.get(accessor.getFrameIndex());

        int curIndex = animationFrame.getIndex();
        int nextIndex = ((AnimationFrameAccessor) animation2.getFrames().get((accessor.getFrameIndex() + 1) % frames.size())).getIndex();

        if (curIndex == nextIndex) {
            return;
        }

        float delta = 1.0F - (float) accessor.getFrameTicks() / (float) animationFrame.getTime();

        int f1 = ColorMixer.getStartRatio(delta);
        int f2 = ColorMixer.getEndRatio(delta);

        for (int layer = 0; layer < this.images.length; layer++) {
            int width = this.parent.getWidth() >> layer;
            int height = this.parent.getHeight() >> layer;

            int curX = ((curIndex % animation2.getFrameCount()) * width);
            int curY = ((curIndex / animation2.getFrameCount()) * height);

            int nextX = ((nextIndex % animation2.getFrameCount()) * width);
            int nextY = ((nextIndex / animation2.getFrameCount()) * height);

            NativeImage src = ((SpriteInfoAccessor) this.parent).getImages()[layer];
            NativeImage dst = this.images[layer];

            // Source pointers
            long s1p = src.pointer + (curX + (long) curY * src.getWidth() * STRIDE);
            long s2p = src.pointer + (nextX + (long) nextY * src.getWidth() * STRIDE);

            // Destination pointers
            long dp = dst.pointer;

            int pixelCount = width * height;

            for (int i = 0; i < pixelCount; i++) {
                MemoryUtil.memPutInt(dp, ColorMixer.mixARGB(MemoryUtil.memGetInt(s1p), MemoryUtil.memGetInt(s2p), f1, f2));

                s1p += STRIDE;
                s2p += STRIDE;
                dp += STRIDE;
            }
        }

        this.parent.upload(x, y, 0, 0, this.images);
    }
}
