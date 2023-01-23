package me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class BlockRenderContext {
    private final BlockRenderView world;

    private final BlockPos.Mutable pos = new BlockPos.Mutable();

    private final Vector3f origin = new Vector3f();

    private BlockState state;
    private BakedModel model;

    private long seed;

    public BlockRenderContext(BlockRenderView world) {
        this.world = world;
    }

    public void update(BlockPos pos, BlockPos origin, BlockState state, BakedModel model, long seed) {
        this.pos.set(pos);
        this.origin.set(origin.getX(), origin.getY(), origin.getZ());

        this.state = state;
        this.model = model;

        this.seed = seed;
    }

    /**
     * @return The position (in world space) of the block being rendered
     */
    public BlockPos pos() {
        return this.pos;
    }

    /**
     * @return The world which the block is being rendered from
     */
    public BlockRenderView world() {
        return this.world;
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
