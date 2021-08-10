package me.jellysquid.mods.sodium.client.render.chunk.passes;

import me.jellysquid.mods.sodium.client.gl.shader.ShaderConstants;
import net.minecraft.client.render.RenderLayer;

public class BlockRenderPass {
    private final RenderLayer layer;
    private final boolean translucent;

    private final RenderPassShader vertexShader;
    private final RenderPassShader fragmentShader;

    BlockRenderPass(RenderLayer layer, boolean translucent, RenderPassShader vertexShader, RenderPassShader fragmentShader) {
        this.layer = layer;
        this.translucent = translucent;
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
    }

    public boolean isTranslucent() {
        return this.translucent;
    }

    public boolean isSolid() {
        return !this.translucent;
    }

    public RenderLayer getLayer() {
        return this.layer;
    }

    @Deprecated
    public void endDrawing() {
        this.layer.endDrawing();
    }

    @Deprecated
    public void startDrawing() {
        this.layer.startDrawing();
    }

    public RenderPassShader getVertexShader() {
        return this.vertexShader;
    }

    public RenderPassShader getFragmentShader() {
        return this.fragmentShader;
    }
}
