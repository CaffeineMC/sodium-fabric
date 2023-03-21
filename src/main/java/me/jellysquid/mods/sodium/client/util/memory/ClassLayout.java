package me.jellysquid.mods.sodium.client.util.memory;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class ClassLayout {
    private static final Unsafe UNSAFE;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);

            UNSAFE = (Unsafe) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to obtain sun.misc.Unsafe instance", e);
        }
    }

    public static void printLayout(Class<?> clazz) {
        System.out.println("Class layout for " + clazz.getName());

        var fields = clazz.getDeclaredFields();

        for (var field : fields) {
            System.out.println("%s = %s".formatted(field.getName(), UNSAFE.objectFieldOffset(field)));
        }
    }
}
