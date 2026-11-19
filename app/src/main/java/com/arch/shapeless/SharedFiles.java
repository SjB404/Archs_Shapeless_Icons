package com.arch.shapeless;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Hands the pictures this app makes (in its own cache folder) to the app you share them with,
 * read-only and only for that share. A tiny stand-in for AndroidX's FileProvider, so the app keeps
 * needing no extra libraries.
 */
public class SharedFiles extends ContentProvider {

    static final String AUTHORITY = "com.arch.shapeless.shared";

    /** The folder for files to share; emptied before each new share. */
    static File dir(Context c) {
        File d = new File(c.getCacheDir(), "shared");
        //noinspection ResultOfMethodCallIgnored
        d.mkdirs();
        return d;
    }

    static Uri uriFor(File f) {
        return new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath(f.getName()).build();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    private File file(Uri uri) throws FileNotFoundException {
        List<String> parts = uri.getPathSegments();
        String name = parts.size() == 1 ? parts.get(0) : null;
        if (name == null || name.startsWith(".") || name.contains("/") || name.contains("\\")) {
            throw new FileNotFoundException(String.valueOf(uri));
        }
        File f = new File(dir(getContext()), name);
        if (!f.isFile()) throw new FileNotFoundException(name);
        return f;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (mode == null || !mode.equals("r")) throw new SecurityException("Read only");
        return ParcelFileDescriptor.open(file(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public String getType(Uri uri) {
        String p = uri.getLastPathSegment();
        return p != null && p.endsWith(".png") ? "image/png" : "application/octet-stream";
    }

    /** Name and size, which apps ask for to show the file. */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        File f;
        try {
            f = file(uri);
        } catch (FileNotFoundException e) {
            return null;
        }
        String[] wanted = projection != null ? projection : new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        List<String> cols = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (String col : wanted) {
            if (OpenableColumns.DISPLAY_NAME.equals(col)) {
                cols.add(col);
                values.add(f.getName());
            } else if (OpenableColumns.SIZE.equals(col)) {
                cols.add(col);
                values.add(f.length());
            }
        }
        MatrixCursor c = new MatrixCursor(cols.toArray(new String[0]), 1);
        c.addRow(values.toArray());
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Read only");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Read only");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Read only");
    }
}
