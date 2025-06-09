package com.github.nutt1101.command;

import com.github.nutt1101.CatchBall;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.Plugin;

public class BrigadierCommandHandler {

    private final CatchBall plugin;
    private final Command commandExecutor;
    private final TabComplete tabCompleter;

    public BrigadierCommandHandler(CatchBall plugin) {
        this.plugin = plugin;
        this.commandExecutor = new Command();
        this.tabCompleter = new TabComplete();
    }

    public void registerCommands() {
        LifecycleEventManager<Plugin> manager = plugin.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();

            commands.register(
                    Commands.literal("ctb")
                            .executes(context -> {
                                return commandExecutor.onCommand(
                                        context.getSource().getSender(),
                                        null,
                                        "ctb",
                                        new String[0]
                                ) ? 1 : 0;
                            })
                            .then(buildReloadCommand())
                            .then(buildListCommand())
                            .then(buildAddCommand())
                            .then(buildRemoveCommand())
                            .then(buildGiveCommand())
                            .build(),
                    "CatchBall main command"
            );
        });
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<io.papermc.paper.command.brigadier.CommandSourceStack> buildReloadCommand() {
        return Commands.literal("reload")
                .executes(context -> {
                    return commandExecutor.onCommand(
                            context.getSource().getSender(),
                            null,
                            "ctb",
                            new String[]{"reload"}
                    ) ? 1 : 0;
                });
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<io.papermc.paper.command.brigadier.CommandSourceStack> buildListCommand() {
        return Commands.literal("list")
                .executes(context -> {
                    return commandExecutor.onCommand(
                            context.getSource().getSender(),
                            null,
                            "ctb",
                            new String[]{"list"}
                    ) ? 1 : 0;
                });
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<io.papermc.paper.command.brigadier.CommandSourceStack> buildAddCommand() {
        return Commands.literal("add")
                .then(Commands.argument("entity", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests((context, builder) -> {
                            return getTabCompletions(builder, new String[]{"add", ""}, context);
                        })
                        .executes(context -> {
                            String entity = context.getArgument("entity", String.class);
                            return commandExecutor.onCommand(
                                    context.getSource().getSender(),
                                    null,
                                    "ctb",
                                    new String[]{"add", entity}
                            ) ? 1 : 0;
                        }));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<io.papermc.paper.command.brigadier.CommandSourceStack> buildRemoveCommand() {
        return Commands.literal("remove")
                .then(Commands.argument("entity", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests((context, builder) -> {
                            return getTabCompletions(builder, new String[]{"remove", ""}, context);
                        })
                        .executes(context -> {
                            String entity = context.getArgument("entity", String.class);
                            return commandExecutor.onCommand(
                                    context.getSource().getSender(),
                                    null,
                                    "ctb",
                                    new String[]{"remove", entity}
                            ) ? 1 : 0;
                        }));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<io.papermc.paper.command.brigadier.CommandSourceStack> buildGiveCommand() {
        return Commands.literal("give")
                .then(Commands.argument("player", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests((context, builder) -> {
                            return getTabCompletions(builder, new String[]{"give", ""}, context);
                        })
                        .then(Commands.argument("item", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    String player = context.getArgument("player", String.class);
                                    return getTabCompletions(builder, new String[]{"give", player, ""}, context);
                                })
                                .executes(context -> {
                                    String player = context.getArgument("player", String.class);
                                    String item = context.getArgument("item", String.class);
                                    return commandExecutor.onCommand(
                                            context.getSource().getSender(),
                                            null,
                                            "ctb",
                                            new String[]{"give", player, item}
                                    ) ? 1 : 0;
                                })
                                .then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                        .suggests((context, builder) -> {
                                            String player = context.getArgument("player", String.class);
                                            String item = context.getArgument("item", String.class);
                                            return getTabCompletions(builder, new String[]{"give", player, item, ""}, context);
                                        })
                                        .executes(context -> {
                                            String player = context.getArgument("player", String.class);
                                            String item = context.getArgument("item", String.class);
                                            int amount = context.getArgument("amount", Integer.class);
                                            return commandExecutor.onCommand(
                                                    context.getSource().getSender(),
                                                    null,
                                                    "ctb",
                                                    new String[]{"give", player, item, String.valueOf(amount)}
                                            ) ? 1 : 0;
                                        }))));
    }

    private java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> getTabCompletions(
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder,
            String[] args,
            com.mojang.brigadier.context.CommandContext<io.papermc.paper.command.brigadier.CommandSourceStack> context) {

        try {
            java.util.List<String> completions = tabCompleter.onTabComplete(
                    context.getSource().getSender(),
                    null,
                    "ctb",
                    args
            );

            if (completions != null) {
                for (String completion : completions) {
                    if (completion != null && !completion.isEmpty()) {
                        builder.suggest(completion);
                    }
                }
            }
        } catch (Exception e) {
            // Fallback: provide basic completions
            provideFallbackCompletions(builder, args);
        }

        return builder.buildFuture();
    }

    private void provideFallbackCompletions(com.mojang.brigadier.suggestion.SuggestionsBuilder builder, String[] args) {
        if (args.length == 2) {
            if (args[0].equals("give")) {
                // Suggest online player names
                plugin.getServer().getOnlinePlayers().forEach(player ->
                        builder.suggest(player.getName()));
            } else if (args[0].equals("add")) {
                builder.suggest("ALL");
                builder.suggest("ZOMBIE");
                builder.suggest("SKELETON");
                builder.suggest("CREEPER");
            } else if (args[0].equals("remove")) {
                builder.suggest("ALL");
            }
        } else if (args.length == 3 && args[0].equals("give")) {
            builder.suggest("CatchBall");
            builder.suggest("DropItem");
        } else if (args.length == 4 && args[0].equals("give")) {
            for (int i = 1; i <= 9; i++) {
                builder.suggest(String.valueOf(i));
            }
        }
    }
}
