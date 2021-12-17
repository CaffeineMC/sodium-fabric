package me.jellysquid.mods.sodium.client.render.chunk.region;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.arena.AsyncBufferArena;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.arena.SwapBufferArena;
import me.jellysquid.mods.sodium.client.gl.arena.staging.StagingBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderLayer;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderLayerManager;
import me.jellysquid.mods.sodium.client.render.chunk.passes.ChunkMeshType;
import net.minecraft.world.ChunkRegion;
import org.apache.commons.lang3.Validate;

import java.lang.reflect.Array;
import java.util.EnumMap;
import java.util.Map;

public class RenderRegionStorage<E extends Enum<E> & ChunkMeshType.StorageBufferTarget> {
    private final Map<E, GlBufferArena> buffers;
    private final Map<BlockRenderLayer, ChunkGraphicsState<E>[]> stateContainer;

    @SuppressWarnings("unchecked")
    public RenderRegionStorage(ChunkMeshType<E> meshType, CommandList commandList, StagingBuffer stagingBuffer, BlockRenderLayerManager renderLayers) {
        this.buffers = new EnumMap<>(meshType.getStorageType());

        for (E target : meshType.getStorageType().getEnumConstants()) {
            this.buffers.put(target, createArena(commandList, target.getExpectedSize() * RenderRegion.REGION_SIZE, stagingBuffer));
        }

        this.stateContainer = new Reference2ReferenceOpenHashMap<>();

        for (BlockRenderLayer layer : renderLayers.getRenderLayers()) {
            this.stateContainer.put(layer, new ChunkGraphicsState[RenderRegion.REGION_SIZE]);
        }
    }

    public void delete(CommandList commandList) {
        for (GlBufferArena arena : this.buffers.values()) {
            arena.delete(commandList);
        }
    }

    public boolean isEmpty() {
        for (GlBufferArena arena : this.buffers.values()) {
            if (!arena.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public long getDeviceUsedMemory() {
        long bytes = 0L;

        for (GlBufferArena arena : this.buffers.values()) {
            bytes += arena.getDeviceUsedMemory();
        }

        return bytes;
    }

    public long getDeviceAllocatedMemory() {
        long bytes = 0L;

        for (GlBufferArena arena : this.buffers.values()) {
            bytes += arena.getDeviceAllocatedMemory();
        }

        return bytes;
    }

    private static GlBufferArena createArena(CommandList commandList, int initialCapacity, StagingBuffer stagingBuffer) {
        return switch (SodiumClientMod.options().advanced.arenaMemoryAllocator) {
            case ASYNC -> new AsyncBufferArena(commandList, initialCapacity, stagingBuffer);
            case SWAP -> new SwapBufferArena(commandList);
        };
    }

    public GlBufferArena getArena(E target) {
        return Validate.notNull(this.buffers.get(target));
    }

    public GlBuffer getBuffer(E target) {
        return Validate.notNull(this.buffers.get(target)).getBufferObject();
    }

    public void deleteChunkState(int index) {
        for (ChunkGraphicsState<E>[] states : this.stateContainer.values()) {
            if (states[index] != null) {
                states[index].delete();
                states[index] = null;
            }
        }
    }

    public void setChunkState(int index, BlockRenderLayer layer, ChunkGraphicsState<E> state) {
        var states = this.stateContainer.get(layer);

        if (states[index] != null) {
            throw new IllegalStateException();
        }

        states[index] = state;
    }

    public ChunkGraphicsState<E> getGraphicsState(BlockRenderLayer layer, RenderSection section) {
        var states = this.stateContainer.get(layer);

        if (states == null) {
            return null;
        }

        return states[section.getLocalIndex()];
    }
}
