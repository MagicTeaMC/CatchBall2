package com.github.nutt1101.menu;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MenuTextTest {
    @Test void keepsMiniMessageRgbAndTreatsDynamicInputLiterally() {
        var component = MenuText.render("<color:#123456>Hello <query>", Map.of("query", "<red>input</red>"));
        assertEquals("Hello <red>input</red>", PlainTextComponentSerializer.plainText().serialize(component));
        assertTrue(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(component).contains("#123456"));
    }

    @Test void supportsExistingLocaleColorsAndPlaceholders() {
        var component = MenuText.render("&aPage {PAGE}", Map.of("page", "2"));
        assertEquals("Page 2", PlainTextComponentSerializer.plainText().serialize(component));
        assertTrue(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(component).contains("green"));
    }
}
