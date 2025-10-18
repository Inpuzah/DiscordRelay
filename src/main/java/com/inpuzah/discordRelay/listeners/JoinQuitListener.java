package com.inpuzah.discordRelay.listeners;

import com.inpuzah.discordRelay.discord.DiscordBridge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class JoinQuitListener implements Listener {
    private final DiscordBridge bridge;

    public JoinQuitListener(Plugin plugin, DiscordBridge bridge) {
        this.bridge = bridge;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        boolean first = !e.getPlayer().hasPlayedBefore();
        bridge.sendJoin(e.getPlayer().getName(), e.getPlayer().getUniqueId(), first);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        bridge.sendQuit(e.getPlayer().getName(), e.getPlayer().getUniqueId());
    }
}
