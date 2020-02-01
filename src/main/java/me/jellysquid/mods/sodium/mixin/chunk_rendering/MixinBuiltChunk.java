package me.jellysquid.mods.sodium.mixin.chunk_rendering;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.render.chunk.ExtendedBuiltChunk;
import me.jellysquid.mods.sodium.client.render.gl.GlVertexArray;
import me.jellysquid.mods.sodium.client.render.chunk.VertexBufferWithArray;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(ChunkBuilder.BuiltChunk.class)
public class MixinBuiltChunk implements ExtendedBuiltChunk {
    @Shadow
    @Final
    private Map<RenderLayer, VertexBuffer> buffers;

    private HashMap<RenderLayer, VertexBufferWithArray> buffersWithVAOs;

    @Inject(method = "delete", at = @At("RETURN"))
    private void onDelete(CallbackInfo ci) {
        if (this.buffersWithVAOs != null) {
            this.buffersWithVAOs.values().forEach(VertexBufferWithArray::delete);
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        if (GlVertexArray.isSupported() && SodiumClientMod.options().performance.useVAOs) {
            this.buffersWithVAOs = new HashMap<>();

            for (Map.Entry<RenderLayer, VertexBuffer> entry : this.buffers.entrySet()) {
                this.buffersWithVAOs.put(entry.getKey(), new VertexBufferWithArray(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, entry.getValue(), new GlVertexArray()));
            }
        }
    }

    @Override
    public VertexBufferWithArray getBufferWithArray(RenderLayer layer) {
        return this.buffersWithVAOs.get(layer);
    }

    @Override
    public boolean usesVAORendering() {
        return this.buffersWithVAOs != null;
    }
}
