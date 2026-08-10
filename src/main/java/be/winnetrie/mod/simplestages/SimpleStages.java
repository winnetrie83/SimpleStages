package be.winnetrie.mod.simplestages;

import be.winnetrie.mod.simplestages.event.LockedItemEvents;
import be.winnetrie.mod.simplestages.event.MobEvents;
import be.winnetrie.mod.simplestages.event.PlayerStageEvents;
import be.winnetrie.mod.simplestages.network.NetworkEvents;
import be.winnetrie.mod.simplestages.registry.ModLootModifiers;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(SimpleStages.MODID)
public final class SimpleStages {

    public static final String MODID = "simplestages";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SimpleStages(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.addListener(NetworkEvents::registerPayloads);
        ModLootModifiers.register(modEventBus);

        NeoForge.EVENT_BUS.register(LockedItemEvents.class);
        NeoForge.EVENT_BUS.register(MobEvents.class);
        NeoForge.EVENT_BUS.register(PlayerStageEvents.class);

        LOGGER.info("Simple Stages loaded");
    }

    public static void debug(String message, Object... args) {
        if (Config.ENABLE_DEBUG_LOGGING.get()) {
            LOGGER.info("[DEBUG] " + message, args);
        }
    }
}
