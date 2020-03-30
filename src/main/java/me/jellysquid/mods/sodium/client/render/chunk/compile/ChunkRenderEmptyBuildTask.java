package me.jellysquid.mods.sodium.client.render.chunk.compile;

import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.util.math.Direction;

import java.util.EnumSet;

public class ChunkRenderEmptyBuildTask extends ChunkRenderBuildTask {
    private final ChunkRender<?> render;

    public ChunkRenderEmptyBuildTask(ChunkRender<?> render) {
        this.render = render;
    }

    @Override
    public ChunkRenderUploadTask performBuild(VertexBufferCache buffers) {
        return new ChunkRenderEmptyBuildTask.EmptyUploadTask(render);
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
