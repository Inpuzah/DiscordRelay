package com.inpuzah.discordRelay.discord;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class DiscordMessageListener extends ListenerAdapter {
    private final DiscordBridge bridge;
    private final String chatId;
    private final String consoleId;
    private final Plugin plugin;

    public DiscordMessageListener(DiscordBridge bridge, String chatId, String consoleId, Plugin plugin) {
        this.bridge = bridge;
        this.chatId = chatId;
        this.consoleId = consoleId;
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getAuthor().isBot()) return;

        String channelId = event.getChannel().getId();
        String content = event.getMessage().getContentDisplay();

        // Handle chat channel messages
        if (chatId != null && !chatId.isEmpty() && channelId.equals(chatId)) {
            String author = event.getAuthor().getName();
            bridge.onDiscordChat(author, content);
            return;
        }

        // Handle console channel commands
        if (consoleId != null && !consoleId.isEmpty() && channelId.equals(consoleId)) {
            handleConsoleCommand(event, content);
        }
    }

    private void handleConsoleCommand(MessageReceivedEvent event, String command) {
        // Run on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                ConsoleCommandSender console = Bukkit.getConsoleSender();

                // Capture command output
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos, true);
                PrintStream oldOut = System.out;

                // Temporarily redirect output (this may not capture all output types)
                System.setOut(ps);

                // Execute command
                boolean success = Bukkit.dispatchCommand(console, command);

                // Restore output
                System.setOut(oldOut);

                // Get output
                String output = baos.toString().trim();

                // Send response back to Discord
                if (output.isEmpty()) {
                    output = success ? "Command executed successfully (no output)" : "Command failed or returned no output";
                }

                // Split long messages
                if (output.length() > 1900) {
                    int i = 0;
                    while (i < output.length()) {
                        int end = Math.min(i + 1900, output.length());
                        String chunk = output.substring(i, end);
                        event.getChannel().sendMessage("```\n" + chunk + "\n```").queue();
                        i = end;
                    }
                } else {
                    event.getChannel().sendMessage("```\n" + output + "\n```").queue();
                }

            } catch (Exception e) {
                event.getChannel().sendMessage("```\nError executing command: " + e.getMessage() + "\n```").queue();
            }
        });
    }
}