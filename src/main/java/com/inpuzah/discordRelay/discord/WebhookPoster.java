package com.inpuzah.discordRelay.discord;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WebhookPoster {
    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    public static void post(String webhookUrl, String username, String avatarUrl, String content) {
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.addRequestProperty("Content-Type", "application/json");

            String body = "{"
                    + "\"username\":\"" + esc(username) + "\","
                    + "\"avatar_url\":\"" + esc(avatarUrl) + "\","
                    + "\"content\":\"" + esc(content) + "\""
                    + "}";

            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = con.getOutputStream()) { os.write(bytes); }
            if (con.getResponseCode() >= 400) {
                try { con.getErrorStream().close(); } catch (Exception ignored) {}
            } else {
                try { con.getInputStream().close(); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }
}
