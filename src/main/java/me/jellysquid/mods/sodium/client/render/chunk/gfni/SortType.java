package me.jellysquid.mods.sodium.client.render.chunk.gfni;

/**
 * What type of sorting to use for a section. Calculated by a heuristic after
 * building a section.
 * 
 * TODO: maybe remove needsDynamicSort if they remain unused
 */
public enum SortType {
    /**
     * No sorting is required and the sort order doesn't matter.
     */
    NONE(false, false, false, false),

    /**
     * There is only one sort order. No active sorting is required, but an initial
     * sort where quads of each facing are sorted according to their distances in
     * regard to their normal.
     * 
     * Currently assumes that there are no UNASSIGNED quads present. If this
     * changes, remove this note and adjust StaticTranslucentData and anything that
     * reads from it to handle UNASSIGNED quads.
     */
    STATIC_NORMAL_RELATIVE(true, false, false, false),

    /**
     * There is only one sort order and not active sorting is required, but
     * determining the static sort order involves doing a toplogical sort of the
     * quads.
     */
    STATIC_TOPO_ACYCLIC(true, true, false, false),

    /**
     * There is more than one sort order but instead of sorting one each plane
     * trigger, a small number of sort orders is precomputed by breaking any cycles
     * in the graph. They are swapped out when the cycle-breaking planes are
     * triggered in GFNI.
     */
    // DYNAMIC_TOPO_CYCLIC(true, true, true, false),

    /**
     * There are multiple sort orders. Sorting is required every time GFNI triggers
     * this section.
     */
    DYNAMIC_ALL(true, true, true, true);

    public final boolean needsIndexBuffer;
    public final boolean needsDirectionMixing;
    public final boolean needsPlaneTrigger;
    public final boolean needsDynamicSort;

    private SortType(
            boolean needsIndexBuffer,
            boolean needsDirectionMixing,
            boolean needsPlaneTrigger,
            boolean needsDynamicSort) {
        this.needsIndexBuffer = needsIndexBuffer;
        this.needsDirectionMixing = needsDirectionMixing;
        this.needsPlaneTrigger = needsPlaneTrigger;
        this.needsDynamicSort = needsDynamicSort;
    }
}
