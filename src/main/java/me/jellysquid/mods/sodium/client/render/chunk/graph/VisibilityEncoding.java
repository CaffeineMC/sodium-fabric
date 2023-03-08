package me.jellysquid.mods.sodium.client.render.chunk.graph;

/**
 * Demonstration of an encoding the 6x6 visibility matrix into a 15-bit integer.
 * @author douira
 */
public class VisibilityEncoding {
	private static int getIndex(int a, int b) {
		// returns the index in the upper right triangle in which a > b.
		// undefined for a == b.
        int max = Math.max(a, b);
        int min = Math.min(a, b);

        return (((5 - max) * 5) + min) - (0b1100 >> max);
    }

	public static int addConnection(int data, int a, int b) {
        return data | (1 << getIndex(a, b));
	}

	public static boolean isConnected(int data, int a, int b) {
        return (data & (1 << getIndex(a, b))) != 0;
	}
}