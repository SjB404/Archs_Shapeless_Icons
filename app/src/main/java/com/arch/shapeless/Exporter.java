package com.arch.shapeless;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.DisplayMetrics;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Saves every installed app's icon, split into its layers, as a zip in Downloads (sent to Claude for new icons). */
final class Exporter {

    interface Listener {
        void onProgress(int done, int total);

        void onDone(Uri zip, int count, String message);

        void onError(String message);
    }

    private static final int LAYER_SIZE = 432; // full size of an Android icon layer, 4x quality
    private static final int FULL_SIZE = 192;  // the finished icon, just for reference

    private Exporter() {
    }

    static void start(Context ctx, boolean onlyMissing, Listener l) {
        Context app = ctx.getApplicationContext();
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                Object[] res = export(app, onlyMissing, (d, t) -> main.post(() -> l.onProgress(d, t)));
                main.post(() -> l.onDone((Uri) res[0], (Integer) res[1], (String) res[2]));
            } catch (Exception e) {
                main.post(() -> l.onError("Export failed: " + e.getMessage()));
            }
        }).start();
    }

    private interface Progress {
        void update(int done, int total);
    }

    /** The launcher entries this pack already has an icon for. */
    static Set<String> coveredComponents(Context ctx) {
        Set<String> out = new HashSet<>();
        try (XmlResourceParser p = ctx.getResources().getXml(R.xml.appfilter)) {
            int ev;
            while ((ev = p.next()) != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG && "item".equals(p.getName())) {
                    String c = p.getAttributeValue(null, "component");
                    if (c != null && c.startsWith("ComponentInfo{") && c.endsWith("}")) {
                        out.add(c.substring(14, c.length() - 1));
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static Object[] export(Context ctx, boolean onlyMissing, Progress progress) throws Exception {
        PackageManager pm = ctx.getPackageManager();
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = new ArrayList<>(pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL));
        Collections.sort(apps, (a, b) -> a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()));

        Set<String> covered = coveredComponents(ctx);
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new Date());
        String fileName = "shapeless-export-" + stamp + ".zip";

        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        cv.put(MediaStore.Downloads.MIME_TYPE, "application/zip");
        cv.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        ContentResolver cr = ctx.getContentResolver();
        Uri uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
        if (uri == null) throw new IllegalStateException("could not create the file in Downloads");

        JSONArray list = new JSONArray();
        int done = 0, exported = 0;
        try (OutputStream os = cr.openOutputStream(uri); ZipOutputStream zip = new ZipOutputStream(os)) {
            for (ResolveInfo ri : apps) {
                done++;
                progress.update(done, apps.size());

                ActivityInfo ai = ri.activityInfo;
                String component = ai.packageName + "/" + ai.name;
                boolean isCovered = covered.contains(component);
                if (onlyMissing && isCovered) continue;
                if (ai.packageName.equals(ctx.getPackageName())) continue;

                String label = ri.loadLabel(pm).toString();
                String folder = "apps/" + safe(ai.packageName) + "__" + safe(shortName(ai.name)) + "/";

                JSONObject o = new JSONObject();
                o.put("label", label);
                o.put("component", "ComponentInfo{" + component + "}");
                o.put("folder", folder);
                o.put("in_pack", isCovered);

                // Straight from the app's own files, in the sharpest version it has.
                Drawable icon = LiveIcons.own(pm, ai, DisplayMetrics.DENSITY_XXXHIGH);
                if (icon == null) icon = ai.loadUnbadgedIcon(pm);
                put(zip, folder + "full.png", render(icon, FULL_SIZE));

                if (icon instanceof AdaptiveIconDrawable) {
                    AdaptiveIconDrawable a = (AdaptiveIconDrawable) icon;
                    o.put("adaptive", true);
                    // The logo layer and the background layer, without the launcher's shape cut-out.
                    if (a.getForeground() != null) put(zip, folder + "fg.png", render(a.getForeground(), LAYER_SIZE));
                    if (a.getBackground() != null) {
                        put(zip, folder + "bg.png", render(a.getBackground(), LAYER_SIZE));
                        if (a.getBackground() instanceof ColorDrawable) {
                            o.put("bg_color", String.format("#%08X", ((ColorDrawable) a.getBackground()).getColor()));
                        }
                    }
                    // The one-colour version used for themed icons (Android 13+), when the app has one.
                    if (Build.VERSION.SDK_INT >= 33 && a.getMonochrome() != null) {
                        put(zip, folder + "mono.png", render(a.getMonochrome(), LAYER_SIZE));
                    }
                } else {
                    o.put("adaptive", false);
                    put(zip, folder + "legacy.png", render(icon, LAYER_SIZE));
                }
                list.put(o);
                exported++;
            }

            JSONObject meta = new JSONObject();
            meta.put("exported_at", stamp);
            meta.put("device", Build.MANUFACTURER + " " + Build.MODEL);
            meta.put("android", Build.VERSION.RELEASE);
            meta.put("pack_version", versionName(ctx));
            meta.put("apps", list);
            put(zip, "apps.json", meta.toString(2).getBytes("UTF-8"));
        }
        if (exported == 0) {
            // Don't leave an empty zip behind in Downloads.
            try {
                cr.delete(uri, null, null);
            } catch (Exception ignored) {
            }
            uri = null;
        }
        String msg = exported == 0 ? "Nothing to export: every app already has an icon"
                : "Saved " + exported + (exported == 1 ? " app" : " apps") + " to Downloads/" + fileName;
        return new Object[]{uri, exported, msg};
    }

    static Intent shareIntent(Uri zip) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("application/zip");
        send.putExtra(Intent.EXTRA_STREAM, zip);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return Intent.createChooser(send, "Send export");
    }

    private static byte[] render(Drawable d, int size) {
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        d.setBounds(0, 0, size, size);
        d.draw(c);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        bmp.recycle();
        return out.toByteArray();
    }

    private static void put(ZipOutputStream zip, String name, byte[] data) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }

    private static String shortName(String activity) {
        int i = activity.lastIndexOf('.');
        return i >= 0 ? activity.substring(i + 1) : activity;
    }

    private static String safe(String s) {
        return s.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    static String versionName(Context ctx) {
        try {
            return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }
}
