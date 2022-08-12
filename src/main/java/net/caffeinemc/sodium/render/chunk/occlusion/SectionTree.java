package net.caffeinemc.sodium.render.chunk.occlusion;

import java.util.Arrays;
import net.caffeinemc.sodium.interop.vanilla.math.frustum.Frustum;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.util.DirectionUtil;
import net.caffeinemc.sodium.util.collections.BitArray;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.HeightLimitView;

public class SectionTree {
    public static final int ABSENT_VALUE = 0xFFFFFFFF;
    
    private final int chunkViewDistance;
    private final int maxDepth; // inclusive
    
    private final int sectionHeightMin;
    private final int sectionHeightMax;
    private final int sectionWidth;
    private final int sectionHeight;
    private final int sectionWidthOffset;
    private final int sectionHeightOffset;
    private final int sectionTableSize;
    
    private final RenderSection[] sections;
    private final BitArray sectionExistenceBits;
    private final byte[] sectionVisibilityData;
    private final int[] sectionAdjacent;
    
    private int loadedSections;
    
    private int originSectionX;
    private int originSectionY;
    private int originSectionZ;

    public SectionTree(int maxDepth, int chunkViewDistance, HeightLimitView heightLimitView) {
        this.sectionHeightMin = heightLimitView.getBottomSectionCoord();
        this.sectionHeightMax = heightLimitView.getTopSectionCoord() - 1;
    
        this.chunkViewDistance = chunkViewDistance;
        this.maxDepth = maxDepth;
        
        this.sectionWidth = chunkViewDistance * 2 + 1;
        this.sectionWidthOffset = chunkViewDistance;
        this.sectionHeight = heightLimitView.countVerticalSections();
        this.sectionHeightOffset = -heightLimitView.getBottomSectionCoord();
        this.sectionTableSize = this.sectionWidth * this.sectionWidth * this.sectionHeight;
        
        this.sections = new RenderSection[this.sectionTableSize];
        this.sectionExistenceBits = new BitArray(this.sectionTableSize);
        this.sectionVisibilityData = new byte[this.sectionTableSize * DirectionUtil.COUNT];
        this.sectionAdjacent = new int[this.sectionTableSize * DirectionUtil.COUNT];
        Arrays.fill(this.sectionAdjacent, ABSENT_VALUE);
    }
    
    public int getSectionIdx(int x, int y, int z) {
        int tableY = this.originSectionY - y + this.sectionHeightOffset;
        int tableZ = this.originSectionZ - z + this.sectionWidthOffset;
        int tableX = this.originSectionX - x + this.sectionWidthOffset;
        if (tableY < 0 || tableY >= this.sectionHeight || tableZ < 0 || tableZ >= this.sectionWidth || tableX < 0 || tableX >= this.sectionWidth) {
            return ABSENT_VALUE;
        } else {
            return tableY * this.sectionWidth * this.sectionWidth
                   + tableZ * this.sectionWidth
                   + tableX;
        }
    }
    
    public void setOrigin(BlockPos origin) {
        this.originSectionY = MathHelper.clamp(
                ChunkSectionPos.getSectionCoord(origin.getY()),
                this.sectionHeightMin,
                this.sectionHeightMax
        );
        
        this.originSectionX = ChunkSectionPos.getSectionCoord(origin.getX());
        this.originSectionZ = ChunkSectionPos.getSectionCoord(origin.getZ());
    }
    
    public int getOriginSectionX() {
        return this.originSectionX;
    }
    
    public int getOriginSectionY() {
        return this.originSectionY;
    }
    
    public int getOriginSectionZ() {
        return this.originSectionZ;
    }
    
    public void findVisibleSections(BitArray resultBitsDest, Frustum frustum) {
        for (int idx = startIdx; idx < endIdx; idx++) {
            this.checkNode(resultBitsDest, this.nodes[idx], frustum);
        }
    }

    private void checkNode(BitArray vis, Node node, Frustum frustum) {
        if (node == null) {
            return;
        }
        
        switch (node.testBox(frustum)) {
            case Frustum.INTERSECT -> {
                // TODO: reimpl?
//                if (node.sectionIdx != ABSENT_VALUE) {
//                    vis.set(node.sectionIdx);
//                }
                vis.set(node.sectionIdx);

                for (var child : node.children) {
                    this.checkNode(vis, child, frustum);
                }
            }
            case Frustum.INSIDE -> this.markVisibleRecursive(vis, node);
        }
    }

    private void markVisibleRecursive(BitArray vis, Node node) {
        // TODO: reimpl?
//        if (node.sectionId != ABSENT_VALUE) {
//            vis.set(node.sectionId);
//        }
        
        // TODO: be smart about this, just figure out the whole array region indices and translate it into the bit
        //  array, no recursion needed
        
        vis.set(node.sectionIdx);

        for (var child : node.children) {
            this.markVisibleRecursive(vis, child);
        }
    }

    public int getSectionTableSize() {
        return this.sectionTableSize;
    }

    public RenderSection add(int x, int y, int z) {
        Node parentNode = null;
        int sectionIdx = ABSENT_VALUE;
        
        for (var depth = this.maxDepth; depth >= 0; depth--) {
            // TODO: keep track of origin, do bounds check and return absent if outside
            int nodeIdx = this.getNodeIdx(x, y, z, depth);
            Node node = this.nodes[nodeIdx];

            if (node == null) {
                node = this.createNode(x, y, z, depth, nodeIdx);

                if (parentNode != null) {
                    parentNode.addChild(node);
                }
            }

            parentNode = node;
            sectionIdx = nodeIdx;
        }

        if (parentNode == null) {
            throw new IllegalStateException();
        }
    
        RenderSection section = this.createSection(x, y, z);

        parentNode.sectionIdx = sectionIdx;

        return section;
    }

    private RenderSection createSection(int x, int y, int z) {
        this.loadedSections++;
    
        RenderSection section = new RenderSection(x, y, z);
        int sectionIdx = this.getSectionIdx(x, y, z);
        
        this.sections[sectionIdx] = section;

        this.connectAdjacentSections(x, y, z, sectionIdx);

        return section;
    }

    private void connectAdjacentSections(int x, int y, int z, int sectionIdx) {
        for (Direction direction : DirectionUtil.ALL_DIRECTIONS) {
            int adjacentSectionIdx = this.getSectionIdx(
                    x + direction.getOffsetX(),
                    y + direction.getOffsetY(),
                    z + direction.getOffsetZ()
            );

            if (adjacentSectionIdx != ABSENT_VALUE) {
                this.sectionAdjacent[(adjacentSectionIdx * DirectionUtil.COUNT) + DirectionUtil.getOppositeId(direction.ordinal())] = sectionIdx;
            }

            this.sectionAdjacent[(sectionIdx * DirectionUtil.COUNT) + direction.ordinal()] = adjacentSectionIdx;
        }
    }

    private void disconnectAdjacentSections(int x, int y, int z) {
        for (var direction : DirectionUtil.ALL_DIRECTIONS) {
            int adjacentSectionIdx = this.getSectionIdx(
                    x + direction.getOffsetX(),
                    y + direction.getOffsetY(),
                    z + direction.getOffsetZ()
            );

            if (adjacentSectionIdx != ABSENT_VALUE) {
                this.sectionAdjacent[(adjacentSectionIdx * DirectionUtil.COUNT) + DirectionUtil.getOppositeId(direction.ordinal())] = ABSENT_VALUE;
            }
        }
    }

    public RenderSection remove(int x, int y, int z) {
        this.loadedSections--;
        
        int sectionIdx = this.getSectionIdx(x, y, z);
        
        // TODO: reimpl?
//        if (sectionId == ABSENT_VALUE) {
//            throw new NoSuchElementException();
//        }

        var section = this.sections[sectionIdx];

        this.sections[sectionIdx] = null;

        this.disconnectAdjacentSections(x, y, z);

        Node lastChildNode = null;

        for (var depth = 0; depth <= this.maxDepth; depth++) {
            var nodeX = x >> depth;
            var nodeY = y >> depth;
            var nodeZ = z >> depth;

            var nodeMap = this.nodeLookup[depth];
            var nodeKey = ChunkSectionPos.asLong(nodeX, nodeY, nodeZ);
            var nodeId = nodeMap.get(nodeKey);

            if (nodeId == ABSENT_VALUE) {
                throw new IllegalStateException();
            }

            var node = this.nodes[nodeId];

            if (lastChildNode != null) {
                node.removeChild(lastChildNode);
            } else {
                node.sectionId = ABSENT_VALUE;
            }

            if (node.isEmpty()) {
                nodeMap.remove(nodeKey);

                this.nodeIdPool.free(nodeId);
                this.nodes[nodeId] = null;
            } else {
                break;
            }

            lastChildNode = node;
        }

        return section;
    }

    private Node createNode(int x, int y, int z, int depth, int nodeIdx) {
        int length = (1 << depth) * 16;
        
        int nodeX = x >> depth;
        int nodeY = y >> depth;
        int nodeZ = z >> depth;

        float minX = nodeX * length;
        float minY = nodeY * length;
        float minZ = nodeZ * length;

        float maxX = minX + length;
        float maxY = minY + length;
        float maxZ = minZ + length;

        var node = new Node(minX, minY, minZ, maxX, maxY, maxZ);

        this.nodes[nodeIdx] = node;

        return node;
    }

    public RenderSection getSection(int x, int y, int z) {
        return this.getSection(this.getSectionIdx(x, y, z));
    }

    public RenderSection getSection(int sectionIdx) {
        // TODO: reimpl?
//        if (sectionId == ABSENT_VALUE) {
//            return null;
//        }

        return this.sections[sectionIdx];
    }

    public RenderSection getSectionById(int id) {
        return this.sections[id];
    }

    public int getAdjacent(int id, int direction) {
        return this.sectionAdjacent[(id * DirectionUtil.COUNT) + direction];
    }
    
    public int getChunkViewDistance() {
        return this.chunkViewDistance;
    }
    
    public int getSectionHeightMin() {
        return this.sectionHeightMin;
    }
    
    public int getSectionHeightMax() {
        return this.sectionHeightMax;
    }
    
    public int getSectionWidth() {
        return this.sectionWidth;
    }
    
    public int getSectionHeight() {
        return this.sectionHeight;
    }
    
    public int getLoadedSections() {
        return this.loadedSections;
    }

    public void setVisibilityData(int sectionIdx, ChunkOcclusionData data) {
        for (var from : DirectionUtil.ALL_DIRECTIONS) {
            int bits = 0;

            if (data != null) {
                for (var to : DirectionUtil.ALL_DIRECTIONS) {
                    if (data.isVisibleThrough(from, to)) {
                        bits |= 1 << to.ordinal();
                    }
                }
            }

            this.sectionVisibilityData[(sectionIdx * DirectionUtil.COUNT) + from.ordinal()] = (byte) bits;
        }
    }

    public int getVisibilityData(int sectionIdx, int incomingDirection) {
        return this.sectionVisibilityData[(sectionIdx * DirectionUtil.COUNT) + incomingDirection];
    }
    
}
