package com.arch.shapeless;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Reads each app's own icon straight out of the installed app (never a saved copy), so the
 * comparison always shows the icon the app has right now. Icons load in the background and are
 * kept while the screen is open; an app update gives a new key, so its new icon is read again.
 */
final class LiveIcons {

    interface Callback {
        /** Called on the main thread; null when the app has no icon that can be read. */
        void done(Bitmap icon);
    }

    /** Stand-in kept in the cache for "this app has no readable icon". */
    private static final Bitmap NONE = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8);

    private final PackageManager pm;
    private final int sizePx;
    private final int density;
    private final LruCache<String, Bitmap> cache;
    private final ExecutorService pool = Executors.newFixedThreadPool(2);
    private final Handler main = new Handler(Looper.getMainLooper());
    /** Views waiting for an icon that is still loading (main thread only). */
    private final Map<String, List<WeakReference<ImageView>>> waiting = new HashMap<>();

    LiveIcons(Context c, int sizePx) {
        this.pm = c.getPackageManager();
        this.sizePx = sizePx;
        this.density = c.getResources().getDisplayMetrics().densityDpi;
        this.cache = new LruCache<String, Bitmap>(24 * 1024 * 1024) {
            @Override
            protected int sizeOf(String key, Bitmap b) {
                return b.getByteCount();
            }
        };
    }

    /** Shows the app's own icon in the view (grid size), loading it the first time. */
    void into(ImageView v, PackIndex.Entry e) {
        if (e.component == null || e.info == null) {
            v.setTag(null);
            v.setImageDrawable(null);
            return;
        }
        String key = e.component.flattenToString() + "@" + e.updated;
        v.setTag(key);
        Bitmap cached = cache.get(key);
        if (cached != null) {
            show(v, cached);
            return;
        }
        v.setImageDrawable(null);
        List<WeakReference<ImageView>> list = waiting.get(key);
        if (list != null) {
            list.add(new WeakReference<>(v));
            return;
        }
        list = new ArrayList<>();
        list.add(new WeakReference<>(v));
        waiting.put(key, list);
        final ActivityInfo info = e.info;
        pool.execute(() -> {
            Bitmap b = render(own(pm, info, density), sizePx);
            Bitmap stored = b != null ? b : NONE;
            cache.put(key, stored);
            main.post(() -> {
                List<WeakReference<ImageView>> l = waiting.remove(key);
                if (l == null) return;
                for (WeakReference<ImageView> r : l) {
                    ImageView iv = r.get();
                    if (iv != null && key.equals(iv.getTag())) show(iv, stored);
                }
            });
        });
    }

    /** Loads a big, sharp copy of the app's own icon (for the side-by-side view). Not kept. */
    void large(PackIndex.Entry e, int px, Callback cb) {
        final ActivityInfo info = e.info;
        if (info == null) {
            cb.done(null);
            return;
        }
        pool.execute(() -> {
            Bitmap b = render(own(pm, info, DisplayMetrics.DENSITY_XXXHIGH), px);
            main.post(() -> cb.done(b));
        });
    }

    void shutdown() {
        pool.shutdownNow();
    }

    private static void show(ImageView v, Bitmap b) {
        if (b == NONE) v.setImageDrawable(null);
        else v.setImageBitmap(b);
    }

    /**
     * The app's own icon, read from the app's own files. Goes to the app's resources directly
     * (sharpest version for the given screen density); falls back to asking the system.
     */
    static Drawable own(PackageManager pm, ActivityInfo ai, int density) {
        if (ai == null) return null;
        try {
            int res = ai.getIconResource();
            if (res != 0) {
                Resources r = pm.getResourcesForApplication(ai.applicationInfo);
                Drawable d = r.getDrawableForDensity(res, density, null);
                if (d != null) return d;
            }
        } catch (Exception | OutOfMemoryError ignored) {
        }
        try {
            return ai.loadUnbadgedIcon(pm);
        } catch (Exception | OutOfMemoryError ignored) {
            return null;
        }
    }

    static Bitmap render(Drawable d, int size) {
        if (d == null || size <= 0) return null;
        try {
            Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            d.setBounds(0, 0, size, size);
            d.draw(c);
            return b;
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }
}
