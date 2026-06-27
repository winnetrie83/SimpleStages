package be.winnetrie.mod.simplestages;

import be.winnetrie.mod.simplestages.event.ClientTooltipEvents;
import be.winnetrie.mod.simplestages.stage.data.StageDefinitionReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = SimpleStages.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = SimpleStages.MODID, value = Dist.CLIENT)
public class SimpleStagesClient {

    public SimpleStagesClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        NeoForge.EVENT_BUS.register(ClientTooltipEvents.class);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        SimpleStages.LOGGER.info("HELLO FROM CLIENT SETUP");
        SimpleStages.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    @SubscribeEvent
    static void onRegisterClientReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(
                Identifier.fromNamespaceAndPath(SimpleStages.MODID, "stage_definitions"),
                new StageDefinitionReloadListener()
        );
    }
}