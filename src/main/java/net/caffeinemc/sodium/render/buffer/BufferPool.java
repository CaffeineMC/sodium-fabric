package net.caffeinemc.sodium.render.buffer;

import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectSortedSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.LongFunction;
import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.device.RenderDevice;

// TODO: split cache of vertex and index buffers?
public class BufferPool<B extends Buffer> {
    private final ObjectSortedSet<B> recycledBuffers = new ObjectRBTreeSet<>(
            Comparator.comparingLong(B::capacity)
                      .thenComparingInt(B::hashCode) // this *should* work, especially if the hashcode is derived from the buffer's ID, mapped pointer, etc
    );
    
    private final RenderDevice device;
    private final LongFunction<B> createBufferFunction;
    
    public BufferPool(RenderDevice device, LongFunction<B> createBufferFunction) {
        this.device = device;
        this.createBufferFunction = createBufferFunction;
    }
    
    public B getBufferLenient(long capacityTarget) {
        B key = this.createKey(capacityTarget);
        Iterator<B> itr = this.recycledBuffers.iterator(key);
        
        if (itr.hasNext()) {
            B buffer = itr.next();
            itr.remove();
            return buffer;
        } else {
            return this.createBufferFunction.apply(capacityTarget);
        }
    }
    
    public B getBufferStrict(long capacity) {
        B key = this.createKey(capacity);
        Iterator<B> itr = this.recycledBuffers.subSet(key, key).iterator();
    
        if (itr.hasNext()) {
            B buffer = itr.next();
            itr.remove();
            return buffer;
        } else {
            return this.createBufferFunction.apply(capacity);
        }
    }
    
    public void recycleBuffer(B buffer) {
        if (!this.recycledBuffers.add(buffer)) {
            throw new IllegalArgumentException("Buffer already recycled!");
        }
    }
    
    public void cleanup() {
        for (Buffer buffer : this.recycledBuffers) {
            this.device.deleteBuffer(buffer);
        }
        
        this.recycledBuffers.clear();
    }
    
    public int getDeviceBufferObjects() {
        return this.recycledBuffers.size();
    }
    
    public long getDeviceAllocatedMemory() {
        return this.recycledBuffers.stream().mapToLong(Buffer::capacity).sum();
    }
    
    public void delete() {
        for (Buffer buffer : this.recycledBuffers) {
            this.device.deleteBuffer(buffer);
        }
    }
    
    // this is disgusting
    @SuppressWarnings("unchecked")
    private B createKey(long capacity) {
        return (B) new BufferKey(capacity);
    }
    
    private record BufferKey(long capacity) implements Buffer {
    
        @Override
        public int hashCode() {
            // we always want to include all buffer values in the search, so we start as early as possible in the tree
            // with this, given the lengths are the same.
            return Integer.MIN_VALUE;
        }
    }
}
