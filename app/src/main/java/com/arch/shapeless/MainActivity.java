package com.arch.shapeless;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
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
import android.view.animation.DecelerateInterpolator;
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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The app's only screen. It has two jobs:
 *  1. Being the icon pack (launchers find it through the manifest, nothing to do here).
 *  2. Showing every icon in the pack under a search bar, next to each app's own icon when asked,
 *     and exporting app icons as a zip so new icons can be made.
 *  3. Picking apps (select mode) and copying their names, one per line, to paste in a chat.
 *
 * Background: the drawer grey, or see-through so the phone's real wallpaper shows behind the icons
 * (the window always lets the wallpaper through; in drawer mode the screen is painted grey on top).
 */
public class MainActivity extends Activity {

    private static final int SHOW_ALL = 0, SHOW_ON_PHONE = 1, SHOW_MISSING = 2;
    private static final int DARK = 0xFF1C1C1E;      // icons and text on white buttons
    /** See-through mode: light darkening behind the status bar and the top controls, so they stay readable on any wallpaper. */
    private static final int SCRIM = 0x4D000000;
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

    private FrameLayout frame;      // the whole screen
    private LinearLayout content;   // title, search, buttons and the icon list
    private LinearLayout top;       // everything above the icon list
    private LinearLayout header;    // title row (swapped for the selection bar while picking apps)
    private TextView title, subtitle, compareHint, emptyText;
    private ImageView emptyIcon;
    private LinearLayout searchPill, exportButton;
    private EditText search;
    private ImageButton helpButton, clearButton, compareButton, colourButton;
    private final TextView[] chips = new TextView[3];
    private GridView grid;
    private IconAdapter adapter;

    // One app's close-up, shown over everything else.
    private FrameLayout detail;
    private PackIndex.Entry detailEntry;
    private FrameLayout detailStage;
    private final List<TextView> detailTexts = new ArrayList<>(), detailNotes = new ArrayList<>();
    private TextView[] detailBgChips;

    // Picking apps to copy.
    private boolean selecting;
    private final Set<String> selected = new HashSet<>();
    private LinearLayout selectBar, fab;
    private TextView selectCount, selectAllButton, fabText, flash;
    private ImageView fabIcon;
    private ImageButton copySettingsButton;

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

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (detailEntry != null) {
            hideDetail();
            return;
        }
        if (selecting) {
            setSelecting(false);
            return;
        }
        super.onBackPressed();
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
        frame = new FrameLayout(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setFocusableInTouchMode(true); // lets the search bar give up focus
        top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        int side = dp(16);

        // Title with the export and help buttons, and the counts under it.
        header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(side + dp(4), dp(16), side, 0);
        title = Ui.text(this, "Shapeless Icons", 22, Ui.TEXT, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        header.addView(title, new LinearLayout.LayoutParams(0, WRAP, 1f));

        exportButton = Ui.pillButton(this, R.drawable.ic_download, "Export", false);
        exportButton.setContentDescription("Export icons");
        exportButton.setOnClickListener(v -> showExportSheet());
        header.addView(exportButton, marginLp(WRAP, dp(44), dp(8)));
        helpButton = Ui.iconButton(this, R.drawable.ic_info, "How it works");
        helpButton.setOnClickListener(v -> showInfoSheet());
        header.addView(helpButton, marginLp(dp(44), dp(44), dp(8)));
        top.addView(header);
        subtitle = Ui.text(this, "Loading…", 13, Ui.TEXT_DIM, false);
        subtitle.setPadding(side + dp(4), 0, side, 0);
        top.addView(subtitle);

        // While picking apps: how many are picked, select all, and what to copy.
        selectBar = new LinearLayout(this);
        selectBar.setGravity(Gravity.CENTER_VERTICAL);
        selectBar.setPadding(side - dp(2), dp(16), side, dp(18));
        selectBar.setVisibility(View.GONE);
        ImageButton stop = Ui.iconButton(this, R.drawable.ic_close, "Stop selecting");
        stop.setOnClickListener(v -> setSelecting(false));
        selectBar.addView(stop, new LinearLayout.LayoutParams(dp(44), dp(44)));
        selectCount = Ui.text(this, "", 20, Ui.TEXT, true);
        selectCount.setSingleLine(true);
        selectCount.setPadding(dp(10), 0, 0, 0);
        selectBar.addView(selectCount, new LinearLayout.LayoutParams(0, WRAP, 1f));
        selectAllButton = Ui.text(this, "Select all", 14, Ui.TEXT, true);
        selectAllButton.setGravity(Gravity.CENTER);
        selectAllButton.setPadding(dp(16), 0, dp(16), 0);
        selectAllButton.setOnClickListener(v -> selectAllShown());
        selectBar.addView(selectAllButton, marginLp(WRAP, dp(40), dp(8)));
        copySettingsButton = Ui.iconButton(this, R.drawable.ic_settings, "What to copy");
        copySettingsButton.setOnClickListener(v -> showCopySettings());
        selectBar.addView(copySettingsButton, marginLp(dp(44), dp(44), dp(8)));
        top.addView(selectBar);

        // Search bar, with the compare and background buttons next to it.
        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        searchRow.setPadding(side, dp(16), side, 0);

        searchPill = new LinearLayout(this);
        searchPill.setGravity(Gravity.CENTER_VERTICAL);
        searchPill.setPadding(dp(14), 0, dp(4), 0);
        ImageView glass = new ImageView(this);
        glass.setImageDrawable(Ui.icon(this, R.drawable.ic_search, Ui.TEXT_DIM));
        searchPill.addView(glass, new LinearLayout.LayoutParams(dp(22), dp(22)));

        search = new EditText(this);
        search.setBackground(null);
        search.setHint("Search icons");
        search.setHintTextColor(Ui.TEXT_DIM);
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
        searchPill.addView(search, new LinearLayout.LayoutParams(0, dp(48), 1f));
        searchPill.setOnClickListener(v -> showKeyboard());

        clearButton = new ImageButton(this);
        clearButton.setImageDrawable(Ui.icon(this, R.drawable.ic_close, Ui.TEXT_DIM));
        clearButton.setBackground(Ui.ripple(0, dp(20)));
        clearButton.setContentDescription("Clear search");
        clearButton.setVisibility(View.GONE);
        clearButton.setOnClickListener(v -> search.setText(""));
        searchPill.addView(clearButton, new LinearLayout.LayoutParams(dp(40), dp(40)));
        searchRow.addView(searchPill, new LinearLayout.LayoutParams(0, dp(48), 1f));

        compareButton = Ui.iconButton(this, R.drawable.ic_compare, "Compare with each app's own icon");
        compareButton.setOnClickListener(v -> {
            compare = !compare;
            prefs.edit().putBoolean("compare", compare).apply();
            applyCompare();
        });
        searchRow.addView(compareButton, marginLp(dp(48), dp(48), dp(8)));
        colourButton = Ui.iconButton(this, R.drawable.ic_palette, "Background: drawer grey or your wallpaper");
        colourButton.setOnClickListener(v -> setWallpaper(!onWallpaper));
        searchRow.addView(colourButton, marginLp(dp(48), dp(48), dp(8)));
        top.addView(searchRow);

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
        top.addView(chipScroll);

        compareHint = Ui.text(this, "Left: Shapeless · right: the app's own icon", 12, Ui.TEXT_DIM, false);
        compareHint.setPadding(side + dp(4), dp(6), side, dp(2));
        top.addView(compareHint);
        content.addView(top);

        // The icons.
        FrameLayout gridBox = new FrameLayout(this);
        grid = new GridView(this);
        grid.setNumColumns(GridView.AUTO_FIT);
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setVerticalSpacing(dp(2));
        grid.setSelector(new ColorDrawable(Color.TRANSPARENT));
        grid.setCacheColorHint(Color.TRANSPARENT); // the background can be see-through
        grid.setClipToPadding(false);
        grid.setPadding(dp(10), dp(6), dp(10), dp(96)); // room to scroll the last row above the floating button
        grid.setFastScrollEnabled(true);
        adapter = new IconAdapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((parent, view, position, id) -> {
            PackIndex.Entry e = shown.get(position);
            if (selecting) toggle(e);
            else showDetail(e);
        });
        grid.setOnItemLongClickListener((parent, view, position, id) -> {
            hideKeyboard();
            if (!selecting) setSelecting(true);
            toggle(shown.get(position));
            return true;
        });
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
        content.addView(gridBox, new LinearLayout.LayoutParams(MATCH, 0, 1f));
        frame.addView(content, new FrameLayout.LayoutParams(MATCH, MATCH));

        // Floating button, bottom right: "Select" to start picking apps, then a check that copies them.
        fab = new LinearLayout(this);
        fab.setGravity(Gravity.CENTER);
        fab.setElevation(dp(6));
        fab.setClickable(true);
        fab.setFocusable(true);
        fabIcon = new ImageView(this);
        fab.addView(fabIcon, new LinearLayout.LayoutParams(dp(24), dp(24)));
        fabText = Ui.text(this, "Select", 15, Ui.TEXT, true);
        fab.addView(fabText, marginLp(WRAP, WRAP, dp(10)));
        fab.setOnClickListener(v -> {
            if (selecting) copySelection();
            else setSelecting(true);
        });
        FrameLayout.LayoutParams fablp = new FrameLayout.LayoutParams(WRAP, dp(56), Gravity.BOTTOM | Gravity.END);
        fablp.setMargins(0, 0, dp(20), dp(20));
        frame.addView(fab, fablp);

        flash = Ui.text(this, "", 14, Ui.TEXT, true);
        flash.setBackground(Ui.rounded(0xF2232325, dp(18)));
        flash.setPadding(dp(16), dp(10), dp(16), dp(10));
        flash.setElevation(dp(6));
        flash.setVisibility(View.GONE);
        FrameLayout.LayoutParams flashlp = new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM | Gravity.END);
        flashlp.setMargins(dp(20), 0, dp(20), dp(88));
        frame.addView(flash, flashlp);
        styleFab();

        detail = new FrameLayout(this);
        detail.setVisibility(View.GONE);
        detail.setClickable(true); // nothing behind it reacts while it's open
        frame.addView(detail, new FrameLayout.LayoutParams(MATCH, MATCH));

        setContentView(frame);
        content.requestFocus();
    }

    private void setWallpaper(boolean on) {
        onWallpaper = on;
        prefs.edit().putBoolean("wallpaper", on).apply();
        applyBackground();
    }

    private void applyBackground() {
        // The window always lets the wallpaper through; drawer mode paints the screen grey on top of it.
        frame.setBackgroundColor(onWallpaper ? Color.TRANSPARENT : Ui.DRAWER);
        Window w = getWindow();
        w.setStatusBarColor(onWallpaper ? SCRIM : Ui.DRAWER);
        w.setNavigationBarColor(onWallpaper ? Color.TRANSPARENT : Ui.DRAWER);
        restyle();
        adapter.notifyDataSetChanged(); // labels and badges look a little different on the wallpaper
    }

    /** Colours that depend on the background: on the wallpaper, controls get a darker see-through fill and text a soft shadow. */
    private void restyle() {
        top.setBackground(onWallpaper ? fade() : null);
        searchPill.setBackground(surface(dp(24), false));
        exportButton.setBackground(surface(dp(22), false));
        helpButton.setBackground(surface(dp(22), false));
        colourButton.setBackground(surface(dp(24), false));
        selectAllButton.setBackground(surface(dp(20), false));
        copySettingsButton.setBackground(surface(dp(22), false));
        shadow(selectCount);
        styleCompareButton();
        styleChips();
        shadow(title);
        soft(subtitle);
        soft(compareHint);
        soft(emptyText);
        if (detailStage != null) detailStage.setBackground(onWallpaper ? fade() : null);
        for (TextView t : detailTexts) shadow(t);
        for (TextView t : detailNotes) soft(t);
        if (detailBgChips != null) {
            for (int i = 0; i < detailBgChips.length; i++) {
                boolean on = (i == 1) == onWallpaper;
                detailBgChips[i].setBackground(Ui.ripple(on ? Ui.ACCENT : Ui.FILL, dp(17)));
                detailBgChips[i].setTextColor(on ? DARK : Ui.TEXT);
            }
        }
    }

    /** Background for a button or the search bar. "on" means switched on: white. */
    private Drawable surface(float radius, boolean on) {
        if (on) return Ui.ripple(Ui.ACCENT, radius);
        if (!onWallpaper) return Ui.ripple(Ui.FILL, radius);
        GradientDrawable fill = Ui.rounded(0x52000000, radius);
        fill.setStroke(Math.max(1, dp(1)), 0x2EFFFFFF);
        return new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), fill, Ui.rounded(Color.WHITE, radius));
    }

    /** Darkening at the top that fades out, carrying on from the status bar's. */
    private static Drawable fade() {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{SCRIM, 0x00000000});
    }

    /** On the wallpaper, white text gets a dark shadow so it stays readable on light wallpapers too. */
    private void shadow(TextView t) {
        if (onWallpaper) t.setShadowLayer(dp(3), 0, dp(1), 0xD9000000);
        else t.setShadowLayer(0, 0, 0, 0);
    }

    /** Text that is dimmed on the drawer: full white with a shadow on the wallpaper. */
    private void soft(TextView t) {
        t.setTextColor(onWallpaper ? Ui.TEXT : Ui.TEXT_DIM);
        shadow(t);
    }

    private void styleCompareButton() {
        compareButton.setImageDrawable(Ui.icon(this, R.drawable.ic_compare, compare ? DARK : Ui.TEXT));
        compareButton.setBackground(surface(dp(24), compare));
    }

    private void styleChips() {
        for (int i = 0; i < chips.length; i++) {
            boolean on = i == filter;
            chips[i].setBackground(surface(dp(18), on));
            chips[i].setTextColor(on ? DARK : Ui.TEXT);
        }
    }

    private void applyCompare() {
        styleCompareButton();
        compareHint.setVisibility(compare ? View.VISIBLE : View.GONE);
        grid.setColumnWidth(dp(compare ? 110 : 70));
        adapter.notifyDataSetChanged();
    }

    private void applyFilter() {
        styleChips();
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
        updateSelectBar();
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
        emptyIcon.setImageDrawable(icon == 0 ? null : Ui.icon(this, icon, Ui.TEXT_DIM));
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
        content.requestFocus();
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
        final ImageView pack, own, badge, tick;
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
            tick = new ImageView(c);
            int tp = dp(3);
            tick.setPadding(tp, tp, tp, tp);
            packBox.addView(tick, new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.TOP | Gravity.START));
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
            shadow(label);
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
            if (selecting) {
                boolean on = selected.contains(e.key);
                tick.setVisibility(View.VISIBLE);
                tick.setImageDrawable(on ? Ui.icon(MainActivity.this, R.drawable.ic_done, DARK) : null);
                tick.setBackground(tickBackground(on));
                root.setBackground(Ui.ripple(on ? 0x38FFFFFF : 0, dp(16)));
            } else {
                tick.setVisibility(View.GONE);
                root.setBackground(Ui.ripple(0, dp(16)));
            }
        }
    }

    /** The circle in a cell's corner while picking apps: white with a check when picked, an empty ring when not. */
    private Drawable tickBackground(boolean on) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        if (on) {
            g.setColor(Ui.ACCENT);
            g.setStroke(dp(2), onWallpaper ? DARK : Ui.DRAWER);
        } else {
            g.setColor(0x4D000000);
            g.setStroke(dp(2), 0xE6FFFFFF);
        }
        return g;
    }

    private Drawable badgeBackground(int colour) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(colour);
        g.setStroke(dp(2), onWallpaper ? DARK : Ui.DRAWER);
        return g;
    }

    /** A dashed outline where an icon would go, for "no icon here". */
    private Drawable placeholder() {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.TRANSPARENT);
        g.setCornerRadius(dp(12));
        g.setStroke(Math.max(2, dp(1.5f)), Ui.TEXT_DIM, dp(4), dp(3));
        return new InsetDrawable(g, dp(4));
    }

    // ---------------------------------------------------------------- one app, close up

    /**
     * Shows one app: the pack's icon and the app's own icon big, straight on the chosen background
     * (so on the real wallpaper in see-through mode), with the details in a panel below.
     */
    private void showDetail(PackIndex.Entry e) {
        hideKeyboard();
        if (sheet != null) sheet.dismiss();
        detailEntry = e;
        fab.setVisibility(View.INVISIBLE);
        flash.setVisibility(View.GONE);
        detail.animate().cancel();
        content.animate().cancel();
        detail.removeAllViews();
        detailTexts.clear();
        detailNotes.clear();

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);

        // The two icons, side by side on the background. Tapping the empty space closes.
        detailStage = new FrameLayout(this);
        detailStage.setOnClickListener(v -> hideDetail());
        LinearLayout pair = new LinearLayout(this);
        pair.setGravity(Gravity.TOP);
        ImageView packImg = new ImageView(this);
        if (e.hasPackIcon()) packImg.setImageResource(e.resId);
        else packImg.setImageDrawable(placeholder());
        pair.addView(stageColumn(packImg, "Shapeless", e.hasPackIcon() ? "This pack's icon" : "No icon yet"));
        ImageView ownImg = new ImageView(this);
        String ownNote;
        if (e.onPhone()) {
            ownNote = "Read from the app just now";
            live.large(e, dp(120), b -> {
                if (b != null) ownImg.setImageBitmap(b);
                else ownImg.setImageDrawable(placeholder());
            });
        } else {
            ownNote = "App not on this phone";
            ownImg.setImageDrawable(placeholder());
        }
        pair.addView(stageColumn(ownImg, "App's own icon", ownNote), marginLp(WRAP, WRAP, dp(24)));
        detailStage.addView(pair, new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER));
        // Smaller icons when there isn't much room (phone on its side).
        detailStage.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            int size = Math.max(dp(48), Math.min(dp(120), (b - t) - dp(120)));
            if (packImg.getLayoutParams().width == size) return;
            v.post(() -> {
                for (ImageView iv : new ImageView[]{packImg, ownImg}) {
                    ViewGroup.LayoutParams lp = iv.getLayoutParams();
                    lp.width = size;
                    lp.height = size;
                    iv.setLayoutParams(lp);
                }
            });
        });
        column.addView(detailStage, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        // The panel with the details.
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setClickable(true);
        float r = dp(28);
        card.setBackground(Ui.rounded(Ui.SHEET, r, r, 0, 0));
        card.setPadding(dp(20), dp(10), dp(20), dp(24));
        addSheetTop(card, e.label, v -> hideDetail());

        TextView pkg = Ui.text(this, e.packageName, 13, Ui.TEXT_FAINT, false);
        pkg.setTextIsSelectable(true);
        card.addView(pkg);

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
        card.addView(iconLine(statusIcon, statusColour, statusText));

        // Background choice for the close-up (and the list).
        LinearLayout bgRow = new LinearLayout(this);
        bgRow.setGravity(Gravity.CENTER_VERTICAL);
        bgRow.setPadding(0, dp(16), 0, 0);
        bgRow.addView(Ui.text(this, "Background", 14, Ui.TEXT_DIM, false), new LinearLayout.LayoutParams(0, WRAP, 1f));
        String[] bgNames = {"Drawer", "Wallpaper"};
        detailBgChips = new TextView[2];
        for (int i = 0; i < 2; i++) {
            TextView chip = Ui.text(this, bgNames[i], 14, Ui.TEXT, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(14), 0, dp(14), 0);
            final boolean wall = i == 1;
            chip.setOnClickListener(v -> setWallpaper(wall));
            bgRow.addView(chip, marginLp(WRAP, dp(34), i == 0 ? 0 : dp(8)));
            detailBgChips[i] = chip;
        }
        card.addView(bgRow);

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
            exp.setOnClickListener(v -> {
                closeDetailNow();
                showExportSheet();
            });
            buttons.addView(exp, marginLp(WRAP, WRAP, buttons.getChildCount() > 0 ? dp(10) : 0));
        }
        if (buttons.getChildCount() > 0) card.addView(buttons);
        column.addView(card, new LinearLayout.LayoutParams(MATCH, WRAP));
        detail.addView(column, new FrameLayout.LayoutParams(MATCH, MATCH));
        restyle();

        // Fade the list out and the close-up in, with the panel sliding up.
        detail.setVisibility(View.VISIBLE);
        detail.setAlpha(0f);
        detail.animate().alpha(1f).setDuration(180).start();
        content.animate().alpha(0f).setDuration(180).withEndAction(() -> content.setVisibility(View.INVISIBLE)).start();
        card.setTranslationY(dp(80));
        card.animate().translationY(0).setDuration(260).setInterpolator(new DecelerateInterpolator(2f)).start();
    }

    private LinearLayout stageColumn(ImageView img, String title, String note) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        c.addView(img, new LinearLayout.LayoutParams(dp(120), dp(120)));
        TextView t = Ui.text(this, title, 15, Ui.TEXT, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(14), 0, 0);
        c.addView(t, new LinearLayout.LayoutParams(WRAP, WRAP));
        TextView n = Ui.text(this, note, 12, Ui.TEXT_DIM, false);
        n.setGravity(Gravity.CENTER);
        n.setMaxWidth(dp(150));
        n.setPadding(0, dp(2), 0, 0);
        c.addView(n, new LinearLayout.LayoutParams(WRAP, WRAP));
        detailTexts.add(t);
        detailNotes.add(n);
        return c;
    }

    private void hideDetail() {
        if (detailEntry == null) return;
        detailEntry = null;
        fab.setVisibility(View.VISIBLE);
        content.animate().cancel();
        content.setVisibility(View.VISIBLE);
        content.animate().alpha(1f).setDuration(180).start();
        detail.animate().cancel();
        detail.animate().alpha(0f).setDuration(160).withEndAction(this::clearDetail).start();
    }

    private void closeDetailNow() {
        detailEntry = null;
        fab.setVisibility(View.VISIBLE);
        content.animate().cancel();
        detail.animate().cancel();
        content.setVisibility(View.VISIBLE);
        content.setAlpha(1f);
        clearDetail();
    }

    private void clearDetail() {
        if (detailEntry != null) return; // opened again meanwhile
        detail.setVisibility(View.GONE);
        detail.removeAllViews();
        detailStage = null;
        detailTexts.clear();
        detailNotes.clear();
        detailBgChips = null;
    }

    private void openApp(PackIndex.Entry e) {
        try {
            Intent i = Intent.makeMainActivity(e.component);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(i);
            closeDetailNow();
        } catch (Exception ex) {
            Toast.makeText(this, "Couldn't open " + e.label, Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------------------------------------------------------- picking apps and copying them

    private void setSelecting(boolean on) {
        if (selecting == on) return;
        selecting = on;
        if (!on) selected.clear();
        header.setVisibility(on ? View.GONE : View.VISIBLE);
        subtitle.setVisibility(on ? View.GONE : View.VISIBLE);
        selectBar.setVisibility(on ? View.VISIBLE : View.GONE);
        updateSelectBar();
        adapter.notifyDataSetChanged();
        fab.setScaleX(0.85f);
        fab.setScaleY(0.85f);
        fab.animate().scaleX(1f).scaleY(1f).setDuration(160).start();
        if (on && !prefs.getBoolean("select_hint_seen", false)) {
            prefs.edit().putBoolean("select_hint_seen", true).apply();
            flash("Tap apps to pick them, then tap the check to copy");
        }
    }

    private void toggle(PackIndex.Entry e) {
        if (!selected.remove(e.key)) selected.add(e.key);
        updateSelectBar();
        adapter.notifyDataSetChanged();
    }

    /** Picks every app the list shows right now (after search and filter); if they're all picked already, clears. */
    private void selectAllShown() {
        if (allShownSelected()) selected.clear();
        else for (PackIndex.Entry e : shown) selected.add(e.key);
        updateSelectBar();
        adapter.notifyDataSetChanged();
    }

    private boolean allShownSelected() {
        if (shown.isEmpty()) return false;
        for (PackIndex.Entry e : shown) if (!selected.contains(e.key)) return false;
        return true;
    }

    private void updateSelectBar() {
        int n = selected.size();
        selectCount.setText(n == 0 ? "Pick apps" : n + " selected");
        selectAllButton.setText(allShownSelected() ? "Clear" : "Select all");
        styleFab();
    }

    private void styleFab() {
        if (selecting) {
            boolean any = !selected.isEmpty(); // white once something is picked, grey before
            fabIcon.setImageDrawable(Ui.icon(this, R.drawable.ic_done, any ? DARK : 0x99FFFFFF));
            fabText.setVisibility(View.GONE);
            fab.setPadding(dp(16), 0, dp(16), 0); // 24 + 2 x 16 = 56: a circle
            fab.setBackground(Ui.ripple(any ? Ui.ACCENT : 0xFF4A4A4E, dp(28)));
            fab.setContentDescription("Copy the picked apps");
            fab.setTooltipText("Copy the picked apps");
        } else {
            fabIcon.setImageDrawable(Ui.icon(this, R.drawable.ic_checklist, Ui.TEXT));
            fabText.setVisibility(View.VISIBLE);
            fab.setPadding(dp(18), 0, dp(22), 0);
            fab.setBackground(Ui.ripple(0xFF3A3A3D, dp(28)));
            fab.setContentDescription("Select apps");
            fab.setTooltipText("Select apps");
        }
    }

    private final Runnable hideFlash = () -> flash.animate().alpha(0f).setDuration(250)
            .withEndAction(() -> flash.setVisibility(View.GONE)).start();

    /** A short message above the floating button that fades away by itself. */
    private void flash(String message) {
        flash.removeCallbacks(hideFlash);
        flash.animate().cancel();
        flash.setText(message);
        flash.setVisibility(View.VISIBLE);
        flash.setAlpha(0f);
        flash.animate().alpha(1f).setDuration(150).start();
        flash.postDelayed(hideFlash, 2200);
    }

    /** The picked apps, in list order. */
    private List<PackIndex.Entry> selectedEntries() {
        List<PackIndex.Entry> out = new ArrayList<>();
        if (index == null) return out;
        for (PackIndex.Entry e : index.entries) if (selected.contains(e.key)) out.add(e);
        return out;
    }

    /**
     * Copies the picked apps, one per line: names, package names or both, as set with the gear.
     * When an icon is switched on there too, the share menu also opens with a picture of the icons.
     */
    private void copySelection() {
        List<PackIndex.Entry> picked = selectedEntries();
        if (picked.isEmpty()) {
            flash("Pick some apps first");
            return;
        }
        boolean name = prefs.getBoolean("copy_name", true), pkg = prefs.getBoolean("copy_pkg", false);
        boolean packIcon = prefs.getBoolean("copy_pack", false), ownIcon = prefs.getBoolean("copy_own", false);
        if (!name && !pkg && !packIcon && !ownIcon) name = true;

        StringBuilder sb = new StringBuilder();
        if (name || pkg) {
            for (PackIndex.Entry e : picked) {
                if (sb.length() > 0) sb.append('\n');
                if (name && pkg) sb.append(e.label).append(" (").append(e.packageName).append(')');
                else if (name) sb.append(e.label);
                else sb.append(e.packageName);
            }
        }
        final String text = sb.toString();
        int n = picked.size();
        String apps = n + (n == 1 ? " app" : " apps");
        if (!text.isEmpty()) {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Apps", text));
        }
        if (!packIcon && !ownIcon) {
            flash("Copied " + apps);
            return;
        }
        flash(text.isEmpty() ? "Making the picture…" : "Copied " + apps + ", making the picture…");
        final Context app = getApplicationContext();
        final boolean withPkg = pkg;
        new Thread(() -> {
            ArrayList<Uri> pictures;
            try {
                pictures = IconSheet.make(app, picked, packIcon, ownIcon, withPkg);
            } catch (Exception | OutOfMemoryError ex) {
                pictures = null;
            }
            final ArrayList<Uri> result = pictures;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                if (result == null || result.isEmpty()) {
                    flash("Couldn't make the picture");
                    return;
                }
                shareIcons(result, text);
            });
        }).start();
    }

    private void shareIcons(ArrayList<Uri> pictures, String text) {
        Intent send = new Intent(pictures.size() == 1 ? Intent.ACTION_SEND : Intent.ACTION_SEND_MULTIPLE);
        send.setType("image/png");
        if (pictures.size() == 1) send.putExtra(Intent.EXTRA_STREAM, pictures.get(0));
        else send.putParcelableArrayListExtra(Intent.EXTRA_STREAM, pictures);
        if (!text.isEmpty()) send.putExtra(Intent.EXTRA_TEXT, text);
        ClipData clip = ClipData.newRawUri("Icons", pictures.get(0));
        for (int i = 1; i < pictures.size(); i++) clip.addItem(new ClipData.Item(pictures.get(i)));
        send.setClipData(clip);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(send, "Send the icons"));
        } catch (Exception ex) {
            flash("No app to send it to");
        }
    }

    /** What the check button copies. */
    private void showCopySettings() {
        Dialog d = newSheet();
        LinearLayout box = sheetBox(d, "What to copy");
        TextView intro = Ui.text(this, "Each picked app goes on its own line.", 14, Ui.TEXT_DIM, false);
        intro.setPadding(0, dp(2), 0, dp(4));
        box.addView(intro);
        box.addView(toggleRow("App name", "Spotify", "copy_name", true));
        box.addView(toggleRow("Package name", "com.spotify.music", "copy_pkg", false));
        box.addView(toggleRow("Shapeless icon", "The icon from this pack", "copy_pack", false));
        box.addView(toggleRow("App's own icon", "The app's default icon", "copy_own", false));
        TextView note = Ui.text(this, "Icons can't go on the clipboard together with text. With an icon switched on, "
                + "the names are still copied, and the share menu opens with one picture of all the icons: pick Claude there.",
                13, Ui.TEXT_DIM, false);
        note.setPadding(0, dp(14), 0, 0);
        box.addView(note);
        showSheet(d, box);
    }

    private View toggleRow(String title, String example, String key, boolean byDefault) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(4), dp(10), dp(2), dp(10));
        row.setBackground(Ui.ripple(0, dp(14)));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(Ui.text(this, title, 16, Ui.TEXT, true));
        TextView ex = Ui.text(this, example, 13, Ui.TEXT_FAINT, false);
        ex.setPadding(0, dp(2), 0, 0);
        texts.addView(ex);
        row.addView(texts, new LinearLayout.LayoutParams(0, WRAP, 1f));
        Switch sw = new Switch(this);
        int[][] states = {{android.R.attr.state_checked}, {}};
        sw.setThumbTintList(new ColorStateList(states, new int[]{Ui.ACCENT, 0xFFB8B8BC}));
        sw.setTrackTintList(new ColorStateList(states, new int[]{0x99FFFFFF, 0x33FFFFFF}));
        sw.setChecked(prefs.getBoolean(key, byDefault));
        sw.setOnCheckedChangeListener((b, on) -> prefs.edit().putBoolean(key, on).apply());
        row.addView(sw);
        row.setOnClickListener(v -> sw.toggle());
        return row;
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
                "Switches between the drawer grey and your wallpaper: the app turns see-through, so you see the icons on your real wallpaper."));
        box.addView(step(R.drawable.ic_checklist, Ui.TEXT, "Pick and copy",
                "Tap Select (bottom right), or long-press an icon, then tap the apps you want. The check button copies "
                        + "their names, one per line, ready to paste in a chat. The gear sets what's copied: names, package names, icons."));
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
        addSheetTop(box, title, v -> d.dismiss());
        return box;
    }

    /** Grab handle, title and close button at the top of a panel. */
    private void addSheetTop(LinearLayout box, String title, View.OnClickListener onClose) {
        View handle = new View(this);
        handle.setBackground(Ui.rounded(Ui.FILL_STRONG, dp(2)));
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(dp(36), dp(4));
        hlp.gravity = Gravity.CENTER_HORIZONTAL;
        hlp.bottomMargin = dp(8);
        box.addView(handle, hlp);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = Ui.text(this, title, 20, Ui.TEXT, true);
        row.addView(t, new LinearLayout.LayoutParams(0, WRAP, 1f));
        ImageButton close = Ui.iconButton(this, R.drawable.ic_close, "Close");
        close.setTag("close");
        close.setOnClickListener(onClose);
        row.addView(close, marginLp(dp(40), dp(40), dp(8)));
        box.addView(row);
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
