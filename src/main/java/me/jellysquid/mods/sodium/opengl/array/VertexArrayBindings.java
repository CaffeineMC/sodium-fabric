package me.jellysquid.mods.sodium.opengl.array;

import java.util.Map;

public class VertexArrayBindings<T extends Enum<T>> {
    public final Class<T> type;
    public final VertexArrayBuffer[] slots;

    VertexArrayBindings(Class<T> type, Map<T, VertexArrayBuffer> map) {
        T[] universe = type.getEnumConstants();

        this.slots = new VertexArrayBuffer[universe.length];

        for (int i = 0; i < universe.length; i++) {
            var target = universe[i];
            var buffer = map.get(target);

            if (buffer == null) {
                throw new IllegalArgumentException("No buffer provided for target " + target.name());
            }

            this.slots[i] = buffer;
        }

        this.type = type;
    }
}
