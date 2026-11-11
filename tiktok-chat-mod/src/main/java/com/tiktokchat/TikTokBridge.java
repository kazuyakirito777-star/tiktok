package com.tiktokchat;

import io.github.jwdeveloper.tiktok.TikTokLive;
import io.github.jwdeveloper.tiktok.live.LiveClient;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/** Menghubungkan mod ke TikTok Live dan memasukkan event ke antrean. */
public class TikTokBridge {
    private final ChatConfig config;
    private final ConcurrentLinkedDeque<LiveMessage> incoming = new ConcurrentLinkedDeque<>();
    private volatile LiveClient client;
    private volatile String status = "Belum tersambung";

    // untuk gabung like
    private final AtomicLong pendingLikes = new AtomicLong();
    private volatile long lastLikeFlush = System.currentTimeMillis();
    private volatile String lastLiker = "";

    public TikTokBridge(ChatConfig config) {
        this.config = config;
    }

    public String getStatus() {
        return status;
    }

    public boolean isConnected() {
        return client != null;
    }

    public LiveMessage poll() {
        return incoming.pollFirst();
    }

    public void connect(String username) {
        disconnect();
        final String user = username.startsWith("@") ? username.substring(1) : username;
        if (user.isBlank()) {
            status = "Username kosong";
            return;
        }
        status = "Menyambung ke @" + user + "...";
        // jalankan di thread sendiri supaya game tidak freeze
        Thread t = new Thread(() -> {
            try {
                client = TikTokLive.newClient(user)
                        .onConnected((c, e) -> {
                            status = "Tersambung ke live @" + user;
                            incoming.add(new LiveMessage(LiveMessage.Type.SYSTEM, "", status));
                        })
                        .onComment((c, e) -> {
                            incoming.add(new LiveMessage(
                                    LiveMessage.Type.COMMENT,
                                    e.getUser().getProfileName(),
                                    e.getText()));
                        })
                        .onGift((c, e) -> {
                            if (!config.showGifts) return;
                            incoming.add(new LiveMessage(
                                    LiveMessage.Type.GIFT,
                                    e.getUser().getProfileName(),
                                    "kirim " + e.getGift().getName()));
                        })
                        .onLike((c, e) -> {
                            if (!config.showLikes) return;
                            pendingLikes.addAndGet(e.getLikes());
                            lastLiker = e.getUser().getProfileName();
                            flushLikes(false);
                        })
                        .onJoin((c, e) -> {
                            if (!config.showJoins) return;
                            incoming.add(new LiveMessage(
                                    LiveMessage.Type.JOIN,
                                    e.getUser().getProfileName(),
                                    "bergabung"));
                        })
                        .onError((c, e) -> {
                            status = "Error: " + e.getException().getMessage();
                        })
                        .onDisconnected((c, e) -> {
                            status = "Terputus";
                            client = null;
                        })
                        .buildAndConnect();
            } catch (Throwable ex) {
                status = "Gagal: " + ex.getMessage();
                client = null;
                TikTokChatMod.LOGGER.error("Gagal konek TikTok Live", ex);
            }
        }, "TikTokLive-Connect");
        t.setDaemon(true);
        t.start();
    }

    /** Dipanggil tiap tick agar like yang menumpuk tetap keluar walau tidak ada like baru. */
    public void tick() {
        flushLikes(false);
    }

    private void flushLikes(boolean force) {
        long now = System.currentTimeMillis();
        long total = pendingLikes.get();
        boolean timeUp = now - lastLikeFlush >= config.mergeLikesSeconds * 1000L;
        if (total >= config.minLikesPerNotification || (force && total > 0) || (timeUp && total >= config.minLikesPerNotification)) {
            pendingLikes.addAndGet(-total);
            lastLikeFlush = now;
            incoming.add(new LiveMessage(LiveMessage.Type.LIKE, lastLiker, "+" + total + " like"));
        } else if (timeUp) {
            lastLikeFlush = now;
        }
    }

    public void disconnect() {
        LiveClient c = client;
        client = null;
        if (c != null) {
            try {
                c.disconnect();
            } catch (Throwable ignored) {
            }
        }
        status = "Belum tersambung";
    }
}
