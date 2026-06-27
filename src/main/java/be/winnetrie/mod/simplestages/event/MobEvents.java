package be.winnetrie.mod.simplestages.event;

import be.winnetrie.mod.simplestages.stage.StageLockHelper;
import be.winnetrie.mod.simplestages.stage.data.StageDefinitionManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class MobEvents {

    

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();

        Identifier mobId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());

        if (!StageLockHelper.isMobLocked(player, mobId)) {
            return;
        }

        sendMobMessage(player, mobId);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();

        Identifier mobId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());

        if (!StageLockHelper.isMobLocked(player, mobId)) {
            return;
        }

        sendMobMessage(player, mobId);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    @SubscribeEvent
    public static void onMobJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Identifier mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());

        Player nearestPlayer = level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), StageDefinitionManager.getMobSpawnRadius(mobId), false);

        if (!(nearestPlayer instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (StageLockHelper.canMobExistNearPlayer(serverPlayer, mobId)) {
            return;
        }

        event.setCanceled(true);
    }

    private static void sendMobMessage(Player player, Identifier mobId) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(
                    Component.literal(StageLockHelper.getMobUseMessage(mobId))
            );
        }
    }
}