package com.arch.shapeless;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The app's only screen. It has two jobs:
 *  1. Being the icon pack (launchers find it through the manifest, nothing to do here).
 *  2. Showing every icon in the pack under a search bar, next to each app's own icon when asked,
 *     and exporting app icons as a zip so new icons can be made.
 */
public class MainActivity extends Activity {

    private static final int SHOW_ALL = 0, SHOW_ON_PHONE = 1, SHOW_MISSING = 2;
    private static final int DARK = 0xFF1C1C1E;      // icons and text on white buttons
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private SharedPreferences prefs;
    private LiveIcons live;
    private PackIndex index;
    private int loadRun;
    private final List<PackIndex.Entry> shown = new ArrayList<>();
    private int filter = SHOW_ALL;
    private String query = "";
    private boolean compare, onWallpaper;

    private LinearLayout root;
    private TextView subtitle, compareHint, emptyText;
    private ImageView emptyIcon;
    private EditText search;
    private ImageButton clearButton, compareButton, colourButton;
    private final TextView[] chips = new TextView[3];
    private GridView grid;
    private IconAdapter adapter;
    private Dialog sheet;
    private Uri lastZip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("ui", MODE_PRIVATE);
        onWallpaper = prefs.getBoolean("wallpaper", false);
        compare = prefs.getBoolean("compare", false);
        live = new LiveIcons(this, dp(56));
        buildScreen();
        applyBackground();
        applyCompare();
        applyFilter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload(); // apps may have been installed, removed or updated in the meantime
    }

    @Override
    protected void onDestroy() {
        if (sheet != null) sheet.dismiss();
        live.shutdown();
        super.onDestroy();
    }

    // ---------------------------------------------------------------- loading

    private void reload() {
        final int run = ++loadRun;
        final Context app = getApplicationContext();
        new Thread(() -> {
            PackIndex fresh = PackIndex.load(app);
            runOnUiThread(() -> {
                if (run != loadRun || isDestroyed()) return;
                index = fresh;
                updateCounts();
                refilter(false);
            });
        }).start();
    }

    private void updateCounts() {
        StringBuilder sb = new StringBuilder();
        sb.append(index.packIcons).append(" icons · ").append(index.onPhone).append(" apps on this phone");
        if (index.needIcon > 0) sb.append(" · ").append(index.needIcon).append(" missing");
        subtitle.setText(sb);
        chips[SHOW_MISSING].setText(index.needIcon > 0 ? "Missing " + index.needIcon : "Missing");
    }

    // ---------------------------------------------------------------- the home screen

    private void buildScreen() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setFocusableInTouchMode(true); // lets the search bar give up focus
        int side = dp(16);

        // Title with the export and help buttons, and the counts under it.
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(side + dp(4), dp(16), side, 0);
        TextView title = Ui.text(this, "Shapeless Icons", 22, Ui.TEXT, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        header.addView(title, new LinearLayout.LayoutParams(0, WRAP, 1f));

        LinearLayout export = Ui.pillButton(this, R.drawable.ic_download, "Export", false);
        export.setContentDescription("Export icons");
        export.setOnClickListener(v -> showExportSheet());
        header.addView(export, marginLp(WRAP, dp(44), dp(8)));
        ImageButton help = Ui.iconButton(this, R.drawable.ic_info, "How it works");
        help.setOnClickListener(v -> showInfoSheet());
        header.addView(help, marginLp(dp(44), dp(44), dp(8)));
        root.addView(header);
        subtitle = Ui.text(this, "Loading…", 13, Ui.TEXT_DIM, false);
        subtitle.setPadding(side + dp(4), 0, side, 0);
        root.addView(subtitle);

        // Search bar, with the compare and background buttons next to it.
        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        searchRow.setPadding(side, dp(16), side, 0);

        LinearLayout pill = new LinearLayout(this);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setBackground(Ui.rounded(Ui.FILL, dp(24)));
        pill.setPadding(dp(14), 0, dp(4), 0);
        ImageView glass = new ImageView(this);
        glass.setImageDrawable(Ui.icon(this, R.drawable.ic_search, Ui.TEXT_DIM));
        pill.addView(glass, new LinearLayout.LayoutParams(dp(22), dp(22)));

        search = new EditText(this);
        search.setBackground(null);
        search.setHint("Search icons");
        search.setHintTextColor(Ui.TEXT_FAINT);
        search.setTextColor(Ui.TEXT);
        search.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        search.setSingleLine(true);
        search.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setPadding(dp(10), 0, dp(4), 0);
        GradientDrawable cursor = new GradientDrawable();
        cursor.setColor(Ui.TEXT);
        cursor.setSize(dp(2), dp(20));
        search.setTextCursorDrawable(cursor);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                query = s.toString().trim();
                clearButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                refilter(true);
            }
        });
        search.setOnEditorActionListener((v, actionId, event) -> {
            hideKeyboard();
            return true;
        });
        pill.addView(search, new LinearLayout.LayoutParams(0, dp(48), 1f));
        pill.setOnClickListener(v -> showKeyboard());

        clearButton = new ImageButton(this);
        clearButton.setImageDrawable(Ui.icon(this, R.drawable.ic_close, Ui.TEXT_DIM));
        clearButton.setBackground(Ui.ripple(0, dp(20)));
        clearButton.setContentDescription("Clear search");
        clearButton.setVisibility(View.GONE);
        clearButton.setOnClickListener(v -> search.setText(""));
        pill.addView(clearButton, new LinearLayout.LayoutParams(dp(40), dp(40)));
        searchRow.addView(pill, new LinearLayout.LayoutParams(0, dp(48), 1f));

        compareButton = Ui.iconButton(this, R.drawable.ic_compare, "Compare with each app's own icon");
        compareButton.setOnClickListener(v -> {
            compare = !compare;
            prefs.edit().putBoolean("compare", compare).apply();
            applyCompare();
        });
        searchRow.addView(compareButton, marginLp(dp(48), dp(48), dp(8)));
        colourButton = Ui.iconButton(this, R.drawable.ic_palette, "Switch background: drawer or wallpaper");
        colourButton.setOnClickListener(v -> setWallpaper(!onWallpaper));
        searchRow.addView(colourButton, marginLp(dp(48), dp(48), dp(8)));
        root.addView(searchRow);

        // Which apps to show.
        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        chipScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout chipRow = new LinearLayout(this);
        chipRow.setPadding(side, dp(12), side, dp(4));
        String[] names = {"All", "On this phone", "Missing"};
        for (int i = 0; i < chips.length; i++) {
            TextView chip = Ui.text(this, names[i], 14, Ui.TEXT, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(16), 0, dp(16), 0);
            final int f = i;
            chip.setOnClickListener(v -> {
                filter = f;
                applyFilter();
            });
            chipRow.addView(chip, marginLp(WRAP, dp(36), i == 0 ? 0 : dp(8)));
            chips[i] = chip;
        }
        chipScroll.addView(chipRow);
        root.addView(chipScroll);

        compareHint = Ui.text(this, "Left: Shapeless · right: the app's own icon", 12, Ui.TEXT_DIM, false);
        compareHint.setPadding(side + dp(4), dp(6), side, 0);
        root.addView(compareHint);

        // The icons.
        FrameLayout gridBox = new FrameLayout(this);
        grid = new GridView(this);
        grid.setNumColumns(GridView.AUTO_FIT);
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setVerticalSpacing(dp(2));
        grid.setSelector(new ColorDrawable(Color.TRANSPARENT));
        grid.setClipToPadding(false);
        grid.setPadding(dp(10), dp(6), dp(10), dp(24));
        grid.setFastScrollEnabled(true);
        adapter = new IconAdapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((parent, view, position, id) -> showDetail(shown.get(position)));
        grid.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int state) {
                if (state == SCROLL_STATE_TOUCH_SCROLL) hideKeyboard();
            }

            @Override
            public void onScroll(AbsListView view, int first, int visible, int total) {
            }
        });
        gridBox.addView(grid, new FrameLayout.LayoutParams(MATCH, MATCH));

        LinearLayout empty = new LinearLayout(this);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER_HORIZONTAL);
        emptyIcon = new ImageView(this);
        empty.addView(emptyIcon, new LinearLayout.LayoutParams(dp(40), dp(40)));
        emptyText = Ui.text(this, "", 15, Ui.TEXT_DIM, false);
        emptyText.setGravity(Gravity.CENTER);
        emptyText.setPadding(dp(32), dp(12), dp(32), 0);
        empty.addView(emptyText);
        FrameLayout.LayoutParams elp = new FrameLayout.LayoutParams(MATCH, WRAP, Gravity.TOP);
        elp.topMargin = dp(64);
        gridBox.addView(empty, elp);
        grid.setEmptyView(empty);
        root.addView(gridBox, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        setContentView(root);
        root.requestFocus();
    }

    private void setWallpaper(boolean on) {
        onWallpaper = on;
        prefs.edit().putBoolean("wallpaper", on).apply();
        applyBackground();
    }

    private int previewColour() {
        return onWallpaper ? Ui.WALLPAPER : Ui.DRAWER;
    }

    private void applyBackground() {
        int bg = previewColour();
        root.setBackgroundColor(bg);
        Window w = getWindow();
        w.setBackgroundDrawable(new ColorDrawable(bg));
        w.setStatusBarColor(bg);
        w.setNavigationBarColor(bg);
        adapter.notifyDataSetChanged(); // the badges have a ring in the background colour
    }

    private void applyCompare() {
        Ui.setActive(this, compareButton, R.drawable.ic_compare, compare);
        compareHint.setVisibility(compare ? View.VISIBLE : View.GONE);
        grid.setColumnWidth(dp(compare ? 110 : 70));
        adapter.notifyDataSetChanged();
    }

    private void applyFilter() {
        for (int i = 0; i < chips.length; i++) {
            boolean on = i == filter;
            chips[i].setBackground(Ui.ripple(on ? Ui.ACCENT : Ui.FILL, dp(18)));
            chips[i].setTextColor(on ? DARK : Ui.TEXT);
        }
        refilter(true);
    }

    /** Fills the grid with the apps that match the chosen button and the search text. */
    private void refilter(boolean toTop) {
        shown.clear();
        if (index != null) {
            String[] words = query.toLowerCase(Locale.ROOT).split("\\s+");
            for (PackIndex.Entry e : index.entries) {
                if (filter == SHOW_ON_PHONE && !e.onPhone()) continue;
                if (filter == SHOW_MISSING && !e.needsIcon()) continue;
                boolean match = true;
                for (String w : words) {
                    if (!w.isEmpty() && !e.search.contains(w)) {
                        match = false;
                        break;
                    }
                }
                if (match) shown.add(e);
            }
        }
        adapter.notifyDataSetChanged();
        if (toTop) grid.setSelection(0);
        updateEmpty();
    }

    private void updateEmpty() {
        int icon;
        String text;
        if (index == null) {
            icon = 0;
            text = "Loading…";
        } else if (!query.isEmpty()) {
            icon = R.drawable.ic_search;
            text = "No apps match “" + query + "”";
        } else if (filter == SHOW_MISSING) {
            icon = R.drawable.ic_check;
            text = "Every app on this phone has an icon";
        } else {
            icon = R.drawable.ic_apps;
            text = "No apps here";
        }
        emptyIcon.setImageDrawable(icon == 0 ? null : Ui.icon(this, icon, Ui.TEXT_FAINT));
        emptyIcon.setVisibility(icon == 0 ? View.GONE : View.VISIBLE);
        emptyText.setText(text);
    }

    private void showKeyboard() {
        search.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(search, 0);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
        root.requestFocus();
    }

    // ---------------------------------------------------------------- the grid

    private final class IconAdapter extends BaseAdapter {
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
            Cell cell = convertView != null ? (Cell) convertView.getTag() : null;
            if (cell == null || cell.compare != compare) cell = new Cell(compare);
            cell.bind(shown.get(position));
            return cell.root;
        }
    }

    /** One spot in the grid: the pack icon (and when comparing, the app's own icon next to it) and the app's name. */
    private final class Cell {
        final boolean compare;
        final LinearLayout root;
        final ImageView pack, own, badge;
        final TextView label;

        Cell(boolean compare) {
            this.compare = compare;
            Context c = MainActivity.this;
            int size = dp(compare ? 44 : 56);
            int room = dp(5); // space around the icon, so the little badge can stick out of its corner

            root = new LinearLayout(c);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setGravity(Gravity.CENTER_HORIZONTAL);
            root.setPadding(dp(2), dp(8), dp(2), dp(6));
            root.setBackground(Ui.ripple(0, dp(16)));
            root.setLayoutParams(new AbsListView.LayoutParams(MATCH, WRAP));

            LinearLayout icons = new LinearLayout(c);
            FrameLayout packBox = new FrameLayout(c);
            pack = new ImageView(c);
            FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(size, size);
            plp.setMargins(room, room, room, 0);
            packBox.addView(pack, plp);
            badge = new ImageView(c);
            int bp = dp(3);
            badge.setPadding(bp, bp, bp, bp);
            packBox.addView(badge, new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.TOP | Gravity.END));
            icons.addView(packBox);
            own = new ImageView(c);
            if (compare) {
                LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(size, size);
                olp.topMargin = room;
                olp.setMarginStart(dp(3));
                olp.setMarginEnd(room);
                icons.addView(own, olp);
            }
            root.addView(icons);

            label = Ui.text(c, "", 12, Ui.TEXT, false);
            label.setGravity(Gravity.CENTER_HORIZONTAL);
            label.setMinLines(2);
            label.setMaxLines(2);
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setPadding(0, dp(4), 0, 0);
            root.addView(label, new LinearLayout.LayoutParams(MATCH, WRAP));
            root.setTag(this);
        }

        void bind(PackIndex.Entry e) {
            label.setText(e.label);
            if (e.hasPackIcon()) {
                pack.setTag(null);
                pack.setImageResource(e.resId);
            } else if (compare) {
                pack.setTag(null);
                pack.setImageDrawable(placeholder());
            } else {
                live.into(pack, e); // no pack icon yet: show the app's own
            }
            if (compare) {
                if (e.onPhone()) {
                    live.into(own, e);
                } else {
                    own.setTag(null);
                    own.setImageDrawable(placeholder());
                }
            }
            if (e.state == PackIndex.ENTRY_CHANGED) {
                badge.setVisibility(View.VISIBLE);
                badge.setImageDrawable(Ui.icon(MainActivity.this, R.drawable.ic_warning, DARK));
                badge.setBackground(badgeBackground(Ui.WARN));
            } else if (e.state == PackIndex.MISSING && !compare) {
                badge.setVisibility(View.VISIBLE);
                badge.setImageDrawable(Ui.icon(MainActivity.this, R.drawable.ic_block, Ui.TEXT));
                badge.setBackground(badgeBackground(0xFF6B6B70));
            } else {
                badge.setVisibility(View.GONE);
            }
        }
    }

    private Drawable badgeBackground(int colour) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(colour);
        g.setStroke(dp(2), previewColour());
        return g;
    }

    /** A dashed outline where an icon would go, for "no icon here". */
    private Drawable placeholder() {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.TRANSPARENT);
        g.setCornerRadius(dp(12));
        g.setStroke(Math.max(2, dp(1.5f)), Ui.TEXT_FAINT, dp(4), dp(3));
        return new InsetDrawable(g, dp(4));
    }

    // ---------------------------------------------------------------- one app, side by side

    private void showDetail(PackIndex.Entry e) {
        hideKeyboard();
        Dialog d = newSheet();
        LinearLayout box = sheetBox(d, e.label);

        TextView pkg = Ui.text(this, e.packageName, 13, Ui.TEXT_FAINT, false);
        pkg.setTextIsSelectable(true);
        box.addView(pkg);

        int statusIcon, statusColour = Ui.TEXT_DIM;
        String statusText;
        switch (e.state) {
            case PackIndex.ON_PHONE:
                statusIcon = R.drawable.ic_check;
                statusText = "In the pack, and the app is on this phone.";
                break;
            case PackIndex.NOT_INSTALLED:
                statusIcon = R.drawable.ic_info;
                statusText = "In the pack. The app isn't on this phone, so its own icon can't be shown.";
                break;
            case PackIndex.ENTRY_CHANGED:
                statusIcon = R.drawable.ic_warning;
                statusColour = Ui.WARN;
                statusText = "The app changed since this icon was made, so the pack doesn't recognise it "
                        + "and your phone shows the app's own icon. Export apps without an icon to get it fixed.";
                break;
            default:
                statusIcon = R.drawable.ic_block;
                statusText = "No icon in the pack yet. Export apps without an icon to get one made.";
        }
        box.addView(iconLine(statusIcon, statusColour, statusText));

        // The pack's icon and the app's own icon, next to each other on the background colour.
        List<GradientDrawable> previews = new ArrayList<>();
        LinearLayout panels = new LinearLayout(this);
        panels.setPadding(0, dp(16), 0, 0);
        ImageView packImg = new ImageView(this);
        if (e.hasPackIcon()) packImg.setImageResource(e.resId);
        else packImg.setImageDrawable(placeholder());
        panels.addView(panel(packImg, "Shapeless", e.hasPackIcon() ? "This pack's icon" : "No icon yet", previews),
                new LinearLayout.LayoutParams(0, MATCH, 1f));
        ImageView ownImg = new ImageView(this);
        String ownNote;
        if (e.onPhone()) {
            ownNote = "Read from the app just now";
            live.large(e, dp(96), b -> {
                if (b != null) ownImg.setImageBitmap(b);
                else ownImg.setImageDrawable(placeholder());
            });
        } else {
            ownNote = "App not on this phone";
            ownImg.setImageDrawable(placeholder());
        }
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(0, MATCH, 1f);
        olp.setMarginStart(dp(12));
        panels.addView(panel(ownImg, "App's own icon", ownNote, previews), olp);
        box.addView(panels);

        // Background choice for the comparison.
        LinearLayout bgRow = new LinearLayout(this);
        bgRow.setGravity(Gravity.CENTER_VERTICAL);
        bgRow.setPadding(dp(4), dp(14), 0, 0);
        bgRow.addView(Ui.text(this, "Background", 13, Ui.TEXT_DIM, false), new LinearLayout.LayoutParams(0, WRAP, 1f));
        GradientDrawable drawerDot = swatch(Ui.DRAWER), wallDot = swatch(Ui.WALLPAPER);
        View drawerSwatch = new View(this), wallSwatch = new View(this);
        drawerSwatch.setBackground(drawerDot);
        drawerSwatch.setContentDescription("Drawer grey");
        wallSwatch.setBackground(wallDot);
        wallSwatch.setContentDescription("Wallpaper blue");
        Runnable mark = () -> {
            ringSwatch(drawerDot, !onWallpaper);
            ringSwatch(wallDot, onWallpaper);
            for (GradientDrawable g : previews) g.setColor(previewColour());
        };
        drawerSwatch.setOnClickListener(v -> {
            setWallpaper(false);
            mark.run();
        });
        wallSwatch.setOnClickListener(v -> {
            setWallpaper(true);
            mark.run();
        });
        mark.run();
        bgRow.addView(drawerSwatch, marginLp(dp(30), dp(30), 0));
        bgRow.addView(wallSwatch, marginLp(dp(30), dp(30), dp(10)));
        box.addView(bgRow);

        // What you can do from here.
        LinearLayout buttons = new LinearLayout(this);
        buttons.setPadding(0, dp(20), 0, 0);
        if (e.component != null) {
            LinearLayout open = Ui.pillButton(this, R.drawable.ic_open, "Open app", true);
            open.setOnClickListener(v -> openApp(e));
            buttons.addView(open, marginLp(WRAP, WRAP, 0));
        }
        if (e.needsIcon()) {
            LinearLayout exp = Ui.pillButton(this, R.drawable.ic_download, "Export", e.component == null);
            exp.setOnClickListener(v -> showExportSheet());
            buttons.addView(exp, marginLp(WRAP, WRAP, buttons.getChildCount() > 0 ? dp(10) : 0));
        }
        if (buttons.getChildCount() > 0) box.addView(buttons);
        showSheet(d, box);
    }

    private LinearLayout panel(ImageView img, String title, String note, List<GradientDrawable> previews) {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setGravity(Gravity.CENTER_HORIZONTAL);
        GradientDrawable bg = Ui.rounded(previewColour(), dp(20));
        previews.add(bg);
        p.setBackground(bg);
        p.setPadding(dp(10), dp(18), dp(10), dp(14));
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        p.addView(img, new LinearLayout.LayoutParams(dp(96), dp(96)));
        TextView t = Ui.text(this, title, 14, Ui.TEXT, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(12), 0, 0);
        p.addView(t, new LinearLayout.LayoutParams(MATCH, WRAP));
        TextView n = Ui.text(this, note, 12, Ui.TEXT_DIM, false);
        n.setGravity(Gravity.CENTER);
        n.setPadding(0, dp(2), 0, 0);
        p.addView(n, new LinearLayout.LayoutParams(MATCH, WRAP));
        return p;
    }

    private GradientDrawable swatch(int colour) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(colour);
        return g;
    }

    private void ringSwatch(GradientDrawable g, boolean chosen) {
        if (chosen) g.setStroke(dp(2), Ui.TEXT);
        else g.setStroke(dp(1), Ui.FILL_STRONG);
    }

    private void openApp(PackIndex.Entry e) {
        try {
            Intent i = Intent.makeMainActivity(e.component);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(i);
            if (sheet != null) sheet.dismiss();
        } catch (Exception ex) {
            Toast.makeText(this, "Couldn't open " + e.label, Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------------------------------------------------------- export

    private void showExportSheet() {
        hideKeyboard();
        Dialog d = newSheet();
        LinearLayout box = sheetBox(d, "Export icons");
        View close = box.findViewWithTag("close");
        TextView intro = Ui.text(this, "Saves app icons as a zip file in your Downloads folder. "
                + "Send that file to Claude to get new icons made.", 14, Ui.TEXT_DIM, false);
        intro.setPadding(0, dp(2), 0, 0);
        box.addView(intro);

        int need = index == null ? -1 : index.needIcon;
        String missingNote = need == 0 ? "Every app on this phone already has an icon."
                : need > 0 ? need + (need == 1 ? " app" : " apps") + " without an icon. This is the one to send to Claude."
                : "Only the apps the pack doesn't cover yet. This is the one to send to Claude.";
        LinearLayout options = new LinearLayout(this);
        options.setOrientation(LinearLayout.VERTICAL);
        options.setPadding(0, dp(16), 0, 0);
        LinearLayout missingRow = Ui.optionRow(this, R.drawable.ic_download, "Apps without an icon", missingNote);
        LinearLayout allRow = Ui.optionRow(this, R.drawable.ic_apps, "All apps",
                "Every app on this phone, also the ones that already have an icon.");
        options.addView(missingRow);
        options.addView(Ui.spacer(this, 10));
        options.addView(allRow);
        box.addView(options);

        LinearLayout progressBox = new LinearLayout(this);
        progressBox.setOrientation(LinearLayout.VERTICAL);
        progressBox.setPadding(0, dp(18), 0, 0);
        progressBox.setVisibility(View.GONE);
        TextView status = Ui.text(this, "", 15, Ui.TEXT, false);
        progressBox.addView(status);
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setProgressTintList(ColorStateList.valueOf(Ui.ACCENT));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(Ui.FILL_STRONG));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(MATCH, dp(6));
        blp.topMargin = dp(12);
        progressBox.addView(bar, blp);
        LinearLayout doneRow = new LinearLayout(this);
        doneRow.setPadding(0, dp(18), 0, 0);
        doneRow.setVisibility(View.GONE);
        LinearLayout share = Ui.pillButton(this, R.drawable.ic_share, "Share", true);
        LinearLayout ok = Ui.pillButton(this, R.drawable.ic_check, "Done", false);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(WRAP, WRAP);
        slp.setMarginEnd(dp(10));
        doneRow.addView(share, slp);
        doneRow.addView(ok, new LinearLayout.LayoutParams(WRAP, WRAP));
        progressBox.addView(doneRow);
        box.addView(progressBox);

        ok.setOnClickListener(v -> d.dismiss());
        share.setOnClickListener(v -> {
            if (lastZip != null) startActivity(Exporter.shareIntent(lastZip));
        });

        View.OnClickListener run = v -> {
            boolean onlyMissing = v == missingRow;
            options.setVisibility(View.GONE);
            close.setVisibility(View.INVISIBLE);
            progressBox.setVisibility(View.VISIBLE);
            d.setCancelable(false);
            status.setText("Getting ready…");
            Exporter.start(this, onlyMissing, new Exporter.Listener() {
                @Override
                public void onProgress(int done, int total) {
                    bar.setMax(total);
                    bar.setProgress(done);
                    status.setText("Checking app " + done + " of " + total);
                }

                @Override
                public void onDone(Uri zip, int count, String message) {
                    lastZip = zip;
                    status.setText(message);
                    bar.setProgress(bar.getMax());
                    share.setVisibility(zip != null && count > 0 ? View.VISIBLE : View.GONE);
                    wrapUp();
                }

                @Override
                public void onError(String message) {
                    status.setText(message);
                    share.setVisibility(View.GONE);
                    wrapUp();
                }

                private void wrapUp() {
                    doneRow.setVisibility(View.VISIBLE);
                    close.setVisibility(View.VISIBLE);
                    d.setCancelable(true);
                }
            });
        };
        if (need == 0) {
            missingRow.setClickable(false);
            missingRow.setAlpha(0.45f);
        } else {
            missingRow.setOnClickListener(run);
        }
        allRow.setOnClickListener(run);
        showSheet(d, box);
    }

    // ---------------------------------------------------------------- help

    private void showInfoSheet() {
        hideKeyboard();
        Dialog d = newSheet();
        LinearLayout box = sheetBox(d, "How it works");
        box.addView(step(R.drawable.ic_apps, Ui.TEXT, "Use the pack",
                "In your launcher's settings, pick Shapeless Icons as the icon pack."));
        box.addView(step(R.drawable.ic_search, Ui.TEXT, "Find icons",
                "Type an app's name in the search bar. The buttons under it show all icons, only the apps on this phone, or the apps still missing an icon."));
        box.addView(step(R.drawable.ic_compare, Ui.TEXT, "Compare",
                "Tap an icon to see it next to the app's own icon. The compare button does this for every app at once. "
                        + "The app's own icon is read from the app itself, so it's always the current one."));
        box.addView(step(R.drawable.ic_palette, Ui.TEXT, "Background",
                "Switches between the drawer grey and the wallpaper blue, to check icons on both."));
        box.addView(step(R.drawable.ic_warning, Ui.WARN, "Orange mark",
                "The app changed since its icon was made, so the pack doesn't recognise it right now."));
        box.addView(step(R.drawable.ic_download, Ui.TEXT, "Missing icons",
                "Tap Export, pick Apps without an icon, then send the zip to Claude."));
        TextView version = Ui.text(this, "Version " + Exporter.versionName(this)
                + (index != null ? " · " + index.packIcons + " icons" : ""), 12, Ui.TEXT_FAINT, false);
        version.setPadding(0, dp(20), 0, 0);
        box.addView(version);
        showSheet(d, box);
    }

    private View step(int icon, int colour, String title, String body) {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, dp(14), 0, 0);
        ImageView i = new ImageView(this);
        i.setImageDrawable(Ui.icon(this, icon, colour));
        i.setBackground(Ui.rounded(Ui.FILL, dp(18)));
        int p = dp(8);
        i.setPadding(p, p, p, p);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(36), dp(36));
        ilp.setMarginEnd(dp(14));
        row.addView(i, ilp);
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(Ui.text(this, title, 15, Ui.TEXT, true));
        TextView b = Ui.text(this, body, 13, Ui.TEXT_DIM, false);
        b.setPadding(0, dp(2), 0, 0);
        texts.addView(b);
        row.addView(texts, new LinearLayout.LayoutParams(0, WRAP, 1f));
        return row;
    }

    // ---------------------------------------------------------------- pop-up sheets

    /** A pop-up panel that slides up from the bottom of the screen. Only one is open at a time. */
    private Dialog newSheet() {
        if (sheet != null) sheet.dismiss();
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setGravity(Gravity.BOTTOM);
            w.setWindowAnimations(android.R.style.Animation_InputMethod);
        }
        d.setCanceledOnTouchOutside(true);
        d.setOnDismissListener(x -> {
            if (sheet == d) sheet = null;
        });
        sheet = d;
        return d;
    }

    /** The inside of a sheet: a grab handle, the title and a close button. The rest goes below. */
    private LinearLayout sheetBox(Dialog d, String title) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(10), dp(20), dp(24));

        View handle = new View(this);
        handle.setBackground(Ui.rounded(Ui.FILL_STRONG, dp(2)));
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(dp(36), dp(4));
        hlp.gravity = Gravity.CENTER_HORIZONTAL;
        hlp.bottomMargin = dp(8);
        box.addView(handle, hlp);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = Ui.text(this, title, 20, Ui.TEXT, true);
        top.addView(t, new LinearLayout.LayoutParams(0, WRAP, 1f));
        ImageButton close = Ui.iconButton(this, R.drawable.ic_close, "Close");
        close.setTag("close");
        close.setOnClickListener(v -> d.dismiss());
        top.addView(close, marginLp(dp(40), dp(40), dp(8)));
        box.addView(top);
        return box;
    }

    private void showSheet(Dialog d, View content) {
        ScrollView scroll = new ScrollView(this);
        float r = dp(28);
        scroll.setBackground(Ui.rounded(Ui.SHEET, r, r, 0, 0));
        scroll.addView(content);
        d.setContentView(scroll);
        d.show();
        Window w = d.getWindow();
        if (w != null) w.setLayout(MATCH, WRAP);
    }

    private View iconLine(int icon, int colour, String text) {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, dp(12), 0, 0);
        ImageView i = new ImageView(this);
        i.setImageDrawable(Ui.icon(this, icon, colour));
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(18), dp(18));
        ilp.setMarginEnd(dp(10));
        ilp.topMargin = dp(1);
        row.addView(i, ilp);
        row.addView(Ui.text(this, text, 14, Ui.TEXT_DIM, false), new LinearLayout.LayoutParams(0, WRAP, 1f));
        return row;
    }

    // ---------------------------------------------------------------- small helpers

    private int dp(float v) {
        return Ui.dp(this, v);
    }

    private static LinearLayout.LayoutParams marginLp(int w, int h, int marginStart) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
        lp.setMarginStart(marginStart);
        return lp;
    }
}
