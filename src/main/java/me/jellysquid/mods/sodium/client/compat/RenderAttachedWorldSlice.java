package me.jellysquid.mods.sodium.client.compat;

import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

class RenderAttachedWorldSlice extends WorldSlice implements RenderAttachedBlockView {
    public RenderAttachedWorldSlice(World world) {
        super(world);
    }

    @Override
    public @Nullable Object getBlockEntityRenderAttachment(BlockPos pos) {
        int relX = pos.getX() - this.baseX;
        int relY = pos.getY() - this.baseY;
        int relZ = pos.getZ() - this.baseZ;

        return this.sections[WorldSlice.getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4)]
                .getBlockEntityRenderAttachment(relX & 15, relY & 15, relZ & 15);
    }
}
