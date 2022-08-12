package net.caffeinemc.sodium.util.collections;

import java.util.Arrays;
import net.caffeinemc.sodium.util.MathUtil;

public class BitArray {
    private static final int ADDRESS_BITS_PER_WORD = 6;
    private static final int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;
    private static final int BIT_INDEX_MASK = BITS_PER_WORD - 1;

    private final long[] words;
    private final int count;

    public BitArray(int count) {
        this.words = new long[(MathUtil.align(count, BITS_PER_WORD) >> ADDRESS_BITS_PER_WORD)];
        this.count = count;
    }

    public boolean get(int index) {
        return (this.words[wordIndex(index)] & 1L << bitIndex(index)) != 0;
    }

    public void set(int index) {
        this.words[wordIndex(index)] |= 1L << bitIndex(index);
    }
    
    /**
     * Sets the bits from startIdx (inclusive) to endIdx (exclusive) to 1
     */
    public void set(int startIdx, int endIdx) {
        int startWordIndex = wordIndex(startIdx);
        int endWordIndex = wordIndex(endIdx - 1);
    
        long firstWordMask = 0xFFFFFFFFFFFFFFFFL << startIdx;
        long lastWordMask = 0xFFFFFFFFFFFFFFFFL >>> -endIdx;
        if (startWordIndex == endWordIndex) {
            this.words[startWordIndex] |= (firstWordMask & lastWordMask);
        } else {
            this.words[startWordIndex] |= firstWordMask;
            
            for (int i = startWordIndex + 1; i < endWordIndex; i++) {
                this.words[i] = 0xFFFFFFFFFFFFFFFFL;
            }
            
            this.words[endWordIndex] |= lastWordMask;
        }
    }

    public void unset(int index) {
        this.words[wordIndex(index)] &= ~(1L << bitIndex(index));
    }
    
    /**
     * Sets the bits from startIdx (inclusive) to endIdx (exclusive) to 0
     */
    public void unset(int startIdx, int endIdx) {
        int startWordIndex = wordIndex(startIdx);
        int endWordIndex = wordIndex(endIdx - 1);
        
        long firstWordMask = ~(0xFFFFFFFFFFFFFFFFL << startIdx);
        long lastWordMask = ~(0xFFFFFFFFFFFFFFFFL >>> -endIdx);
        if (startWordIndex == endWordIndex) {
            this.words[startWordIndex] &= (firstWordMask & lastWordMask);
        } else {
            this.words[startWordIndex] &= firstWordMask;
            
            for (int i = startWordIndex + 1; i < endWordIndex; i++) {
                this.words[i] = 0x0000000000000000L;
            }
            
            this.words[endWordIndex] &= lastWordMask;
        }
    }
    
    public void copy(BitArray src, int startIdx, int endIdx) {
        int startWordIndex = wordIndex(startIdx);
        int endWordIndex = wordIndex(endIdx - 1);
        
        long firstWordMask = 0xFFFFFFFFFFFFFFFFL << startIdx;
        long lastWordMask = 0xFFFFFFFFFFFFFFFFL >>> -endIdx;
        if (startWordIndex == endWordIndex) {
            this.words[startWordIndex] = src.words[startWordIndex] & (firstWordMask & lastWordMask);
        } else {
            this.words[startWordIndex] = src.words[startWordIndex] & firstWordMask;
    
            int length = endWordIndex - (startWordIndex + 1);
            if (length >= 0) {
                System.arraycopy(
                        src.words,
                        startWordIndex + 1,
                        this.words,
                        startWordIndex + 1,
                        length
                );
            }
            
            this.words[endWordIndex] = src.words[startWordIndex] & lastWordMask;
        }
    }

    private static int wordIndex(int index) {
        return index >> ADDRESS_BITS_PER_WORD;
    }

    private static int bitIndex(int index) {
        return index & BIT_INDEX_MASK;
    }

    public void fill(boolean value) {
        Arrays.fill(this.words, value ? 0xFFFFFFFFFFFFFFFFL : 0x0000000000000000L);
    }

    public int count() {
        int sum = 0;

        for (long word : this.words) {
            sum += Long.bitCount(word);
        }

        return sum;
    }

    public int capacity() {
        return this.count;
    }

    public boolean getAndUnset(int index) {
        var wordIndex = wordIndex(index);
        var bit = 1L << bitIndex(index);

        var word = this.words[wordIndex];
        this.words[wordIndex] = word & ~bit;

        return (word & bit) != 0;
    }
}
