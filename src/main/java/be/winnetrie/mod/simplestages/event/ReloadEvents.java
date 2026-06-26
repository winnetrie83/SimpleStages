package be.winnetrie.mod.simplestages.event;

import be.winnetrie.mod.simplestages.SimpleStages;
import be.winnetrie.mod.simplestages.stage.data.StageDefinitionReloadListener;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

@EventBusSubscriber(modid = SimpleStages.MODID)
public class ReloadEvents {

    @SubscribeEvent
    public static void onAddServerReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(
                Identifier.fromNamespaceAndPath(SimpleStages.MODID, "stage_definitions"),
                new StageDefinitionReloadListener()
        );
    }
}