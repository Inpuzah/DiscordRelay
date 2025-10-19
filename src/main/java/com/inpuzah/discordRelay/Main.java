package com.inpuzah.discordRelay;

import com.inpuzah.discordRelay.discord.DiscordBridge;
import com.inpuzah.discordRelay.listeners.AdvancementListener;
import com.inpuzah.discordRelay.listeners.ChatListener;
import com.inpuzah.discordRelay.listeners.JoinQuitListener;
import com.inpuzah.discordRelay.logs.ConsoleRelayAppender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private DiscordBridge discord;
    private ConsoleRelayAppender consoleAppender;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration cfg = getConfig();

        try {
            discord = new DiscordBridge(this, cfg);
            discord.start();
        } catch (Exception e) {
            getLogger().severe("Failed to start Discord bot: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(new ChatListener(this, discord), this);
        getServer().getPluginManager().registerEvents(new JoinQuitListener(this, discord), this);
        getServer().getPluginManager().registerEvents(new AdvancementListener(this, discord), this);

        if (cfg.getBoolean("options.relay_console", true)) {
            consoleAppender = new ConsoleRelayAppender("DiscordRelayConsole", discord::sendConsoleLine);
            consoleAppender.attach(); // start + add + update
        }

        getLogger().info("DiscordRelay enabled.");
    }

    public DiscordBridge getDiscord() {
        return discord;
    }

    @Override
    public void onDisable() {
        // From here on, AVOID Bukkit/Paper logging once the appender is detached.
        System.out.println("[DiscordRelay] Shutting down...");

        if (consoleAppender != null) {
            try {
                // 1) Detach so Log4j forgets about our appender
                consoleAppender.detach();
                // 2) Mute consumer in case any late events slip in
                consoleAppender.mute();
            } catch (Throwable ignored) {}

            try {
                // 3) Now safe to stop; no appends, no exceptions
                consoleAppender.stop();
            } catch (Throwable ignored) {}
        }

        if (discord != null) {
            try {
                discord.shutdown();
            } catch (Throwable ignored) {}
        }

        // Final message direct to stdout to avoid Log4j entirely
        System.out.println("[DiscordRelay] Stopped.");
    }
}
