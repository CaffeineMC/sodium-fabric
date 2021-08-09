package me.jellysquid.mods.sodium.client.gl.util;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class EnumBitField<T extends Enum<T> & EnumBit> {
    private final EnumSet<T> set;
    private final int bitfield;

    private EnumBitField(EnumSet<T> set) {
        this.set = set;
        this.bitfield = computeBitField(set);
    }

    private static <T extends Enum<T> & EnumBit> int computeBitField(Set<T> set) {
        int field = 0;

        for (T e : set) {
            field |= e.getBits();
        }

        return field;
    }

    @SafeVarargs
    public static <T extends Enum<T> & EnumBit> EnumBitField<T> of(T... values) {
        List<T> list = Arrays.asList(values);
        EnumSet<T> set = EnumSet.copyOf(list);

        return new EnumBitField<>(set);
    }

    public int getBitField() {
        return this.bitfield;
    }

    public boolean contains(T flag) {
        return this.set.contains(flag);
    }
}
