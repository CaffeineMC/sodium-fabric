package me.jellysquid.mods.sodium.client.render.chunk.lists;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionFlags;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import org.jetbrains.annotations.Nullable;

public class ChunkRenderList {
    private RenderRegion region;

    private final byte[] sectionsWithGeometry = new byte[RenderRegion.REGION_SIZE + 1];
    private int sectionsWithGeometryCount = 0;

    private final byte[] sectionsWithSprites = new byte[RenderRegion.REGION_SIZE + 1];
    private int sectionsWithSpritesCount = 0;

    private final byte[] sectionsWithEntities = new byte[RenderRegion.REGION_SIZE + 1];
    private int sectionsWithEntitiesCount = 0;

    private int size;

    ChunkRenderList() {

    }

    public void init(RenderRegion region) {
        this.region = region;

        this.sectionsWithGeometryCount = 0;
        this.sectionsWithSpritesCount = 0;
        this.sectionsWithEntitiesCount = 0;

        this.size = 0;
    }

    public void add(RenderSection render) {
        if (this.size >= RenderRegion.REGION_SIZE) {
            throw new ArrayIndexOutOfBoundsException("Render list is full");
        }

        this.size++;

        int index = render.getSectionIndex();
        int flags = render.getFlags();

        this.sectionsWithGeometry[this.sectionsWithGeometryCount] = (byte) index;
        this.sectionsWithGeometryCount += (flags >>> RenderSectionFlags.HAS_BLOCK_GEOMETRY) & 1;

        this.sectionsWithSprites[this.sectionsWithSpritesCount] = (byte) index;
        this.sectionsWithSpritesCount += (flags >>> RenderSectionFlags.HAS_ANIMATED_SPRITES) & 1;

        this.sectionsWithEntities[this.sectionsWithEntitiesCount] = (byte) index;
        this.sectionsWithEntitiesCount += (flags >>> RenderSectionFlags.HAS_BLOCK_ENTITIES) & 1;
    }

    public @Nullable ReversibleSectionIterator sectionsWithGeometryIterator(boolean reverse) {
        if (this.sectionsWithGeometryCount == 0) {
            return null;
        }

        return new ReversibleSectionIterator(this.sectionsWithGeometry, 0, this.sectionsWithGeometryCount, reverse);
    }

    public @Nullable SectionIterator sectionsWithSpritesIterator() {
        if (this.sectionsWithSpritesCount == 0) {
            return null;
        }

        return new SectionIterator(this.sectionsWithSprites, this.sectionsWithSpritesCount);
    }

    public @Nullable SectionIterator sectionsWithEntitiesIterator() {
        if (this.sectionsWithEntitiesCount == 0) {
            return null;
        }

        return new SectionIterator(this.sectionsWithEntities, this.sectionsWithEntitiesCount);
    }

    public int getSectionsWithGeometryCount() {
        return this.sectionsWithGeometryCount;
    }

    public int getSectionsWithSpritesCount() {
        return this.sectionsWithSpritesCount;
    }

    public int getSectionsWithEntitiesCount() {
        return this.sectionsWithEntitiesCount;
    }

    public int getSize() {
        return this.size;
    }

    public RenderRegion getRegion() {
        return this.region;
    }

    public static class ReversibleSectionIterator {
        private final byte[] elements;

        private final int step;

        private int cur;
        private int rem;

        public ReversibleSectionIterator(byte[] elements, int start, int end, boolean reverse) {
            this.elements = elements;
            this.rem = end - start;

            this.step = reverse ? -1 : 1;
            this.cur = reverse ? end - 1 : start;
        }

        public boolean hasNext() {
            return this.rem > 0;
        }

        public int next() {
            int result = Byte.toUnsignedInt(this.elements[this.cur]);

            this.cur += this.step;
            this.rem--;

            return result;
        }
    }

    public static class SectionIterator {
        private final byte[] elements;
        private final int lastIndex;

        private int index;

        public SectionIterator(byte[] elements, int lastIndex) {
            this.elements = elements;
            this.lastIndex = lastIndex;
            this.index = 0;
        }

        public boolean hasNext() {
            return this.index < this.lastIndex;
        }

        public int next() {
            return Byte.toUnsignedInt(this.elements[this.index++]);
        }
    }
}
