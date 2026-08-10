package be.winnetrie.mod.simplestages.mixin;

import be.winnetrie.mod.simplestages.SimpleStages;
import be.winnetrie.mod.simplestages.network.SyncStonecutterRecipesPayload;
import be.winnetrie.mod.simplestages.recipe.RecipeStageHelper;
import be.winnetrie.mod.simplestages.recipe.StonecutterMenuStageBridge;
import be.winnetrie.mod.simplestages.stage.StageManager;
import be.winnetrie.mod.simplestages.stage.data.StageDefinitionManager;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Player-relative stonecutter recipe staging.
 *
 * The important part is that recipes are filtered BEFORE the StonecutterScreen
 * assigns button indexes. Both logical sides therefore see the same compacted
 * list. This prevents a forbidden client button from referring to a different,
 * previously-selected recipe on the server.
 */
@Mixin(StonecutterMenu.class)
public abstract class StonecutterMenuMixin implements StonecutterMenuStageBridge {

    @Shadow
    private SelectableRecipe.SingleInputSet<StonecutterRecipe> recipesForInput;

    @Shadow
    @Final
    private DataSlot selectedRecipeIndex;

    @Shadow
    @Final
    private ResultContainer resultContainer;

    @Shadow
    private Runnable slotUpdateListener;

    @Unique
    private Player simplestages$owner;

    @Inject(
            method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at = @At("TAIL")
    )
    private void simplestages$captureOwner(
            int containerId,
            Inventory inventory,
            ContainerLevelAccess access,
            CallbackInfo ci
    ) {
        this.simplestages$owner = inventory.player;
    }

    /**
     * Vanilla has just selected all stonecutter recipes for the input item.
     *
     * Client: hide the local vanilla list until the authoritative per-player
     * server list arrives. This means a forbidden option never becomes a
     * clickable transient button while waiting for the network round-trip.
     *
     * Server: remove recipes whose required stage is missing, then send that
     * exact filtered list to this menu's client.
     */
    @Inject(method = "setupRecipeList", at = @At("RETURN"))
    private void simplestages$filterRecipeList(ItemStack item, CallbackInfo ci) {
        if (!(this.simplestages$owner instanceof ServerPlayer serverPlayer)) {
            simplestages$replaceVisibleRecipes(SelectableRecipe.SingleInputSet.empty());
            return;
        }

        List<SelectableRecipe.SingleInputEntry<StonecutterRecipe>> allowed = new ArrayList<>();
        for (SelectableRecipe.SingleInputEntry<StonecutterRecipe> entry : this.recipesForInput.entries()) {
            var holder = entry.recipe().recipe();
            if (holder.isEmpty()) {
                // Defensive compatibility for display-only/modded entries. A server
                // entry normally has its RecipeHolder, but an unknown entry should
                // not be silently destroyed by Simple Stages.
                allowed.add(entry);
                continue;
            }

            var recipeId = holder.get().id().identifier();
            String requiredStage = StageDefinitionManager.getRequiredStageForRecipe(recipeId);
            if (requiredStage == null || StageManager.hasStage(serverPlayer, requiredStage)) {
                allowed.add(entry);
            }
        }

        SelectableRecipe.SingleInputSet<StonecutterRecipe> filtered =
                new SelectableRecipe.SingleInputSet<>(List.copyOf(allowed));
        simplestages$replaceVisibleRecipes(filtered);

        // Defer by one server task. The menu click that inserted/changed the input
        // may still emit vanilla slot/data packets after slotsChanged(). Sending
        // our filtered list afterwards prevents that later vanilla sync from
        // repopulating the client's unfiltered list.
        var server = serverPlayer.level().getServer();
        if (server != null) {
            int menuId = ((StonecutterMenu) (Object) this).containerId;
            server.schedule(new TickTask(server.getTickCount() + 1, () -> {
                if (serverPlayer.containerMenu.containerId != menuId) {
                    return;
                }
                PacketDistributor.sendToPlayer(
                        serverPlayer,
                        new SyncStonecutterRecipesPayload(menuId, filtered)
                );
            }));
        }
    }

    /**
     * Secondary server-side guard. The filtered list is the main enforcement,
     * but keeping this check means a malformed/stale button packet can never
     * select a recipe that has become locked between list generation and click.
     */
    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void simplestages$lockStonecutterRecipe(
            Player player,
            int recipeIndex,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (this.recipesForInput == null
                || recipeIndex < 0
                || recipeIndex >= this.recipesForInput.size()) {
            return;
        }

        var selectableRecipe = this.recipesForInput.entries().get(recipeIndex).recipe();
        var recipeHolder = selectableRecipe.recipe();
        if (recipeHolder.isEmpty()) {
            return;
        }

        var recipeId = recipeHolder.get().id().identifier();
        if (RecipeStageHelper.canUseRecipe(serverPlayer, recipeId)) {
            return;
        }

        SimpleStages.debug("Blocked stonecutter recipe: {}", recipeId);
        simplestages$clearSelection();
        cir.setReturnValue(false);
    }

    @Override
    public void simplestages$applyVisibleRecipes(
            SelectableRecipe.SingleInputSet<StonecutterRecipe> recipes
    ) {
        simplestages$replaceVisibleRecipes(recipes);
    }

    @Unique
    private void simplestages$replaceVisibleRecipes(
            SelectableRecipe.SingleInputSet<StonecutterRecipe> recipes
    ) {
        this.recipesForInput = recipes;
        simplestages$clearSelection();
        if (this.slotUpdateListener != null) {
            this.slotUpdateListener.run();
        }
    }

    @Unique
    private void simplestages$clearSelection() {
        this.selectedRecipeIndex.set(-1);
        this.resultContainer.setItem(0, ItemStack.EMPTY);
    }
}
