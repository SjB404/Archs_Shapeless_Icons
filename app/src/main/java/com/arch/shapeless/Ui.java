package com.arch.shapeless;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small helpers for building the screens in code (the app has no layout files). */
final class Ui {

    /** Arch's dark grey app drawer. The other preview background is his real wallpaper (the app turns see-through). */
    static final int DRAWER = 0xFF2F2F2F;

    static final int TEXT = 0xFFFFFFFF;
    static final int TEXT_DIM = 0xB3FFFFFF;
    static final int TEXT_FAINT = 0x80FFFFFF;
    static final int FILL = 0x1FFFFFFF;        // see-through white for buttons, search bar, chips
    static final int FILL_STRONG = 0x33FFFFFF;
    static final int SHEET = 0xFF232325;       // pop-up sheets
    static final int SHEET_CARD = 0xFF2E2E31;
    static final int ACCENT = 0xFFFFFFFF;
    static final int WARN = 0xFFFFB74D;

    private Ui() {
    }

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    static GradientDrawable rounded(int color, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx);
        return g;
    }

    static GradientDrawable rounded(int color, float tl, float tr, float br, float bl) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadii(new float[]{tl, tl, tr, tr, br, br, bl, bl});
        return g;
    }

    /** A filled, rounded background that shows a ripple when touched. */
    static Drawable ripple(int fill, float radiusPx) {
        GradientDrawable mask = rounded(Color.WHITE, radiusPx);
        Drawable content = fill == 0 ? null : rounded(fill, radiusPx);
        return new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), content, mask);
    }

    static Drawable icon(Context c, int res, int color) {
        Drawable d = c.getDrawable(res).mutate();
        d.setTint(color);
        return d;
    }

    static TextView text(Context c, CharSequence s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    /** Round icon-only button with a long-press hint. */
    static ImageButton iconButton(Context c, int iconRes, String hint) {
        ImageButton b = new ImageButton(c);
        b.setImageDrawable(icon(c, iconRes, TEXT));
        b.setBackground(ripple(FILL, dp(c, 20)));
        b.setScaleType(ImageView.ScaleType.CENTER);
        b.setContentDescription(hint);
        b.setTooltipText(hint);
        return b;
    }

    /** Rounded button with an icon in front of its text. */
    static LinearLayout pillButton(Context c, int iconRes, String label, boolean primary) {
        LinearLayout b = new LinearLayout(c);
        b.setOrientation(LinearLayout.HORIZONTAL);
        b.setGravity(Gravity.CENTER);
        int h = dp(c, 16), v = dp(c, 11);
        b.setPadding(h, v, h + dp(c, 4), v);
        b.setBackground(ripple(primary ? ACCENT : FILL, dp(c, 24)));
        b.setClickable(true);
        b.setFocusable(true);
        int fg = primary ? 0xFF1C1C1E : TEXT;
        if (iconRes != 0) {
            ImageView i = new ImageView(c);
            i.setImageDrawable(icon(c, iconRes, fg));
            int s = dp(c, 20);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(s, s);
            lp.setMarginEnd(dp(c, 8));
            b.addView(i, lp);
        }
        TextView t = text(c, label, 15, fg, true);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        b.addView(t);
        return b;
    }

    /** One tappable row in a sheet: icon, title and a line of explanation. */
    static LinearLayout optionRow(Context c, int iconRes, String title, String body) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int p = dp(c, 16);
        row.setPadding(p, p, p, p);
        row.setBackground(ripple(SHEET_CARD, dp(c, 18)));
        row.setClickable(true);
        row.setFocusable(true);

        ImageView i = new ImageView(c);
        i.setImageDrawable(icon(c, iconRes, TEXT));
        i.setBackground(rounded(FILL, dp(c, 20)));
        int pad = dp(c, 9);
        i.setPadding(pad, pad, pad, pad);
        int s = dp(c, 40);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(s, s);
        ilp.setMarginEnd(dp(c, 14));
        row.addView(i, ilp);

        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(text(c, title, 16, TEXT, true));
        TextView b = text(c, body, 13, TEXT_DIM, false);
        b.setPadding(0, dp(c, 2), 0, 0);
        texts.addView(b);
        row.addView(texts, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    static View spacer(Context c, int heightDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, heightDp)));
        return v;
    }
}
