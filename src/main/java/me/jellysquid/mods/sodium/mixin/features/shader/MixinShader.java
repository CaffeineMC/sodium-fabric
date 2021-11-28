package me.jellysquid.mods.sodium.mixin.features.shader;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.Shader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(Shader.class)
public class MixinShader {
    private static final Supplier<Object2IntMap<CharSequence>> UNIFORM_CACHE_CREATOR = () -> {
        Object2IntMap<CharSequence> out = new Object2IntOpenHashMap<>();
        out.defaultReturnValue(-1);
        return out;
    };

    private Object2IntMap<CharSequence> uniformLocationCache = UNIFORM_CACHE_CREATOR.get();

    @Redirect(method = "upload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/GlUniform;getUniformLocation(ILjava/lang/CharSequence;)I"))
    private int redirectGetUniformLocation(int program, CharSequence name) {
        // DashLoader fix as its using Unsafe.allocateInstance()
        if(uniformLocationCache == null) uniformLocationCache = UNIFORM_CACHE_CREATOR.get();

        int id = this.uniformLocationCache.computeIfAbsent(name, (key) -> GlUniform.getUniformLocation(program, key));

        if (id < 0) {
            throw new NullPointerException("Couldn't find uniform " + name + "for program " + program);
        }

        return id;
    }
}
