package me.jellysquid.mods.sodium.client.render.chunk;

import com.google.common.collect.Queues;
import net.minecraft.class_8901;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class NonStoringSectionPack extends class_8901 {
    public NonStoringSectionPack() {
        super(Collections.emptyList());
    }

    @Nullable
    @Override
    public BlockBufferBuilderStorage method_54642() {
        return null;
    }

    @Override
    public void method_54644(BlockBufferBuilderStorage blockBufferBuilderStorage) {
    }

    @Override
    public boolean method_54645() {
        return true;
    }

    @Override
    public int method_54646() {
        return 0;
    }
}
