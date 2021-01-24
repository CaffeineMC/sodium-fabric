package me.jellysquid.mods.sodium.client.render.chunk.format.xhfp;

import java.nio.ByteBuffer;

public class QuadView {
	ByteBuffer buffer;
	int writeOffset;
	private static final int STRIDE = 48;

	float x(int index) {
		return normalizeShortAsFloat(buffer.getShort(writeOffset - STRIDE * (3 - index)));
	}

	float y(int index) {
		return normalizeShortAsFloat(buffer.getShort(writeOffset + 2 - STRIDE * (3 - index)));
	}

	float z(int index) {
		return normalizeShortAsFloat(buffer.getShort(writeOffset + 4 - STRIDE * (3 - index)));
	}

	private static float normalizeShortAsFloat(short value) {
		return (value & 0xFFFF) * (1.0f / 65535.0f);
	}
}
