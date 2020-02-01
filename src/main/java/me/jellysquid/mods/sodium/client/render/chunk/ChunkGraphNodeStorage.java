package me.jellysquid.mods.sodium.client.render.chunk;

import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class ChunkGraphNodeStorage {
    private final int sizeY;
    private final int sizeX;
    private final int sizeZ;

    public final ChunkGraphNode[] data;

    public ChunkGraphNodeStorage(int renderDistance, ChunkBuilder.BuiltChunk[] builtChunks) {
        int diameter = (renderDistance * 2) + 1;

        this.sizeX = diameter;
        this.sizeZ = diameter;
        this.sizeY = 16;

        this.data = new ChunkGraphNode[this.sizeX * this.sizeY * this.sizeZ];

        for (int i = 0; i < builtChunks.length; i++) {
            BlockPos origin = builtChunks[i].getOrigin();
            int j = Math.floorMod(origin.getZ() >> 4, diameter) * diameter + Math.floorMod(origin.getX() >> 4, diameter);

            this.data[i] = new ChunkGraphNode(builtChunks[i], j);
        }
    }

    public int getIndex(int x, int y, int z) {
        return (z * this.sizeY + y) * this.sizeX + x;
    }

    public void set(int x, int y, int z, ChunkGraphNode obj) {
        int y2 = MathHelper.floorDiv(y, 16);

        if (y2 < 0 || y2 >= this.sizeY) {
            return;
        }

        int x2 = MathHelper.floorMod(MathHelper.floorDiv(x, 16), this.sizeX);
        int z2 = MathHelper.floorMod(MathHelper.floorDiv(z, 16), this.sizeZ);

        this.data[this.getIndex(x2, y2, z2)] = obj;
    }

    public ChunkGraphNode get(BlockPos pos) {
        return this.get(pos.getX(), pos.getY(), pos.getZ());
    }

    public ChunkGraphNode get(int x, int y, int z) {
        int y2 = MathHelper.floorDiv(y, 16);

        if (y2 < 0 || y2 >= this.sizeY) {
            return null;
        }

        int x2 = MathHelper.floorMod(MathHelper.floorDiv(x, 16), this.sizeX);
        int z2 = MathHelper.floorMod(MathHelper.floorDiv(z, 16), this.sizeZ);

        return this.data[this.getIndex(x2, y2, z2)];
    }

    public void set(BlockPos pos, ChunkGraphNode obj) {
        this.set(pos.getX(), pos.getY(), pos.getZ(), obj);
    }
}
