package com.operit.printhelper.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Rendering helpers: PDF page / image -> fixed-size bitmap -> PWG grayscale page.
 */
public class PdfToPwg {

    /** Physical pixel size for a media keyword at given dpi. Returns {w,h}. */
    public static int[] mediaPixels(String media, int dpi) {
        if (media == null) media = "";
        if (media.contains("letter"))
            return new int[]{(int) Math.round(8.5 * dpi), 11 * dpi};
        if (media.contains("a5"))
            return new int[]{(int) Math.round(148.0 / 25.4 * dpi), (int) Math.round(210.0 / 25.4 * dpi)};
        // default A4
        return new int[]{(int) Math.round(210.0 / 25.4 * dpi), (int) Math.round(297.0 / 25.4 * dpi)};
    }

    /** Render a PDF page onto an exact tw x th white bitmap (aspect preserved, centered). */
    public static Bitmap renderPdfPage(PdfRenderer.Page pg, int tw, int th) {
        float pw = pg.getWidth();
        float ph = pg.getHeight();
        Bitmap full = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888);
        full.eraseColor(Color.WHITE);
        if (Math.abs(pw / ph - tw / (float) th) < 0.01f) {
            pg.render(full, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
            return full;
        }
        Canvas cv = new Canvas(full);
        float scale = Math.min(tw / pw, th / ph);
        int bw = Math.max(1, Math.round(pw * scale));
        int bh = Math.max(1, Math.round(ph * scale));
        Bitmap tmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
        tmp.eraseColor(Color.WHITE);
        pg.render(tmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
        cv.drawBitmap(tmp, (tw - bw) / 2f, (th - bh) / 2f, null);
        tmp.recycle();
        return full;
    }

    /** Fit an arbitrary bitmap onto tw x th white bitmap (aspect preserved, centered). */
    public static Bitmap fitBitmap(Bitmap src, int tw, int th) {
        Bitmap page = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(page);
        cv.drawColor(Color.WHITE);
        float scale = Math.min(tw * 1f / src.getWidth(), th * 1f / src.getHeight());
        int bw = Math.max(1, Math.round(src.getWidth() * scale));
        int bh = Math.max(1, Math.round(src.getHeight() * scale));
        Rect dst = new Rect((tw - bw) / 2, (th - bh) / 2, (tw - bw) / 2 + bw, (th - bh) / 2 + bh);
        cv.drawBitmap(src, null, dst, null);
        return page;
    }

    /** Decode a content Uri image fitted onto tw x th white bitmap. */
    public static Bitmap renderImage(Context ctx, Uri uri, int tw, int th) throws IOException {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        InputStream in = ctx.getContentResolver().openInputStream(uri);
        if (in != null) { BitmapFactory.decodeStream(in, null, o); in.close(); }
        int sample = 1;
        while (o.outWidth / (sample * 2) >= tw && o.outHeight / (sample * 2) >= th) sample *= 2;
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = sample;
        in = ctx.getContentResolver().openInputStream(uri);
        Bitmap src = in != null ? BitmapFactory.decodeStream(in, null, o2) : null;
        if (in != null) in.close();
        if (src == null) throw new IOException("无法解码图片");
        Bitmap page = fitBitmap(src, tw, th);
        src.recycle();
        return page;
    }

    /**
     * Convert an exact-size bitmap into a complete single-page PWG document
     * (RaS2 header + grayscale raster), row-strip based to limit memory.
     * rotate180 flips both axes (for short-edge duplex back side).
     */
    public static byte[] pageToPwg(Bitmap page, int w, int h, boolean rotate180, int dpi) throws IOException {
        int[] row = new int[w];
        byte[] gray = new byte[w * h];
        for (int y = 0; y < h; y++) {
            page.getPixels(row, 0, w, 0, y, w, 1);
            int dy = rotate180 ? h - 1 - y : y;
            for (int x = 0; x < w; x++) {
                int p = row[x];
                int g = (Color.red(p) * 2125 + Color.green(p) * 7154 + Color.blue(p) * 721) / 10000;
                int dx = rotate180 ? w - 1 - x : x;
                gray[dy * w + dx] = (byte) g;
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(4 + 1796 + gray.length);
        PwgEncoder.writeDocHeader(out);
        PwgEncoder.writePage(out, w, h, gray, dpi);
        return out.toByteArray();
    }
}
