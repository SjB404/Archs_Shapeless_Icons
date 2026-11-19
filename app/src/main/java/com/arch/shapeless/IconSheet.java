package com.arch.shapeless;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Draws the selected apps' icons into one picture (more when there are many apps), with each app's
 * name under its icons, so they can be sent to a chat in one go. Icons can't go on the clipboard
 * together with text, so these pictures are shared instead.
 */
final class IconSheet {

    private static final int PER_PICTURE = 30;
    private static final int ICON = 160, GAP = 20, PAD = 28, SPACE = 20, MARGIN = 40;
    private static final int TILE = 0xFF3A3A3C;

    private IconSheet() {
    }

    /** Makes the picture(s) and returns them ready to share. Run off the main thread. */
    static ArrayList<Uri> make(Context c, List<PackIndex.Entry> apps, boolean pack, boolean own, boolean withPackage)
            throws IOException {
        File dir = SharedFiles.dir(c);
        File[] old = dir.listFiles();
        if (old != null) for (File f : old) //noinspection ResultOfMethodCallIgnored
            f.delete();
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
        int parts = (apps.size() + PER_PICTURE - 1) / PER_PICTURE;
        ArrayList<Uri> out = new ArrayList<>();
        for (int p = 0; p < parts; p++) {
            List<PackIndex.Entry> part = apps.subList(p * PER_PICTURE, Math.min(apps.size(), (p + 1) * PER_PICTURE));
            Bitmap b = draw(c, part, pack, own, withPackage, apps.size(), p, parts);
            File f = new File(dir, "shapeless-icons-" + stamp + (parts > 1 ? "-" + (p + 1) : "") + ".png");
            try (FileOutputStream os = new FileOutputStream(f)) {
                b.compress(Bitmap.CompressFormat.PNG, 100, os);
            } finally {
                b.recycle();
            }
            out.add(SharedFiles.uriFor(f));
        }
        return out;
    }

    private static Bitmap draw(Context c, List<PackIndex.Entry> apps, boolean pack, boolean own,
                               boolean withPackage, int total, int part, int parts) {
        PackageManager pm = c.getPackageManager();
        boolean both = pack && own;
        int tileW = PAD * 2 + (both ? ICON * 2 + GAP : ICON);
        int cols = Math.min(both ? 3 : 4, apps.size());
        int rows = (apps.size() + cols - 1) / cols;
        int nameH = 44, pkgH = withPackage ? 30 : 0;
        int tileH = PAD + ICON + 14 + nameH + pkgH + PAD - 8;
        int headH = 64 + (both ? 40 : 0);
        int width = Math.max(MARGIN * 2 + cols * tileW + (cols - 1) * SPACE, 640);
        int height = MARGIN + headH + rows * tileH + (rows - 1) * SPACE + MARGIN;

        Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        cv.drawColor(Ui.DRAWER);

        Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        TextPaint head = paint(36, Color.WHITE, medium);
        TextPaint hint = paint(24, 0xB3FFFFFF, Typeface.DEFAULT);
        TextPaint name = paint(28, Color.WHITE, medium);
        TextPaint pkgPaint = paint(21, 0xB3FFFFFF, Typeface.DEFAULT);
        TextPaint small = paint(20, 0x99FFFFFF, Typeface.DEFAULT);
        name.setTextAlign(Paint.Align.CENTER);
        pkgPaint.setTextAlign(Paint.Align.CENTER);
        small.setTextAlign(Paint.Align.CENTER);

        String title = "Shapeless Icons · " + total + (total == 1 ? " app" : " apps")
                + (parts > 1 ? " · picture " + (part + 1) + " of " + parts : "");
        cv.drawText(title, MARGIN, MARGIN + 36, head);
        if (both) cv.drawText("Left: Shapeless · right: the app's own icon", MARGIN, MARGIN + 36 + 42, hint);

        Paint tile = new Paint(Paint.ANTI_ALIAS_FLAG);
        tile.setColor(TILE);
        for (int i = 0; i < apps.size(); i++) {
            PackIndex.Entry e = apps.get(i);
            int x = MARGIN + (i % cols) * (tileW + SPACE);
            int y = MARGIN + headH + (i / cols) * (tileH + SPACE);
            cv.drawRoundRect(new RectF(x, y, x + tileW, y + tileH), 28, 28, tile);
            int ix = x + PAD, iy = y + PAD;
            if (pack) {
                Drawable d = e.resId != 0 ? load(() -> c.getDrawable(e.resId)) : null;
                drawIcon(cv, d, ix, iy, "no icon yet", small);
                ix += ICON + GAP;
            }
            if (own) {
                Drawable d = e.info != null ? LiveIcons.own(pm, e.info, DisplayMetrics.DENSITY_XXXHIGH) : null;
                drawIcon(cv, d, ix, iy, "not on phone", small);
            }
            float cx = x + tileW / 2f;
            int textW = tileW - PAD;
            int ty = y + PAD + ICON + 14 + 32;
            cv.drawText(TextUtils.ellipsize(e.label, name, textW, TextUtils.TruncateAt.END).toString(), cx, ty, name);
            if (withPackage) {
                cv.drawText(TextUtils.ellipsize(e.packageName, pkgPaint, textW, TextUtils.TruncateAt.END).toString(),
                        cx, ty + 32, pkgPaint);
            }
        }
        return b;
    }

    private interface Loader {
        Drawable get();
    }

    private static Drawable load(Loader l) {
        try {
            return l.get();
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }

    private static void drawIcon(Canvas cv, Drawable d, int x, int y, String missing, TextPaint small) {
        if (d != null) {
            try {
                d.setBounds(x, y, x + ICON, y + ICON);
                d.draw(cv);
                return;
            } catch (Exception ignored) {
                // fall through to the empty box
            }
        }
        Paint dash = new Paint(Paint.ANTI_ALIAS_FLAG);
        dash.setStyle(Paint.Style.STROKE);
        dash.setStrokeWidth(3);
        dash.setColor(0x80FFFFFF);
        dash.setPathEffect(new DashPathEffect(new float[]{12, 9}, 0));
        cv.drawRoundRect(new RectF(x + 8, y + 8, x + ICON - 8, y + ICON - 8), 24, 24, dash);
        cv.drawText(missing, x + ICON / 2f, y + ICON / 2f + 7, small);
    }

    private static TextPaint paint(float size, int colour, Typeface face) {
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(size);
        p.setColor(colour);
        p.setTypeface(face);
        return p;
    }
}
