package me.jellysquid.mods.sodium.client.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;

public class SodiumChunkManager extends ClientChunkManager implements ChunkManagerWithStatusListener {
    private final Long2ObjectOpenHashMap<WorldChunk> chunks = new Long2ObjectOpenHashMap<>();
    private final ClientWorld world;
    private final WorldChunk emptyChunk;

    private ChunkStatusListener listener;
    private int centerX, centerZ;
    private int radius;

    private long prevChunkKey = Long.MIN_VALUE;
    private WorldChunk prevChunk;

    public SodiumChunkManager(ClientWorld world, int radius) {
        super(world, radius);

        this.world = world;
        this.emptyChunk = new EmptyChunk(world, new ChunkPos(0, 0));
        this.radius = getChunkMapRadius(radius);
    }

    @Override
    public void unload(int x, int z) {
        if (this.chunks.remove(toChunkKey(x, z)) != null) {
            this.onChunkUnloaded(x, z);
        }

        this.clearCache();
    }

    private void unload(long pos) {
        if (this.chunks.remove(pos) != null) {
            this.onChunkUnloaded(ChunkPos.getPackedX(pos), ChunkPos.getPackedZ(pos));
        }

        this.clearCache();
    }

    private void clearCache() {
        this.prevChunk = null;
        this.prevChunkKey = Long.MIN_VALUE;
    }

    @Override
    public WorldChunk getChunk(int x, int z, ChunkStatus status, boolean create) {
        long key = toChunkKey(x, z);

        if (key == this.prevChunkKey) {
            return this.prevChunk;
        }

        WorldChunk chunk = this.chunks.get(key);

        if (chunk == null) {
            return create ? this.emptyChunk : null;
        }

        this.prevChunkKey = key;
        this.prevChunk = chunk;

        return chunk;
    }

    @Override
    public WorldChunk loadChunkFromPacket(int x, int z, BiomeArray biomes, PacketByteBuf buf, CompoundTag tag, int flag) {
        if (!this.isWithinLoadDistance(x, z)) {
            return null;
        }

        long key = toChunkKey(x, z);

        WorldChunk chunk = this.chunks.get(key);

        if (chunk == null) {
            if (biomes == null) {
                return null;
            }

            this.chunks.put(key, chunk = new WorldChunk(this.world, new ChunkPos(x, z), biomes));
        }

        chunk.loadFromPacket(biomes, buf, tag, flag);

        this.onChunkLoaded(x, z, chunk);

        return chunk;
    }

    @Override
    public void setChunkMapCenter(int x, int z) {
        this.centerX = x;
        this.centerZ = z;
    }

    @Override
    public void updateLoadDistance(int dist) {
        int radius = getChunkMapRadius(dist);

        if (this.radius == radius) {
            return;
        }

        this.radius = dist;

        this.checkChunks();
    }

    private void checkChunks() {
        LongList queue = new LongArrayList();

        LongIterator it = this.chunks.keySet().iterator();

        while (it.hasNext()) {
            long pos = it.nextLong();

            int x = ChunkPos.getPackedX(pos);
            int z = ChunkPos.getPackedZ(pos);

            if (!this.isWithinLoadDistance(x, z)) {
                queue.add(pos);
            }
        }

        if (!queue.isEmpty()) {
            it = queue.iterator();

            while (it.hasNext()) {
                this.unload(it.nextLong());
            }
        }
    }

    private boolean isWithinLoadDistance(int x, int z) {
        return Math.abs(x - this.centerX) <= this.radius && Math.abs(z - this.centerZ) <= this.radius;
    }

    @Override
    public String getDebugString() {
        return "SodiumChunkCache: " + this.getLoadedChunkCount();
    }

    @Override
    public int getLoadedChunkCount() {
        return this.chunks.size();
    }

    @Override
    public void setListener(ChunkStatusListener listener) {
        this.listener = listener;
    }

    private void onChunkLoaded(int x, int z, WorldChunk chunk) {
        LightingProvider lightEngine = this.getLightingProvider();
        lightEngine.setLightEnabled(new ChunkPos(x, z), true);

        ChunkSection[] sections = chunk.getSectionArray();

        for (int y = 0; y < sections.length; ++y) {
            lightEngine.updateSectionStatus(ChunkSectionPos.from(x, y, z), ChunkSection.isEmpty(sections[y]));
        }

        this.world.resetChunkColor(x, z);

        if (this.listener != null) {
            this.listener.onChunkAdded(x, z);
        }
    }

    private void onChunkUnloaded(int x, int z) {
        if (this.listener != null) {
            this.listener.onChunkRemoved(x, z);
        }
    }

    private static long toChunkKey(int x, int z) {
        return ChunkPos.toLong(x, z);
    }

    private static int getChunkMapRadius(int radius) {
        return Math.max(2, radius) + 3;
    }
}
