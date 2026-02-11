package pl.anacode.antylogout.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

public class ColorUtil {

    private static final LegacyComponentSerializer SERIALIZER = 
        LegacyComponentSerializer.legacyAmpersand();

    public static String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static Component colorizeComponent(String message) {
        if (message == null) return Component.empty();
        return SERIALIZER.deserialize(message);
    }
}
