package net.caffeinemc.gfx.api.shader;

import java.util.Collection;

public record ShaderDescription(byte[] source, Collection<SpecializationConstant> specializationConstants) {
}
