package com.github.nutt1101.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;


import java.util.Map;

/** Shared Adventure rendering for chat, items and menus, with literal dynamic values. */
public final class MessageText {
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final java.util.regex.Pattern LEGACY = java.util.regex.Pattern.compile(
            "(?i)&(#[0-9a-f]{6}|x(?:&[0-9a-f]){6}|[0-9a-fk-or])");
    private static final String[] LEGACY_COLORS = {"black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
            "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"};
    private MessageText() {}

    public static String plain(String template) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(render(template, Map.of()));
    }

    public static Component render(String template, Map<String, String> values) {
        return render(template, values, Map.of());
    }

    public static Component render(String template, Map<String, String> values, Map<String, Component> components) {
        String formatted = LEGACY.matcher(template.replace('§', '&')).replaceAll(match -> {
            String code = match.group(1).toLowerCase(java.util.Locale.ROOT);
            String tag;
            if (code.startsWith("#")) tag = code;
            else if (code.startsWith("x")) tag = "#" + code.substring(1).replace("&", "");
            else {
                int color = "0123456789abcdef".indexOf(code);
                tag = color >= 0 ? LEGACY_COLORS[color] : switch (code) {
                    case "k" -> "obfuscated"; case "l" -> "bold"; case "m" -> "strikethrough";
                    case "n" -> "underlined"; case "o" -> "italic"; default -> "reset";
                };
            }
            // Legacy color changes reset decorations; MiniMessage alone keeps its native semantics.
            boolean color = code.startsWith("#") || code.startsWith("x") || "0123456789abcdef".contains(code);
            return java.util.regex.Matcher.quoteReplacement((color ? "<reset>" : "") + "<" + tag + ">");
        });
        TagResolver.Builder resolver = TagResolver.builder();
        for (var value : values.entrySet()) {
            formatted = formatted.replace("{" + value.getKey().toUpperCase(java.util.Locale.ROOT) + "}", "<" + value.getKey() + ">");
            resolver.resolver(Placeholder.unparsed(value.getKey(), value.getValue()));
        }
        for (var value : components.entrySet()) {
            formatted = formatted.replace("{" + value.getKey().toUpperCase(java.util.Locale.ROOT) + "}", "<" + value.getKey() + ">");
            resolver.resolver(Placeholder.component(value.getKey(), value.getValue()));
        }
        return MINI.deserialize(formatted, resolver.build()).decoration(TextDecoration.ITALIC, false);
    }
}
