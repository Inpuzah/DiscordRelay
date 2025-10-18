package com.inpuzah.discordRelay.discord;

import com.inpuzah.discordRelay.image.AdvancementCard;
import com.inpuzah.discordRelay.util.Text;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DiscordBridge {
    private final Plugin plugin;
    private final FileConfiguration cfg;
    private JDA jda;

    private final String token;
    private final String chatChannelId;
    private final String consoleChannelId;
    private final String eventsChannelId;
    private final String chatWebhookUrl;

    public DiscordBridge(Plugin plugin, FileConfiguration cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.token = cfg.getString("discord.token", "");
        this.chatChannelId = cfg.getString("discord.chat_channel_id", "");
        this.consoleChannelId = cfg.getString("discord.console_channel_id", "");
        this.eventsChannelId = cfg.getString("discord.events_channel_id", "");
        this.chatWebhookUrl = cfg.getString("discord.chat_webhook_url", "");
    }

    public void start() throws Exception {
        if (token == null || token.isEmpty()) throw new IllegalStateException("Discord token missing in config.yml");

        jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(new DiscordMessageListener(this, chatChannelId, consoleChannelId, plugin))
                .build();
        jda.awaitReady();
        applyPresenceFromConfig();

        if (cfg.getBoolean("dynamic_presence.enabled", false)) {
            long secs = Math.max(30, cfg.getInt("dynamic_presence.interval_seconds", 60));
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                int players = Bukkit.getOnlinePlayers().size();
                String pat = cfg.getString("dynamic_presence.pattern", "Playing with {players} online");
                jda.getPresence().setActivity(Activity.playing(pat.replace("{players}", String.valueOf(players))));
            }, 20L, secs * 20L);
        }
    }

    public void shutdown() {
        if (jda != null) {
            try {
                // Graceful shutdown with timeout
                jda.shutdown();
                if (!jda.awaitShutdown(10, TimeUnit.SECONDS)) {
                    jda.shutdownNow();
                    jda.awaitShutdown(5, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                jda.shutdownNow();
            }
        }
    }

    private void applyPresenceFromConfig() {
        if (!cfg.getBoolean("presence.enabled", true)) return;
        String s = cfg.getString("presence.status", "ONLINE").toUpperCase();
        OnlineStatus st = switch (s) {
            case "IDLE" -> OnlineStatus.IDLE;
            case "DO_NOT_DISTURB", "DND" -> OnlineStatus.DO_NOT_DISTURB;
            case "INVISIBLE" -> OnlineStatus.INVISIBLE;
            default -> OnlineStatus.ONLINE;
        };
        String t = cfg.getString("presence.activity.type", "PLAYING").toUpperCase();
        String txt = cfg.getString("presence.activity.text", "");
        String url = cfg.getString("presence.activity.url", "");
        Activity a = switch (t) {
            case "WATCHING" -> Activity.watching(txt);
            case "LISTENING" -> Activity.listening(txt);
            case "COMPETING" -> Activity.competing(txt);
            case "STREAMING" -> url.isBlank() ? Activity.playing(txt) : Activity.streaming(txt, url);
            default -> Activity.playing(txt);
        };
        jda.getPresence().setStatus(st);
        jda.getPresence().setActivity(a);
    }

    private TextChannel channel(String id) {
        if (id == null || id.isEmpty() || jda == null) return null;
        return jda.getTextChannelById(id);
    }

    public boolean sanitizeMentions() { return cfg.getBoolean("options.sanitize_mentions", true); }
    public int maxIngameLen() { return cfg.getInt("options.max_ingame_message_length", 256); }
    public String bedrockTag() { return cfg.getString("floodgate.bedrock_tag", ""); }

    // ===== Discord -> MC
    public void onDiscordChat(String authorTag, String content) {
        String msg = sanitizeMentions() ? Text.sanitizeDiscordMentions(content) : content;
        Bukkit.getScheduler().runTask(plugin, () -> {
            String prefix = Text.toLegacy(cfg.getString("formatting.from_discord_prefix", "&b[DC]&r "));
            Bukkit.broadcastMessage(prefix + authorTag + ": " + msg);
        });
    }

    // ===== MC -> Discord chat
    public void sendChatMessage(String playerName, String bedrockTag, String message, String skinUrl) {
        if (chatWebhookUrl != null && !chatWebhookUrl.isEmpty()) {
            WebhookPoster.post(chatWebhookUrl, bedrockTag + playerName, skinUrl, Text.stripColor(message));
            return;
        }
        var ch = channel(chatChannelId);
        if (ch != null) ch.sendMessage("**" + bedrockTag + playerName + "**: " + Text.stripColor(message)).queue();
    }

    // ===== Events (embeds with attached avatar/render)
    private String crafatarHead(UUID uuid) { return "https://crafatar.com/avatars/" + uuid + "?overlay"; }
    private String crafatarBody(UUID uuid) { return "https://crafatar.com/renders/body/" + uuid + "?overlay"; }
    private String mcHeadsHead(UUID uuid) { return "https://mc-heads.net/avatar/" + uuid + "/180"; }
    private String minotarHead(UUID uuid) { return "https://minotar.net/avatar/" + uuid + "/180"; }

    private byte[] httpGetBytes(String url) {
        try {
            var con = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            con.setConnectTimeout(5000); con.setReadTimeout(7000);
            con.addRequestProperty("User-Agent", "DiscordRelay/0.1 (+mc)");
            try (var in = con.getInputStream()) { return in.readAllBytes(); }
        } catch (Exception e) { return null; }
    }

    private byte[] fetchAvatarBytes(UUID uuid, boolean body) {
        String[] tries = body
                ? new String[]{ crafatarBody(uuid) }
                : new String[]{ crafatarHead(uuid), mcHeadsHead(uuid), minotarHead(uuid) };
        for (String u : tries) {
            byte[] b = httpGetBytes(u);
            if (b != null && b.length > 0) return b;
        }
        return null;
    }

    private void postEventEmbed(String description, UUID uuid, int color) {
        var ch = channel(eventsChannelId);
        if (ch == null) return;

        if (!cfg.getBoolean("events.embeds", true)) { ch.sendMessage(description).queue(); return; }

        var eb = new EmbedBuilder().setDescription(description).setColor(color).setTimestamp(Instant.now());

        boolean body = "body".equalsIgnoreCase(cfg.getString("events.render", "head"));
        byte[] img = fetchAvatarBytes(uuid, body);
        if (img != null) {
            String name = body ? "render.png" : "avatar.png";
            if (body) eb.setImage("attachment://" + name); else eb.setThumbnail("attachment://" + name);
            ch.sendMessageEmbeds(eb.build())
                    .addFiles(FileUpload.fromData(img, name))
                    .queue();
        } else {
            ch.sendMessageEmbeds(eb.build()).queue();
        }
    }

    public void sendJoin(String name, UUID uuid, boolean first) {
        String fmt = cfg.getString(first ? "formatting.first_join" : "formatting.join", "**{name}** joined");
        postEventEmbed(fmt.replace("{name}", name), uuid, 0x57F287);
    }

    public void sendQuit(String name, UUID uuid) {
        String fmt = cfg.getString("formatting.quit", "**{name}** left");
        postEventEmbed(fmt.replace("{name}", name), uuid, 0xED4245);
    }

    // ===== Advancement banner using mcasset toast + avatar
    private String toastUrlForFrame(String frame) {
        String ver = cfg.getString("assets.version", "1.21.5");
        String base = "https://mcasset.cloud/" + ver + "/assets/minecraft/textures/gui/sprites/advancements/toast/";
        String file = switch (String.valueOf(frame).toUpperCase()) {
            case "GOAL" -> "goal.png";
            case "CHALLENGE" -> "challenge.png";
            default -> "task.png";
        };
        return base + file;
    }

    public void sendAdvancement(String name, UUID uuid, String advName) {
        if (!cfg.getBoolean("events.advancement_card", true)) {
            String fmt = cfg.getString("formatting.advancement", "**{name}** made the advancement **{advancement}**");
            postEventEmbed(fmt.replace("{name}", name).replace("{advancement}", advName), uuid, 0xFEE75C);
        } else {
            sendAdvancementCard(uuid, name, advName, "TASK");
        }
    }

    public void sendAdvancementCard(UUID uuid, String playerName, String advTitle, String frame) {
        var ch = channel(eventsChannelId);
        if (ch == null) return;

        boolean body = "body".equalsIgnoreCase(cfg.getString("assets.render", "head"));
        byte[] toast = httpGetBytes(toastUrlForFrame(frame));
        byte[] avatar = fetchAvatarBytes(uuid, body);

        byte[] png = AdvancementCard.renderMcToast(advTitle, playerName, toast, avatar);

        var eb = new EmbedBuilder()
                .setColor(0xFEE75C)
                .setDescription("**" + playerName + "** made the advancement **" + advTitle + "**")
                .setImage("attachment://advancement.png")
                .setTimestamp(Instant.now())
                .build();

        ch.sendMessageEmbeds(eb)
                .addFiles(FileUpload.fromData(png, "advancement.png"))
                .queue();
    }

    // ===== Console relay
    public void sendConsoleLine(String line) {
        var ch = channel(consoleChannelId);
        if (ch != null) ch.sendMessage("```\n" + Text.trimForDiscordCode(line) + "\n```").queue();
    }
}