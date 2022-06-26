package net.caffeinemc.sodium.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteOrder;
import sun.misc.Unsafe;

public class UnsafeUtil {
    private static final Unsafe UNSAFE = getUnsafe();
    private static final MethodHandles.Lookup PRIVILEGED_LOOKUP = getPrivilegedLookup();
    private static final MethodHandle vectorizedMismatchMethod = getVectorizedMismatchMethod();
    private static final boolean IS_BIG_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
    private static final int LOG2_LONG_BYTES = Integer.numberOfTrailingZeros(Long.BYTES);
    private static final int TRUNCATE_TAIL_LENGTH_MASK = -1 << LOG2_LONG_BYTES;
    private static final int TAIL_LENGTH_MASK = ~(TRUNCATE_TAIL_LENGTH_MASK);

    private static Unsafe getUnsafe() {
        Field[] unsafeFields = Unsafe.class.getDeclaredFields();

        // try to find field in Unsafe that fits our qualifications
        for (Field field : unsafeFields) {
            if (field.getType().equals(Unsafe.class)) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
                    try {
                        field.setAccessible(true);
                        return (Unsafe) field.get(null);
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // if that didn't work, try to initialize a new Unsafe object with the constructor
        try {
            Constructor<Unsafe> c = Unsafe.class.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (Exception e) {
            // if it STILL didn't work, there's not much else we can do
            throw new UnsupportedOperationException("Sodium requires sun.misc.Unsafe to be available.", e);
        }
    }

    private static MethodHandles.Lookup getPrivilegedLookup() {
        Field[] lookupFields = MethodHandles.Lookup.class.getDeclaredFields();

        // find field with the right qualifications
        for (Field field : lookupFields) {
            if (field.getType().equals(MethodHandles.Lookup.class)) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
                    try {
                        // use our unsafe instance to grab the field's value
                        Object base = UNSAFE.staticFieldBase(field);
                        long offset = UNSAFE.staticFieldOffset(field);
                        MethodHandles.Lookup lookup = (MethodHandles.Lookup) UNSAFE.getObject(base, offset);

                        // make sure we didn't find the public lookup
                        if (!MethodHandles.publicLookup().equals(lookup)) {
                            return lookup;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        throw new UnsupportedOperationException("Sodium requires access to the privileged lookup.");
    }

    private static MethodHandle getVectorizedMismatchMethod() {
        try {
            return PRIVILEGED_LOOKUP.findStatic(
                    Class.forName(
                            "jdk.internal.util.ArraysSupport",
                            false,
                            ClassLoader.getSystemClassLoader()
                    ),
                    "vectorizedMismatch",
                    MethodType.methodType(
                            int.class,
                            Object.class,
                            long.class,
                            Object.class,
                            long.class,
                            int.class,
                            int.class
                    )
            );
        } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Compares the contents of the two buffers across a given length to see if they're equal.
     *
     * The byte order used for comparison will be the system's native byte order. Because java's memory manipulation
     * functions will always convert from big endian (the default for java) to the system's native endianness, this may
     * produce unexpected results if the region being compared happens to intersect a type, rather than being aligned.
     *
     * @param address1 the address of the first buffer
     * @param address2 the address of the second buffer
     * @param length the length of the section to check between the two buffers. must not be negative.
     * @return true if the contents of the two buffers are equal across the given length
     */
    public static boolean nmemEquals(long address1, long address2, int length) {
        try {
            int tailLength = length & TAIL_LENGTH_MASK;
            int tailStartPos = length & TRUNCATE_TAIL_LENGTH_MASK;

            if (tailStartPos > 0) {
                // WARNING: the output of this function depends on the endianness of the machine.
                //
                // While that *shouldn't* matter when we're only checking for equality, given that this function ensures
                // that it will align itself, if we were to eventually implement something like a memcmp, we would need
                // to round the length to the largest unit and do an inverse of the alignment to get the real position
                // in whatever endianness is requested.
                int mismatch = (int) vectorizedMismatchMethod.invokeExact(
                        (Object) null,
                        address1,
                        (Object) null,
                        address2,
                        length >> LOG2_LONG_BYTES,
                        LOG2_LONG_BYTES
                );

                if (mismatch >= 0) {
                    return false;
                }

                // The actual value returned from the function is the complement of the remaining elements, so we need
                // to bitwise NOT it to get the real value.
                int remainingBytes = (~mismatch) * Long.BYTES;

                // check remaining up to tail with long comparisons
                for (int offset = tailStartPos - remainingBytes; offset < tailStartPos; offset += Long.BYTES) {
                    if (UNSAFE.getLong(address1 + offset) != UNSAFE.getLong(address2 + offset)) {
                        return false;
                    }
                }
            }

            // tail long, compare with masked bits
            if (tailLength > 0) {
                int bits = Long.SIZE - (tailLength * Byte.SIZE);
                // Make the shift work in the same direction regardless of endianness, as Unsafe.getLong will always
                // return a big endian value through conversion, even if it does not represent the true byte order.
                long mask = IS_BIG_ENDIAN ? -1L << bits : -1L >>> bits;
                return (UNSAFE.getLong(address1 + tailStartPos) & mask) == (UNSAFE.getLong(address2 + tailStartPos) & mask);
            }

            return true;
        } catch (Throwable t) {
            // shouldn't ever hit
            throw new RuntimeException("Unable to compare data", t);
        }
    }
}
