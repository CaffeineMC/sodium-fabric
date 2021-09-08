package me.jellysquid.mods.thingl.device;

import me.jellysquid.mods.thingl.functions.DeviceFunctions;
import me.jellysquid.mods.thingl.lists.PipelineCommandList;
import me.jellysquid.mods.thingl.pipeline.RenderPipeline;
import me.jellysquid.mods.thingl.state.StateTracker;

import java.util.function.Consumer;

public interface RenderDevice extends ResourceFactory, ResourceAccess, ResourceDestructors {
    static RenderDevice create(StateTracker tracker) {
        return new RenderDeviceImpl(tracker);
    }

    DeviceFunctions getDeviceFunctions();

    void usePipeline(RenderPipeline pipeline, Consumer<PipelineCommandList> consumer);
}
