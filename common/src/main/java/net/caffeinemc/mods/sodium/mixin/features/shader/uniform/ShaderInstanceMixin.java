package net.caffeinemc.mods.sodium.mixin.features.shader.uniform;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;

/**
 * On the NVIDIA drivers (and maybe some others), the OpenGL submission thread requires expensive state synchronization
 * to happen when glGetUniformLocation and glGetInteger are called. In our case, this is rather unnecessary, since
 * these uniform locations can be trivially cached.
 */
@Mixin(ShaderInstance.class)
public class ShaderInstanceMixin {
    @Shadow
    @Final
    private List<String> samplerNames;

    @Shadow
    @Final
    private int programId;

    @Unique
    private Object2IntMap<String> uniformCache;

    @Unique
    private void initCache() {
        this.uniformCache = new Object2IntOpenHashMap<>();
        this.uniformCache.defaultReturnValue(-1);

        for (var samplerName : this.samplerNames) {
            var location = Uniform.glGetUniformLocation(this.programId, samplerName);

            if (location == -1) {
                throw new IllegalStateException("Failed to find uniform '%s' during shader init".formatted(samplerName));
            }

            this.uniformCache.put(samplerName, location);
        }
    }

    @Inject(method = "updateLocations", at = @At("RETURN"), require = 0)
    private void initCache(CallbackInfo ci) {
        this.initCache();
    }

    @Redirect(method = "apply", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/shaders/Uniform;glGetUniformLocation(ILjava/lang/CharSequence;)I"))
    private int redirectGetUniformLocation(int program, CharSequence name) {
        var location = this.uniformCache.getInt(name);

        if (location == -1) {
            throw new IllegalStateException("Failed to find uniform '%s' during shader bind");
        }

        return location;
    }
}
