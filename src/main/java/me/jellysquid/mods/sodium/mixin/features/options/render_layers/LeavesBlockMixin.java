package me.jellysquid.mods.sodium.mixin.features.options.render_layers;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LeavesBlock.class)
public class LeavesBlockMixin extends Block {
    public LeavesBlockMixin() {
        super(Settings.copy(Blocks.AIR));
        throw new AssertionError("Mixin constructor called!");
    }

    @Override
    public boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
        if (SodiumClientMod.options().quality.leavesQuality.isFancy(MinecraftClient.getInstance().options.getGraphicsMode().getValue())) {
            return super.isSideInvisible(state, stateFrom, direction);
        } else {
            return stateFrom.getBlock() instanceof LeavesBlock || super.isSideInvisible(state, stateFrom, direction);
        }
    }
}
