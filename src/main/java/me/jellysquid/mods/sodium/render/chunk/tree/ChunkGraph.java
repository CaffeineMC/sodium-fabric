package me.jellysquid.mods.sodium.render.chunk.tree;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import me.jellysquid.mods.sodium.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.util.DirectionUtil;
import me.jellysquid.mods.sodium.util.collections.BitArray;
import me.jellysquid.mods.sodium.util.collections.table.IntTable;
import me.jellysquid.mods.sodium.util.collections.table.Table;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import org.joml.FrustumIntersection;
import org.joml.Vector3i;

import java.util.Arrays;
import java.util.Random;
import java.util.Set;

public class ChunkGraph<T> {
    private static final long DEFAULT_VISIBILITY_DATA = calculateVisibilityData(ChunkRenderData.EMPTY.getOcclusionData());

    private final int depth;

    private final Long2IntMap nodeLookup;
    private final Long2IntMap[] leafLookup;

    private final Factory<T> nodeFactory;

    private final Table<T> nodeTable;
    private final IntTable leafTable;

    private int[] leafChildren = new int[256];

    private long[] nodeVisibilityData = new long[256];
    private int[] nodeConnections = new int[1536];

    public interface Factory<T> {
        T create(int x, int y, int z, int id);
    }

    public ChunkGraph(int depth, Factory<T> nodeFactory) {
        this.nodeFactory = nodeFactory;

        this.nodeLookup = new Long2IntOpenHashMap();

        this.nodeTable = new Table<>();
        this.leafTable = new IntTable();

        this.depth = depth;
        this.leafLookup = new Long2IntOpenHashMap[depth];

        for (int i = 0; i < this.leafLookup.length; i++) {
            this.leafLookup[i] = new Long2IntOpenHashMap();
        }
    }

    private int createLeaf() {
        int id = this.leafTable.create();

        int childrenCount = (id * 8) + 8;
        int adjacentCount = (id * 6) + 6;

        if (this.leafChildren.length <= childrenCount) {
            this.leafChildren = Arrays.copyOf(this.leafChildren, Math.max(childrenCount + 1, this.leafChildren.length * 2));
        }

        return id;
    }

    public int add(int x, int y, int z) {
        var objKey = ChunkSectionPos.asLong(x, y, z);
        var objId = this.nodeLookup.get(objKey);

        if (objId != 0) {
            return objId;
        }

        objId = this.createNode(x, y, z);

        int last = 0;

        for (int level = 1; level <= this.depth; level++) {
            var key = leafKey(x, y, z, level);

            var table = this.leafLookup[level - 1];
            var leafIndex = table.get(key);

            if (leafIndex == 0) {
                table.put(key, leafIndex = this.createLeaf());
            }

            var localIndex = elementIndex(x, y, z, level - 1);

            var childId = (last != 0) ? last : objId;

            if (this.containsChild(leafIndex, localIndex, childId)) {
                break;
            }

            this.setChild(leafIndex, localIndex, childId);

            last = leafIndex;
        }

        return objId;
    }

    private int createNode(int x, int y, int z) {
        var objKey = ChunkSectionPos.asLong(x, y, z);
        var objId = this.nodeTable.create((id) -> this.nodeFactory.create(x, y, z, id));

        if (this.nodeVisibilityData.length <= objId) {
            this.nodeVisibilityData = Arrays.copyOf(this.nodeVisibilityData, Math.max(objId + 1, this.nodeVisibilityData.length * 2));
        }

        if (this.nodeConnections.length <= objId * 6) {
            this.nodeConnections = Arrays.copyOf(this.nodeConnections, Math.max(objId * 6, this.nodeConnections.length * 2));
        }

        this.nodeLookup.put(objKey, objId);
        this.nodeVisibilityData[objId] = DEFAULT_VISIBILITY_DATA;

        this.connectNode(x, y, z);

        return objId;
    }

    public void remove(int x, int y, int z, int objId) {
        var objKey = ChunkSectionPos.asLong(x, y, z);

        if (!this.nodeLookup.remove(objKey, objId)) {
            return;
        }

        this.disconnectNode(x, y, z);

        for (int level = 1; level <= this.depth; level++) {
            var leafKey = leafKey(x, y, z, level);

            var leafTable = this.leafLookup[level - 1];
            var leafIndex = leafTable.get(leafKey);

            this.removeChild(leafIndex, elementIndex(x, y, z, level - 1));

            if (!this.isLeafEmpty(leafIndex)) {
                break;
            }

            this.leafTable.remove(leafIndex);
            leafTable.remove(leafKey);
        }

        this.nodeTable.remove(objId);
    }

    public int getAdjacent(int node, Direction dir) {
        return this.getAdjacent(node, dir.ordinal());
    }

    private void connectNode(int x, int y, int z) {
        int origin = this.getNodeId(x, y, z);

        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            int adj = this.getNodeId(x + dir.getOffsetX(),
                    y + dir.getOffsetY(),
                    z + dir.getOffsetZ());

            this.setAdjacent(origin, dir.ordinal(), adj);

            if (adj != 0) {
                this.setAdjacent(adj, DirectionUtil.getOpposite(dir).ordinal(), origin);
            }
        }
    }

    private void disconnectNode(int x, int y, int z) {
        int origin = this.getNodeId(x, y, z);

        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            int adj = this.getNodeId(x + dir.getOffsetX(),
                    y + dir.getOffsetY(),
                    z + dir.getOffsetZ());

            this.setAdjacent(origin, dir.ordinal(), 0);

            if (adj != 0) {
                this.setAdjacent(adj, DirectionUtil.getOpposite(dir).ordinal(), 0);
            }
        }
    }

    private void setAdjacent(int node, int direction, int adjacent) {
        this.nodeConnections[(node * 6) + direction] = adjacent;
    }

    private int getAdjacent(int node, int direction) {
        return this.nodeConnections[(node * 6) + direction];
    }

    public void getFrustumBits(BitArray visible, FrustumIntersection frustum) {
        if (visible.size() < this.nodeTable.size()) {
            throw new IllegalStateException();
        }

        int width = 16 << this.depth;

        for (Long2IntMap.Entry entry : Long2IntMaps.fastIterable(this.leafLookup[this.depth - 1])) {
            long key = entry.getLongKey();

            var x1 = ChunkSectionPos.unpackX(key) << this.depth << 4;
            var y1 = ChunkSectionPos.unpackY(key) << this.depth << 4;
            var z1 = ChunkSectionPos.unpackZ(key) << this.depth << 4;

            this.markVisibleLeaves(visible, frustum, x1, y1, z1, entry.getIntValue(), this.depth, width);
        }
    }

    private void markVisibleLeaves(BitArray visible, FrustumIntersection frustum, int x1, int y1, int z1, int vertex, int level, int width) {
        if (vertex == -1) {
            return;
        }

        int result = frustum.intersectAab(x1, y1, z1, x1 + width, y1 + width, z1 + width);

        switch (result) {
            case FrustumIntersection.INTERSECT -> this.markVisibleChildren(visible, frustum, x1, y1, z1, vertex, level, width);
            case FrustumIntersection.INSIDE -> this.markAllChildren(visible, vertex, level);
        }
    }

    private void markVisibleChildren(BitArray visible, FrustumIntersection frustum, int x1, int y1, int z1, int vertex, int level, int width) {
        if (level == 0) {
            visible.set(vertex);
            return;
        }

        int[] children = this.leafChildren;
        int childStart = vertex * 8;

        int nextLevel = level - 1;
        int nextWidth = width >> 1;

        int x2 = x1 + nextWidth;
        int y2 = y1 + nextWidth;
        int z2 = z1 + nextWidth;

        this.markVisibleLeaves(visible, frustum, x1, y1, z1, children[childStart + 0], nextLevel, nextWidth);
        this.markVisibleLeaves(visible, frustum, x2, y1, z1, children[childStart + 1], nextLevel, nextWidth);
        this.markVisibleLeaves(visible, frustum, x1, y2, z1, children[childStart + 2], nextLevel, nextWidth);
        this.markVisibleLeaves(visible, frustum, x2, y2, z1, children[childStart + 3], nextLevel, nextWidth);
        this.markVisibleLeaves(visible, frustum, x1, y1, z2, children[childStart + 4], nextLevel, nextWidth);
        this.markVisibleLeaves(visible, frustum, x2, y1, z2, children[childStart + 5], nextLevel, nextWidth);
        this.markVisibleLeaves(visible, frustum, x1, y2, z2, children[childStart + 6], nextLevel, nextWidth);
        this.markVisibleLeaves(visible, frustum, x2, y2, z2, children[childStart + 7], nextLevel, nextWidth);
    }

    private void markAllChildren(BitArray visible, int vertex, int level) {
        if (level > 0) {
            for (int childIndex = vertex * 8, lastChildIndex = childIndex + 8; childIndex < lastChildIndex; childIndex++) {
                var child = this.leafChildren[childIndex];

                if (child == -1) {
                    continue;
                }

                this.markAllChildren(visible, child, level - 1);
            }
        } else {
            visible.set(vertex);
        }
    }

    static long leafKey(int x, int y, int z) {
        return ChunkSectionPos.asLong(x, y, z);
    }

    static long leafKey(int x, int y, int z, int level) {
        return leafKey(x >> level, y >> level, z >> level);
    }

    static int elementIndex(int x, int y, int z) {
        return (z << 2) + (y << 1) + x;
    }

    static int elementIndex(int x, int y, int z, int level) {
        return elementIndex((x >> level) & 1, (y >> level) & 1, (z >> level) & 1);
    }

    private boolean isLeafEmpty(int leaf) {
        int[] children = this.leafChildren;

        for (int childIndex = getChildIndex(leaf, 0), lastChildIndex = childIndex + 8; childIndex < lastChildIndex; childIndex++) {
            if (children[childIndex] > 0) {
                return false;
            }
        }

        return true;
    }

    private void setChild(int leaf, int idx, int obj) {
        this.leafChildren[getChildIndex(leaf, idx)] = obj;
    }

    private void removeChild(int leaf, int idx) {
        this.leafChildren[getChildIndex(leaf, idx)] = 0;
    }

    private boolean containsChild(int leaf, int idx, int obj) {
        return this.leafChildren[getChildIndex(leaf, idx)] == obj;
    }

    private static int getChildIndex(int leaf, int child) {
        return (leaf * 8) + child;
    }

    public static void main(String[] args) {
        ChunkGraph<Vector3i> graph = new ChunkGraph<>(4, (x, y, z, id) -> null);
        Set<Vector3i> set = new ObjectOpenHashSet<>();
        Random random = new Random();

        while (true) {
            while (set.size() < 1024) {
                set.add(new Vector3i(
                        random.nextInt(64) - 32,
                        random.nextInt(16),
                        random.nextInt(64) - 32
                ));
            }

            Reference2IntMap<Vector3i> map = new Reference2IntOpenHashMap<>();

            for (Vector3i coord : set) {
                map.put(coord, graph.add(coord.x, coord.y, coord.z));
            }

            var visible = new BitArray(graph.getNodeCount());
            graph.getFrustumBits(visible, new FrustumIntersection());

            for (Vector3i coord : set) {
                graph.remove(coord.x, coord.y, coord.z, map.removeInt(coord));
            }

            set.clear();
        }
    }

    public boolean isVisibleThrough(int id, Direction from, Direction to) {
        if (from == null) {
            return true;
        }

        return (this.nodeVisibilityData[id] & 1L << (from.ordinal() << 3) + to.ordinal()) != 0L;
    }

    public void setOcclusionData(int id, ChunkOcclusionData occlusionData) {
        this.nodeVisibilityData[id] = calculateVisibilityData(occlusionData);
    }

    private static long calculateVisibilityData(ChunkOcclusionData occlusionData) {
        long visibilityData = 0;

        for (Direction from : DirectionUtil.ALL_DIRECTIONS) {
            for (Direction to : DirectionUtil.ALL_DIRECTIONS) {
                if (occlusionData == null || occlusionData.isVisibleThrough(from, to)) {
                    visibilityData |= (1L << ((from.ordinal() << 3) + to.ordinal()));
                }
            }
        }

        return visibilityData;
    }

    public int getNodeCount() {
        return this.nodeTable.size();
    }

    public T getNode(int id) {
        return this.nodeTable.get(id);
    }

    public T getNode(int x, int y, int z) {
        return this.getNode(this.getNodeId(x, y, z));
    }

    public int getNodeId(int x, int y, int z) {
        return this.nodeLookup.get(leafKey(x, y, z));
    }

    public T getNode(long pos) {
        return this.getNode(ChunkSectionPos.unpackX(pos),
                ChunkSectionPos.unpackY(pos),
                ChunkSectionPos.unpackZ(pos));
    }
}
