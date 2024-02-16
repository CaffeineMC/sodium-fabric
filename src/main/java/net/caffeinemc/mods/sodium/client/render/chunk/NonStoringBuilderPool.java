package net.caffeinemc.mods.sodium.client.render.chunk;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SectionBufferBuilderPool;

public class NonStoringBuilderPool extends SectionBufferBuilderPool {
    public NonStoringBuilderPool() {
        super(Collections.emptyList());
    }

    @Nullable
    @Override
    public SectionBufferBuilderPack acquire() {
        return null;
    }

    @Override
    public void release(SectionBufferBuilderPack blockBufferBuilderStorage) {
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public int getFreeBufferCount() {
        return 0;
    }
}
