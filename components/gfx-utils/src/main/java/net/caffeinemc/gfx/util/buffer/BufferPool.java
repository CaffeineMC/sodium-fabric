package net.caffeinemc.gfx.util.buffer;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntRBTreeMap;
import it.unimi.dsi.fastutil.longs.Long2IntSortedMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceRBTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceSortedMap;
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
    private static final int INVALID_INDEX = -1;
    
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
    
    /**
     * Calculates and removes the least-necessary buffers given the sampler state.
     *
     * @param percentModifier a value between -1.0 (exclusive) and 1.0 (exclusive) which will be added to the percent
     *                        removed from the buffer groups.
     * @return true if the prune was successful
     */
    public boolean prune(float percentModifier) {
        // A request group represents all the requests for a particular buffer capacity, and is created from the sampler
        // data.
        // A buffer group represents all the existing entries for a particular buffer capacity, and is stored in the
        // recycled buffers tree.
        //
        // One of the problems with pruning a buffer using sampler data is that the sampler data won't always line up
        // with the buffers in the recycled tree. Because of this, we need a way to sort of "apply" this sampler data to
        // arbitrary capacity targets (the buffer group capacities). To do this, we take the prevalence of the capacity
        // in the request samples and use an exponential decay to see how much they relate to the buffer group.
        //
        // Because larger buffers can be used in place of smaller buffers in many cases, we add weight to larger buffers
        // with the requests from smaller buffers. We call this method avalanche. To avalanche correctly, we need to
        // take into account all the previous percentages. This strategy helps save a lot of potentially useful buffers
        // that can be used in place of smaller requests.
        
        // these indices are used to index into the counts and frequencies arrays
        Long2IntSortedMap requestCapIdxMap = new Long2IntRBTreeMap();
        requestCapIdxMap.defaultReturnValue(INVALID_INDEX);
    
        //// generate counts
        
        // we use a list here because it will auto-resize
        IntList requestGroupCounts = new IntArrayList(20);
        int totalRequestCount = 0;
        
        for (long capacitySample : this.samples) {
            if (capacitySample == INVALID_SAMPLE) {
                // don't try to prune without a full set of samples
                return false;
            }
            
            int existingIndex = requestCapIdxMap.get(capacitySample);
            if (existingIndex == INVALID_INDEX) {
                // if the index doesn't exist yet, and add a spot in the count list for it
                requestGroupCounts.add(1);
                
                // this will point to the last index in the count list
                requestCapIdxMap.put(capacitySample, requestGroupCounts.size() - 1);
            } else {
                // increment existing value at the index
                int prevValue = requestGroupCounts.getInt(existingIndex);
                requestGroupCounts.set(existingIndex, prevValue + 1);
            }
            
            totalRequestCount++;
        }
    
        //// generate frequency percentages
        float[] requestGroupFrequencies = new float[requestGroupCounts.size()];
    
        for (int i = 0; i < requestGroupCounts.size(); i++) {
            requestGroupFrequencies[i] = (float) requestGroupCounts.getInt(i) / totalRequestCount;
        }
        
        //// avalanche request group frequencies to get buffer group frequencies and create buffer groups
        @SuppressWarnings("unchecked")
        Deque<B>[] bufferGroups = (Deque<B>[]) new Deque[this.recycledBuffers.size()];
        float[] bufferGroupFrequencies = new float[this.recycledBuffers.size()];
        int arrayIdx = 0;
    
        float minFreq = Float.POSITIVE_INFINITY;
        float maxFreq = Float.NEGATIVE_INFINITY;
        
        for (Long2ReferenceMap.Entry<Deque<B>> bufferGroupEntry : this.recycledBuffers.long2ReferenceEntrySet()) {
            Deque<B> bufferGroup = bufferGroupEntry.getValue();
            
            // skip if the buffer group is empty
            if (bufferGroup.isEmpty()) {
                continue;
            }
            
            long bufferGroupCapacity = bufferGroupEntry.getLongKey();
            
            float bufferGroupFreq;
            // if the buffer group happens to land perfectly on a request group,
            int exactIdx = requestCapIdxMap.get(bufferGroupCapacity);
            if (exactIdx != INVALID_INDEX) {
                bufferGroupFreq = requestGroupFrequencies[exactIdx];
            } else {
                bufferGroupFreq = 0.0f;
            }
            
            // apply portions of all the previous entries through avalanching
            for (Long2IntMap.Entry prevEntry : requestCapIdxMap.headMap(bufferGroupCapacity).long2IntEntrySet()) {
                float requestFreq = requestGroupFrequencies[prevEntry.getIntValue()];
                long requestCapacity = prevEntry.getLongKey();
                
                // Avalanche:
                // Take a portion of the previous frequency and add it to this one, using the ratio between the two sizes
                // as the indicator for how much it should be applied.
                bufferGroupFreq += (requestFreq * Math.pow(SAMPLE_AVALANCHE_FACTOR, (double) (bufferGroupCapacity + SAMPLE_BIAS) / (requestCapacity + SAMPLE_BIAS)));
            }
            
            bufferGroups[arrayIdx] = bufferGroup;
            bufferGroupFrequencies[arrayIdx] = bufferGroupFreq;
            arrayIdx++;
            
            minFreq = Math.min(minFreq, bufferGroupFreq);
            maxFreq = Math.max(maxFreq, bufferGroupFreq);
        }
        
        //// scale and invert frequencies
        //// from there, multiply by the target group's size, round the values, and delete that many buffers from each
        //// buffer group
        float freqDivisor = maxFreq - minFreq;
        
        // arrayIdx will end up representing the size
        for (int i = 0; i < arrayIdx; i++) {
            float invScaledFreq = 1.0f - ((bufferGroupFrequencies[i] - minFreq) / freqDivisor);
            
            // the percent of the buffer group entries to be deleted
            float removalPercentage = Math.min(invScaledFreq + percentModifier, 1.0f);
            
            Deque<B> bufferGroup = bufferGroups[i];
            int removalCount = Math.round(removalPercentage * bufferGroup.size());
            
            for (int j = 0; j < removalCount; j++) {
                B buffer = bufferGroup.poll();
                
                this.device.deleteBuffer(buffer);
            }
        }
        
        return true;
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
