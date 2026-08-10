package be.winnetrie.mod.simplestages.network;

import be.winnetrie.mod.simplestages.SimpleStages;
import be.winnetrie.mod.simplestages.admin.StageEditorCatalog;
import be.winnetrie.mod.simplestages.admin.StageEditorSnapshot;
import be.winnetrie.mod.simplestages.stage.StageCommands;
import be.winnetrie.mod.simplestages.stage.data.BlockMaskEntry;
import be.winnetrie.mod.simplestages.stage.data.MobStageEntry;
import be.winnetrie.mod.simplestages.stage.data.StageDefinition;
import be.winnetrie.mod.simplestages.stage.data.StageDefinitionManager;
import be.winnetrie.mod.simplestages.stage.data.StageDefinitionStorage;
import be.winnetrie.mod.simplestages.stage.data.StructureStageEntry;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Server-authoritative CRUD endpoint and on-demand picker data for the in-game Stage Manager. */
public final class StageEditorNetwork {
    private StageEditorNetwork() {
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playBidirectional(
                StageEditorPayload.TYPE,
                StageEditorPayload.STREAM_CODEC,
                StageEditorNetwork::handleServerPayload
        );
    }

    public static void openEditor(ServerPlayer player) {
        if (!StageCommands.canEdit(player)) {
            return;
        }
        sendSnapshot(player, "", false);
    }

    private static void handleServerPayload(StageEditorPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!"action".equals(payload.kind())) {
            return;
        }
        if (!StageCommands.canEdit(player)) {
            sendSnapshot(player, "You no longer have permission to edit stages.", true);
            return;
        }

        String action = "";
        JsonObject root = null;
        try {
            root = JsonParser.parseString(payload.json()).getAsJsonObject();
            action = requiredString(root, "action");
            switch (action) {
                case "save" -> handleSave(player, root);
                case "delete" -> handleDelete(player, root);
                case "refresh" -> sendSnapshot(player, "Refreshed stage definitions.", false);
                case "reload" -> handleReload(player);
                case "export_pack" -> handleExportPack(player);
                case "recipe_catalog" -> handleRecipeCatalog(player, root);
                case "registry_catalog" -> handleRegistryCatalog(player, root);
                default -> sendSnapshot(player, "Unknown stage editor action: " + action, true);
            }
        } catch (Exception exception) {
            SimpleStages.LOGGER.warn("Rejected invalid stage editor request from {}", player.getScoreboardName(), exception);
            if ("recipe_catalog".equals(action)) {
                String item = root == null ? "" : optionalString(root, "item");
                sendCatalog(player, new StageEditorCatalog("recipes", item, List.of(),
                        "Could not load recipes: " + safeMessage(exception), true));
            } else if ("registry_catalog".equals(action)) {
                String catalog = root == null ? "" : optionalString(root, "catalog");
                sendCatalog(player, new StageEditorCatalog(catalog, "", List.of(),
                        "Could not load catalog: " + safeMessage(exception), true));
            } else {
                sendSnapshot(player, "Change rejected: " + safeMessage(exception), true);
            }
        }
    }

    private static void handleSave(ServerPlayer player, JsonObject root) throws Exception {
        if (!root.has("stage")) {
            throw new IllegalArgumentException("Missing stage definition");
        }

        String originalStage = optionalString(root, "original_stage").trim();
        StageEditorSnapshot.StageEntry editorEntry = StageEditorSnapshot.entryFromJson(root.get("stage"));
        StageDefinition definition = editorEntry.toDefinition();
        validateRegistryReferences(player.level().getServer(), definition);

        Map<String, StageDefinition> current = new LinkedHashMap<>(StageDefinitionManager.getDefinitionsById());
        boolean editingExisting = !originalStage.isBlank() && current.containsKey(originalStage);

        if (editingExisting && !originalStage.equals(definition.stage())) {
            if (current.containsKey(definition.stage())) {
                throw new IllegalArgumentException("A stage named '" + definition.stage() + "' already exists.");
            }
            current.remove(originalStage);
        } else if (!editingExisting && current.containsKey(definition.stage())) {
            throw new IllegalArgumentException("A stage named '" + definition.stage() + "' already exists.");
        }

        current.put(definition.stage(), definition);
        StageDefinitionStorage.saveAll(player.level().getServer(), current.values());
        sendSnapshot(player, "Saved stage '" + definition.stage() + "'. Changes are active immediately.", false);
    }

    private static void handleDelete(ServerPlayer player, JsonObject root) throws Exception {
        String stage = requiredString(root, "stage").trim();
        if (!StageDefinitionStorage.delete(player.level().getServer(), stage)) {
            throw new IllegalArgumentException("Stage no longer exists: " + stage);
        }
        sendSnapshot(player, "Deleted stage '" + stage + "'.", false);
    }

    private static void handleReload(ServerPlayer player) throws Exception {
        StageDefinitionStorage.reload(player.level().getServer());
        sendSnapshot(player, "Reloaded stages from this world's stages.json.", false);
    }

    private static void handleExportPack(ServerPlayer player) throws Exception {
        Path path = StageDefinitionStorage.exportPackDefaults(player.level().getServer());
        sendSnapshot(player, "Exported modpack defaults to " + path + ".", false);
    }

    /**
     * Recipes intentionally stay server-side. The picker asks for recipes only after
     * the admin selects a result item, which keeps the payload small and includes
     * recipes that are actually loaded by the current server/modpack.
     */
    private static void handleRecipeCatalog(ServerPlayer player, JsonObject root) {
        Identifier itemId = Identifier.parse(requiredString(root, "item"));
        if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
            throw new IllegalArgumentException("Unknown item id: " + itemId);
        }
        Item wanted = BuiltInRegistries.ITEM.getValue(itemId);
        List<StageEditorCatalog.Entry> matches = new ArrayList<>();

        for (RecipeHolder<?> holder : player.level().getServer().getRecipeManager().getRecipes()) {
            try {
                boolean producesItem = false;
                for (RecipeDisplay display : holder.value().display()) {
                    ItemStack result = display.result().resolveForFirstStack(SlotDisplayContext.fromLevel(player.level()));
                    if (!result.isEmpty() && result.getItem() == wanted) {
                        producesItem = true;
                        break;
                    }
                }
                if (!producesItem) {
                    continue;
                }

                Identifier serializerId = BuiltInRegistries.RECIPE_SERIALIZER.getKey(holder.value().getSerializer());
                matches.add(new StageEditorCatalog.Entry(
                        holder.id().identifier().toString(),
                        serializerId == null ? "" : serializerId.toString()
                ));
            } catch (RuntimeException exception) {
                // A third-party recipe with an unusual display must never break the admin editor.
                SimpleStages.LOGGER.debug("Could not resolve display output for recipe {}", holder.id(), exception);
            }
        }

        matches.sort(Comparator.comparing(StageEditorCatalog.Entry::id));
        sendCatalog(player, new StageEditorCatalog(
                "recipes",
                itemId.toString(),
                matches,
                matches.isEmpty() ? "No loaded recipes produce this item." : "",
                false
        ));
    }

    private static void handleRegistryCatalog(ServerPlayer player, JsonObject root) {
        String catalog = requiredString(root, "catalog");
        List<StageEditorCatalog.Entry> entries;

        switch (catalog) {
            case "dimensions" -> entries = player.level().getServer().levelKeys().stream()
                    .map(key -> new StageEditorCatalog.Entry(key.identifier().toString(), ""))
                    .sorted(Comparator.comparing(StageEditorCatalog.Entry::id))
                    .toList();
            case "structures" -> entries = player.level().getServer().registryAccess()
                    .lookupOrThrow(Registries.STRUCTURE)
                    .listElementIds()
                    .map(key -> new StageEditorCatalog.Entry(key.identifier().toString(), ""))
                    .sorted(Comparator.comparing(StageEditorCatalog.Entry::id))
                    .toList();
            default -> throw new IllegalArgumentException("Unknown picker catalog: " + catalog);
        }

        sendCatalog(player, new StageEditorCatalog(catalog, "", entries, "", false));
    }

    private static void validateRegistryReferences(MinecraftServer server, StageDefinition definition) {
        for (Identifier recipeId : definition.recipes()) {
            if (server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, recipeId)).isEmpty()) {
                throw new IllegalArgumentException("Unknown recipe id: " + recipeId);
            }
        }
        for (Identifier itemId : definition.items()) {
            if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                throw new IllegalArgumentException("Unknown item id: " + itemId);
            }
        }
        for (BlockMaskEntry blockEntry : definition.blocks()) {
            if (!BuiltInRegistries.BLOCK.containsKey(blockEntry.block())) {
                throw new IllegalArgumentException("Unknown block id: " + blockEntry.block());
            }
            blockEntry.mask().ifPresent(mask -> {
                if (!BuiltInRegistries.BLOCK.containsKey(mask)) {
                    throw new IllegalArgumentException("Unknown block mask id: " + mask);
                }
            });
        }
        for (Identifier dimensionId : definition.dimensions()) {
            if (!server.levelKeys().contains(ResourceKey.create(Registries.DIMENSION, dimensionId))) {
                throw new IllegalArgumentException("Unknown dimension id: " + dimensionId);
            }
        }
        for (MobStageEntry mobEntry : definition.mobs()) {
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(mobEntry.mob())) {
                throw new IllegalArgumentException("Unknown mob id: " + mobEntry.mob());
            }
        }
        for (StructureStageEntry structureEntry : definition.structures()) {
            boolean exists = server.registryAccess().lookupOrThrow(Registries.STRUCTURE)
                    .listElementIds()
                    .anyMatch(key -> key.identifier().equals(structureEntry.structure()));
            if (!exists) {
                throw new IllegalArgumentException("Unknown structure id: " + structureEntry.structure());
            }
        }
    }

    private static void sendSnapshot(ServerPlayer player, String notice, boolean noticeError) {
        StageEditorSnapshot snapshot = StageEditorSnapshot.fromServer(notice, noticeError);
        PacketDistributor.sendToPlayer(player, StageEditorPayload.snapshot(snapshot.toJson()));
    }

    private static void sendCatalog(ServerPlayer player, StageEditorCatalog catalog) {
        PacketDistributor.sendToPlayer(player, StageEditorPayload.catalog(catalog.toJson()));
    }

    private static String requiredString(JsonObject root, String key) {
        if (!root.has(key) || !root.get(key).isJsonPrimitive()) {
            throw new IllegalArgumentException("Missing '" + key + "'");
        }
        String value = root.get(key).getAsString();
        if (value.isBlank()) {
            throw new IllegalArgumentException("'" + key + "' cannot be empty");
        }
        return value;
    }

    private static String optionalString(JsonObject root, String key) {
        return root.has(key) && root.get(key).isJsonPrimitive() ? root.get(key).getAsString() : "";
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
