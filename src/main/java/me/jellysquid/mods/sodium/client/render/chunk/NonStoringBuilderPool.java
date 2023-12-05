package me.jellysquid.mods.sodium.client.render.chunk;

import net.minecraft.client.render.chunk.BlockBufferBuilderPool;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public class NonStoringBuilderPool extends BlockBufferBuilderPool {
    public NonStoringBuilderPool() {
        super(Collections.emptyList());
    }

    @Nullable
    @Override
    public BlockBufferBuilderStorage acquire() {
        return null;
    }

    @Override
    public void release(BlockBufferBuilderStorage blockBufferBuilderStorage) {
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
