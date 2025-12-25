package pl.anacode.antylogout.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import pl.anacode.antylogout.AnacodeAntylogout;

public class MessageUtil {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    public static void sendMessage(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) return;

        String prefix = AnacodeAntylogout.getInstance().getConfig()
            .getString("messages.prefix", "&8[&4ANACODE&8] &7");

        Component component = SERIALIZER.deserialize(prefix + message);
        player.sendMessage(component);
    }

    public static void sendRawMessage(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) return;

        Component component = SERIALIZER.deserialize(message);
        player.sendMessage(component);
    }

    public static Component colorize(String message) {
        return SERIALIZER.deserialize(message);
    }
}
