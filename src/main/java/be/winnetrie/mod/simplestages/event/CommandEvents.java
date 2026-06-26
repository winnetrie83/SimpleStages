package be.winnetrie.mod.simplestages.event;

import be.winnetrie.mod.simplestages.SimpleStages;
import be.winnetrie.mod.simplestages.stage.StageCommands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = SimpleStages.MODID)
public class CommandEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        StageCommands.register(event.getDispatcher());
    }
}