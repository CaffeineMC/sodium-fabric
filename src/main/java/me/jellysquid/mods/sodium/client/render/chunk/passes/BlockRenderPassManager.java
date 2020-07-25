package me.jellysquid.mods.sodium.client.render.chunk.passes;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class BlockRenderPassManager {
    private final EnumMap<WorldRenderPhase, List<BlockRenderPass>> byPhase = new EnumMap<>(WorldRenderPhase.class);
    private final List<BlockRenderPass> all = new ArrayList<>();

    public BlockRenderPassManager() {
        for (WorldRenderPhase pass : WorldRenderPhase.values()) {
            this.byPhase.put(pass, new ArrayList<>());
        }
    }

    public <R extends BlockRenderPass> void add(WorldRenderPhase phase, Identifier id, Factory<R> factory, BlockLayer... layers) {
        R pass = factory.create(this.all.size(), id, layers);

        this.byPhase.get(phase).add(pass);
        this.all.add(pass);
    }

    public Iterable<BlockRenderPass> getSortedPasses() {
        return this.all;
    }

    public Iterable<BlockRenderPass> getPassesForPhase(WorldRenderPhase phase) {
        return this.byPhase.get(phase);
    }

    public int getPassCount() {
        return this.all.size();
    }

    public interface Factory<R> {
        R create(int ordinal, Identifier id, BlockLayer[] layers);
    }
}
