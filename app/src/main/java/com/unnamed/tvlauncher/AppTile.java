package com.unnamed.tvlauncher;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;

/** One locally installed app tile. Artwork is loaded from the app's own resources, never from the network. */
final class AppTile {
    final String packageName;
    final String activityName;
    final String label;
    final Drawable artwork;
    final boolean hasBanner;
    final boolean leanbackEntry;

    AppTile(String packageName, String activityName, String label,
            Drawable artwork, boolean hasBanner, boolean leanbackEntry) {
        this.packageName = packageName;
        this.activityName = activityName;
        this.label = label;
        this.artwork = artwork;
        this.hasBanner = hasBanner;
        this.leanbackEntry = leanbackEntry;
    }

    Intent makeLaunchIntent() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(leanbackEntry
                ? Intent.CATEGORY_LEANBACK_LAUNCHER
                : Intent.CATEGORY_LAUNCHER);
        intent.setComponent(new ComponentName(packageName, activityName));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
}
