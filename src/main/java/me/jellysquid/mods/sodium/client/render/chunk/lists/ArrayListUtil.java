package me.jellysquid.mods.sodium.client.render.chunk.lists;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;

import java.util.Arrays;

public class ArrayListUtil {
    static int arrayClear(int[] sections, int count) {
        Arrays.fill(sections, 0, count, 0);
        return 0;
    }

    static <T> int arrayClear(T[] sections, int count) {
        Arrays.fill(sections, 0, count, null);
        return 0;
    }

    static int arrayPush(int[] sections, int count, int value) {
        sections[count++] = value;
        return count;
    }

    static <T> int arrayPush(T[] sections, int count, T value) {
        sections[count++] = value;
        return count;
    }
}
