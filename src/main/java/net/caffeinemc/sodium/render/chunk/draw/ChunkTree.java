package net.caffeinemc.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.caffeinemc.sodium.interop.vanilla.math.frustum.Frustum;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.util.DirectionUtil;
import net.caffeinemc.sodium.util.collections.BitArray;
import net.minecraft.util.math.ChunkSectionPos;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.NoSuchElementException;

public class ChunkTree {
    private static final int ABSENT_VALUE = 0;

    private final IntPool idPool = new IntPool();

    private final Long2IntOpenHashMap[] nodeLookup;
    private final Long2IntOpenHashMap sectionLookup;

    private RenderSection[] sections = new RenderSection[4096];
    private Node[] nodes = new Node[4096];

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

        this.sectionLookup = this.nodeLookup[0];
    }

    public void calculateVisible(Frustum frustum, BitArray vis) {
        if (vis.capacity() < this.getSectionTableSize()) {
            throw new IllegalArgumentException("Visibility array does not contain enough elements");
        }

        var root = this.nodeLookup[this.maxDepth - 1];
        var it = root.values().intIterator();

        while (it.hasNext()) {
            this.calculateChildrenVisibility(frustum, vis, it.nextInt(), this.maxDepth - 1);
        }
    }

    public int getSectionTableSize() {
        return this.idPool.capacity();
    }

    private void calculateChildrenVisibility(Frustum frustum, BitArray vis, int nodeId, int nodeDepth) {
        var node = this.nodes[nodeId];
        var result = node.testVisibility(frustum);

        if (result != Frustum.Visibility.OUTSIDE) {
            vis.set(node.id);

            if (nodeDepth > 0) {
                if (result == Frustum.Visibility.INSIDE) {
                    this.markAllChildrenVisible(vis, node, nodeDepth - 1);
                } else if (result == Frustum.Visibility.INTERSECT) {
                    for (int childNodeId : node.children) {
                        this.calculateChildrenVisibility(frustum, vis, childNodeId, nodeDepth - 1);
                    }
                }
            }
        }
    }

    private void markAllChildrenVisible(BitArray vis, Node node, int depth) {
        for (int childNodeId : node.children) {
            var childNode = this.nodes[childNodeId];

            if (depth > 0) {
                this.markAllChildrenVisible(vis, childNode, depth - 1);
            }

            vis.set(childNode.id);
        }
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

            if (nodeId == ABSENT_VALUE) {
                this.createNode(nodeX, nodeY, nodeZ, depth, nodeId = this.idPool.create());

                if (parentNode != null) {
                    parentNode.addChild(nodeId);
                }
            }

            parentNode = this.nodes[nodeId];
        }

        if (parentNode == null) {
            throw new IllegalStateException("No parent node exists");
        }

        return this.createSection(chunkX, chunkY, chunkZ, parentNode.id);
    }

    private RenderSection createSection(int x, int y, int z, int id) {
        var section = this.factory.create(x, y, z, id);

        if (this.sections.length <= id) {
            this.sections = Arrays.copyOf(this.sections, Math.max(id, this.sections.length * 2));
        }

        this.sections[id] = section;
        this.sectionLookup.put(ChunkSectionPos.asLong(x, y, z), id);

        return section;
    }

    private void connectAdjacentNodes(int x, int y, int z, int depth) {
        var nodeMap = this.nodeLookup[depth];

        var nodePosition = ChunkSectionPos.asLong(x, y, z);
        var nodeId = nodeMap.get(nodePosition);
        var node = this.nodes[nodeId];

        for (var direction : DirectionUtil.ALL_DIRECTIONS) {
            var adjacentNodeId = nodeMap.get(ChunkSectionPos.offset(nodePosition, direction));

            if (adjacentNodeId != ABSENT_VALUE) {
                var adjacentNode = this.nodes[adjacentNodeId];
                adjacentNode.adjacent[DirectionUtil.getOpposite(direction).ordinal()] = nodeId;
            }

            node.adjacent[direction.ordinal()] = adjacentNodeId;
        }
    }

    private void disconnectAdjacentNodes(int x, int y, int z, int depth) {
        var nodeMap = this.nodeLookup[depth];

        var nodePosition = ChunkSectionPos.asLong(x, y, z);
        var nodeId = nodeMap.get(nodePosition);
        var node = this.nodes[nodeId];

        for (var direction : DirectionUtil.ALL_DIRECTIONS) {
            var adjacentNodeId = nodeMap.get(ChunkSectionPos.offset(nodePosition, direction));

            if (adjacentNodeId != ABSENT_VALUE) {
                var adjacentNode = this.nodes[adjacentNodeId];
                adjacentNode.adjacent[DirectionUtil.getOpposite(direction).ordinal()] = ABSENT_VALUE;
            }

            node.adjacent[direction.ordinal()] = ABSENT_VALUE;
        }
    }

    public void remove(int chunkX, int chunkY, int chunkZ) {
        var lastChildNodeId = ABSENT_VALUE;

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

            if (lastChildNodeId != ABSENT_VALUE) {
                node.removeChild(lastChildNodeId);
            }

            if (node.isEmpty()) {
                this.disconnectAdjacentNodes(nodeX, nodeY, nodeZ, depth);

                nodeMap.remove(nodeKey);

                this.idPool.free(nodeId);
                this.nodes[nodeId] = null;
                this.sections[nodeId] = null;
            } else {
                break;
            }

            lastChildNodeId = nodeId;
        }
    }

    private void createNode(int x, int y, int z, int depth, int id) {
        int length = (1 << depth) * 16;

        float minX = x * length;
        float minY = y * length;
        float minZ = z * length;

        float maxX = minX + length;
        float maxY = minY + length;
        float maxZ = minZ + length;

        var node = new Node(id, minX, minY, minZ, maxX, maxY, maxZ);

        if (this.nodes.length <= id) {
            this.nodes = Arrays.copyOf(this.nodes, Math.max(id, this.nodes.length * 2));
        }

        this.nodes[id] = node;
        this.nodeLookup[depth].put(ChunkSectionPos.asLong(x, y, z), id);

        this.connectAdjacentNodes(x, y, z, depth);
    }

    public RenderSection getSection(int x, int y, int z) {
        return this.getSection(ChunkSectionPos.asLong(x, y, z));
    }

    public RenderSection getSection(long key) {
        var sectionId = this.sectionLookup.get(key);

        if (sectionId == ABSENT_VALUE) {
            return null;
        }

        return this.sections[sectionId];
    }

    public RenderSection getSectionForNode(int id) {
        return this.sections[id];
    }

    public int getAdjacent(int id, int direction) {
        return this.nodes[id].adjacent[direction];
    }

    public int getLoadedSections() {
        return this.sectionLookup.size();
    }

    public Node getNodeById(int sectionId) {
        return this.nodes[sectionId];
    }

    public interface Factory<T> {
        T create(int x, int y, int z, int id);
    }

    public static class Node {
        private static final int[] EMPTY_CHILDREN = new int[0];

        private final int id;

        private final float minX, minY, minZ;
        private final float maxX, maxY, maxZ;

        private final int[] adjacent = new int[6];
        private int[] children = EMPTY_CHILDREN;

        private long visibilityData = 0L;

        public Node(int id, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            this.id = id;

            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        public Frustum.Visibility testVisibility(Frustum frustum) {
            return frustum.testBox(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
        }

        public boolean isEmpty() {
            return this.children.length == 0;
        }

        public void addChild(int child) {
            this.children = ArrayUtils.add(this.children, child);
        }

        public void removeChild(int child) {
            var index = ArrayUtils.indexOf(this.children, child);

            if (index == ArrayUtils.INDEX_NOT_FOUND) {
                throw new NoSuchElementException();
            }

            this.children = ArrayUtils.remove(this.children, index);
        }

        public void setVisibilityData(long data) {
            this.visibilityData = data;
        }

        public boolean isVisibleThrough(int incomingDirection, int outgoingDirection) {
            return ((this.visibilityData & (1L << ((incomingDirection << 3) + outgoingDirection))) != 0L);
        }
    }
}
