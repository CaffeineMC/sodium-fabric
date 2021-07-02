package me.jellysquid.mods.sodium.mixin.core;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.Shader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Shader.class)
public class MixinShader {
    private final Object2IntMap<CharSequence> uniformLocationCache = new Object2IntOpenHashMap<>();

    public MixinShader() {
        this.uniformLocationCache.defaultReturnValue(-1);
    }

    @Redirect(method = "upload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/GlUniform;getUniformLocation(ILjava/lang/CharSequence;)I"))
    private int redirectGetUniformLocation(int program, CharSequence name) {
        int id = this.uniformLocationCache.computeIfAbsent(name, (key) -> GlUniform.getUniformLocation(program, key));

        if (id < 0) {
            throw new NullPointerException("Couldn't find uniform " + name + "for program " + program);
        }

        return id;
    }
}
