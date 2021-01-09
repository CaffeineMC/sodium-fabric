// Copyright 2020 Grondag
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

package me.jellysquid.mods.sodium.client.gl.shader;

import java.nio.ByteBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/**
 * Contains a workaround for a crash in nglShaderSource on some AMD drivers. Copied from the following Canvas commit:
 * https://github.com/grondag/canvas/commit/820bf754092ccaf8d0c169620c2ff575722d7d96
 */
class ShaderWorkarounds {
	/**
	 * Identical in function to {@link GL20C#glShaderSource(int, CharSequence)} but
	 * passes a null pointer for string length to force the driver to rely on the null
	 * terminator for string length.  This is a workaround for an apparent flaw with some
	 * AMD drivers that don't receive or interpret the length correctly, resulting in
	 * an access violation when the driver tries to read past the string memory.
	 *
	 * <p>Hat tip to fewizz for the find and the fix.
	 */
	static void safeShaderSource(int glId, CharSequence source) {
		final MemoryStack stack = MemoryStack.stackGet();
		final int stackPointer = stack.getPointer();

		try {
			final ByteBuffer sourceBuffer = MemoryUtil.memUTF8(source, true);
			final PointerBuffer pointers = stack.mallocPointer(1);
			pointers.put(sourceBuffer);

			GL20C.nglShaderSource(glId, 1, pointers.address0(), 0);
			org.lwjgl.system.APIUtil.apiArrayFree(pointers.address0(), 1);
		} finally {
			stack.setPointer(stackPointer);
		}
	}
}
