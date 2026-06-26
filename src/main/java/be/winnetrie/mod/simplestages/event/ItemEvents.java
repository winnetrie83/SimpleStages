package be.winnetrie.mod.simplestages.event;

import be.winnetrie.mod.simplestages.stage.StageLockHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber
public class ItemEvents {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack stack = event.getItemStack();

        if (stack.isEmpty()) {
            return;
        }

        if (!StageLockHelper.isLocked(player, stack)) {
            return;
        }

        player.sendSystemMessage(Component.literal("You have not identified this item yet."));

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }
}