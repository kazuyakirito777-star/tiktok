package com.tiktokchat;

/** Satu notifikasi yang akan ditampilkan di layar. */
public class LiveMessage {
    public enum Type { COMMENT, GIFT, LIKE, JOIN, SYSTEM }

    public final Type type;
    public final String user;
    public final String text;
    public final long createdAt = System.currentTimeMillis();

    public LiveMessage(Type type, String user, String text) {
        this.type = type;
        this.user = user;
        this.text = text;
    }
}
