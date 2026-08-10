package be.winnetrie.mod.simplestages.network;

import be.winnetrie.mod.simplestages.SimpleStages;
import be.winnetrie.mod.simplestages.recipe.StonecutterMenuStageBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;

/**
 * Per-player stonecutter recipe view.
 *
 * Vanilla intentionally synchronizes stonecutter displays without the actual
 * RecipeHolder. We do the same here: recipe IDs are used only on the server to
 * filter, while the resulting display/input entries are sent to the client.
 */
public record SyncStonecutterRecipesPayload(
        int containerId,
        SelectableRecipe.SingleInputSet<StonecutterRecipe> recipes
) implements CustomPacketPayload {

    public static final Type<SyncStonecutterRecipesPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleStages.MODID, "sync_stonecutter_recipes"));

    private static final StreamCodec<RegistryFriendlyByteBuf, SelectableRecipe.SingleInputSet<StonecutterRecipe>>
            RECIPE_SET_CODEC = SelectableRecipe.SingleInputSet.noRecipeCodec();

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncStonecutterRecipesPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeVarInt(payload.containerId());
                        RECIPE_SET_CODEC.encode(buf, payload.recipes());
                    },
                    buf -> new SyncStonecutterRecipesPayload(
                            buf.readVarInt(),
                            RECIPE_SET_CODEC.decode(buf)
                    )
            );

    public static void handle(
            SyncStonecutterRecipesPayload payload,
            net.neoforged.neoforge.network.handling.IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return;
            }
            if (!(minecraft.player.containerMenu instanceof StonecutterMenu menu)) {
                return;
            }
            if (menu.containerId != payload.containerId()) {
                return;
            }
            if (menu instanceof StonecutterMenuStageBridge bridge) {
                bridge.simplestages$applyVisibleRecipes(payload.recipes());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
