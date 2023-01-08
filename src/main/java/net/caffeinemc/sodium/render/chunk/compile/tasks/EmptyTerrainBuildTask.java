package net.caffeinemc.sodium.render.chunk.compile.tasks;

import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.state.BuiltSectionGeometry;
import net.caffeinemc.sodium.render.chunk.state.SectionRenderData;
import net.caffeinemc.sodium.render.terrain.TerrainBuildContext;
import net.caffeinemc.sodium.util.tasks.CancellationSource;

/**
 * A build task which does no computation and always return an empty build result. These tasks are created whenever
 * chunk meshes need to be deleted as the only way to change graphics state is to send a message to the main
 * actor thread. In cases where new chunk renders are being created and scheduled, the scheduler will prefer to just
 * synchronously update the section's data to an empty state to speed things along.
 */
@Deprecated
public class EmptyTerrainBuildTask extends AbstractBuilderTask {
    private final RenderSection render;
    private final int frame;

    public EmptyTerrainBuildTask(RenderSection render, int frame) {
        this.render = render;
        this.frame = frame;
    }

    @Override
    public SectionBuildResult performBuild(TerrainBuildContext context, CancellationSource cancellationSource) {
        return new SectionBuildResult(this.render, SectionRenderData.EMPTY, BuiltSectionGeometry.empty(), this.frame);
    }
}
