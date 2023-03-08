package me.jellysquid.mods.sodium.client.render.chunk.lists;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionFlags;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.util.SectionIterator;

public class RegionRenderLists {
    private RenderRegion region;

    private final int[] sectionsWithGeometry = createSectionList();
    private int sectionsWithGeometryCount;

    private final int[] sectionsWithEntities = createSectionList();
    private int sectionsWithEntitiesCount;

    private final int[] sectionsWithSprites = createSectionList();
    private int sectionsWithSpritesCount;

    protected void setRegion(RenderRegion region) {
        this.region = region;
    }

    protected void reset() {
        this.region = null;

        this.sectionsWithGeometryCount = ArrayListUtil.arrayClear(this.sectionsWithGeometry, this.sectionsWithGeometryCount);
        this.sectionsWithEntitiesCount = ArrayListUtil.arrayClear(this.sectionsWithEntities, this.sectionsWithEntitiesCount);
        this.sectionsWithSpritesCount = ArrayListUtil.arrayClear(this.sectionsWithSprites, this.sectionsWithSpritesCount);
    }

    public int size() {
        return this.sectionsWithGeometryCount;
    }

    public SectionIterator getSectionsWithGeometry(boolean reverse) {
        return new SectionIterator(this.region.getChunks(), this.sectionsWithGeometry, 0, this.sectionsWithGeometryCount, reverse);
    }

    public SectionIterator getSectionsWithBlockEntities(boolean reverse) {
        return new SectionIterator(this.region.getChunks(), this.sectionsWithEntities, 0, this.sectionsWithEntitiesCount, reverse);
    }

    public SectionIterator getSectionsWithSprites(boolean reverse) {
        return new SectionIterator(this.region.getChunks(), this.sectionsWithSprites, 0, this.sectionsWithSpritesCount, reverse);
    }
    public RenderRegion getRegion() {
        return this.region;
    }

    private static int[] createSectionList() {
        return new int[RenderRegion.REGION_SIZE];
    }

    public void add(int section, int flags) {
        if ((flags & RenderSectionFlags.HAS_BLOCK_GEOMETRY) != 0) {
            this.sectionsWithGeometryCount = ArrayListUtil.arrayPush(this.sectionsWithGeometry, this.sectionsWithGeometryCount, section);
        }

        if ((flags & RenderSectionFlags.HAS_BLOCK_ENTITIES) != 0) {
            this.sectionsWithEntitiesCount = ArrayListUtil.arrayPush(this.sectionsWithEntities, this.sectionsWithEntitiesCount, section);
        }

        if ((flags & RenderSectionFlags.HAS_ANIMATED_SPRITES) != 0) {
            this.sectionsWithSpritesCount = ArrayListUtil.arrayPush(this.sectionsWithSprites, this.sectionsWithSpritesCount, section);
        }
    }
}
