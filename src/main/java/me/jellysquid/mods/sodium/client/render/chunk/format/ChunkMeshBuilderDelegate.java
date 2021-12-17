package me.jellysquid.mods.sodium.client.render.chunk.format;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.passes.ChunkMeshType;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Vec3i;

import java.util.Map;
import java.util.Set;

public class ChunkMeshBuilderDelegate<E extends ChunkMeshType.StorageBufferTarget> {
    private final Map<ModelQuadFacing, ChunkMeshBuilder<E>> builders;
    private final Set<Sprite> sprites = new ReferenceOpenHashSet<>();

    public ChunkMeshBuilderDelegate(Map<ModelQuadFacing, ChunkMeshBuilder<E>> builders) {
        this.builders = builders;
    }

    public void reset() {
        this.sprites.clear();

        for (ChunkMeshBuilder<?> builder : this.builders.values()) {
            builder.reset();
        }
    }

    public void destroy() {
        for (ChunkMeshBuilder<?> builder : this.builders.values()) {
            builder.destroy();
        }
    }

    public ChunkMeshBuilder<E> getSink(ModelQuadFacing facing) {
        return this.builders.get(facing);
    }

    public void addQuad(Vec3i position, ModelQuadView quad, ModelQuadFacing facing) {
        this.builders.get(facing)
                .add(position, quad, facing);

        if (quad.getSprite() != null) {
            this.sprites.add(quad.getSprite());
        }
    }
}
