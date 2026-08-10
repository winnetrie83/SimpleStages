package be.winnetrie.mod.simplestages.mixin;

import be.winnetrie.mod.simplestages.recipe.FurnaceRecipeStageHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Updates the furnace's player-relative stage view around real inventory
 * actions. Nothing is cancelled here: clicks, shift-clicks and item placement
 * remain 100% vanilla.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Inject(method = "clicked", at = @At("HEAD"))
    private void simplestages$refreshFurnaceStageViewBeforeClick(
            int slotId,
            int button,
            ContainerInput input,
            Player player,
            CallbackInfo ci
    ) {
        simplestages$refreshFurnaceStageView(player);
    }

    @Inject(method = "clicked", at = @At("RETURN"))
    private void simplestages$refreshFurnaceStageViewAfterClick(
            int slotId,
            int button,
            ContainerInput input,
            Player player,
            CallbackInfo ci
    ) {
        simplestages$refreshFurnaceStageView(player);
    }

    @Unique
    private void simplestages$refreshFurnaceStageView(Player player) {
        if (!((Object) this instanceof AbstractFurnaceMenu menu)
                || !(player instanceof ServerPlayer serverPlayer)
                || menu.slots.isEmpty()) {
            return;
        }

        var inputSlot = menu.getSlot(0);
        if (inputSlot.container instanceof AbstractFurnaceBlockEntity furnace) {
            FurnaceRecipeStageHelper.refreshForPlayer(serverPlayer, furnace);
        }
    }
}
