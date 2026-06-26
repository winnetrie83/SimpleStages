package be.winnetrie.mod.simplestages.event;

import be.winnetrie.mod.simplestages.stage.StageLockHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class ClientTooltipEvents {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();

        if (player == null) {
            return;
        }

        if (!StageLockHelper.isLocked(player, event.getItemStack())) {
            return;
        }

        String requiredStage = StageLockHelper.getRequiredStage(event.getItemStack());

        event.getToolTip().clear();

        event.getToolTip().add(
                Component.literal("Unidentified Item")
                        .withStyle(ChatFormatting.GRAY)
        );

        event.getToolTip().add(
                Component.literal("Requires Stage: " + requiredStage)
                        .withStyle(ChatFormatting.RED)
        );
    }
}