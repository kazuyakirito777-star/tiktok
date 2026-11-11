package com.tiktokchat;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/** Menggambar popup chat TikTok di layar (HUD). */
public class ChatOverlay {
    private final ChatConfig config;
    private final TikTokBridge bridge;
    private final List<LiveMessage> visible = new ArrayList<>();
    private long lastAccepted = 0;

    public ChatOverlay(ChatConfig config, TikTokBridge bridge) {
        this.config = config;
        this.bridge = bridge;
    }

    /** Dipanggil tiap client tick: ambil pesan baru dari antrean. */
    public void tick() {
        long now = System.currentTimeMillis();
        long life = config.popupDuration * 1000L;
        visible.removeIf(m -> now - m.createdAt > life);

        if (!config.enableNotifications) {
            while (bridge.poll() != null) { /* buang */ }
            return;
        }

        while (visible.size() < config.maxQueue
                && now - lastAccepted >= config.delayBetweenChats * 1000L) {
            LiveMessage m = bridge.poll();
            if (m == null) break;
            visible.add(new LiveMessage(m.type, m.user, m.text));
            lastAccepted = now;

            if (config.mirrorToMinecraftChat && Minecraft.getInstance().gui != null) {
                Minecraft.getInstance().gui.getChat().addMessage(
                        net.minecraft.network.chat.Component.literal(
                                "[TikTok] " + m.user + ": " + m.text));
            }
        }
    }

    public void render(GuiGraphics g, DeltaTracker delta) {
        if (visible.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        Font font = mc.font;

        int sw = g.guiWidth();
        int sh = g.guiHeight();
        final int pad = 4;
        final int maxWidth = Math.min(220, sw / 2);
        final int lineH = font.lineHeight;

        int y;
        boolean bottom = config.position >= 2;
        boolean right = config.position == 1 || config.position == 3;
        y = bottom ? sh - config.offsetY : config.offsetY;

        List<LiveMessage> copy = new ArrayList<>(visible);
        long now = System.currentTimeMillis();
        long life = config.popupDuration * 1000L;

        for (LiveMessage m : copy) {
            String head = m.user == null || m.user.isEmpty() ? "" : m.user + ": ";
            String full = head + m.text;
            List<net.minecraft.util.FormattedCharSequence> lines =
                    font.split(net.minecraft.network.chat.Component.literal(full), maxWidth - pad * 2);
            int boxH = lines.size() * (lineH + 1) + pad * 2;
            int boxW = maxWidth;
            int x = right ? sw - boxW - config.offsetX : config.offsetX;
            int top = bottom ? y - boxH : y;

            // fade-out di 1 detik terakhir
            long left = life - (now - m.createdAt);
            float alpha = left < 1000 ? Math.max(0f, left / 1000f) : 1f;
            int a = (int) (alpha * 170);
            int bg = (a << 24) | 0x101018;
            int accent = (((int) (alpha * 255)) << 24) | colorFor(m.type);

            g.fill(x, top, x + boxW, top + boxH, bg);
            g.fill(x, top, x + 2, top + boxH, accent);

            int ty = top + pad;
            for (net.minecraft.util.FormattedCharSequence line : lines) {
                int textColor = (((int) (alpha * 255)) << 24) | 0xFFFFFF;
                if (alpha > 0.05f) g.drawString(font, line, x + pad + 2, ty, textColor);
                ty += lineH + 1;
            }
            y = bottom ? top - 3 : top + boxH + 3;
        }
    }

    private static int colorFor(LiveMessage.Type t) {
        return switch (t) {
            case COMMENT -> 0x25F4EE; // cyan TikTok
            case GIFT -> 0xFE2C55;    // merah TikTok
            case LIKE -> 0xFFB703;
            case JOIN -> 0x80ED99;
            case SYSTEM -> 0xAAAAAA;
        };
    }
}
