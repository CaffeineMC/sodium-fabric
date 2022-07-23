package net.caffeinemc.sodium.interop.vanilla.sequence;

import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.util.buffer.streaming.SequenceBuilder;
import net.minecraft.client.render.VertexFormat;

public class Blaze3DSequences {
    
    public static SequenceBuilder map(VertexFormat.DrawMode drawMode, ElementFormat elementFormat) {
        return switch (elementFormat) {
            case UNSIGNED_BYTE -> switch (drawMode) {
                case LINES -> SequenceBuilder.LINES_BYTE;
                case QUADS -> SequenceBuilder.QUADS_BYTE;
                default -> SequenceBuilder.DEFAULT_BYTE;
            };
            case UNSIGNED_SHORT -> switch (drawMode) {
                case LINES -> SequenceBuilder.LINES_SHORT;
                case QUADS -> SequenceBuilder.QUADS_SHORT;
                default -> SequenceBuilder.DEFAULT_SHORT;
            };
            case UNSIGNED_INT -> switch (drawMode) {
                case LINES -> SequenceBuilder.LINES_INT;
                case QUADS -> SequenceBuilder.QUADS_INT;
                default -> SequenceBuilder.DEFAULT_INT;
            };
        };
    }
}
