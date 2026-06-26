package be.winnetrie.mod.simplestages.stage.data;

import net.minecraft.resources.Identifier;

public record ItemMaskEntry(
        Identifier item,
        String stage
) {
}