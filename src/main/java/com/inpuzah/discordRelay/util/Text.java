package com.inpuzah.discordRelay.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class Text {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    public static String plain(Component c) { return PLAIN.serialize(c); }

    public static String stripColor(String s) { return s.replaceAll("§[0-9A-FK-ORa-fk-or]", ""); }

    public static String sanitizeDiscordMentions(String s) { return s.replaceAll("@(everyone|here)", "@\u200B$1"); }

    public static String trimForDiscordCode(String s) { return s.length() > 1900 ? s.substring(0, 1900) + "…" : s; }

    public static String toLegacy(String amp) { return amp.replace('&', '§'); }
}
