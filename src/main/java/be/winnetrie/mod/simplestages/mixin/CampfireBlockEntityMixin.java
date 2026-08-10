package be.winnetrie.mod.simplestages.mixin;

import be.winnetrie.mod.simplestages.recipe.RecipeStageHelper;
import be.winnetrie.mod.simplestages.stage.data.StageDefinitionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents a staged campfire-cooking recipe from being placed on the campfire
 * unless the acting player owns the required stage.
 */
@Mixin(CampfireBlockEntity.class)
public abstract class CampfireBlockEntityMixin {

    @Inject(method = "placeFood", at = @At("HEAD"), cancellable = true)
    private void simplestages$lockCampfireRecipe(
        ServerLevel level,
        LivingEntity entity,
        ItemStack stack,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (stack.isEmpty()) {
            return;
        }

        var recipe = level.getServer().getRecipeManager().getRecipeFor(
            RecipeType.CAMPFIRE_COOKING,
            new SingleRecipeInput(stack),
            level
        );

        if (recipe.isEmpty()) {
            return;
        }

        var recipeId = recipe.get().id().identifier();
        if (!StageDefinitionManager.hasRecipeLocked(recipeId)) {
            return;
        }

        if (entity instanceof ServerPlayer serverPlayer
            && RecipeStageHelper.canUseRecipe(serverPlayer, recipeId)) {
            return;
        }

        // No valid player-stage context: do not start the staged cook at all.
        cir.setReturnValue(false);
    }
}
