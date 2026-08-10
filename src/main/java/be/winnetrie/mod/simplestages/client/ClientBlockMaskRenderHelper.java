package be.winnetrie.mod.simplestages.client;

import net.minecraft.client.Minecraft;

/** Client-only render invalidation used when stage visibility changes. */
public final class ClientBlockMaskRenderHelper {
    private ClientBlockMaskRenderHelper() {
    }

    public static void refreshWorld() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            // Since 26.1 the level extraction layer owns full world render
            // invalidation; LevelRenderer#allChanged no longer exists.
            minecraft.levelExtractor.allChanged();
        }
    }
}
