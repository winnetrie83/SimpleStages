package be.winnetrie.mod.simplestages.stage;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.List;

public class StageCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("stage")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))

                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("stage", StringArgumentType.word())
                                                .executes(context -> addStage(context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        StringArgumentType.getString(context, "stage")))
                                        )
                                )
                        )

                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("stage", StringArgumentType.word())
                                                .executes(context -> removeStage(context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        StringArgumentType.getString(context, "stage")))
                                        )
                                )
                        )

                        .then(Commands.literal("check")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("stage", StringArgumentType.word())
                                                .executes(context -> checkStage(context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        StringArgumentType.getString(context, "stage")))
                                        )
                                )
                        )

                        .then(Commands.literal("list")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> listStages(context.getSource(),
                                                EntityArgument.getPlayer(context, "player")))
                                )
                        )
        );
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
                () -> Component.literal(
                        player.getName().getString() + " stages: " + finalStages
                ),
                false
        );

        return 1;
    }
}