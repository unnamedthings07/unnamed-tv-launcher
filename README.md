# UNNAMED TV Launcher

Android TV home-screen launcher built with Java and Android SDK.

## Features
- Landscape-first TV launcher interface.
- D-pad and touch navigation.
- Local discovery of installed Leanback and launcher apps.
- Preferred app ordering for Netflix, Prime Video, JioHotstar, NoTubeTV, VLC, and Airtel Xstream.
- Input, Wi-Fi, and Settings shortcuts.
- Local wallpaper and startup splash support.
- No network permission or network-based telemetry.

## Build
Open this repository in Android Studio, allow Gradle sync, and build the `app` module. Requires Android SDK 34 and Java 17.

## Note about media assets
The source ZIP's original JPG wallpapers, splash image, and launcher artwork were not uploaded by the current GitHub integration. The app handles missing splash/wallpaper images with a dark background; add your own images under `app/src/main/assets/` to restore the original visual theme.
