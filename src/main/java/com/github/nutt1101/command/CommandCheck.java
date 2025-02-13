package com.github.nutt1101.command;

import com.github.nutt1101.ConfigSetting;
import com.github.nutt1101.utils.TranslationFileReader;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

public class CommandCheck {
    private static List<String> argmumentList = Arrays.asList("reload", "list", "add", "remove", "give");

    public static Boolean check(CommandSender sender, Command command, String label, String[] args) {

        // Check if player has permission
        if (!(sender.hasPermission("catchball.op"))) {
            // Check if player has specific command permission
            if (args.length > 0 && !sender.hasPermission("catchball.command." + args[0])) {
                sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.noPermission, "", ""));
                return false;
            }
        }

        // Check if commandSender does not exist argument
        if (args.length == 0) {
            sender.sendMessage(ConfigSetting.toChat(TranslationFileReader.argDoesNotExist, "", ""));
            return false;
        }

        return true;
    }

    public static List<String> getCommandArgument() { return argmumentList; };
}
