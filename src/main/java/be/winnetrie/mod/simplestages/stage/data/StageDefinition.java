package be.winnetrie.mod.simplestages.stage.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

public record StageDefinition(
        String stage,
        String displayName,
        List<Identifier> recipes,
        List<Identifier> items,
        List<BlockMaskEntry> blocks
) {
    public static final Codec<StageDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("stage")
                            .forGetter(StageDefinition::stage),

                    Codec.STRING.optionalFieldOf("display_name", "")
                            .forGetter(StageDefinition::displayName),

                    Identifier.CODEC.listOf().optionalFieldOf("recipes", List.of())
                            .forGetter(StageDefinition::recipes),

                    Identifier.CODEC.listOf().optionalFieldOf("items", List.of())
                            .forGetter(StageDefinition::items),

                    BlockMaskEntry.CODEC.listOf().optionalFieldOf("blocks", List.of())
                            .forGetter(StageDefinition::blocks)
            ).apply(instance, StageDefinition::new)
    );

    public String displayNameOrStage() {
        return displayName == null || displayName.isBlank()
                ? stage
                : displayName;
    }
}