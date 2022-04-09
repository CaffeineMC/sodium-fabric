package net.caffeinemc.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.bytes.ByteArrays;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import net.caffeinemc.sodium.interop.vanilla.math.frustum.Frustum;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.util.DirectionUtil;
import net.caffeinemc.sodium.util.collections.BitArray;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.util.math.ChunkSectionPos;
import org.apache.commons.lang3.ArrayUtils;

import java.util.NoSuchElementException;

public class ChunkTree {
    public static final int ABSENT_VALUE = 0;

    private final IntPool nodeIdPool = new IntPool();
    private final IntPool sectionIdPool = new IntPool();

    private final Long2IntOpenHashMap[] nodeLookup;
    private final Long2IntOpenHashMap sectionLookup;

    private RenderSection[] sections = new RenderSection[4096];
    private byte[] sectionVisibilityData = new byte[4096 * 6];
    private int[] sectionAdjacent = new int[4096 * 6];

    private Node[] nodes = new Node[5120];

    private final int maxDepth;

    private final Factory<RenderSection> factory;

    public ChunkTree(int maxDepth, Factory<RenderSection> factory) {
        this.maxDepth = maxDepth;
        this.factory = factory;

        this.nodeLookup = new Long2IntOpenHashMap[maxDepth];

        for (var i = 0; i < maxDepth; i++) {
            this.nodeLookup[i] = new Long2IntOpenHashMap();
            this.nodeLookup[i].defaultReturnValue(ABSENT_VALUE);
        }

        this.sectionLookup = new Long2IntOpenHashMap();
        this.sectionLookup.defaultReturnValue(ABSENT_VALUE);
    }

    public BitArray findVisibleSections(Frustum frustum) {
        var results = new BitArray(this.getSectionTableSize());

        var depth = this.maxDepth - 1;
        var nodes = this.nodeLookup[depth].values();
        var iterator = nodes.intIterator();

        while (iterator.hasNext()) {
            this.checkNode(results, this.nodes[iterator.nextInt()], frustum);
        }

        return results;
    }

    private void checkNode(BitArray vis, Node node, Frustum frustum) {
        switch (node.testBox(frustum)) {
            case Frustum.INTERSECT -> {
                if (node.sectionId != ABSENT_VALUE) {
                    vis.set(node.sectionId);
                }

                for (var child : node.children) {
                    this.checkNode(vis, child, frustum);
                }
            }
            case Frustum.INSIDE -> this.markVisibleRecursive(vis, node);
        }
    }

    private void markVisibleRecursive(BitArray vis, Node node) {
        if (node.sectionId != ABSENT_VALUE) {
            vis.set(node.sectionId);
        }

        for (var child : node.children) {
            this.markVisibleRecursive(vis, child);
        }
    }

    public int getSectionTableSize() {
        return this.sectionIdPool.capacity();
    }

    public RenderSection add(int chunkX, int chunkY, int chunkZ) {
        Node parentNode = null;

        for (var depth = this.maxDepth - 1; depth >= 0; depth--) {
            var nodeX = chunkX >> depth;
            var nodeY = chunkY >> depth;
            var nodeZ = chunkZ >> depth;

            var nodeMap = this.nodeLookup[depth];
            var nodeKey = ChunkSectionPos.asLong(nodeX, nodeY, nodeZ);
            var nodeId = nodeMap.get(nodeKey);

            Node node;

            if (nodeId == ABSENT_VALUE) {
                node = this.createNode(nodeX, nodeY, nodeZ, depth, this.nodeIdPool.create());

                if (parentNode != null) {
                    parentNode.addChild(node);
                }
            } else {
                node = this.nodes[nodeId];
            }

            parentNode = node;
        }

        if (parentNode == null) {
            throw new IllegalStateException();
        }

        var sectionId = this.sectionIdPool.create();
        var section = this.createSection(chunkX, chunkY, chunkZ, sectionId);

        parentNode.sectionId = sectionId;

        return section;
    }

    private RenderSection createSection(int x, int y, int z, int id) {
        var section = this.factory.create(x, y, z, id);

        if (this.sections.length <= id) {
            this.sections = ObjectArrays.grow(this.sections, id + 1);
            this.sectionVisibilityData = ByteArrays.grow(this.sectionVisibilityData, (id * 6) + 6);
            this.sectionAdjacent = IntArrays.grow(this.sectionAdjacent, (id * 6) + 6);
        }

        this.sections[id] = section;
        this.sectionLookup.put(ChunkSectionPos.asLong(x, y, z), id);

        this.connectAdjacentSections(x, y, z);

        return section;
    }

    private void connectAdjacentSections(int x, int y, int z) {
        var nodePosition = ChunkSectionPos.asLong(x, y, z);
        var nodeId = this.sectionLookup.get(nodePosition);

        for (var direction : DirectionUtil.ALL_DIRECTIONS) {
            var adjacentNodeId = this.sectionLookup.get(ChunkSectionPos.offset(nodePosition, direction));

            if (adjacentNodeId != ABSENT_VALUE) {
                this.sectionAdjacent[(adjacentNodeId * 6) + DirectionUtil.getOppositeId(direction.ordinal())] = nodeId;
            }

            this.sectionAdjacent[(nodeId * 6) + direction.ordinal()] = adjacentNodeId;
        }
    }

    private void disconnectAdjacentSections(int x, int y, int z) {
        var nodePosition = ChunkSectionPos.asLong(x, y, z);

        for (var direction : DirectionUtil.ALL_DIRECTIONS) {
            var adjacentNodeId = this.sectionLookup.get(ChunkSectionPos.offset(nodePosition, direction));

            if (adjacentNodeId != ABSENT_VALUE) {
                this.sectionAdjacent[(adjacentNodeId * 6) + DirectionUtil.getOppositeId(direction.ordinal())] = ABSENT_VALUE;
            }
        }
    }

    public RenderSection remove(int chunkX, int chunkY, int chunkZ) {
        var sectionId = this.sectionLookup.remove(ChunkSectionPos.asLong(chunkX, chunkY, chunkZ));

        if (sectionId == ABSENT_VALUE) {
            throw new NoSuchElementException();
        }

        var section = this.sections[sectionId];

        this.sections[sectionId] = null;
        this.sectionIdPool.free(sectionId);

        this.disconnectAdjacentSections(chunkX, chunkY, chunkZ);

        Node lastChildNode = null;

        for (var depth = 0; depth < this.maxDepth; depth++) {
            var nodeX = chunkX >> depth;
            var nodeY = chunkY >> depth;
            var nodeZ = chunkZ >> depth;

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

    private Node createNode(int x, int y, int z, int depth, int nodeId) {
        int length = (1 << depth) * 16;

        float minX = x * length;
        float minY = y * length;
        float minZ = z * length;

        float maxX = minX + length;
        float maxY = minY + length;
        float maxZ = minZ + length;

        var node = new Node(minX, minY, minZ, maxX, maxY, maxZ);

        if (this.nodes.length <= nodeId) {
            this.nodes = ObjectArrays.grow(this.nodes, nodeId + 1);
        }

        this.nodes[nodeId] = node;
        this.nodeLookup[depth].put(ChunkSectionPos.asLong(x, y, z), nodeId);

        return node;
    }

    public RenderSection getSection(int x, int y, int z) {
        return this.getSection(ChunkSectionPos.asLong(x, y, z));
    }

    public RenderSection getSection(long key) {
        var sectionId = this.getSectionId(key);

        if (sectionId == ABSENT_VALUE) {
            return null;
        }

        return this.sections[sectionId];
    }

    public int getSectionId(int x, int y, int z) {
        return this.getSectionId(ChunkSectionPos.asLong(x, y, z));
    }

    public int getSectionId(long key) {
        return this.sectionLookup.get(key);
    }

    public RenderSection getSectionById(int id) {
        return this.sections[id];
    }

    public int getAdjacent(int id, int direction) {
        return this.sectionAdjacent[(id * 6) + direction];
    }

    public int getLoadedSections() {
        return this.sectionLookup.size();
    }

    public void setVisibilityData(int id, ChunkOcclusionData data) {
        for (var from : DirectionUtil.ALL_DIRECTIONS) {
            int bits = 0;

            for (var to : DirectionUtil.ALL_DIRECTIONS) {
                if (data != null && !data.isVisibleThrough(from, to)) {
                    bits |= 1 << to.ordinal();
                }
            }

            this.sectionVisibilityData[(id * 6) + from.ordinal()] = (byte) bits;
        }
    }

    public int getVisibilityData(int id, int incomingDirection) {
        return this.sectionVisibilityData[(id * 6) + incomingDirection];
    }

    public interface Factory<T> {
        T create(int x, int y, int z, int id);
    }

    public static class Node {
        private static final Node[] EMPTY_CHILDREN = new Node[0];

        private int sectionId;

        private final float minX, minY, minZ;
        private final float maxX, maxY, maxZ;

        private Node[] children = EMPTY_CHILDREN;

        public Node(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        public int testBox(Frustum frustum) {
            return frustum.testBox(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
        }

        public boolean isEmpty() {
            return this.children.length == 0;
        }

        public void addChild(Node child) {
            this.children = ArrayUtils.add(this.children, child);
        }

        public void removeChild(Node child) {
            var index = ArrayUtils.indexOf(this.children, child);

            if (index == ArrayUtils.INDEX_NOT_FOUND) {
                throw new NoSuchElementException();
            }

            this.children = ArrayUtils.remove(this.children, index);
        }
    }
}
