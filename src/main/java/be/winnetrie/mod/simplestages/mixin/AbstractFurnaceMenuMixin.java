package be.winnetrie.mod.simplestages.mixin;

import be.winnetrie.mod.simplestages.recipe.FurnaceRecipeStageHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Initializes the furnace stage view as soon as a server-side furnace menu is
 * opened. This avoids requiring a dummy click before a player who owns the
 * stage can start an already-loaded machine.
 */
@Mixin(AbstractFurnaceMenu.class)
public abstract class AbstractFurnaceMenuMixin {

    @Inject(
            method = "<init>(Lnet/minecraft/world/inventory/MenuType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/inventory/RecipeBookType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",
            at = @At("TAIL")
    )
    private void simplestages$initializeFurnaceStageView(
            MenuType<?> menuType,
            ResourceKey<RecipePropertySet> allowedInputs,
            RecipeBookType recipeBookType,
            int containerId,
            Inventory inventory,
            Container container,
            ContainerData data,
            CallbackInfo ci
    ) {
        if (inventory.player instanceof ServerPlayer serverPlayer
                && container instanceof AbstractFurnaceBlockEntity furnace) {
            FurnaceRecipeStageHelper.refreshForPlayer(serverPlayer, furnace);
        }
    }
}
