package be.winnetrie.mod.simplestages.event;

import be.winnetrie.mod.simplestages.stage.StageLockHelper;
import be.winnetrie.mod.simplestages.stage.data.StageDefinitionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Set;

@EventBusSubscriber
public class DimensionEvents {

    @SubscribeEvent
    public static void onDimensionTravel(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Identifier dimensionId = event.getDimension().identifier();
        String requiredStage = StageDefinitionManager.getRequiredStageForDimension(dimensionId);

        if (requiredStage == null) {
            return;
        }

        if (StageLockHelper.canEnterDimension(player, dimensionId)) {
            return;
        }

        player.sendSystemMessage(
                Component.literal(StageLockHelper.getDimensionMessage(requiredStage))
        );

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % 20 != 0) {
            return;
        }

        Identifier currentDimensionId = player.level().dimension().identifier();
        String requiredStage = StageDefinitionManager.getRequiredStageForDimension(currentDimensionId);

        if (requiredStage == null) {
            return;
        }

        if (StageLockHelper.canEnterDimension(player, currentDimensionId)) {
            return;
        }

        sendBackToOverworld(player, requiredStage);
    }

    private static void sendBackToOverworld(ServerPlayer player, String requiredStage) {
        MinecraftServer server = player.level().getServer();

        if (server == null) {
            return;
        }

        ServerLevel overworld = server.getLevel(Level.OVERWORLD);

        if (overworld == null) {
            return;
        }

        BlockPos spawnPos = overworld.getRespawnData().pos();

        player.sendSystemMessage(
                Component.literal(StageLockHelper.getDimensionMessage(requiredStage))
        );

        player.teleportTo(
                overworld,
                spawnPos.getX() + 0.5D,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5D,
                Set.of(),
                player.getYRot(),
                player.getXRot(),
                false
        );
    }
}