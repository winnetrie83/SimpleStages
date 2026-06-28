package be.winnetrie.mod.simplestages.stage.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record StructureStageEntry(
        Identifier structure,
        int buffer
) {
    public static final int DEFAULT_BUFFER = 3;

    private static final Codec<StructureStageEntry> STRING_CODEC =
            Identifier.CODEC.xmap(
                    id -> new StructureStageEntry(id, DEFAULT_BUFFER),
                    StructureStageEntry::structure
            );

    private static final Codec<StructureStageEntry> OBJECT_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Identifier.CODEC.fieldOf("structure")
                            .forGetter(StructureStageEntry::structure),

                    Codec.INT.optionalFieldOf("buffer", DEFAULT_BUFFER)
                            .forGetter(StructureStageEntry::buffer)
            ).apply(instance, StructureStageEntry::new));

    public static final Codec<StructureStageEntry> CODEC =
            Codec.withAlternative(OBJECT_CODEC, STRING_CODEC);

    public int bufferOrDefault() {
        return buffer <= 0 ? DEFAULT_BUFFER : buffer;
    }
}