package net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.frapi.SpriteFinderCache;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;

public class ChunkVertexConsumer implements VertexConsumer {
    private static final int ATTRIBUTE_POSITION_BIT = 1 << 0;
    private static final int ATTRIBUTE_COLOR_BIT = 1 << 1;
    private static final int ATTRIBUTE_TEXTURE_BIT = 1 << 2;
    private static final int ATTRIBUTE_LIGHT_BIT = 1 << 3;
    private static final int ATTRIBUTE_NORMAL_BIT = 1 << 4;
    private static final int REQUIRED_ATTRIBUTES = (1 << 5) - 1;

    private final ChunkModelBuilder modelBuilder;
    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();

    private Material material;
    private int fixedColor = 0xFFFFFFFF;
    private int vertexIndex;
    private int writtenAttributes;
    private ModelQuadFacing facing;

    public ChunkVertexConsumer(ChunkModelBuilder modelBuilder) {
        this.modelBuilder = modelBuilder;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public VertexConsumer endVertexIfDone() {
        if (this.writtenAttributes != REQUIRED_ATTRIBUTES) {
            return this;
        }

        this.vertexIndex++;

        if (this.vertexIndex == 4) {
            this.modelBuilder.getVertexBuffer(this.facing).push(this.vertices, this.material);

            float u = 0;
            float v = 0;

            for (ChunkVertexEncoder.Vertex vertex : this.vertices) {
                u += vertex.u;
                v += vertex.v;
            }

            TextureAtlasSprite sprite = SpriteFinderCache.forBlockAtlas().find(u * 0.25f, v * 0.25f);
            this.modelBuilder.addSprite(sprite);

            this.vertexIndex = 0;
        }

        return this;
    }

    @Override
    public @NotNull VertexConsumer addVertex(float x, float y, float z) {
        ChunkVertexEncoder.Vertex vertex = this.vertices[this.vertexIndex];
        vertex.x = x;
        vertex.y = y;
        vertex.z = z;
        this.writtenAttributes |= ATTRIBUTE_POSITION_BIT;
        return this.endVertexIfDone();
    }

    // Writing color ignores alpha since alpha is used as a color multiplier by Sodium.
    @Override
    public @NotNull VertexConsumer setColor(int red, int green, int blue, int alpha) {
        ChunkVertexEncoder.Vertex vertex = this.vertices[this.vertexIndex];
        vertex.color = ColorABGR.pack(red, green, blue, 0xFF);
        this.writtenAttributes |= ATTRIBUTE_COLOR_BIT;
        return this.endVertexIfDone();
    }

    @Override
    public @NotNull VertexConsumer setColor(float red, float green, float blue, float alpha) {
        ChunkVertexEncoder.Vertex vertex = this.vertices[this.vertexIndex];
        vertex.color = ColorABGR.pack(red, green, blue, 1);
        this.writtenAttributes |= ATTRIBUTE_COLOR_BIT;
        return this.endVertexIfDone();
    }

    @Override
    public @NotNull VertexConsumer setColor(int argb) {
        ChunkVertexEncoder.Vertex vertex = this.vertices[this.vertexIndex];
        vertex.color = ColorARGB.toABGR(argb, 0xFF);
        this.writtenAttributes |= ATTRIBUTE_COLOR_BIT;
        return this.endVertexIfDone();
    }

    @Override
    public @NotNull VertexConsumer setUv(float u, float v) {
        ChunkVertexEncoder.Vertex vertex = this.vertices[this.vertexIndex];
        vertex.u = u;
        vertex.v = v;
        this.writtenAttributes |= ATTRIBUTE_TEXTURE_BIT;
        return this.endVertexIfDone();
    }

    // Overlay is ignored for chunk geometry.
    @Override
    public @NotNull VertexConsumer setUv1(int u, int v) {
        return this.endVertexIfDone();
    }

    @Override
    public @NotNull VertexConsumer setOverlay(int uv) {
        return this.endVertexIfDone();
    }

    @Override
    public @NotNull VertexConsumer setUv2(int u, int v) {
        ChunkVertexEncoder.Vertex vertex = this.vertices[this.vertexIndex];
        vertex.light = ((v & 0xFFFF) << 16) | (u & 0xFFFF);
        this.writtenAttributes |= ATTRIBUTE_LIGHT_BIT;
        return this.endVertexIfDone();
    }

    @Override
    public @NotNull VertexConsumer setLight(int uv) {
        ChunkVertexEncoder.Vertex vertex = this.vertices[this.vertexIndex];
        vertex.light = uv;
        this.writtenAttributes |= ATTRIBUTE_LIGHT_BIT;
        return this.endVertexIfDone();
    }

    @Override
    public @NotNull VertexConsumer setNormal(float x, float y, float z) {
        if (this.vertexIndex == 0) {
            this.facing = ModelQuadFacing.fromDirection(Direction.getNearest(x, y, z));
        }

        this.writtenAttributes |= ATTRIBUTE_NORMAL_BIT;
        return this.endVertexIfDone();
    }
}
