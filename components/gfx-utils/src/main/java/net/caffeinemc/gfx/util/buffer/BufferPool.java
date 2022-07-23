package net.caffeinemc.gfx.util.buffer;

import it.unimi.dsi.fastutil.floats.FloatFloatMutablePair;
import it.unimi.dsi.fastutil.floats.FloatFloatPair;
import it.unimi.dsi.fastutil.longs.*;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.function.LongFunction;
import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.device.RenderDevice;

// TODO: split cache of vertex and index buffers?
public class BufferPool<B extends Buffer> {
    private final Long2ReferenceSortedMap<Deque<B>> recycledBuffers = new Long2ReferenceRBTreeMap<>();
    
    private static final float SAMPLE_AVALANCHE_FACTOR = .45f;
    private static final int SAMPLE_BIAS = 100;
    private static final long INVALID_SAMPLE = -1L;
    
    private final RenderDevice device;
    private final LongFunction<B> createBufferFunction;
    
    private final long[] samples;
    private int samplePointer;
    
    public BufferPool(RenderDevice device, int samplerSize, LongFunction<B> createBufferFunction) {
        this.device = device;
        this.samples = new long[samplerSize];
        Arrays.fill(this.samples, INVALID_SAMPLE);
        this.createBufferFunction = createBufferFunction;
    }
    
    public B getBufferLenient(long capacityTarget) {
        this.addSample(capacityTarget);
    
        Iterator<Deque<B>> bufferGroupItr = this.recycledBuffers.tailMap(capacityTarget).values().iterator();
    
        if (bufferGroupItr.hasNext()) {
            Deque<B> bufferGroup = bufferGroupItr.next();
            if (!bufferGroup.isEmpty()) {
                return bufferGroup.pollLast();
            }
        }
        
        return this.createBufferFunction.apply(capacityTarget);
    }
    
    public B getBufferStrict(long capacity) {
        this.addSample(capacity);
        
        Deque<B> bufferGroup = this.recycledBuffers.get(capacity);
    
        if (bufferGroup != null && !bufferGroup.isEmpty()) {
            return bufferGroup.pollLast();
        }
        
        return this.createBufferFunction.apply(capacity);
    }
    
    public void addSample(long value) {
        this.samples[this.samplePointer] = value;
        this.samplePointer = (this.samplePointer + 1) % this.samples.length;
    }
    
    public void recycleBuffer(B buffer) {
        Deque<B> bufferGroup = this.recycledBuffers.computeIfAbsent(
                buffer.capacity(),
                ignored -> new ArrayDeque<>()
        );
    
        bufferGroup.add(buffer);
    }
    
    public void prune() {
        // left = frequency percentage, right = count
        Long2ReferenceSortedMap<FloatFloatPair> capacityVarsMap = new Long2ReferenceRBTreeMap<>();
        int totalRequestCount = 0;
        
        // generate counts
        for (long capacitySample : this.samples) {
            if (capacitySample == INVALID_SAMPLE) {
                continue;
            }
            
            // add one to the existing pair, or create a new pair if it doesn't exist
            capacityVarsMap.compute(
                    capacitySample,
                    (ignored, oldValue) -> {
                        if (oldValue == null) {
                            return new FloatFloatMutablePair(0.0f, 1.0f);
                        } else {
                            oldValue.right(oldValue.rightFloat() + 1.0f);
                            return oldValue;
                        }
                    }
            );
            totalRequestCount++;
        }
        
        if (capacityVarsMap.isEmpty()) {
            return;
        }
        
        // generate frequency percentages
        for (FloatFloatPair freqCountPair : capacityVarsMap.values()) {
            freqCountPair.left(freqCountPair.rightFloat() / totalRequestCount);
        }
    
    
        // Because larger buffers can be used in place of smaller buffers in many cases, we add weight to larger buffers
        // with the requests from smaller buffers. We call this method avalanching. To avalanche correctly, we need to
        // take into account all the previous percentages.
        BufferGroup[] bufferGroups = new BufferGroup[this.recycledBuffers.size()];
        int arrayIdx = 0;
    
        float minFreq = Float.POSITIVE_INFINITY;
        float maxFreq = Float.NEGATIVE_INFINITY;
        
        for (Long2ReferenceMap.Entry<Deque<B>> entry : this.recycledBuffers.long2ReferenceEntrySet()) {
            Deque<B> buffers = entry.getValue();
            
            // skip if the buffer group is empty
            if (buffers.isEmpty()) {
                continue;
            }
            
            long bufferGroupCapacity = entry.getLongKey();
            
            float freq;
            // this is for if the buffer group happens to land perfectly on an entry
            FloatFloatPair exactPair = capacityVarsMap.get(bufferGroupCapacity);
            if (exactPair != null) {
                freq = exactPair.leftFloat();
            } else {
                freq = 0.0f;
            }
            
            // apply portions of all the previous entries through avalanching
            for (Long2ReferenceMap.Entry<FloatFloatPair> prevEntry : capacityVarsMap.headMap(bufferGroupCapacity).long2ReferenceEntrySet()) {
                float prevFreq = prevEntry.getValue().leftFloat();
                long prevCapacity = prevEntry.getLongKey();
                
                // Avalanche:
                // Take a portion of the previous frequency and add it to this one, using the ratio between the two sizes
                // as the indicator for how much it should be applied.
                freq += (prevFreq * Math.pow(SAMPLE_AVALANCHE_FACTOR, (double) (bufferGroupCapacity + SAMPLE_BIAS) / (prevCapacity + SAMPLE_BIAS)));
            }
    
            //noinspection unchecked
            bufferGroups[arrayIdx++] = new BufferGroup((Deque<Buffer>) buffers, freq);
            
            minFreq = Math.min(minFreq, freq);
            maxFreq = Math.max(maxFreq, freq);
        }
        
        // scale and invert frequencies
        // from there, multiply by the target group's size, round the values, and delete that many buffers from each buffer group
        float freqDivisor = maxFreq - minFreq;
        
        for (BufferGroup bufferGroup : bufferGroups) {
            // skip null groups
            if (bufferGroup == null) {
                continue;
            }
            
            float invScaledFreq = 1.0f - ((bufferGroup.freqPercent - minFreq) / freqDivisor);
            
            Deque<Buffer> buffers = bufferGroup.buffers;
            int removalCount = Math.round(invScaledFreq * bufferGroup.buffers.size());
    
            for (int i = 0; i < removalCount; i++) {
                Buffer buffer = buffers.poll();
        
                this.device.deleteBuffer(buffer);
            }
        }
    }
    
    private record BufferGroup(Deque<Buffer> buffers, float freqPercent) {
    }
    
    public int getDeviceBufferObjects() {
        return this.recycledBuffers.values().stream().mapToInt(Deque::size).sum();
    }
    
    public long getDeviceAllocatedMemory() {
        long size = 0;
        
        for (Collection<B> bufferCollection : this.recycledBuffers.values()) {
            for (B buffer : bufferCollection) {
                size += buffer.capacity();
            }
        }
        
        return size;
    }
    
    public void delete() {
        for (Collection<B> bufferCollection : this.recycledBuffers.values()) {
            for (B buffer : bufferCollection) {
                this.device.deleteBuffer(buffer);
            }
        }
    }
}
