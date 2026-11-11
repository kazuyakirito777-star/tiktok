# TikTok Live Chat (Fabric, Minecraft 1.21.11)

Menampilkan chat, gift, dan like TikTok Live sebagai popup di layar Minecraft.

## Build
Butuh JDK 21 dan Gradle (atau IntelliJ IDEA yang sudah punya Gradle):

    gradle wrapper --gradle-version 9.2.1
    ./gradlew build

Hasil: `build/libs/tiktok-live-chat-1.0.0.jar` (jangan ambil yang `-sources`).

## Install
Taruh jar di folder `mods/` bersama **Fabric Loader 0.18.1+** dan **Fabric API 0.141.3+1.21.11**.

## Pakai
Di dalam game (di world / server):

    /tiktok connect <username>     sambung ke live (username tersimpan)
    /tiktok disconnect
    /tiktok status
    /tiktok auto true|false        auto connect saat masuk world
    /tiktok set duration <detik>   lama popup tampil
    /tiktok set delay <detik>      jeda antar chat
    /tiktok set maxqueue <n>       maks popup bersamaan
    /tiktok set minlikes <n>       minimal like per notifikasi
    /tiktok set position <0-3>     0 kiri atas, 1 kanan atas, 2 kiri bawah, 3 kanan bawah

Config lengkap ada di `config/tiktok_live_chat.json`.
Host live harus sedang LIVE saat connect.
