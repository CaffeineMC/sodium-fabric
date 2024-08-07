package net.caffeinemc.mods.sodium.client.services;

import net.caffeinemc.mods.sodium.client.world.SodiumAuxiliaryLightManager;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

public interface PlatformLevelAccess {
    PlatformLevelAccess INSTANCE = Services.load(PlatformLevelAccess.class);

    static PlatformLevelAccess getInstance() {
        return INSTANCE;
    }

    /**
     * Gets the specialized render data for this block entity.
     * @param blockEntity The block entity to get the render data of.
     * @return The specialized render data for this block entity. If the platform does not support it or there is no data, null.
     */
    @Nullable
    Object getBlockEntityData(BlockEntity blockEntity);

    /**
     * Gets the current light manager for the chunk section.
     * @param chunk The current chunk.
     * @param pos The section within that chunk being drawn.
     * @return The current light manager, or null
     */
    @Nullable SodiumAuxiliaryLightManager getLightManager(LevelChunk chunk, SectionPos pos);
}
