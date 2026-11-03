package com.github.nutt1101.utils;


import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MessageTextTest {
    @Test void keepsMiniMessageRgbAndTreatsDynamicInputLiterally() {
        var component = MessageText.render("<color:#123456>Hello <query>", Map.of("query", "<red>input</red>"));
        assertEquals("Hello <red>input</red>", PlainTextComponentSerializer.plainText().serialize(component));
        assertTrue(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(component).contains("#123456"));
    }

    @Test void supportsExistingLocaleColorsAndPlaceholders() {
        var component = MessageText.render("&aPage {PAGE}", Map.of("page", "2"));
        assertEquals("Page 2", PlainTextComponentSerializer.plainText().serialize(component));
        assertTrue(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(component).contains("green"));
    }
    @Test void mixesLegacyCodesWithMiniMessageAndPreservesRgbComponents() {
        var ball = MessageText.render("<gradient:#123456:#654321>Catch ball</gradient>", Map.of());
        var rendered = MessageText.render("&aGiven <ball> to {PLAYER}", Map.of("player", "<red>Eve"), Map.of("ball", ball));
        assertEquals("Given Catch ball to <red>Eve", PlainTextComponentSerializer.plainText().serialize(rendered));
        String serialized = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(rendered);
        assertTrue(serialized.contains("#123456"));
        assertFalse(PlainTextComponentSerializer.plainText().serialize(rendered).contains("&a"));
    }

    @Test void acceptsBothLegacyHexFormats() {
        assertEquals("RGB", MessageText.plain("&#123456R&x&6&5&4&3&2&1GB"));
    }
    @Test void rendersMessagesWithoutAnEntityPlaceholder() {
        String previous = TranslationFileReader.catchBallName;
        try {
            TranslationFileReader.catchBallName = "<green>Catch ball";
            assertEquals("Use Catch ball", PlainTextComponentSerializer.plainText().serialize(
                    com.github.nutt1101.ConfigSetting.toChat("Use {BALL}", "", "")));
        } finally {
            TranslationFileReader.catchBallName = previous;
        }
    }
}
