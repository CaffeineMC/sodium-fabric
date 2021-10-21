package me.jellysquid.mods.sodium.mixin.features.render_layer.leaves;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
// set priority to 100 so other mods can take over this mixin like cull leaves
@Mixin(value = LeavesBlock.class, priority = 100)
public class MixinLeavesBlock extends Block {
    public MixinLeavesBlock() {
        super(Settings.of(Material.AIR));
        throw new AssertionError("Mixin constructor called!");
    }

    @Override
    public boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
        if (SodiumClientMod.options().quality.leavesQuality.isFancy(MinecraftClient.getInstance().options.graphicsMode)) {
            return super.isSideInvisible(state, stateFrom, direction);
        } else {
            return stateFrom.getBlock() instanceof LeavesBlock || super.isSideInvisible(state, stateFrom, direction);
        }
    }
}
