package me.jellysquid.mods.sodium.client.render.chunk.compile.tasks;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkMeshInfo;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRender;
import me.jellysquid.mods.sodium.client.render.chunk.compile.VertexBufferCache;
import me.jellysquid.mods.sodium.client.render.pipeline.ChunkRenderPipeline;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.util.math.Direction;

import java.util.EnumSet;

public class ChunkRenderEmptyBuildTask extends ChunkRenderBuildTask {
    private final ChunkRender<?> render;

    public ChunkRenderEmptyBuildTask(ChunkRender<?> render) {
        this.render = render;
    }

    @Override
    public ChunkRenderUploadTask performBuild(ChunkRenderPipeline pipeline, VertexBufferCache buffers) {
        return new ChunkRenderEmptyBuildTask.EmptyUploadTask(this.render);
    }

    public static class EmptyUploadTask extends ChunkRenderUploadTask {
        private final ChunkRender<?> render;

        public EmptyUploadTask(ChunkRender<?> render) {
            this.render = render;
        }

        @Override
        public void performUpload() {
            ChunkOcclusionData occlusionData = new ChunkOcclusionData();
            occlusionData.addOpenEdgeFaces(EnumSet.allOf(Direction.class));

            ChunkMeshInfo.Builder meshInfo = new ChunkMeshInfo.Builder();
            meshInfo.setOcclusionData(occlusionData);

            this.render.upload(meshInfo.build());
        }
    }
}
