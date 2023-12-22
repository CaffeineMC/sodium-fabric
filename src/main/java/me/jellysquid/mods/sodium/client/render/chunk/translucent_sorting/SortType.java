package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting;

/**
 * What type of sorting to use for a section. Calculated by a heuristic after
 * building a section.
 */
public enum SortType {
    /**
     * No sorting is required and the sort order doesn't matter.
     */
    NONE(false, false, false),

    /**
     * There is only one sort order. No active sorting is required, but an initial
     * sort where quads of each facing are sorted according to their distances in
     * regard to their normal.
     * 
     * Currently assumes that there are no UNASSIGNED quads present. If this
     * changes, remove this note and adjust StaticTranslucentData and anything that
     * reads from it to handle UNASSIGNED quads.
     */
    STATIC_NORMAL_RELATIVE(true, false, false),

    /**
     * There is only one sort order and not active sorting is required, but
     * determining the static sort order involves doing a toplogical sort of the
     * quads.
     */
    STATIC_TOPO(true, true, false),

    /**
     * There are multiple sort orders. Sorting is required every time GFNI triggers
     * this section.
     */
    DYNAMIC(true, true, true);

    public final boolean needsIndexBuffer;
    public final boolean needsDirectionMixing;
    public final boolean needsTrigger;

    private SortType(
            boolean needsIndexBuffer,
            boolean needsDirectionMixing,
            boolean needsTrigger) {
        this.needsIndexBuffer = needsIndexBuffer;
        this.needsDirectionMixing = needsDirectionMixing;
        this.needsTrigger = needsTrigger;
    }
}
