package com.arch.shapeless;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.XmlResourceParser;

import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Everything the home screen lists: every icon in the pack, plus the apps on this phone that don't
 * have an icon yet. Reads the pack's own appfilter.xml and asks the phone which apps are installed.
 */
final class PackIndex {

    /** The app is on this phone and the pack icon applies to it. */
    static final int ON_PHONE = 0;
    /** The pack has an icon, but the app isn't on this phone. */
    static final int NOT_INSTALLED = 1;
    /** The app is on this phone, but the way it starts changed, so the pack icon doesn't apply any more. */
    static final int ENTRY_CHANGED = 2;
    /** The app is on this phone and the pack has no icon for it yet. */
    static final int MISSING = 3;

    static final class Entry {
        String key;                 // unique: the drawable name, or "missing:" + component
        String drawable;            // pack icon name, null for MISSING
        int resId;                  // pack icon, 0 for MISSING
        String label;
        String packageName;
        ComponentName component;    // the app's launcher entry on this phone, null when not installed
        ActivityInfo info;          // details of that entry (where its own icon is), null when not installed
        long updated;               // when the app was last installed or updated, 0 when not installed
        final List<ComponentName> packComponents = new ArrayList<>();
        int state;
        String search;              // lower-case text the search bar matches against

        boolean hasPackIcon() {
            return resId != 0;
        }

        boolean onPhone() {
            return state != NOT_INSTALLED;
        }

        boolean needsIcon() {
            return state == MISSING || state == ENTRY_CHANGED;
        }
    }

    final List<Entry> entries = new ArrayList<>();
    /** Icons in the pack; apps on this phone (with or without an icon); apps that still need an icon. */
    int packIcons, onPhone, needIcon;

    static PackIndex load(Context ctx) {
        PackIndex out = new PackIndex();
        PackageManager pm = ctx.getPackageManager();
        Map<String, String> labels = readLabels(ctx);

        // 1. The pack's icons, with every launcher entry each one covers.
        Map<String, Entry> byDrawable = new LinkedHashMap<>();
        try (XmlResourceParser p = ctx.getResources().getXml(R.xml.appfilter)) {
            int ev;
            while ((ev = p.next()) != XmlPullParser.END_DOCUMENT) {
                if (ev != XmlPullParser.START_TAG || !"item".equals(p.getName())) continue;
                String comp = p.getAttributeValue(null, "component");
                String drawable = p.getAttributeValue(null, "drawable");
                if (comp == null || drawable == null) continue;
                ComponentName cn = parse(comp);
                if (cn == null) continue;
                Entry e = byDrawable.get(drawable);
                if (e == null) {
                    e = new Entry();
                    e.key = drawable;
                    e.drawable = drawable;
                    e.resId = ctx.getResources().getIdentifier(drawable, "drawable", ctx.getPackageName());
                    e.packageName = cn.getPackageName();
                    byDrawable.put(drawable, e);
                }
                e.packComponents.add(cn);
            }
        } catch (Exception ignored) {
        }

        // 2. What's on the phone: every app that shows up in the launcher.
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        Map<ComponentName, ResolveInfo> launchable = new HashMap<>();
        Map<String, ComponentName> firstByPackage = new HashMap<>();
        for (ResolveInfo ri : pm.queryIntentActivities(launcher, PackageManager.MATCH_ALL)) {
            ComponentName cn = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
            launchable.put(cn, ri);
            if (!firstByPackage.containsKey(cn.getPackageName())) firstByPackage.put(cn.getPackageName(), cn);
        }

        // When each app was last updated: part of the key for its own icon, so an update shows the new icon.
        Map<String, Long> updated = new HashMap<>();
        try {
            for (PackageInfo pi : pm.getInstalledPackages(0)) updated.put(pi.packageName, pi.lastUpdateTime);
        } catch (Exception ignored) {
        }

        Set<ComponentName> covered = new HashSet<>();
        Set<String> changedPackages = new HashSet<>();
        for (Entry e : byDrawable.values()) {
            covered.addAll(e.packComponents);
            for (ComponentName cn : e.packComponents) {
                if (launchable.containsKey(cn)) {
                    e.component = cn;
                    break;
                }
            }
            if (e.component != null) {
                e.state = ON_PHONE;
            } else if (firstByPackage.containsKey(e.packageName)) {
                // The app is here but starts through an entry the pack doesn't list.
                e.state = ENTRY_CHANGED;
                e.component = firstByPackage.get(e.packageName);
                changedPackages.add(e.packageName);
            } else {
                e.state = NOT_INSTALLED;
                String l = labels.get(e.drawable);
                e.label = l != null ? l : e.packageName;
            }
            if (e.component != null) {
                ResolveInfo ri = launchable.get(e.component);
                e.info = ri.activityInfo;
                e.label = ri.loadLabel(pm).toString();
                Long u = updated.get(e.packageName);
                e.updated = u != null ? u : 0;
            }
            out.entries.add(e);
        }

        // 3. Apps on the phone without an icon (apps whose entry changed are already listed above).
        for (Map.Entry<ComponentName, ResolveInfo> me : launchable.entrySet()) {
            ComponentName cn = me.getKey();
            if (covered.contains(cn) || cn.getPackageName().equals(ctx.getPackageName())) continue;
            if (changedPackages.contains(cn.getPackageName())) continue;
            Entry e = new Entry();
            e.key = "missing:" + cn.flattenToString();
            e.packageName = cn.getPackageName();
            e.component = cn;
            e.info = me.getValue().activityInfo;
            e.state = MISSING;
            e.label = me.getValue().loadLabel(pm).toString();
            Long u = updated.get(e.packageName);
            e.updated = u != null ? u : 0;
            out.entries.add(e);
        }

        for (Entry e : out.entries) {
            e.search = (e.label + " " + e.packageName).toLowerCase(Locale.ROOT);
            if (e.hasPackIcon()) out.packIcons++;
            if (e.onPhone()) out.onPhone++;
            if (e.needsIcon()) out.needIcon++;
        }
        Collections.sort(out.entries, (a, b) -> a.label.compareToIgnoreCase(b.label));
        return out;
    }

    /** "ComponentInfo{pkg/cls}" to a ComponentName. */
    static ComponentName parse(String comp) {
        int open = comp.indexOf('{');
        int close = comp.lastIndexOf('}');
        if (open < 0 || close <= open) return null;
        return ComponentName.unflattenFromString(comp.substring(open + 1, close));
    }

    /** App names written into the pack at build time, for apps that aren't installed. */
    private static Map<String, String> readLabels(Context ctx) {
        Map<String, String> out = new HashMap<>();
        try (InputStream in = ctx.getAssets().open("labels.json")) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) buf.write(b, 0, n);
            JSONObject o = new JSONObject(buf.toString("UTF-8"));
            for (java.util.Iterator<String> it = o.keys(); it.hasNext(); ) {
                String k = it.next();
                out.put(k, o.optString(k, k));
            }
        } catch (Exception ignored) {
        }
        return out;
    }
}
