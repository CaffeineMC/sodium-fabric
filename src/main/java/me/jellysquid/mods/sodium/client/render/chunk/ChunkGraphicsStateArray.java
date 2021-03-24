package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;

public class ChunkGraphicsStateArray {
    private final int[] key;
    private final int[] value;

    private int size;

    public ChunkGraphicsStateArray(int size) {
        this.key = new int[size];
        this.value = new int[size];
    }

    public int get(BlockRenderPass pass) {
        return this.get(pass.ordinal());
    }

    public int get(int pass) {
        for (int i = 0; i < this.size; i++) {
            if (this.key[i] == pass) {
                return this.value[i];
            }
        }

        return -1;
    }

    public void set(BlockRenderPass pass, int value) {
        this.set(pass.ordinal(), value);
    }

    public void set(int pass, int value) {
        if (value == -1) {
            this.remove(pass);

            return;
        }

        for (int i = 0; i < this.size; i++) {
            if (this.key[i] == pass) {
                this.value[i] = value;
                return;
            }
        }

        int i = this.size++;

        this.key[i] = pass;
        this.value[i] = value;
    }

    public int remove(BlockRenderPass pass) {
        return this.remove(pass.ordinal());
    }

    public int remove(int pass) {
        int value = -1;
        int split = -1;

        for (int i = 0; i < this.size; i++) {
            if (this.key[i] == pass) {
                split = i;
                value = this.value[i];

                break;
            }
        }

        if (split < 0) {
            return - 1;
        }

        this.size--;

        System.arraycopy(this.key, split + 1, this.key, split, this.size - split);
        System.arraycopy(this.value, split + 1, this.value, split, this.size - split);

        return value;
    }

    public int getKey(int i) {
        return this.key[i];
    }

    public int getValue(int i) {
        return this.value[i];
    }

    public int getSize() {
        return this.size;
    }

    public void clear() {
        this.size = 0;
    }
}
