package be.winnetrie.mod.simplestages.event;

import be.winnetrie.mod.simplestages.stage.StageLockHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LockedItemEvents {

    private static final Set<UUID> ARMOR_MESSAGE_COOLDOWN = new HashSet<>();

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();

        if (!StageLockHelper.isLocked(player, event.getItemStack())) {
            return;
        }

        sendLockedMessage(player, StageLockHelper.getItemUseMessage(event.getItemStack()));

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();

        Block block = event.getLevel()
                .getBlockState(event.getPos())
                .getBlock();

        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);

        if (StageLockHelper.isBlockLocked(player, blockId)) {
            sendLockedMessage(player, StageLockHelper.getBlockUseMessage(blockId));

            event.setUseBlock(TriState.FALSE);
            event.setUseItem(TriState.FALSE);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        if (!StageLockHelper.isLocked(player, event.getItemStack())) {
            return;
        }

        sendLockedMessage(player, StageLockHelper.getItemUseMessage(event.getItemStack()));

        event.setUseItem(TriState.FALSE);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();

        Block block = event.getLevel()
                .getBlockState(event.getPos())
                .getBlock();

        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);

        if (StageLockHelper.isBlockLocked(player, blockId)) {
            sendLockedMessage(player, StageLockHelper.getBlockUseMessage(blockId));

            event.setUseItem(TriState.FALSE);
            event.setCanceled(true);
            return;
        }

        if (!StageLockHelper.isLocked(player, event.getItemStack())) {
            return;
        }

        sendLockedMessage(player, StageLockHelper.getItemUseMessage(event.getItemStack()));

        event.setUseItem(TriState.FALSE);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();

        if (!StageLockHelper.isLocked(player, player.getMainHandItem())) {
            return;
        }

        sendLockedMessage(player, StageLockHelper.getItemUseMessage(player.getMainHandItem()));

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!StageLockHelper.isLocked(player, event.getItem())) {
            return;
        }

        sendLockedMessage(player, StageLockHelper.getItemUseMessage(event.getItem()));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!StageLockHelper.isLocked(player, event.getTo())) {
            return;
        }

        sendLockedMessage(player, StageLockHelper.getItemUseMessage(event.getTo()));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        boolean hasLockedArmor = false;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) {
                continue;
            }

            ItemStack equipped = player.getItemBySlot(slot);

            if (!StageLockHelper.isLocked(player, equipped)) {
                continue;
            }

            hasLockedArmor = true;

            player.setItemSlot(slot, ItemStack.EMPTY);

            ItemStack copy = equipped.copy();

            if (!player.getInventory().add(copy)) {
                serverPlayer.drop(copy, false);
            }

            serverPlayer.containerMenu.broadcastChanges();

            sendArmorLockedMessage(serverPlayer, StageLockHelper.getItemUseMessage(equipped));
        }

        if (!hasLockedArmor) {
            ARMOR_MESSAGE_COOLDOWN.remove(serverPlayer.getUUID());
        }
    }

    private static void sendLockedMessage(Player player, String message) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.literal(message));
        }
    }

    private static void sendArmorLockedMessage(ServerPlayer player, String message) {
        if (ARMOR_MESSAGE_COOLDOWN.add(player.getUUID())) {
            player.sendOverlayMessage(Component.literal(message));
        }
    }
}