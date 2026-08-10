package be.winnetrie.mod.simplestages.stage;

import be.winnetrie.mod.simplestages.network.StageEditorNetwork;
import be.winnetrie.mod.simplestages.stage.data.StageDefinition;
import be.winnetrie.mod.simplestages.stage.data.StageDefinitionManager;
import be.winnetrie.mod.simplestages.stage.data.StageDefinitionStorage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class StageCommands {
    public static final PermissionCheck STAGE_EDITOR_PERMISSION =
            new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    private StageCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("stage")
                        .requires(Commands.hasPermission(STAGE_EDITOR_PERMISSION))
                        .then(Commands.literal("editor")
                                .executes(context -> openEditor(context.getSource())))
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("stage", StringArgumentType.word())
                                                .executes(context -> addStage(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        StringArgumentType.getString(context, "stage")
                                                ))
                                        )
                                )
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("stage", StringArgumentType.word())
                                                .executes(context -> removeStage(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        StringArgumentType.getString(context, "stage")
                                                ))
                                        )
                                )
                        )
                        .then(Commands.literal("check")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("stage", StringArgumentType.word())
                                                .executes(context -> checkStage(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        StringArgumentType.getString(context, "stage")
                                                ))
                                        )
                                )
                        )
                        .then(Commands.literal("list")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> listStages(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player")
                                        ))
                                )
                        )
                        .then(Commands.literal("definitions")
                                .then(Commands.literal("list")
                                        .executes(context -> listDefinitions(context.getSource())))
                                .then(Commands.literal("reload")
                                        .executes(context -> reloadDefinitions(context.getSource())))
                                .then(Commands.literal("export-pack")
                                        .executes(context -> exportPackDefaults(context.getSource())))
                        )
        );
    }

    public static boolean canEdit(ServerPlayer player) {
        return player != null && STAGE_EDITOR_PERMISSION.check(player.permissions());
    }

    private static int openEditor(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            StageEditorNetwork.openEditor(player);
            return 1;
        } catch (Exception exception) {
            source.sendFailure(Component.literal("The stage editor can only be opened by an in-game OP/admin player."));
            return 0;
        }
    }

    private static int addStage(CommandSourceStack source, ServerPlayer player, String stage) {
        boolean added = StageManager.addStage(player, stage);
        source.sendSuccess(
                () -> Component.literal(
                        added
                                ? "Added stage '" + stage + "' to " + player.getName().getString()
                                : player.getName().getString() + " already has stage '" + stage + "'"
                ),
                true
        );
        return added ? 1 : 0;
    }

    private static int removeStage(CommandSourceStack source, ServerPlayer player, String stage) {
        boolean removed = StageManager.removeStage(player, stage);
        source.sendSuccess(
                () -> Component.literal(
                        removed
                                ? "Removed stage '" + stage + "' from " + player.getName().getString()
                                : player.getName().getString() + " does not have stage '" + stage + "'"
                ),
                true
        );
        return removed ? 1 : 0;
    }

    private static int checkStage(CommandSourceStack source, ServerPlayer player, String stage) {
        boolean hasStage = StageManager.hasStage(player, stage);
        source.sendSuccess(
                () -> Component.literal(
                        player.getName().getString()
                                + (hasStage ? " has stage '" : " does not have stage '")
                                + stage + "'"
                ),
                false
        );
        return hasStage ? 1 : 0;
    }

    private static int listStages(CommandSourceStack source, ServerPlayer player) {
        List<String> playerStages = StageManager.getStages(player);
        String stages = String.join(", ", playerStages);
        if (stages.isBlank()) {
            stages = "none";
        }
        String finalStages = stages;
        source.sendSuccess(
                () -> Component.literal(player.getName().getString() + " stages: " + finalStages),
                false
        );
        return 1;
    }

    private static int listDefinitions(CommandSourceStack source) {
        List<StageDefinition> definitions = StageDefinitionManager.getDefinitions();
        if (definitions.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No Simple Stages definitions are configured."), false);
            return 1;
        }

        String summary = definitions.stream()
                .map(definition -> definition.stage() + " (" + definition.displayNameOrStage() + ")")
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");

        source.sendSuccess(
                () -> Component.literal("Configured stage definitions: " + summary),
                false
        );
        return definitions.size();
    }

    private static int reloadDefinitions(CommandSourceStack source) {
        try {
            StageDefinitionStorage.reload(source.getServer());
            source.sendSuccess(
                    () -> Component.literal(
                            "Reloaded " + StageDefinitionManager.getDefinitions().size() + " Simple Stages definitions."
                    ),
                    true
            );
            return 1;
        } catch (Exception exception) {
            source.sendFailure(Component.literal("Could not reload Simple Stages definitions: " + exception.getMessage()));
            return 0;
        }
    }

    private static int exportPackDefaults(CommandSourceStack source) {
        try {
            Path path = StageDefinitionStorage.exportPackDefaults(source.getServer());
            source.sendSuccess(
                    () -> Component.literal("Exported current stages as modpack defaults to: " + path),
                    true
            );
            return 1;
        } catch (IOException exception) {
            source.sendFailure(Component.literal("Could not export Simple Stages modpack defaults: " + exception.getMessage()));
            return 0;
        }
    }
}
