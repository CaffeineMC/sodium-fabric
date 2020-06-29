package me.jellysquid.mods.sodium.client.render.occlusion;

import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class BlockOcclusionCache {
    private static final byte UNCACHED_VALUE = (byte) 127;

    private final Object2ByteOpenHashMap<CachedOcclusionShapeTest> map;
    private final CachedOcclusionShapeTest cachedTest = new CachedOcclusionShapeTest();
    private final BlockPos.Mutable cpos = new BlockPos.Mutable();

    public BlockOcclusionCache() {
        this.map = new Object2ByteOpenHashMap<>(2048, 0.75F);
        this.map.defaultReturnValue(UNCACHED_VALUE);
    }

    /**
     * @param state The state of the block in the world
     * @param view The world view for this render context
     * @param pos The position of the block
     * @param facing The facing direction of the side to check
     * @return True if the block side facing {@param dir} is not occluded, otherwise false
     */
    public boolean shouldDrawSide(BlockState state, BlockView view, BlockPos pos, Direction facing) {
        BlockPos adjPos = this.cpos.set(pos).offset(facing);
        BlockState adjState = view.getBlockState(adjPos);

        if (state.isSideInvisible(adjState, facing)) {
            return false;
        }

        if (!adjState.isOpaque()) {
            return true;
        }

        VoxelShape selfShape = state.getCullingFace(view, pos, facing);
        VoxelShape adjShape = adjState.getCullingFace(view, adjPos, facing.getOpposite());

        if (selfShape == VoxelShapes.fullCube() && adjShape == VoxelShapes.fullCube()) {
            return false;
        }

        CachedOcclusionShapeTest cache = this.cachedTest;
        cache.a = selfShape;
        cache.b = adjShape;

        byte cached = this.map.getByte(cache);

        if (cached != UNCACHED_VALUE) {
            return cached == 1;
        }

        boolean ret = VoxelShapes.matchesAnywhere(selfShape, adjShape, BooleanBiFunction.ONLY_FIRST);

        this.map.put(cache.copy(), (byte) (ret ? 1 : 0));

        return ret;
    }

    private static final class CachedOcclusionShapeTest {
        private VoxelShape a, b;

        private CachedOcclusionShapeTest() {

        }

        private CachedOcclusionShapeTest(VoxelShape a, VoxelShape b) {
            this.a = a;
            this.b = b;
        }

        public CachedOcclusionShapeTest copy() {
            return new CachedOcclusionShapeTest(this.a, this.b);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CachedOcclusionShapeTest)) {
                return false;
            }

            CachedOcclusionShapeTest that = (CachedOcclusionShapeTest) o;

            return this.a == that.a && this.b == that.b;
        }

        @Override
        public int hashCode() {
            int result = System.identityHashCode(this.a);
            result = 31 * result + System.identityHashCode(this.b);

            return result;
        }
    }
}
