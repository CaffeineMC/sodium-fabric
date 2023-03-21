package me.jellysquid.mods.sodium.client.render.chunk.lists;

import me.jellysquid.mods.sodium.client.render.chunk.graph.GraphNodeFlags;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.util.SectionIterator;

import java.util.Arrays;

public class RegionRenderLists {
    public RenderRegion region;

    private final byte[] sectionsWithGeometry = createSectionList();
    private int sectionsWithGeometryCount;

    private final byte[] sectionsWithEntities = createSectionList();
    private int sectionsWithEntitiesCount;

    private final byte[] sectionsWithSprites = createSectionList();
    private int sectionsWithSpritesCount;

    public int getSectionsWithGeometryCount() {
        return this.sectionsWithGeometryCount;
    }

    public SectionIterator getSectionsWithGeometry(boolean reverse) {
        return new SectionIterator(this.sectionsWithGeometry, 0, this.sectionsWithGeometryCount, reverse);
    }

    public SectionIterator getSectionsWithBlockEntities(boolean reverse) {
        return new SectionIterator(this.sectionsWithEntities, 0, this.sectionsWithEntitiesCount, reverse);
    }

    public SectionIterator getSectionsWithSprites(boolean reverse) {
        return new SectionIterator(this.sectionsWithSprites, 0, this.sectionsWithSpritesCount, reverse);
    }

    public RenderRegion getRegion() {
        return this.region;
    }

    public void add(int flags, byte section) {
        this.sectionsWithGeometry[this.sectionsWithGeometryCount] = section;
        this.sectionsWithGeometryCount += (flags >> GraphNodeFlags.HAS_BLOCK_GEOMETRY) & 1;

        this.sectionsWithEntities[this.sectionsWithEntitiesCount] = section;
        this.sectionsWithEntitiesCount += (flags >> GraphNodeFlags.HAS_BLOCK_ENTITIES) & 1;

        this.sectionsWithSprites[this.sectionsWithSpritesCount] = section;
        this.sectionsWithSpritesCount += (flags >> GraphNodeFlags.HAS_ANIMATED_SPRITES) & 1;
    }

    private static byte[] createSectionList() {
        return new byte[RenderRegion.REGION_SIZE];
    }

    public boolean isEmpty() {
        return this.sectionsWithGeometryCount == 0 && this.sectionsWithEntitiesCount == 0 && this.sectionsWithSpritesCount == 0;
    }

    public void reset() {
        Arrays.fill(this.sectionsWithGeometry, 0, this.sectionsWithGeometryCount, (byte) 0);
        Arrays.fill(this.sectionsWithEntities, 0, this.sectionsWithEntitiesCount, (byte) 0);
        Arrays.fill(this.sectionsWithSprites, 0, this.sectionsWithSpritesCount, (byte) 0);

        this.sectionsWithGeometryCount = 0;
        this.sectionsWithEntitiesCount = 0;
        this.sectionsWithSpritesCount = 0;
    }
}
