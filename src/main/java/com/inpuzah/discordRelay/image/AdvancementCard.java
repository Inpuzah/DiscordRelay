package com.inpuzah.discordRelay.image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class AdvancementCard {

    public static byte[] renderMcToast(String title, String subtitle, byte[] toastPng, byte[] avatarPng) {
        System.setProperty("java.awt.headless", "true");
        int W = 900, H = 240;
        BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // background
        g.setPaint(new GradientPaint(0, 0, new Color(26,27,34), 0, H, new Color(18,19,24)));
        g.fillRect(0, 0, W, H);

        // card
        int pad = 16;
        Shape card = new RoundRectangle2D.Float(pad, pad, W - pad*2, H - pad*2, 24, 24);
        g.setColor(new Color(40,44,52));
        g.fill(card);

        // toast background (mcasset)
        if (toastPng != null && toastPng.length > 0) {
            try {
                BufferedImage toast = ImageIO.read(new ByteArrayInputStream(toastPng));
                int tw = W - pad*2 - 24;
                int th = H - pad*2 - 24;
                int tx = pad + 12, ty = pad + 12;
                Image scaled = toast.getScaledInstance(tw, th, Image.SCALE_DEFAULT);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2.drawImage(scaled, tx, ty, null);
                g2.dispose();
            } catch (Exception ignored) {}
        }

        // avatar/body circle
        if (avatarPng != null && avatarPng.length > 0) {
            try {
                BufferedImage av = ImageIO.read(new ByteArrayInputStream(avatarPng));
                int size = 170;
                int ax = pad + 26;
                int ay = (H - size) / 2;
                BufferedImage circle = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                Graphics2D cg = circle.createGraphics();
                cg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                cg.setClip(new Ellipse2D.Float(0, 0, size, size));
                cg.drawImage(av, 0, 0, size, size, null);
                cg.dispose();

                g.setColor(new Color(0,0,0,80));
                g.fillOval(ax-3, ay-3, size+6, size+6);
                g.drawImage(circle, ax, ay, null);
            } catch (Exception ignored) {}
        }

        // text
        int left = pad + 26 + 170 + 24;
        g.setColor(Color.white);
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        drawClipped(g, title, left, pad + 100, W - left - 24);

        g.setColor(new Color(230,230,230));
        g.setFont(new Font("SansSerif", Font.PLAIN, 22));
        drawClipped(g, subtitle, left, pad + 138, W - left - 24);

        g.dispose();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(out, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) { return new byte[0]; }
    }

    private static void drawClipped(Graphics2D g, String s, int x, int y, int maxW) {
        if (s == null) return;
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(s) <= maxW) { g.drawString(s, x, y); return; }
        String ell = "…"; int len = s.length();
        while (len > 0 && fm.stringWidth(s.substring(0, len) + ell) > maxW) len--;
        g.drawString((len <= 0 ? ell : s.substring(0, len) + ell), x, y);
    }
}
