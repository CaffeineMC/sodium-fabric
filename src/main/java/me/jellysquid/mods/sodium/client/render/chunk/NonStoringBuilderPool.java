package me.jellysquid.mods.sodium.client.render.chunk;

import net.minecraft.client.render.chunk.BlockBufferAllocatorStorage;
import net.minecraft.client.render.chunk.BlockBufferBuilderPool;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public class NonStoringBuilderPool extends BlockBufferBuilderPool {
    public NonStoringBuilderPool() {
        super(Collections.emptyList());
    }

    @Nullable
    @Override
    public BlockBufferAllocatorStorage acquire() {
        return null;
    }

    @Override
    public void release(BlockBufferAllocatorStorage blockBufferBuilderStorage) {
    }

    @Override
    public boolean hasNoAvailableBuilder() {
        return true;
    }

    @Override
    public int getAvailableBuilderCount() {
        return 0;
    }
}
