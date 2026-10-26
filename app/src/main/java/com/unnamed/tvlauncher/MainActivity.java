package com.unnamed.tvlauncher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/** UNNAMED: a lightweight, local-only Android TV home screen. */
public final class MainActivity extends Activity implements LauncherView.Callbacks {
    private static final String PREFS = "unnamed_launcher";
    private static final String KEY_LAST_WALLPAPER = "last_wallpaper";
    private static final long INTRO_DURATION_MS = 1_000L;
    private static final String[] WALLPAPERS = {
            "wallpapers/wallpaper_01.jpg", "wallpapers/wallpaper_02.jpg",
            "wallpapers/wallpaper_03.jpg", "wallpapers/wallpaper_04.jpg",
            "wallpapers/wallpaper_05.jpg"
    };
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private LauncherView launcherView;
    private Bitmap splashBitmap, wallpaperBitmap;
    private List<AppTile> appTiles = new ArrayList<>();
    private int entryGeneration = 0, currentWallpaperIndex = -1, pendingWallpaperIndex = -1;
    private boolean firstResumeHandled, activityResumed, wentAway, awaitingAppReturn,
            pendingControlReturn, suppressNextResume, introVisible = true;
    private Runnable finishIntroRunnable;
    private final Runnable clockTicker = new Runnable() {
        @Override public void run() {
            if (launcherView != null && !isFinishing() && !launcherView.isShowingSplash()) launcherView.invalidate();
            if (!isFinishing() && !isDestroyed()) mainHandler.postDelayed(this, 60_000L);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        launcherView = new LauncherView(this, this);
        setContentView(launcherView);
        splashBitmap = decodeAsset("unnamed_splash.jpg", 1280, 720);
        launcherView.setSplashBitmap(splashBitmap);
        startHomeEntry(true);
    }
    @Override protected void onResume() {
        super.onResume(); activityResumed = true;
        mainHandler.removeCallbacks(clockTicker); mainHandler.postDelayed(clockTicker, 60_000L);
        if (!firstResumeHandled) { firstResumeHandled = true; return; }
        if (suppressNextResume) { suppressNextResume = false; wentAway = false; return; }
        if (wentAway) {
            wentAway = false;
            if (pendingControlReturn) { pendingControlReturn = false; return; }
            if (awaitingAppReturn || !introVisible) { awaitingAppReturn = false; startHomeEntry(false); }
        }
    }
    @Override protected void onPause() { activityResumed = false; mainHandler.removeCallbacks(clockTicker); super.onPause(); }
    @Override protected void onStop() { super.onStop(); if (!isFinishing()) wentAway = true; }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent); pendingControlReturn = false;
        awaitingAppReturn = false; wentAway = false; suppressNextResume = !activityResumed; startHomeEntry(false);
    }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            pendingControlReturn = false; awaitingAppReturn = false; wentAway = false;
            suppressNextResume = !activityResumed; startHomeEntry(false); return true;
        }
        if (launcherView != null && launcherView.handleDpadKey(keyCode)) return true;
        return super.onKeyDown(keyCode, event);
    }
    @Override public void onBackPressed() { if (launcherView != null && launcherView.handleBack()) return; }

    private void startHomeEntry(boolean refreshApps) {
        final int generation = ++entryGeneration;
        final long startedAt = SystemClock.elapsedRealtime();
        introVisible = true;
        if (finishIntroRunnable != null) mainHandler.removeCallbacks(finishIntroRunnable);
        if (refreshApps) launcherView.resetNavigation();
        launcherView.showSplash();
        final int wallpaperIndex = chooseWallpaperIndex();
        pendingWallpaperIndex = wallpaperIndex;
        Thread loader = new Thread(() -> {
            Bitmap nextWallpaper = decodeAsset(WALLPAPERS[wallpaperIndex], 1600, 900);
            List<AppTile> foundApps = refreshApps ? discoverApps() : null;
            mainHandler.post(() -> {
                if (generation != entryGeneration || isFinishing() || isDestroyed()) {
                    if (nextWallpaper != null && !nextWallpaper.isRecycled()) nextWallpaper.recycle(); return;
                }
                if (nextWallpaper != null) {
                    Bitmap previous = wallpaperBitmap; wallpaperBitmap = nextWallpaper;
                    currentWallpaperIndex = wallpaperIndex;
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(KEY_LAST_WALLPAPER, wallpaperIndex).apply();
                    if (pendingWallpaperIndex == wallpaperIndex) pendingWallpaperIndex = -1;
                    if (previous != null && previous != nextWallpaper && !previous.isRecycled()) previous.recycle();
                    launcherView.setWallpaper(wallpaperBitmap);
                } else if (pendingWallpaperIndex == wallpaperIndex) pendingWallpaperIndex = -1;
                if (foundApps != null) { appTiles = foundApps; launcherView.setApps(appTiles); }
                long remaining = HomeEntryTiming.remainingMillis(startedAt, SystemClock.elapsedRealtime(), INTRO_DURATION_MS);
                finishIntroRunnable = () -> {
                    if (generation != entryGeneration || isFinishing() || isDestroyed()) return;
                    introVisible = false; launcherView.showHome(); launcherView.invalidate();
                };
                mainHandler.postDelayed(finishIntroRunnable, remaining);
            });
        }, "unnamed-home-load");
        loader.setDaemon(true); loader.start();
    }
    private int chooseWallpaperIndex() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int previous = currentWallpaperIndex >= 0 ? currentWallpaperIndex : prefs.getInt(KEY_LAST_WALLPAPER, -1);
        int pending = pendingWallpaperIndex, chosen;
        if (WALLPAPERS.length <= 1) chosen = 0;
        else do { chosen = random.nextInt(WALLPAPERS.length); } while (chosen == previous || (pending >= 0 && chosen == pending));
        return chosen;
    }
    private Bitmap decodeAsset(String path, int maxWidth, int maxHeight) {
        try (InputStream input = getAssets().open(path)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565; options.inDither = true;
            Bitmap decoded = BitmapFactory.decodeStream(input, null, options);
            if (decoded == null) return null;
            if (decoded.getWidth() <= maxWidth && decoded.getHeight() <= maxHeight) return decoded;
            float scale = Math.min((float) maxWidth / decoded.getWidth(), (float) maxHeight / decoded.getHeight());
            Bitmap scaled = Bitmap.createScaledBitmap(decoded, Math.max(1, Math.round(decoded.getWidth()*scale)),
                    Math.max(1, Math.round(decoded.getHeight()*scale)), true);
            if (scaled != decoded) decoded.recycle(); return scaled;
        } catch (IOException | RuntimeException ignored) { return null; }
    }
    private List<AppTile> discoverApps() {
        PackageManager pm = getPackageManager(); Map<String, AppTile> byPackage = new HashMap<>();
        addActivities(pm, byPackage, Intent.CATEGORY_LEANBACK_LAUNCHER, true);
        addActivities(pm, byPackage, Intent.CATEGORY_LAUNCHER, false);
        ArrayList<AppTile> result = new ArrayList<>(byPackage.values());
        result.sort(Comparator.comparingInt((AppTile tile) -> appPriority(tile.label, tile.packageName))
                .thenComparing(tile -> tile.label.toLowerCase(Locale.ROOT)));
        return result;
    }
    private void addActivities(PackageManager pm, Map<String, AppTile> out, String category, boolean leanback) {
        Intent query = new Intent(Intent.ACTION_MAIN); query.addCategory(category);
        List<ResolveInfo> resolved;
        try { resolved = pm.queryIntentActivities(query, 0); } catch (RuntimeException ex) { return; }
        for (ResolveInfo info : resolved) {
            if (info.activityInfo == null) continue;
            String pkg = info.activityInfo.packageName;
            if (pkg == null || pkg.equals(getPackageName()) || isInfrastructurePackage(pkg) || out.containsKey(pkg)) continue;
            String label;
            try { label = String.valueOf(info.loadLabel(pm)); } catch (RuntimeException ex) { label = pkg; }
            Drawable art = null; boolean hasBanner = false;
            try { art = info.loadBanner(pm); } catch (RuntimeException ignored) { }
            if (art == null) try { art = info.activityInfo.applicationInfo.loadBanner(pm); } catch (RuntimeException ignored) { }
            if (art != null) hasBanner = true; else try { art = info.loadIcon(pm); } catch (RuntimeException ignored) { }
            if (art != null) out.put(pkg, new AppTile(pkg, info.activityInfo.name, label, art, hasBanner, leanback));
        }
    }
    private boolean isInfrastructurePackage(String p) {
        return p.equals("com.android.settings") || p.equals("com.android.tv.settings")
                || p.equals("com.google.android.tvlauncher") || p.equals("com.google.android.apps.tv.launcherx")
                || p.equals("com.google.android.leanbacklauncher") || p.equals("com.gathin.tvhome");
    }
    private int appPriority(String label, String packageName) {
        String v = (label + " " + packageName).toLowerCase(Locale.ROOT);
        if (v.contains("netflix") || v.contains("com.netflix.ninja")) return 0;
        if (v.contains("prime video") || v.contains("amazonvideo") || v.contains("primevideo")) return 1;
        if (v.contains("jiohotstar") || v.contains("hotstar")) return 2;
        if (v.contains("com.ycngmn.notubetv") || v.contains("notubetv") || v.contains("notube")) return 3;
        if (v.contains("videolan") || v.contains("org.videolan.vlc") || v.matches(".*\\bvlc\\b.*")) return 4;
        if (v.contains("tv.airtel.xstream.tvapp") || v.contains("airtelxstream") || v.contains("airtel xstream") || v.contains("airtel.tv")) return 5;
        return 100;
    }
    @Override public void launchApp(AppTile tile) {
        if (tile == null) return;
        try { awaitingAppReturn = true; pendingControlReturn = false; startActivity(tile.makeLaunchIntent()); }
        catch (ActivityNotFoundException | SecurityException ex) {
            awaitingAppReturn = false; Toast.makeText(this, "Couldn't open " + tile.label, Toast.LENGTH_SHORT).show();
        }
    }
    @Override public void openControl(int control) {
        Intent intent = control == LauncherView.CONTROL_INPUTS ? new Intent("com.android.tv.action.VIEW_INPUTS")
                : control == LauncherView.CONTROL_WIFI ? new Intent(Settings.ACTION_WIFI_SETTINGS) : new Intent(Settings.ACTION_SETTINGS);
        try { pendingControlReturn = true; awaitingAppReturn = false; startActivity(intent); }
        catch (ActivityNotFoundException | SecurityException ex) {
            if (control == LauncherView.CONTROL_INPUTS) try {
                pendingControlReturn = true; startActivity(new Intent(Settings.ACTION_SETTINGS)); return;
            } catch (RuntimeException ignored) { }
            pendingControlReturn = false; Toast.makeText(this, "This control isn't available on this TV", Toast.LENGTH_SHORT).show();
        }
    }
    @Override protected void onDestroy() {
        entryGeneration++; mainHandler.removeCallbacksAndMessages(null);
        if (splashBitmap != null && !splashBitmap.isRecycled()) splashBitmap.recycle();
        if (wallpaperBitmap != null && !wallpaperBitmap.isRecycled()) wallpaperBitmap.recycle();
        super.onDestroy();
    }
}