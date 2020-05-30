package me.jellysquid.mods.sodium.client.render.occlusion;

import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.util.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class BlockOcclusionCache {
    private final Object2ByteOpenHashMap<NeighborGroup> map;
    private final NeighborGroup cache = new NeighborGroup();
    private final BlockPos.Mutable cpos = new BlockPos.Mutable();

    public BlockOcclusionCache() {
        this.map = new Object2ByteOpenHashMap<>(2048, 0.75F);
        this.map.defaultReturnValue((byte) 127);
    }

    /**
     * @param state The state of the block in the world
     * @param view The world view for this render context
     * @param pos The position of the block
     * @param dir The facing direction of the side to check
     * @return True if the block side facing {@param dir} is not occluded, otherwise false
     */
    public boolean shouldDrawSide(BlockState state, BlockView view, BlockPos pos, Direction dir) {
        BlockPos adjPos = this.cpos.set(pos).setOffset(dir);
        BlockState adjState = view.getBlockState(adjPos);

        if (!adjState.isOpaque()) {
            return true;
        }

        if (state.isSideInvisible(adjState, dir)) {
            return false;
        }

        NeighborGroup cache = this.cache;
        cache.self = state;
        cache.other = adjState;
        cache.facing = dir;

        Object2ByteOpenHashMap<NeighborGroup> map = this.map;

        byte cached = map.getByte(cache);

        if (cached != 127) {
            return cached != 0;
        }

        VoxelShape selfShape = state.getCullingFace(view, pos, dir);
        VoxelShape adjShape = adjState.getCullingFace(view, adjPos, dir.getOpposite());

        boolean ret = VoxelShapes.matchesAnywhere(selfShape, adjShape, BooleanBiFunction.ONLY_FIRST);

        map.put(cache.copy(), (byte) (ret ? 1 : 0));

        return ret;

    }

    public static final class NeighborGroup {
        private BlockState self;
        private BlockState other;

        private Direction facing;

        public NeighborGroup copy() {
            return new NeighborGroup(this.self, this.other, this.facing);
        }

        public NeighborGroup() {

        }

        private NeighborGroup(BlockState self, BlockState other, Direction facing) {
            this.self = self;
            this.other = other;
            this.facing = facing;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof NeighborGroup)) {
                return false;
            }

            NeighborGroup that = (NeighborGroup) o;

            return this.self == that.self && this.other == that.other && this.facing == that.facing;
        }

        @Override
        public int hashCode() {
            int result = this.self.hashCode();
            result = 31 * result + this.other.hashCode();
            result = 31 * result + this.facing.hashCode();

            return result;
        }
    }

}
