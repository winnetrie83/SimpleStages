package be.winnetrie.mod.simplestages.client;

import be.winnetrie.mod.simplestages.SimpleStages;
import be.winnetrie.mod.simplestages.admin.StageEditorCatalog;
import be.winnetrie.mod.simplestages.admin.StageEditorSnapshot;
import be.winnetrie.mod.simplestages.client.screen.StageEditorScreen;
import be.winnetrie.mod.simplestages.client.screen.StageRestrictionPickerScreen;
import be.winnetrie.mod.simplestages.network.StageEditorPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class StageEditorClientPayloadHandler {
    private StageEditorClientPayloadHandler() {
    }

    public static void handle(StageEditorPayload payload, IPayloadContext context) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if ("snapshot".equals(payload.kind())) {
                StageEditorSnapshot snapshot = StageEditorSnapshot.fromJson(payload.json());
                if (minecraft.gui.screen() instanceof StageEditorScreen screen) {
                    screen.applyServerSnapshot(snapshot);
                } else {
                    minecraft.setScreenAndShow(new StageEditorScreen(snapshot));
                }
                return;
            }

            if ("catalog".equals(payload.kind())) {
                StageEditorCatalog catalog = StageEditorCatalog.fromJson(payload.json());
                if (minecraft.gui.screen() instanceof StageRestrictionPickerScreen picker) {
                    picker.applyCatalog(catalog);
                }
            }
        } catch (RuntimeException exception) {
            SimpleStages.LOGGER.error("Could not handle Simple Stages editor payload", exception);
        }
    }
}
