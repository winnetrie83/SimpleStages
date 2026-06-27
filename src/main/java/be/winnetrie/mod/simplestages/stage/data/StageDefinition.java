package be.winnetrie.mod.simplestages.stage.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;

public record StageDefinition(
        String stage,
        String displayName,
        Map<String, String> messages,
        List<Identifier> recipes,
        List<Identifier> items,
        List<BlockMaskEntry> blocks,
        List<Identifier> dimensions,
        List<MobStageEntry> mobs
) {
    public static final Codec<StageDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("stage").forGetter(StageDefinition::stage),

                    Codec.STRING.optionalFieldOf("display_name", "")
                            .forGetter(StageDefinition::displayName),

                    Codec.unboundedMap(Codec.STRING, Codec.STRING)
                            .optionalFieldOf("messages", Map.of())
                            .forGetter(StageDefinition::messages),

                    Identifier.CODEC.listOf().optionalFieldOf("recipes", List.of())
                            .forGetter(StageDefinition::recipes),

                    Identifier.CODEC.listOf().optionalFieldOf("items", List.of())
                            .forGetter(StageDefinition::items),

                    BlockMaskEntry.CODEC.listOf().optionalFieldOf("blocks", List.of())
                            .forGetter(StageDefinition::blocks),

                    Identifier.CODEC.listOf().optionalFieldOf("dimensions", List.of())
                            .forGetter(StageDefinition::dimensions),

                    MobStageEntry.CODEC.listOf().optionalFieldOf("mobs", List.of())
                            .forGetter(StageDefinition::mobs)
            ).apply(instance, StageDefinition::new)
    );

    public String displayNameOrStage() {
        return displayName == null || displayName.isBlank() ? stage : displayName;
    }

    public String getMessage(String key) {
        if (messages == null) {
            return "";
        }

        return messages.getOrDefault(key, "");
    }
}