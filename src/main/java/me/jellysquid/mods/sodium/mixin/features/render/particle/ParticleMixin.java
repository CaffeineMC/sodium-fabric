package me.jellysquid.mods.sodium.mixin.features.render.particle;

import me.jellysquid.mods.sodium.client.render.particle.ParticleExtended;
import me.jellysquid.mods.sodium.client.render.particle.ParticleRenderView;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Particle.class)
public class ParticleMixin implements ParticleExtended {
    @Shadow
    protected double x;
    @Shadow
    protected double y;
    @Shadow
    protected double z;

    @Unique
    private ParticleRenderView renderView;

    @Override
    public void sodium$configure(ParticleRenderView renderView) {
        this.renderView = renderView;
    }

    /**
     * @author JellySquid
     * @reason Use render cache
     */
    @Overwrite
    public int getBrightness(float tickDelta) {
        return this.renderView.getBrightness(MathHelper.floor(this.x),
                MathHelper.floor(this.y),
                MathHelper.floor(this.z));
    }
}
