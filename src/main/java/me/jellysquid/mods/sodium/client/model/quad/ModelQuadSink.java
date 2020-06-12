package me.jellysquid.mods.sodium.client.model.quad;

/**
 * A "sink" interface which model quads can be written to for rendering. This is used as to provide an abstraction
 * over the various buffers and targets used by the game. The implementation may perform additional transformations
 * to the quad data which is passed to it.
 */
public interface ModelQuadSink {
    /**
     * Writes the specified quad to the sink. The implementation may transformed the input given to it to avoid memory
     * copies and allocations, and as such, the input {@param quad} will be undefined after calling this method.
     */
    void write(ModelQuadViewMutable quad);
}
