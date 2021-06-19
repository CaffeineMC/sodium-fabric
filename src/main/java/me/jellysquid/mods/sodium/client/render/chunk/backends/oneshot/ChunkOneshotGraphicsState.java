package me.jellysquid.mods.sodium.client.render.chunk.backends.oneshot;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.*;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlPrimitiveType;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.gl.tessellation.TessellationBinding;
import me.jellysquid.mods.sodium.client.gl.util.ElementRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderBindingPoints;

import java.util.Arrays;
import java.util.Map;

public class ChunkOneshotGraphicsState extends ChunkGraphicsState {
    private final GlMutableBuffer vertexBuffer;
    private final GlMutableBuffer indexBuffer;

    protected GlTessellation tessellation;

    private final ElementRange[] parts;

    protected ChunkOneshotGraphicsState(RenderDevice device, ChunkRenderContainer<?> container) {
        super(container);

        this.parts = new ElementRange[ModelQuadFacing.COUNT];

        try (CommandList commands = device.createCommandList()) {
            this.vertexBuffer = commands.createMutableBuffer(GlBufferUsage.GL_STATIC_DRAW);
            this.indexBuffer = commands.createMutableBuffer(GlBufferUsage.GL_STATIC_DRAW);
        }
    }

    public ElementRange getModelPart(int facing) {
        return this.parts[facing];
    }

    protected void setModelPart(ModelQuadFacing facing, ElementRange slice) {
        this.parts[facing.ordinal()] = slice;
    }

    @Override
    public void delete(CommandList commandList) {
        commandList.deleteBuffer(this.vertexBuffer);
        commandList.deleteBuffer(this.indexBuffer);
    }

    public void upload(CommandList commandList, ChunkMeshData meshData) {
        IndexedVertexData vertexData = meshData.takeVertexData();

        commandList.uploadData(this.vertexBuffer, vertexData.vertexBuffer);
        commandList.uploadData(this.indexBuffer, vertexData.indexBuffer);

        GlVertexFormat<ChunkMeshAttribute> vertexFormat = (GlVertexFormat<ChunkMeshAttribute>) vertexData.vertexFormat;

        this.tessellation = commandList.createTessellation(GlPrimitiveType.TRIANGLES, new TessellationBinding[] {
                new TessellationBinding(this.vertexBuffer, new GlVertexAttributeBinding[] {
                        new GlVertexAttributeBinding(ChunkShaderBindingPoints.POSITION, vertexFormat.getAttribute(ChunkMeshAttribute.POSITION)),
                        new GlVertexAttributeBinding(ChunkShaderBindingPoints.COLOR, vertexFormat.getAttribute(ChunkMeshAttribute.COLOR)),
                        new GlVertexAttributeBinding(ChunkShaderBindingPoints.TEX_COORD, vertexFormat.getAttribute(ChunkMeshAttribute.TEXTURE)),
                        new GlVertexAttributeBinding(ChunkShaderBindingPoints.LIGHT_COORD, vertexFormat.getAttribute(ChunkMeshAttribute.LIGHT))
                }, false)
        }, this.indexBuffer);

        Arrays.fill(this.parts, null);

        for (Map.Entry<ModelQuadFacing, ElementRange> entry : meshData.getSlices()) {
            this.setModelPart(entry.getKey(), entry.getValue());
        }
    }
}
