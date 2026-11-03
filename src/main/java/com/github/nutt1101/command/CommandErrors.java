package com.github.nutt1101.command;

import com.github.nutt1101.utils.MessageText;
import com.github.nutt1101.utils.TranslationFileReader;
import java.util.Map;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.exception.BukkitExceptionHandler;
import revxrsal.commands.bukkit.exception.InvalidPlayerException;
import revxrsal.commands.exception.*;
import revxrsal.commands.node.ParameterNode;

/** Keep configured translations for errors raised by Lamp's argument parser. */
public final class CommandErrors extends BukkitExceptionHandler {
    private void reply(BukkitCommandActor actor, String template) {
        actor.sender().sendMessage(MessageText.render(template, Map.of()));
    }

    @Override public void onNoPermission(NoPermissionException error, BukkitCommandActor actor) {
        reply(actor, TranslationFileReader.noPermission);
    }

    @Override public void onUnknownCommand(UnknownCommandException error, BukkitCommandActor actor) {
        reply(actor, TranslationFileReader.unknownCommandArgument);
    }

    @Override public void onMissingArgument(MissingArgumentException error, BukkitCommandActor actor,
                                            ParameterNode<BukkitCommandActor, ?> parameter) {
        reply(actor, TranslationFileReader.argDoesNotExist);
    }

    @Override public void onInvalidInteger(InvalidIntegerException error, BukkitCommandActor actor) {
        reply(actor, TranslationFileReader.invalidItemAmount);
    }

    @Override public void onNumberNotInRange(NumberNotInRangeException error, BukkitCommandActor actor,
                                            ParameterNode<BukkitCommandActor, Number> parameter) {
        reply(actor, TranslationFileReader.invalidItemAmount);
    }

    @Override public void onInvalidPlayer(InvalidPlayerException error, BukkitCommandActor actor) {
        actor.sender().sendMessage(MessageText.render(TranslationFileReader.unknownOrOfflinePlayer,
                Map.of("player", error.input())));
    }
}
