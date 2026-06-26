package be.winnetrie.mod.simplestages.mixin;

import be.winnetrie.mod.simplestages.SimpleStages;
import be.winnetrie.mod.simplestages.recipe.RecipeStageHelper;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(CraftingMenu.class)
public class CraftingMenuMixin {

    @Inject(
            method = "slotChangedCraftingGrid",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void simplestages$lockRecipe(
            AbstractContainerMenu menu,
            ServerLevel level,
            Player player,
            CraftingContainer craftingContainer,
            ResultContainer resultContainer,
            RecipeHolder<CraftingRecipe> previousRecipe,
            CallbackInfo ci
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        CraftingInput input = craftingContainer.asCraftInput();

        Optional<RecipeHolder<CraftingRecipe>> optionalRecipe =
                level.getServer().getRecipeManager().getRecipeFor(
                        RecipeType.CRAFTING,
                        input,
                        level,
                        previousRecipe
                );

        if (optionalRecipe.isEmpty()) {
            return;
        }

        RecipeHolder<CraftingRecipe> recipe = optionalRecipe.get();

        SimpleStages.debug("Recipe id = {}", recipe.id().identifier());

        if (RecipeStageHelper.canUseRecipe(serverPlayer, recipe.id().identifier())) {
            return;
        }

        SimpleStages.debug("Blocked recipe: {}", recipe.id().identifier());

        ItemStack empty = ItemStack.EMPTY;

        resultContainer.setItem(0, empty);
        menu.setRemoteSlot(0, empty);

        serverPlayer.connection.send(
                new ClientboundContainerSetSlotPacket(
                        menu.containerId,
                        menu.incrementStateId(),
                        0,
                        empty
                )
        );

        ci.cancel();
    }
}