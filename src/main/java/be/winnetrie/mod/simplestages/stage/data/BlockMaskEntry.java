package be.winnetrie.mod.simplestages.stage.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

public record BlockMaskEntry(
        Identifier block,
        Identifier mask
) {
    public static final Codec<BlockMaskEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Identifier.CODEC.fieldOf("block").forGetter(BlockMaskEntry::block),
                    Identifier.CODEC.fieldOf("mask").forGetter(BlockMaskEntry::mask)
            ).apply(instance, BlockMaskEntry::new)
    );
}