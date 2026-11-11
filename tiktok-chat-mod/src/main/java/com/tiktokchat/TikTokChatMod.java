package com.tiktokchat;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TikTokChatMod implements ClientModInitializer {
    public static final String MOD_ID = "tiktok_live_chat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ChatConfig CONFIG;
    public static TikTokBridge BRIDGE;
    public static ChatOverlay OVERLAY;

    private boolean autoConnectDone = false;

    @Override
    public void onInitializeClient() {
        CONFIG = ChatConfig.load();
        BRIDGE = new TikTokBridge(CONFIG);
        OVERLAY = new ChatOverlay(CONFIG, BRIDGE);

        // HUD: gambar di atas chat bawaan
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "live_chat"),
                (graphics, delta) -> OVERLAY.render(graphics, delta));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            // auto connect begitu masuk world
            if (!autoConnectDone && mc.level != null) {
                autoConnectDone = true;
                if (CONFIG.autoConnect && !CONFIG.username.isBlank()) {
                    BRIDGE.connect(CONFIG.username);
                }
            }
            if (mc.level == null) autoConnectDone = false;
            BRIDGE.tick();
            OVERLAY.tick();
        });

        registerCommands();
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("tiktok")
                    .then(ClientCommandManager.literal("connect")
                            .then(ClientCommandManager.argument("username", StringArgumentType.word())
                                    .executes(ctx -> {
                                        String u = StringArgumentType.getString(ctx, "username");
                                        CONFIG.username = u.startsWith("@") ? u.substring(1) : u;
                                        CONFIG.save();
                                        BRIDGE.connect(CONFIG.username);
                                        ctx.getSource().sendFeedback(Component.literal("[TikTok] Menyambung ke @" + CONFIG.username));
                                        return 1;
                                    }))
                            .executes(ctx -> {
                                BRIDGE.connect(CONFIG.username);
                                ctx.getSource().sendFeedback(Component.literal("[TikTok] Menyambung ke @" + CONFIG.username));
                                return 1;
                            }))
                    .then(ClientCommandManager.literal("disconnect")
                            .executes(ctx -> {
                                BRIDGE.disconnect();
                                ctx.getSource().sendFeedback(Component.literal("[TikTok] Diputus"));
                                return 1;
                            }))
                    .then(ClientCommandManager.literal("status")
                            .executes(ctx -> {
                                ctx.getSource().sendFeedback(Component.literal("[TikTok] " + BRIDGE.getStatus()));
                                return 1;
                            }))
                    .then(ClientCommandManager.literal("auto")
                            .then(ClientCommandManager.argument("on", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                    .executes(ctx -> {
                                        CONFIG.autoConnect = com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "on");
                                        CONFIG.save();
                                        ctx.getSource().sendFeedback(Component.literal("[TikTok] Auto connect: " + CONFIG.autoConnect));
                                        return 1;
                                    })))
                    .then(ClientCommandManager.literal("set")
                            .then(ClientCommandManager.literal("duration")
                                    .then(ClientCommandManager.argument("detik", IntegerArgumentType.integer(1, 60))
                                            .executes(ctx -> {
                                                CONFIG.popupDuration = IntegerArgumentType.getInteger(ctx, "detik");
                                                CONFIG.save();
                                                return 1;
                                            })))
                            .then(ClientCommandManager.literal("delay")
                                    .then(ClientCommandManager.argument("detik", IntegerArgumentType.integer(0, 30))
                                            .executes(ctx -> {
                                                CONFIG.delayBetweenChats = IntegerArgumentType.getInteger(ctx, "detik");
                                                CONFIG.save();
                                                return 1;
                                            })))
                            .then(ClientCommandManager.literal("maxqueue")
                                    .then(ClientCommandManager.argument("jumlah", IntegerArgumentType.integer(1, 15))
                                            .executes(ctx -> {
                                                CONFIG.maxQueue = IntegerArgumentType.getInteger(ctx, "jumlah");
                                                CONFIG.save();
                                                return 1;
                                            })))
                            .then(ClientCommandManager.literal("minlikes")
                                    .then(ClientCommandManager.argument("jumlah", IntegerArgumentType.integer(1, 100000))
                                            .executes(ctx -> {
                                                CONFIG.minLikesPerNotification = IntegerArgumentType.getInteger(ctx, "jumlah");
                                                CONFIG.save();
                                                return 1;
                                            })))
                            .then(ClientCommandManager.literal("position")
                                    .then(ClientCommandManager.argument("0-3", IntegerArgumentType.integer(0, 3))
                                            .executes(ctx -> {
                                                CONFIG.position = IntegerArgumentType.getInteger(ctx, "0-3");
                                                CONFIG.save();
                                                return 1;
                                            })))));
        });
    }
}
