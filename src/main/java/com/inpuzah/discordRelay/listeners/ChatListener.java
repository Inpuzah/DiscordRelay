package com.inpuzah.discordRelay.listeners;

import com.inpuzah.discordRelay.discord.DiscordBridge;
import com.inpuzah.discordRelay.util.FloodgateUtil;
import com.inpuzah.discordRelay.util.Text;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class ChatListener implements Listener {
    private final Plugin plugin;
    private final DiscordBridge bridge;

    public ChatListener(Plugin plugin, DiscordBridge bridge) {
        this.plugin = plugin;
        this.bridge = bridge;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        // Check if event is cancelled (includes mutes from other plugins)
        if (e.isCancelled()) {
            return;
        }

        Player p = e.getPlayer();

        // Additional permission check for muted players
        // This works with plugins like EssentialsX that remove chat permission when muted
        if (!p.hasPermission("minecraft.command.me") || p.hasMetadata("muted")) {
            return;
        }

        String name = p.getName();
        String bedrockTag = FloodgateUtil.bedrockTagIfPresent(p, bridge.bedrockTag());
        String msg = Text.plain(e.message());

        int max = bridge.maxIngameLen();
        if (msg.length() > max) msg = msg.substring(0, max) + "…";

        String skin = "https://crafatar.com/avatars/" + p.getUniqueId() + "?overlay";
        bridge.sendChatMessage(name, bedrockTag, msg, skin);
    }
}