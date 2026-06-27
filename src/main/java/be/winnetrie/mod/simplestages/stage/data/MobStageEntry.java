package be.winnetrie.mod.simplestages.stage.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record MobStageEntry(
        Identifier mob,
        double radius
) {
    public static final Codec<MobStageEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Identifier.CODEC.fieldOf("mob")
                            .forGetter(MobStageEntry::mob),

                    Codec.DOUBLE.optionalFieldOf("radius", 64.0D)
                            .forGetter(MobStageEntry::radius)
            ).apply(instance, MobStageEntry::new)
    );
}