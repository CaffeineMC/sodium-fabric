package net.caffeinemc.sodium.render.chunk.compile.tasks;

import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.state.BuiltSectionGeometry;
import net.caffeinemc.sodium.render.chunk.state.SectionRenderData;

/**
 * The result of a chunk rebuild task which contains any and all data that needs to be processed or uploaded on
 * the main thread. If a task is cancelled after finishing its work and not before the result is processed, the result
 * will instead be discarded.
 */
public record SectionBuildResult(RenderSection section,
                                 SectionRenderData data,
                                 BuiltSectionGeometry geometry,
                                 int buildTime) {
    public void delete() {
        this.geometry.delete();
    }
}
