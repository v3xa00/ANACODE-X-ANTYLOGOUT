package pl.anacode.antylogout.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import pl.anacode.antylogout.AnacodeAntylogout;

import java.time.Duration;

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

    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) return;

        Component component = SERIALIZER.deserialize(message);
        player.sendActionBar(component);
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) return;

        Component titleComponent = title != null ? SERIALIZER.deserialize(title) : Component.empty();
        Component subtitleComponent = subtitle != null ? SERIALIZER.deserialize(subtitle) : Component.empty();

        Title.Times times = Title.Times.times(
            Duration.ofMillis(fadeIn * 50L),
            Duration.ofMillis(stay * 50L),
            Duration.ofMillis(fadeOut * 50L)
        );

        Title titleObj = Title.title(titleComponent, subtitleComponent, times);
        player.showTitle(titleObj);
    }

    public static Component colorize(String message) {
        return SERIALIZER.deserialize(message);
    }
}
