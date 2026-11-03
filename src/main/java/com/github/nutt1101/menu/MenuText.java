package com.github.nutt1101.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Map;

/** MiniMessage rendering with literal dynamic values and compatibility for old GUI color codes. */
public final class MenuText {
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private MenuText() {}

    public static Component render(String template, Map<String, String> values) {
        String formatted = template.replace('§', '&');
        // Existing language files use legacy codes and {UPPER_CASE} placeholders.
        if (formatted.matches("(?s).*&[0-9a-fk-orA-FK-OR].*") && !formatted.contains("<")) {
            formatted = MINI.serialize(LEGACY.deserialize(formatted));
        }
        TagResolver.Builder resolver = TagResolver.builder();
        for (var value : values.entrySet()) {
            formatted = formatted.replace("{" + value.getKey().toUpperCase(java.util.Locale.ROOT) + "}", "<" + value.getKey() + ">");
            resolver.resolver(Placeholder.unparsed(value.getKey(), value.getValue()));
        }
        return MINI.deserialize(formatted, resolver.build()).decoration(TextDecoration.ITALIC, false);
    }
}
