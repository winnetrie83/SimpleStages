package be.winnetrie.mod.simplestages.mixin;

import be.winnetrie.mod.simplestages.recipe.RecipeStageHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Locks the output-take path for vanilla smithing recipes. The mixin targets
 * the shared combiner menu but branches explicitly to SmithingMenu, so anvils
 * and other combiner menus are untouched.
 */
@Mixin(ItemCombinerMenu.class)
public abstract class ItemCombinerMenuMixin {

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void simplestages$lockSmithingRecipe(
        Player player,
        boolean hasStack,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!hasStack
            || !((Object) this instanceof SmithingMenu smithingMenu)
            || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ServerLevel level = (ServerLevel) serverPlayer.level();
        SmithingRecipeInput input = new SmithingRecipeInput(
            smithingMenu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem(),
            smithingMenu.getSlot(SmithingMenu.BASE_SLOT).getItem(),
            smithingMenu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem()
        );

        var recipe = level.getServer().getRecipeManager().getRecipeFor(
            RecipeType.SMITHING,
            input,
            level
        );

        if (recipe.isPresent()
            && !RecipeStageHelper.canUseRecipe(serverPlayer, recipe.get().id().identifier())) {
            cir.setReturnValue(false);
        }
    }
}
