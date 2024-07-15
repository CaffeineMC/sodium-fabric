package net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline;

import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class BlockRenderContext {
    private final LevelSlice slice;
    public final TranslucentGeometryCollector collector;

    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    private final Vector3f origin = new Vector3f();

    private BlockState state;
    private BakedModel model;

    private long seed;


    public BlockRenderContext(LevelSlice slice, TranslucentGeometryCollector collector) {
        this.slice = slice;
        this.collector = collector;
    }

    public void update(BlockPos pos, BlockPos origin, BlockState state, BakedModel model, long seed) {
        this.pos.set(pos);
        this.origin.set(origin.getX(), origin.getY(), origin.getZ());

        this.state = state;
        this.model = model;

        this.seed = seed;
    }

    /**
     * @return The collector for translucent geometry sorting
     */
    public TranslucentGeometryCollector collector() {
        return this.collector;
    }

    /**
     * @return The position (in block space) of the block being rendered
     */
    public BlockPos pos() {
        return this.pos;
    }

    /**
     * @return The level which the block is being rendered from
     */
    public LevelSlice slice() {
        return this.slice;
    }

    /**
     * @return The state of the block being rendered
     */
    public BlockState state() {
        return this.state;
    }

    /**
     * @return The model used for this block
     */
    public BakedModel model() {
        return this.model;
    }

    /**
     * @return The origin of the block within the model
     */
    public Vector3fc origin() {
        return this.origin;
    }

    /**
     * @return The PRNG seed for rendering this block
     */
    public long seed() {
        return this.seed;
    }
}
