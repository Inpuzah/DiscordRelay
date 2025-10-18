package com.inpuzah.discordRelay.listeners;

import com.inpuzah.discordRelay.discord.DiscordBridge;
import com.inpuzah.discordRelay.util.Text;
import io.papermc.paper.advancement.AdvancementDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.plugin.Plugin;

public class AdvancementListener implements Listener {
    private final DiscordBridge bridge;

    public AdvancementListener(Plugin plugin, DiscordBridge bridge) {
        this.bridge = bridge;
    }

    @EventHandler
    public void onAdv(PlayerAdvancementDoneEvent e) {
        AdvancementDisplay display = e.getAdvancement().getDisplay();
        if (display == null) return;

        String title = Text.plain(display.title());

        String frame = "TASK";
        try {
            Object f = AdvancementDisplay.class.getMethod("frame").invoke(display);
            frame = String.valueOf(f);
        } catch (Throwable ignored) {}

        bridge.sendAdvancementCard(
                e.getPlayer().getUniqueId(),
                e.getPlayer().getName(),
                title,
                frame
        );
    }
}
