package me.jellysquid.mods.sodium.mixin.textures;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.texture.SpriteExtended;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.texture.Sprite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Sprite.class)
public abstract class MixinSprite implements SpriteExtended {
    private boolean onDemand = false;
    private boolean hasPendingUpdate = false;
    private boolean forceNextUpdate;

    @Shadow
    private int frameTicks;

    @Shadow
    private AnimationResourceMetadata animationMetadata;

    @Shadow
    private int frameIndex;

    @Shadow
    public abstract int getFrameCount();

    @Shadow
    protected abstract void upload(int int_1);

    @Shadow
    @Final
    private Sprite.Interpolation interpolation;

    /**
     * @author JellySquid
     */
    @Overwrite
    public void tickAnimation() {
        this.frameTicks++;

        this.hasPendingUpdate = true;
        this.onDemand = SodiumClientMod.options().performance.animateOnlyVisibleTextures;

        if (!this.onDemand || this.forceNextUpdate) {
            this.uploadTexture();
        }
    }

    @Override
    public void uploadPendingChanges() {
        if (this.hasPendingUpdate && this.onDemand) {
            this.uploadTexture();
        }
    }

    private void uploadTexture() {
        if (this.frameTicks >= this.animationMetadata.getFrameTime(this.frameIndex)) {
            int prevFrameIndex = this.animationMetadata.getFrameIndex(this.frameIndex);
            int frameCount = this.animationMetadata.getFrameCount() == 0 ? this.getFrameCount() : this.animationMetadata.getFrameCount();

            this.frameIndex = (this.frameIndex + 1) % frameCount;
            this.frameTicks = 0;

            int frameIndex = this.animationMetadata.getFrameIndex(this.frameIndex);

            if (prevFrameIndex != frameIndex && frameIndex >= 0 && frameIndex < this.getFrameCount()) {
                this.upload(frameIndex);
            }
        } else if (this.interpolation != null) {
            if (!RenderSystem.isOnRenderThread()) {
                RenderSystem.recordRenderCall(this::updateInterpolatedTexture);
            } else {
                this.updateInterpolatedTexture();
            }
        }

        this.hasPendingUpdate = false;
        this.forceNextUpdate = false;
    }

    @Override
    public void markActive() {
        this.forceNextUpdate = true;
    }

    private void updateInterpolatedTexture() {
        this.interpolation.method_24128();
    }
}
