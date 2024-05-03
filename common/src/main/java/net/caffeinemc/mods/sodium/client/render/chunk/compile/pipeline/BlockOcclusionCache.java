package net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenCustomHashMap;
import net.caffeinemc.mods.sodium.client.SodiumMultiPlat;
import net.caffeinemc.mods.sodium.client.services.SodiumPlatformHelpers;
import net.caffeinemc.mods.sodium.client.util.DirectionUtil;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockOcclusionCache {
    private static final int CACHE_SIZE = 512;

    private static final int ENTRY_ABSENT = -1;
    private static final int ENTRY_FALSE = 0;
    private static final int ENTRY_TRUE = 1;


    private final Object2IntLinkedOpenCustomHashMap<ShapeComparison> comparisonLookupTable;
    private final ShapeComparison cachedComparisonObject = new ShapeComparison();
    private final BlockPos.MutableBlockPos cachedPositionObject = new BlockPos.MutableBlockPos();

    public BlockOcclusionCache() {
        this.comparisonLookupTable = new Object2IntLinkedOpenCustomHashMap<>(CACHE_SIZE, 0.5F, new ShapeComparison.ShapeComparisonStrategy());
        this.comparisonLookupTable.defaultReturnValue(ENTRY_ABSENT);
    }

    /**
     * @param selfState The state of the block in the level
     * @param view The block view for this render context
     * @param selfPos The position of the block
     * @param facing The facing direction of the side to check
     * @return True if the block side facing {@param dir} is not occluded, otherwise false
     */
    public boolean shouldDrawSide(BlockState selfState, BlockGetter view, BlockPos selfPos, Direction facing) {
        BlockPos.MutableBlockPos otherPos = this.cachedPositionObject;
        otherPos.set(selfPos.getX() + facing.getStepX(), selfPos.getY() + facing.getStepY(), selfPos.getZ() + facing.getStepZ());

        BlockState otherState = view.getBlockState(otherPos);

        // Blocks can define special behavior to control whether faces are rendered.
        // This is mostly used by transparent blocks (Leaves, Glass, etc.) to not render interior faces between blocks
        // of the same type.
        if (selfState.skipRendering(otherState, facing) || SodiumPlatformHelpers.INSTANCE.shouldSkipRender(view, selfState, otherState, selfPos, facing)) {
            return false;
        }

        // If the other block is not using a solid block model, then it is unable to hide any geometry. This check
        // differs from the one that Minecraft normally uses (checking whether the block state is marked as being
        // able to occlude other blocks), and instead uses heuristics about the block model and render type to determine
        // whether the block model is able to occlude block faces.
        if (isTransparentBlockModel(otherState)) {
            return true;
        }

        // The cull shape of the block being rendered
        VoxelShape selfShape = selfState.getFaceOcclusionShape(view, selfPos, facing);

        // If the block being rendered has an empty cull shape, intersection tests will always fail
        if (selfShape.isEmpty()) {
            return true;
        }

        // The cull shape of the block neighboring the one being rendered
        VoxelShape otherShape = otherState.getFaceOcclusionShape(view, otherPos, DirectionUtil.getOpposite(facing));

        // If the other block has an empty cull shape, then it cannot hide any geometry
        if (otherShape.isEmpty()) {
            return true;
        }

        // If both blocks use a full-cube cull shape, then they will always hide the faces between each other
        if (selfShape == Shapes.block() && otherShape == Shapes.block()) {
            return false;
        }

        // No other simplifications apply, so we need to perform a full shape comparison, which is very slow
        return this.lookup(selfShape, otherShape);
    }

    private static boolean isTransparentBlockModel(BlockState otherState) {
        // Only blocks which are using block models will be baked into the chunk mesh. If the adjacent block
        // is not using a block model, then we can't eliminate this block's face because the other block's geometry
        // would not be statically known.
        if (otherState.getRenderShape() != RenderShape.MODEL) {
            return true;
        }

        // Since all transparent blocks must already be marked as not being able to occlude other blocks, this should
        // not cause any issues. However, it fixes a problem where Leaves (which change their render type in
        // fast graphics mode) cannot occlude the geometry of other blocks.
        return ItemBlockRenderTypes.getChunkRenderType(otherState) != RenderType.solid();
    }

    private boolean lookup(VoxelShape self, VoxelShape other) {
        ShapeComparison comparison = this.cachedComparisonObject;
        comparison.self = self;
        comparison.other = other;

        // Entries at the cache are promoted to the top of the table when accessed
        // The entries at the bottom of the table are removed when it gets too large
        return switch (this.comparisonLookupTable.getAndMoveToFirst(comparison)) {
            case ENTRY_FALSE -> false;
            case ENTRY_TRUE -> true;
            default -> this.calculate(comparison);
        };
    }

    private boolean calculate(ShapeComparison comparison) {
        boolean result = Shapes.joinIsNotEmpty(comparison.self, comparison.other, BooleanOp.ONLY_FIRST);

        // Remove entries while the table is too large
        while (this.comparisonLookupTable.size() >= CACHE_SIZE) {
            this.comparisonLookupTable.removeLastInt();
        }

        this.comparisonLookupTable.putAndMoveToFirst(comparison.copy(), (result ? ENTRY_TRUE : ENTRY_FALSE));

        return result;
    }

    private static final class ShapeComparison {
        private VoxelShape self, other;

        private ShapeComparison() {

        }

        private ShapeComparison(VoxelShape self, VoxelShape other) {
            this.self = self;
            this.other = other;
        }

        public static class ShapeComparisonStrategy implements Hash.Strategy<ShapeComparison> {
            @Override
            public int hashCode(ShapeComparison value) {
                int result = System.identityHashCode(value.self);
                result = 31 * result + System.identityHashCode(value.other);

                return result;
            }

            @Override
            public boolean equals(ShapeComparison a, ShapeComparison b) {
                if (a == b) {
                    return true;
                }

                if (a == null || b == null) {
                    return false;
                }

                return a.self == b.self && a.other == b.other;
            }
        }

        public ShapeComparison copy() {
            return new ShapeComparison(this.self, this.other);
        }
    }
}
