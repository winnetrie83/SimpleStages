package be.winnetrie.mod.simplestages;

import be.winnetrie.mod.simplestages.client.StageEditorClientPayloadHandler;
import be.winnetrie.mod.simplestages.event.ClientTooltipEvents;
import be.winnetrie.mod.simplestages.network.StageEditorPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = SimpleStages.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = SimpleStages.MODID, value = Dist.CLIENT)
public final class SimpleStagesClient {

    public SimpleStagesClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        NeoForge.EVENT_BUS.register(ClientTooltipEvents.class);
    }

    @SubscribeEvent
    static void onRegisterClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(StageEditorPayload.TYPE, StageEditorClientPayloadHandler::handle);
    }
}
