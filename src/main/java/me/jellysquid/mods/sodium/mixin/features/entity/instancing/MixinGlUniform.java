package me.jellysquid.mods.sodium.mixin.features.entity.instancing;

import net.minecraft.client.gl.GlUniform;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.FloatBuffer;

@Mixin(GlUniform.class)
public class MixinGlUniform {
    @Shadow
    @Final
    private int count;

    @Redirect(method = "set([F)V", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put([F)Ljava/nio/FloatBuffer;"))
    public FloatBuffer set(FloatBuffer floatBuffer, float[] src) {
        return floatBuffer.put(src, 0, count);
    }
}
