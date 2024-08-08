package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import java.nio.IntBuffer;
import java.lang.Math;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;
import net.caffeinemc.mods.sodium.client.util.NativeBuffer;

/**
 * The sort state is passed around the tree (similar to visitor pattern) and
 * contains the index buffer being written to alongside additional state for
 * remapping indexes when traversing the subtree of a reused node.
 */
class BSPSortState {
    static final int NO_FIXED_OFFSET = Integer.MIN_VALUE;

    private IntBuffer indexBuffer;

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
            this.indexModificationsRemaining = node.reuseData.indexCount();
        } else if (node.fixedIndexOffset != NO_FIXED_OFFSET) {
            if (this.indexMap != null || this.fixedIndexOffset != NO_FIXED_OFFSET) {
                throw new IllegalStateException("Index modification already in progress");
            }

            this.fixedIndexOffset = node.fixedIndexOffset;
            this.indexModificationsRemaining = node.reuseData.indexCount();
        }
    }

    private void checkModificationCounter(int reduceBy) {
        this.indexModificationsRemaining -= reduceBy;
        if (this.indexModificationsRemaining <= 0) {
            this.indexMap = null;
            this.fixedIndexOffset = NO_FIXED_OFFSET;
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

    /**
     * The minimum size of an index array that will be compressed. This value is
     * non-zero to avoid wasting work on compressing arrays that won't benefit from
     * it and the overhead in setting up the compression. Empirically, the
     * compression ratio is only high for the very largest arrays and largely
     * useless for smaller ones.
     */
    private static final int INDEX_COMPRESSION_MIN_LENGTH = 32;

    private static final int HEADER_LENGTH = 2;
    private static final int[] WIDTHS = new int[] { 1, 2, 3, 4, 5, 6, 8, 10, 16, 32 };
    private static final int CONSTANT_DELTA_WIDTH_INDEX = 15;

    /**
     * ceilDiv was introduced to the JDK in Java 18 but is not available in the here
     * used Java 17.
     */
    private static int ceilDiv(int x, int y) {
        return -Math.floorDiv(-x, y);
    }

    private static boolean isOutOfBounds(int size) {
        return size < INDEX_COMPRESSION_MIN_LENGTH || size > 1 << 10;
    }

    static int[] compressIndexesInPlace(int[] indexes, boolean doSort) {
        if (isOutOfBounds(indexes.length)) {
            return indexes;
        }
        return compressIndexes(IntArrayList.wrap(indexes), doSort);
    }

    static int[] compressIndexes(IntArrayList indexes) {
        return compressIndexes(indexes, true);
    }

    /**
     * Compress a list of quad indexes by applying run length encoding or bit
     * packing to their deltas.
     * 
     * Format: 32 bits, elements described as [length in bits: description]
     * header at position 0: 0b1[4: width index][10: delta count][17: first index]
     * header at position 1: 0b[32: base delta]
     * deltas at position 2..n: 0b[width: delta]...
     * 
     * delta bit widths:
     * 1x32b, 2x16b, 3x10b, 4x8b, 5x6b,
     * 6x5b, 8x4b, 10x3b, 16x2b, 32x1b
     */
    static int[] compressIndexes(IntArrayList indexes, boolean doSort) {
        // bail on short lists
        if (isOutOfBounds(indexes.size())) {
            return indexes.toIntArray();
        }

        IntArrayList workingList = new IntArrayList(indexes);

        // sort for better compression, this also ensures that deltas are positive but
        // as small as possible.
        if (doSort) {
            workingList.sort(null);
        }

        // replace indexes with deltas
        int last = workingList.getInt(0);
        int minDelta = Integer.MAX_VALUE;
        int maxDelta = 0;
        for (int i = 1; i < workingList.size(); i++) {
            int current = workingList.getInt(i);
            int delta = current - last;
            workingList.set(i, delta);
            last = current;
            if (delta < minDelta) {
                minDelta = delta;
            }
            if (delta > maxDelta) {
                maxDelta = delta;
            }
        }
        int deltaRangeWidth = Integer.SIZE - Integer.numberOfLeadingZeros(maxDelta - minDelta);

        // stop if the first index is too large
        int firstIndex = workingList.getInt(0);
        if (firstIndex > 1 << 17) {
            return indexes.toIntArray();
        }

        int deltaCount = workingList.size() - 1;

        // special case 0 bit delta
        if (deltaRangeWidth == 0) {
            // this means there's just a sequential list of indexes with each one +1
            var compressed = new int[HEADER_LENGTH];

            // signal with special width index
            compressed[0] = 1 << 31 | CONSTANT_DELTA_WIDTH_INDEX << 27 | deltaCount << 17 | firstIndex;
            compressed[1] = minDelta;

            return compressed;
        }

        // stop if the width is too large (and compression would make no sense)
        if (deltaRangeWidth > 16) {
            return indexes.toIntArray();
        }

        // find the smallest bit width that can represent the deltas
        int widthIndex = 0;
        while (WIDTHS[widthIndex] < deltaRangeWidth) {
            widthIndex++;
        }
        int width = WIDTHS[widthIndex];
        int countPerInt = WIDTHS[WIDTHS.length - widthIndex - 1];

        // figure out the size of the compressed index array
        int size = HEADER_LENGTH + ceilDiv(deltaCount, countPerInt);
        int[] compressed = new int[size];

        // write the header
        compressed[0] = 1 << 31 | widthIndex << 27 | deltaCount << 17 | firstIndex;
        compressed[1] = minDelta;

        // write the deltas
        final int positionLimit = Integer.SIZE - width;
        int outputIndex = HEADER_LENGTH;
        int gatherInt = 0;
        int bitPosition = 0;
        for (int i = 1; i < workingList.size(); i++) {
            int shiftedDelta = workingList.getInt(i) - minDelta;
            gatherInt |= shiftedDelta << bitPosition;
            bitPosition += width;
            if (bitPosition > positionLimit) {
                compressed[outputIndex++] = gatherInt;
                gatherInt = 0;
                bitPosition = 0;
            }
        }

        // flush the last int if it hasn't been written yet
        if (bitPosition > 0) {
            compressed[outputIndex++] = gatherInt;
        }

        return compressed;
    }

    static int decompressOrRead(int[] indexes, IntConsumer consumer) {
        if (isCompressed(indexes)) {
            return decompress(indexes, consumer);
        } else {
            for (int i = 0; i < indexes.length; i++) {
                consumer.accept(indexes[i]);
            }
            return indexes.length;
        }
    }

    private static int decompress(int[] indexes, IntConsumer consumer) {
        return decompressWithOffset(indexes, 0, consumer);
    }

    private static int decompressWithOffset(int[] indexes, int fixedIndexOffset, IntConsumer consumer) {
        // read compression header
        int header = indexes[0];
        int widthIndex = (header >> 27) & 0b1111;
        int currentValue = header & 0b11111111111111111 + fixedIndexOffset;
        int valueCount = ((header >> 17) & 0b1111111111) + 1;
        int baseDelta = indexes[1]; // second piece of the header

        // handle special case of width index 0, this means there's no delta data
        if (widthIndex == CONSTANT_DELTA_WIDTH_INDEX) {
            for (int i = 0; i < valueCount; i++) {
                consumer.accept(currentValue);
                currentValue += baseDelta;
            }

            return valueCount;
        }

        int width = WIDTHS[widthIndex];
        int mask = (1 << width) - 1;

        // write value (optionally map), read deltas, apply base delta and loop
        final int positionLimit = Integer.SIZE - width;
        int readIndex = HEADER_LENGTH;
        int splitInt = indexes[readIndex++];
        int splitIntBitPosition = 0;
        int totalValueCount = valueCount;
        while (valueCount-- > 0) {
            consumer.accept(currentValue);

            // read the next delta if there is one
            if (valueCount == 0) {
                break;
            }

            int delta = (splitInt >> splitIntBitPosition) & mask;
            splitIntBitPosition += width;
            if (splitIntBitPosition > positionLimit && valueCount > 1) {
                splitInt = indexes[readIndex++];
                splitIntBitPosition = 0;
            }

            // update the current value with the delta and base delta
            currentValue += baseDelta + delta;
        }

        return totalValueCount;
    }

    static boolean isCompressed(int[] indexes) {
        return indexes[0] < 0;
    }

    private IntConsumer indexConsumer = (int index) -> TranslucentData.writeQuadVertexIndexes(
            this.indexBuffer, index);

    private IntConsumer indexMapConsumer = (int index) -> TranslucentData.writeQuadVertexIndexes(
            this.indexBuffer, this.indexMap[index]);

    void writeIndexes(int[] indexes) {
        boolean useIndexMap = this.indexMap != null;
        boolean useFixedIndexOffset = this.fixedIndexOffset != NO_FIXED_OFFSET;

        int valueCount;
        if (isCompressed(indexes)) {
            if (useFixedIndexOffset) {
                valueCount = decompressWithOffset(indexes, this.fixedIndexOffset, this.indexConsumer);
            } else {
                valueCount = decompress(indexes, useIndexMap ? this.indexMapConsumer : this.indexConsumer);
            }
        } else {
            // uncompressed indexes
            if (useIndexMap) {
                for (int i = 0; i < indexes.length; i++) {
                    TranslucentData.writeQuadVertexIndexes(this.indexBuffer, this.indexMap[indexes[i]]);
                }
            } else if (useFixedIndexOffset) {
                for (int i = 0; i < indexes.length; i++) {
                    TranslucentData.writeQuadVertexIndexes(this.indexBuffer, this.fixedIndexOffset + indexes[i]);
                }
            } else {
                TranslucentData.writeQuadVertexIndexes(this.indexBuffer, indexes);
            }
            valueCount = indexes.length;
        }

        // check if the index modification session is over. this is very important or
        // there's an exception
        if (useIndexMap || useFixedIndexOffset) {
            checkModificationCounter(valueCount);
        }
    }
}
