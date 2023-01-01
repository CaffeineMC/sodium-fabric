package net.caffeinemc.sodium.util;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.Collection;
import java.util.function.Supplier;

public class Pool<T> {
    private final Supplier<T> factory;
    private final ReferenceArrayList<T> list;

    public Pool(Supplier<T> factory) {
        this.factory = factory;
        this.list = new ReferenceArrayList<>();
    }
    
    public Pool(int initialSize, Supplier<T> factory) {
        this.factory = factory;
        this.list = new ReferenceArrayList<>(initialSize);
    }

    public T acquire() {
        int size = this.list.size();
        if (size != 0) {
            return this.list.remove(size - 1);
        }

        return this.factory.get();
    }

    public void release(Collection<T> collection) {
        this.list.addAll(collection);
        collection.clear();
    }
    
    public void release(ReferenceArrayList<T> addedList) {
        int addedCount = addedList.size();
        
        if (addedCount == 0) {
            return;
        }
        
        int currentSize = this.list.elements().length;
        int currentCount = this.list.size();
        int requiredSize = currentCount + addedCount;
    
        if (requiredSize > currentSize) {
            this.list.ensureCapacity(Math.max(
                    currentSize + (currentSize >> 1),
                    requiredSize
            ));
        }
        
        System.arraycopy(
                addedList.elements(),
                0,
                this.list.elements(),
                currentCount,
                addedCount
        );
        
        addedList.clear();
    }
    
    public void add(T object) {
        this.list.add(object);
    }
}
