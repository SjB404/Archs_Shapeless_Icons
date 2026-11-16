package com.arch.shapeless;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Icon index: every icon in the pack with its app name, a search bar, and a switch between
 * Arch's app drawer colour and his wallpaper colour so icons can be checked on both.
 */
public class IconIndexActivity extends Activity {

    private static final int DRAWER = 0xFF2F2F2F;
    private static final int WALLPAPER = 0xFF4F7D95;

    private static class Entry {
        String drawable;
        String label;
        String packageName;
        int resId;
        boolean installed;
    }

    private static class Holder {
        ImageView img;
        TextView label;
    }

    private final List<Entry> all = new ArrayList<>();
    private final List<Entry> shown = new ArrayList<>();
    private IconAdapter adapter;
    private TextView count;
    private LinearLayout root;
    private boolean onWallpaper = false;
    private float dp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dp = getResources().getDisplayMetrics().density;
        loadEntries();

        int pad = (int) (16 * dp);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(DRAWER);
        root.setPadding(pad, pad * 2, pad, 0);

        TextView title = new TextView(this);
        title.setText("Icon index");
        title.setTextSize(24);
        title.setTextColor(Color.WHITE);
        root.addView(title);

        EditText search = new EditText(this);
        search.setHint("Search apps");
        search.setSingleLine(true);
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(0xFF9A9A9A);
        root.addView(search, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(0, pad / 2, 0, pad / 2);
        count = new TextView(this);
        count.setTextColor(0xFFBDBDBD);
        bar.addView(count, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView bgToggle = new TextView(this);
        bgToggle.setText("Background: drawer");
        bgToggle.setTextColor(0xFFFFFFFF);
        bgToggle.setOnClickListener(v -> {
            onWallpaper = !onWallpaper;
            root.setBackgroundColor(onWallpaper ? WALLPAPER : DRAWER);
            bgToggle.setText(onWallpaper ? "Background: wallpaper" : "Background: drawer");
        });
        bar.addView(bgToggle);
        root.addView(bar);

        GridView grid = new GridView(this);
        grid.setNumColumns(GridView.AUTO_FIT);
        grid.setColumnWidth((int) (84 * dp));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setVerticalSpacing((int) (14 * dp));
        grid.setHorizontalSpacing((int) (6 * dp));
        adapter = new IconAdapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((parent, view, position, id) -> showBig(shown.get(position)));
        root.addView(grid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filter(s.toString());
            }
        });

        filter("");
        setContentView(root);
    }

    /** One entry per icon, from the pack's own appfilter.xml; names come from the installed apps. */
    private void loadEntries() {
        PackageManager pm = getPackageManager();
        Map<String, Entry> byDrawable = new LinkedHashMap<>();
        try (XmlResourceParser p = getResources().getXml(R.xml.appfilter)) {
            int ev;
            while ((ev = p.next()) != XmlPullParser.END_DOCUMENT) {
                if (ev != XmlPullParser.START_TAG || !"item".equals(p.getName())) continue;
                String comp = p.getAttributeValue(null, "component");
                String drawable = p.getAttributeValue(null, "drawable");
                if (comp == null || drawable == null || byDrawable.containsKey(drawable)) continue;
                Entry e = new Entry();
                e.drawable = drawable;
                int open = comp.indexOf('{');
                int slash = comp.indexOf('/');
                e.packageName = (open >= 0 && slash > open) ? comp.substring(open + 1, slash) : drawable;
                e.resId = getResources().getIdentifier(drawable, "drawable", getPackageName());
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(e.packageName, 0);
                    e.label = pm.getApplicationLabel(ai).toString();
                    e.installed = true;
                } catch (PackageManager.NameNotFoundException notInstalled) {
                    e.label = e.packageName;
                    e.installed = false;
                }
                byDrawable.put(drawable, e);
            }
        } catch (Exception ignored) {
        }
        all.addAll(byDrawable.values());
        Collections.sort(all, (a, b) -> a.label.compareToIgnoreCase(b.label));
    }

    private void filter(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        shown.clear();
        for (Entry e : all) {
            if (q.isEmpty()
                    || e.label.toLowerCase(Locale.ROOT).contains(q)
                    || e.packageName.toLowerCase(Locale.ROOT).contains(q)) {
                shown.add(e);
            }
        }
        count.setText(shown.size() + " of " + all.size() + " icons");
        adapter.notifyDataSetChanged();
    }

    private void showBig(Entry e) {
        int pad = (int) (24 * dp);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setBackgroundColor(onWallpaper ? WALLPAPER : DRAWER);
        box.setPadding(pad, pad, pad, pad);

        ImageView big = new ImageView(this);
        big.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (e.resId != 0) big.setImageResource(e.resId);
        int size = (int) (192 * dp);
        box.addView(big, new LinearLayout.LayoutParams(size, size));

        TextView text = new TextView(this);
        text.setGravity(Gravity.CENTER);
        text.setTextColor(Color.WHITE);
        text.setPadding(0, pad / 2, 0, 0);
        text.setText(e.label + "\n" + e.packageName + (e.installed ? "" : "\n(not installed)"));
        box.addView(text);

        new AlertDialog.Builder(this).setView(box).setPositiveButton("Close", null).show();
    }

    private class IconAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return shown.size();
        }

        @Override
        public Object getItem(int position) {
            return shown.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder h;
            View cell = convertView;
            if (cell == null) {
                LinearLayout box = new LinearLayout(IconIndexActivity.this);
                box.setOrientation(LinearLayout.VERTICAL);
                box.setGravity(Gravity.CENTER_HORIZONTAL);
                h = new Holder();
                h.img = new ImageView(IconIndexActivity.this);
                h.img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                int s = (int) (56 * dp);
                box.addView(h.img, new LinearLayout.LayoutParams(s, s));
                h.label = new TextView(IconIndexActivity.this);
                h.label.setTextColor(0xFFE6E6E6);
                h.label.setTextSize(11);
                h.label.setGravity(Gravity.CENTER);
                h.label.setMaxLines(2);
                h.label.setEllipsize(TextUtils.TruncateAt.END);
                h.label.setPadding(0, (int) (4 * dp), 0, 0);
                box.addView(h.label, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                box.setTag(h);
                cell = box;
            } else {
                h = (Holder) cell.getTag();
            }
            Entry e = shown.get(position);
            if (e.resId != 0) {
                h.img.setImageResource(e.resId);
            } else {
                h.img.setImageDrawable(null);
            }
            h.label.setText(e.label);
            return cell;
        }
    }
}
