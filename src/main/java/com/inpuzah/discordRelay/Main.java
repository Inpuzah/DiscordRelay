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

        // Listeners
        getServer().getPluginManager().registerEvents(new ChatListener(this, discord), this);
        getServer().getPluginManager().registerEvents(new JoinQuitListener(this, discord), this);
        getServer().getPluginManager().registerEvents(new AdvancementListener(this, discord), this);

        // Attach console appender (no double start)
        if (cfg.getBoolean("options.relay_console", true)) {
            consoleAppender = new ConsoleRelayAppender("DiscordRelayConsole", discord::sendConsoleLine);
            consoleAppender.attach(); // start + add to root + update
        }

        getLogger().info("DiscordRelay enabled.");
    }

    public DiscordBridge getDiscord() {
        return discord;
    }

    @Override
    public void onDisable() {
        // Use Bukkit JUL logger here (won’t route through our Log4j appender)
        getLogger().info("Shutting down DiscordRelay...");

        // 1) Detach appender so Log4j stops referencing it
        if (consoleAppender != null) {
            try {
                consoleAppender.detach();
            } catch (Exception ignored) {}
            // 2) Then stop it
            try {
                consoleAppender.stop();
                getLogger().info("Console appender stopped.");
            } catch (Exception e) {
                getLogger().warning("Error stopping console appender: " + e.getMessage());
            }
        }

        // 3) Close Discord connection last
        if (discord != null) {
            try {
                discord.shutdown();
                getLogger().info("Discord connection closed gracefully.");
            } catch (Exception e) {
                getLogger().warning("Error shutting down Discord: " + e.getMessage());
            }
        }

        getLogger().info("DiscordRelay disabled.");
    }
}
