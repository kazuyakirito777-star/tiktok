package com.tiktokchat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Pengaturan mod, disimpan di config/tiktok_live_chat.json */
public class ChatConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("tiktok_live_chat.json");

    public String username = "";
    public boolean autoConnect = true;
    public boolean enableNotifications = true;
    public boolean showGifts = true;
    public boolean showLikes = true;
    public boolean showJoins = false;
    /** Durasi popup (detik) */
    public int popupDuration = 10;
    /** Jeda antar chat yang ditampilkan (detik), mencegah spam */
    public int delayBetweenChats = 1;
    /** Maksimal popup tampil bersamaan */
    public int maxQueue = 5;
    /** Gabungkan like dalam beberapa detik */
    public int mergeLikesSeconds = 3;
    /** Minimal like per notifikasi */
    public int minLikesPerNotification = 100;
    /** Juga kirim chat ke chat Minecraft bawaan */
    public boolean mirrorToMinecraftChat = false;
    /** Posisi popup: 0 = kiri atas, 1 = kanan atas, 2 = kiri bawah, 3 = kanan bawah */
    public int position = 1;
    public int offsetX = 6;
    public int offsetY = 40;

    public static ChatConfig load() {
        try {
            if (Files.exists(FILE)) {
                try (Reader r = Files.newBufferedReader(FILE)) {
                    ChatConfig c = GSON.fromJson(r, ChatConfig.class);
                    if (c != null) return c;
                }
            }
        } catch (Exception e) {
            TikTokChatMod.LOGGER.warn("Gagal baca config, pakai default", e);
        }
        ChatConfig c = new ChatConfig();
        c.save();
        return c;
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE)) {
                GSON.toJson(this, w);
            }
        } catch (Exception e) {
            TikTokChatMod.LOGGER.warn("Gagal simpan config", e);
        }
    }
}
