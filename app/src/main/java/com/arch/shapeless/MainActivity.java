package com.arch.shapeless;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
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
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

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

/**
 * One screen with two jobs:
 *  1. Being the icon pack (launchers find it through the manifest, nothing to do here).
 *  2. Exporting every installed app's icon, split into its layers, as a zip in Downloads.
 */
public class MainActivity extends Activity {

    private static final int LAYER_SIZE = 432; // full size of an Android icon layer, 4x quality
    private static final int FULL_SIZE = 192;  // the finished icon, just for reference

    private TextView status;
    private ProgressBar progress;
    private Button exportAll, exportMissing, share;
    private Uri lastZip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad * 2, pad, pad);

        TextView title = new TextView(this);
        title.setText("Shapeless Icons");
        title.setTextSize(28);
        root.addView(title);

        Set<String> covered = loadCoveredComponents();
        TextView info = new TextView(this);
        info.setText("Icons in this pack: " + covered.size()
                + "\n\nTo use the pack: pick \"Shapeless Icons\" in your launcher's icon pack setting."
                + "\n\nTo request icons: export, then send the zip from Downloads to Claude.");
        info.setPadding(0, pad / 2, 0, pad);
        root.addView(info);

        Button browse = new Button(this);
        browse.setText("Browse icons");
        browse.setOnClickListener(v -> startActivity(new Intent(this, IconIndexActivity.class)));
        root.addView(browse);

        exportAll = new Button(this);
        exportAll.setText("Export all apps");
        exportAll.setOnClickListener(v -> startExport(false));
        root.addView(exportAll);

        exportMissing = new Button(this);
        exportMissing.setText("Export only apps without an icon yet");
        exportMissing.setOnClickListener(v -> startExport(true));
        root.addView(exportMissing);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setVisibility(View.GONE);
        root.addView(progress);

        status = new TextView(this);
        status.setPadding(0, pad / 2, 0, pad / 2);
        root.addView(status);

        share = new Button(this);
        share.setText("Share zip");
        share.setVisibility(View.GONE);
        share.setOnClickListener(v -> shareZip());
        root.addView(share);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    /** Reads this pack's own appfilter.xml to see which apps already have an icon. */
    private Set<String> loadCoveredComponents() {
        Set<String> out = new HashSet<>();
        try (XmlResourceParser p = getResources().getXml(R.xml.appfilter)) {
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

    private void startExport(boolean onlyMissing) {
        exportAll.setEnabled(false);
        exportMissing.setEnabled(false);
        share.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        progress.setProgress(0);
        status.setText("Collecting apps…");

        new Thread(() -> {
            try {
                String msg = export(onlyMissing);
                runOnUiThread(() -> {
                    status.setText(msg);
                    if (lastZip != null) share.setVisibility(View.VISIBLE);
                });
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Export failed: " + e));
            } finally {
                runOnUiThread(() -> {
                    exportAll.setEnabled(true);
                    exportMissing.setEnabled(true);
                    progress.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private String export(boolean onlyMissing) throws Exception {
        PackageManager pm = getPackageManager();
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = new ArrayList<>(pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL));
        Collections.sort(apps, (a, b) -> a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()));

        Set<String> covered = loadCoveredComponents();
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new Date());
        String fileName = "shapeless-export-" + stamp + ".zip";

        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        cv.put(MediaStore.Downloads.MIME_TYPE, "application/zip");
        cv.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        ContentResolver cr = getContentResolver();
        Uri uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
        if (uri == null) throw new IllegalStateException("Could not create file in Downloads");

        JSONArray list = new JSONArray();
        int done = 0, exported = 0;
        runOnUiThread(() -> progress.setMax(apps.size()));

        try (OutputStream os = cr.openOutputStream(uri); ZipOutputStream zip = new ZipOutputStream(os)) {
            for (ResolveInfo ri : apps) {
                done++;
                final int d = done;
                runOnUiThread(() -> progress.setProgress(d));

                ActivityInfo ai = ri.activityInfo;
                String component = ai.packageName + "/" + ai.name;
                boolean isCovered = covered.contains(component);
                if (onlyMissing && isCovered) continue;
                if (ai.packageName.equals(getPackageName())) continue;

                String label = ri.loadLabel(pm).toString();
                String folder = "apps/" + safe(ai.packageName) + "__" + safe(shortName(ai.name)) + "/";

                JSONObject o = new JSONObject();
                o.put("label", label);
                o.put("component", "ComponentInfo{" + component + "}");
                o.put("folder", folder);
                o.put("in_pack", isCovered);

                Drawable icon = ai.loadUnbadgedIcon(pm);
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
            meta.put("pack_version", versionName());
            meta.put("apps", list);
            put(zip, "apps.json", meta.toString(2).getBytes("UTF-8"));
        }

        lastZip = uri;
        return "Saved " + exported + " apps to Downloads/" + fileName;
    }

    private void shareZip() {
        if (lastZip == null) return;
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("application/zip");
        send.putExtra(Intent.EXTRA_STREAM, lastZip);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, "Send export"));
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

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }
}
