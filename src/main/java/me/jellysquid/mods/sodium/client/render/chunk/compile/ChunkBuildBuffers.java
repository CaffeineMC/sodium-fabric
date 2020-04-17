package me.jellysquid.mods.sodium.client.render.chunk.compile;

import com.mojang.datafixers.util.Pair;
import me.jellysquid.mods.sodium.client.gl.buffer.BufferUploadData;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkMesh;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.util.BufferUtil;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.util.math.BlockPos;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ChunkBuildBuffers {
    private final BufferBuilder[] builders = new BufferBuilder[BlockRenderPass.count()];
    private final RenderLayer[] layers = new RenderLayer[BlockRenderPass.count()];

    private final BlockRenderPassManager renderPassManager;

    public ChunkBuildBuffers(BlockRenderPassManager renderPassManager) {
        this.renderPassManager = renderPassManager;

        for (RenderLayer layer : RenderLayer.getBlockLayers()) {
            int i = renderPassManager.getRenderPassId(layer);

            this.builders[i] = new BufferBuilder(layer.getExpectedBufferSize());
            this.layers[i] = layer;
        }
    }

    public BufferBuilder get(RenderLayer layer) {
        return this.builders[this.renderPassManager.getRenderPassId(layer)];
    }

    public void clear() {
        for (BufferBuilder builder : this.builders) {
            if (builder != null) {
                builder.clear();
            }
        }
    }

    public void reset() {
        for (BufferBuilder builder : this.builders) {
            if (builder != null) {
                builder.reset();
            }
        }
    }

    public List<ChunkMesh> createMeshes(Vector3d camera, BlockPos pos) {
        List<ChunkMesh> layers = new ArrayList<>();

        for (int i = 0; i < this.builders.length; i++) {
            BufferBuilder builder = this.builders[i];

            if (builder == null || !builder.isBuilding()) {
                continue;
            }

            BlockRenderPass pass = this.renderPassManager.getRenderPass(i);

            if (pass.isTranslucent()) {
                builder.sortQuads((float) camera.x - (float) pos.getX(),
                        (float) camera.y - (float) pos.getY(),
                        (float) camera.z - (float) pos.getZ());
            }

            builder.end();

            Pair<BufferBuilder.DrawArrayParameters, ByteBuffer> data =  builder.popData();
            ByteBuffer vertices = data.getSecond();

            if (vertices.capacity() == 0) {
                continue;
            }

            BufferUploadData upload = new BufferUploadData(BufferUtil.copy(vertices), VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);

            layers.add(new ChunkMesh(pass, upload));
        }

        return layers;
    }
}
