package be.winnetrie.mod.simplestages.event;

import be.winnetrie.mod.simplestages.SimpleStages;
import be.winnetrie.mod.simplestages.stage.data.StageDefinitionStorage;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = SimpleStages.MODID)
public final class StageStorageEvents {

    private StageStorageEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        StageDefinitionStorage.initialize(event.getServer());
    }
}
