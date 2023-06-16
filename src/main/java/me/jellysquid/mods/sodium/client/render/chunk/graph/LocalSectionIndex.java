package me.jellysquid.mods.sodium.client.render.chunk.graph;

// Bits are formatted as XYZXYZXZ
public class LocalSectionIndex {
    public static final int SIZEOF = 1;

    public static int pack(int x, int y, int z) {
        int packed = z & 0b00000001;
        packed |= (x & 0b00000001) << 1;
        packed |= (z & 0b00000010) << 1;
        packed |= (y & 0b00000001) << 3;
        packed |= (x & 0b00000010) << 3;
        packed |= (z & 0b00000100) << 3;
        packed |= (y & 0b00000010) << 5;
        packed |= (x & 0b00000100) << 5;
        return packed;
    }

    public static int unpackX(int idx) {
        int x = (idx & 0b00000010) >> 1;
        x |= (idx & 0b00010000) >> 3;
        x |= (idx & 0b10000000) >> 5;
        return x;
    }

    public static int unpackY(int idx) {
        int y = (idx & 0b00001000) >> 3;
        y |= (idx & 0b01000000) >> 5;
        return y;
    }

    public static int unpackZ(int idx) {
        int z = idx & 0b00000001;
        z |= (idx & 0b00000100) >> 1;
        z |= (idx & 0b00100000) >> 3;
        return z;
    }

    public static int fromByte(byte idx) {
        return Byte.toUnsignedInt(idx);
    }
}
