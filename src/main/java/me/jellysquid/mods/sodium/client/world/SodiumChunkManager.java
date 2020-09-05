package me.jellysquid.mods.sodium.client.world;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import me.jellysquid.mods.sodium.client.util.collections.FixedLongHashTable;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;

import java.util.concurrent.locks.StampedLock;

/**
 * An implementation of {@link net.minecraft.world.chunk.ChunkManager} for the client world which uses a simple
 * integer key to object hash table. This generally provides improved performance over the vanilla implementation
 * through reducing code complexity, eliminating expensive floor-modulo operations, and removing the usage of atomic
 * references.
 *
 * The usage of an atomic reference array is not necessary with Sodium's renderer implementation as it does not access
 * world state or chunks concurrently from other worker threads, which fixes a number of synchronization issues in the
 * process.
 *
 * This implementation allows for a {@link ChunkStatusListener} to be attached, allowing the game renderer to receive
 * notifications when chunks are loaded or unloaded instead of resorting to expensive polling techniques, which would
 * usually resort in chunk queries being slammed every frame when many chunks have pending rebuilds.
 */
public class SodiumChunkManager extends ClientChunkManager implements ChunkStatusListenerManager {
    private final ClientWorld world;
    private final WorldChunk emptyChunk;

    private final StampedLock lock = new StampedLock();

    private FixedLongHashTable<WorldChunk> chunks;
    private ChunkStatusListener listener;
    private int centerX, centerZ;
    private int radius;

    public SodiumChunkManager(ClientWorld world, int loadDistance) {
        super(world, loadDistance);

        this.world = world;
        this.emptyChunk = new EmptyChunk(world, new ChunkPos(0, 0));
        this.radius = getChunkMapRadius(loadDistance);
        this.chunks = new FixedLongHashTable<>(getChunkMapSize(this.radius), Hash.FAST_LOAD_FACTOR);
    }

    @Override
    public void unload(int x, int z) {
        // If this request unloads a chunk, notify the listener
        WorldChunk unloadedChunk = this.chunks.remove(createChunkKey(x, z));
        if (unloadedChunk != null) {
            this.world.unloadBlockEntities(unloadedChunk);
            this.onChunkUnloaded(x, z);
        }
    }

    @Override
    public WorldChunk getChunk(int x, int z, ChunkStatus status, boolean create) {
        WorldChunk chunk = this.getChunkSafe(createChunkKey(x, z));

        if (chunk == null) {
            return create ? this.emptyChunk : null;
        }

        return chunk;
    }

    private WorldChunk getChunkSafe(long key) {
        long stamp = this.lock.tryOptimisticRead();

        // Perform an optimistic read, hoping that the map will not be mutated while doing so
        WorldChunk chunk = this.chunks.get(key);

        // If the collection changed under our feet, the returned value is to be considered invalid
        // This should happen very rarely.
        if (!this.lock.validate(stamp)) {
            // Retrieve the chunk again, but this time acquire a full lock
            stamp = this.lock.readLock();

            try {
                chunk = this.chunks.get(key);
            } finally {
                this.lock.unlockRead(stamp);
            }
        }

        return chunk;
    }

    @Override
    public WorldChunk loadChunkFromPacket(int x, int z, BiomeArray biomes, PacketByteBuf buf, CompoundTag tag, int verticalStripBitmask, boolean complete) {
        long key = createChunkKey(x, z);

        WorldChunk chunk = this.chunks.get(key);

        // If the chunk does not yet exist, create it now
        if (!complete && chunk != null) {
            chunk.loadFromPacket(biomes, buf, tag, verticalStripBitmask);
        } else {
            // [VanillaCopy] If the packet didn't contain any biome data and the chunk doesn't exist yet, abort
            if (biomes == null) {
                return null;
            }

            chunk = new WorldChunk(this.world, new ChunkPos(x, z), biomes);
            chunk.loadFromPacket(biomes, buf, tag, verticalStripBitmask);

            long stamp = this.lock.writeLock();

            try {
                this.chunks.put(key, chunk);
            } finally {
                this.lock.unlockWrite(stamp);
            }
        }

        // Perform post-load actions and notify the chunk listener that a chunk was just loaded
        this.onChunkLoaded(x, z, chunk);

        return chunk;
    }

    @Override
    public void setChunkMapCenter(int x, int z) {
        this.centerX = x;
        this.centerZ = z;
    }

    @Override
    public void updateLoadDistance(int loadDistance) {
        this.radius = getChunkMapRadius(loadDistance);

        FixedLongHashTable<WorldChunk> copy = new FixedLongHashTable<>(getChunkMapSize(this.radius), Hash.FAST_LOAD_FACTOR);

        long stamp = this.lock.writeLock();

        try {
            ObjectIterator<Long2ObjectMap.Entry<WorldChunk>> it = this.chunks.iterator();

            while (it.hasNext()) {
                Long2ObjectMap.Entry<WorldChunk> entry = it.next();

                long pos = entry.getLongKey();
                int x = ChunkPos.getPackedX(pos);
                int z = ChunkPos.getPackedZ(pos);

                // Remove any chunks which are outside the load radius
                if (Math.abs(x - this.centerX) <= this.radius && Math.abs(z - this.centerZ) <= this.radius) {
                    copy.put(pos, entry.getValue());
                }
            }

            this.chunks = copy;
        } finally {
            this.lock.unlockWrite(stamp);
        }
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
        // [VanillaCopy] Mark the chunk as eligible for block and sky lighting
        LightingProvider lightEngine = this.getLightingProvider();
        lightEngine.setColumnEnabled(new ChunkPos(x, z), true);

        ChunkSection[] sections = chunk.getSectionArray();

        // [VanillaCopy] Notify the light engine that this chunk's sections have been updated
        for (int y = 0; y < sections.length; ++y) {
            lightEngine.setSectionStatus(ChunkSectionPos.from(x, y, z), ChunkSection.isEmpty(sections[y]));
        }

        // Sodium doesn't actually use vanilla's global color cache, but we keep it around for compatibility purposes
        this.world.resetChunkColor(x, z);

        // Notify the chunk listener
        if (this.listener != null) {
            this.listener.onChunkAdded(x, z);
        }
    }

    private void onChunkUnloaded(int x, int z) {
        // Notify the chunk listener
        if (this.listener != null) {
            this.listener.onChunkRemoved(x, z);
        }
    }

    private static long createChunkKey(int x, int z) {
        return ChunkPos.toLong(x, z);
    }

    private static int getChunkMapRadius(int radius) {
        return Math.max(2, radius) + 3;
    }

    private static int getChunkMapSize(int radius) {
        int n = (radius * 2) + 1;
        return n * n;
    }
}
