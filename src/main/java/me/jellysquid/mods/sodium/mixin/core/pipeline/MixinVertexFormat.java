package me.jellysquid.mods.sodium.mixin.core.pipeline;

import me.jellysquid.mods.sodium.client.gl.attribute.BufferVertexFormat;
import net.minecraft.client.render.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VertexFormat.class)
public abstract class MixinVertexFormat implements BufferVertexFormat {
    @Shadow
    public abstract int getVertexSize();

    @Override
    public int getStride() {
        return this.getVertexSize();
    }
}
