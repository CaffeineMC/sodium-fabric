package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import java.nio.IntBuffer;

import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;

class BSPSortState {
    static final int NO_FIXED_OFFSET = Integer.MIN_VALUE;

    private final IntBuffer indexBuffer;

    private int indexModificationsRemaining;
    private int[] indexMap;
    private int fixedIndexOffset = NO_FIXED_OFFSET;

    BSPSortState(NativeBuffer nativeBuffer) {
        this.indexBuffer = nativeBuffer.getDirectBuffer().asIntBuffer();
    }

    void startNode(InnerPartitionBSPNode node) {
        if (node.indexMap != null) {
            if (this.indexMap != null || this.fixedIndexOffset != NO_FIXED_OFFSET) {
                throw new IllegalStateException("Index modification already in progress");
            }

            this.indexMap = node.indexMap;
            this.indexModificationsRemaining = node.reuseData.indexes().length;
        } else if (node.fixedIndexOffset != NO_FIXED_OFFSET) {
            if (this.indexMap != null || this.fixedIndexOffset != NO_FIXED_OFFSET) {
                throw new IllegalStateException("Index modification already in progress");
            }

            this.fixedIndexOffset = node.fixedIndexOffset;
            this.indexModificationsRemaining = node.reuseData.indexes().length;
        }
    }

    private void checkModificationCounter(int reduceBy) {
        this.indexModificationsRemaining -= reduceBy;
        if (this.indexModificationsRemaining <= 0) {
            this.indexMap = null;
            this.fixedIndexOffset = NO_FIXED_OFFSET;
        }
    }

    void writeIndexes(int[] indexes) {
        if (this.indexMap != null) {
            for (int i = 0; i < indexes.length; i++) {
                TranslucentData.writeQuadVertexIndexes(this.indexBuffer, this.indexMap[indexes[i]]);
            }
            checkModificationCounter(indexes.length);
        } else if (this.fixedIndexOffset != NO_FIXED_OFFSET) {
            for (int i = 0; i < indexes.length; i++) {
                TranslucentData.writeQuadVertexIndexes(this.indexBuffer, this.fixedIndexOffset + indexes[i]);
            }
            checkModificationCounter(indexes.length);
        } else {
            TranslucentData.writeQuadVertexIndexes(this.indexBuffer, indexes);
        }
    }

    void writeIndex(int index) {
        if (this.indexMap != null) {
            TranslucentData.writeQuadVertexIndexes(this.indexBuffer, this.indexMap[index]);
            checkModificationCounter(1);
        } else if (this.fixedIndexOffset != NO_FIXED_OFFSET) {
            TranslucentData.writeQuadVertexIndexes(this.indexBuffer, this.fixedIndexOffset + index);
            checkModificationCounter(1);
        } else {
            TranslucentData.writeQuadVertexIndexes(this.indexBuffer, index);
        }
    }
}
