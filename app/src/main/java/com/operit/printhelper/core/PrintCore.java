package com.operit.printhelper.core;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;

import com.operit.printhelper.ui.FlipConfirmActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Print pipeline: render -> PWG grayscale -> IPP Print-Job per page.
 * Manual duplex = two phases (odd pages, flip confirm, even pages).
 */
public class PrintCore {

    public interface Progress { void onStatus(String msg); }

    public static void printPdf(Context ctx, ParcelFileDescriptor pfd, boolean duplex,
                                boolean shortEdge, Progress cb) throws Exception {
        String[] urls = urls(ctx);
        String httpUrl = urls[0];
        String printerUri = urls[1];
        int dpi = PantumSettings.dpi(ctx);
        int[] dim = PdfToPwg.mediaPixels(PantumSettings.media(ctx), dpi);
        boolean reverseEven = PantumSettings.reverseEven(ctx);

        PdfRenderer r = new PdfRenderer(pfd);
        try {
            int n = r.getPageCount();
            if (cb != null) cb.onStatus("共 " + n + " 页，开始打印");
            if (!duplex) {
                for (int i = 0; i < n; i++) {
                    byte[] doc = renderPage(r, i, dim[0], dim[1], false, dpi);
                    send(ctx, httpUrl, printerUri, "Pantum-" + (i + 1), doc);
                    if (cb != null) cb.onStatus("已发送 " + (i + 1) + "/" + n);
                }
            } else {
                // Phase 1: odd pages (1-based), in order, never rotated.
                for (int i = 0; i < n; i += 2) {
                    byte[] doc = renderPage(r, i, dim[0], dim[1], false, dpi);
                    send(ctx, httpUrl, printerUri, "Pantum-o" + (i + 1), doc);
                }
                if (n > 1) {
                    if (cb != null) cb.onStatus("第一面完成，等待翻纸");
                    if (!confirmFlip(ctx)) throw new IOException("已取消：未打印第二面");
                    // Phase 2: even pages; rotate 180 if short-edge; optional reverse order.
                    List<Integer> evens = new ArrayList<Integer>();
                    for (int i = 1; i < n; i += 2) evens.add(i);
                    if (reverseEven) Collections.reverse(evens);
                    int k = 0;
                    for (int idx : evens) {
                        byte[] doc = renderPage(r, idx, dim[0], dim[1], shortEdge, dpi);
                        send(ctx, httpUrl, printerUri, "Pantum-e" + (idx + 1), doc);
                        k++;
                        if (cb != null) cb.onStatus("第二面 " + k + "/" + evens.size());
                    }
                }
            }
        } finally {
            try { r.close(); } catch (Exception ignored) { }
            try { pfd.close(); } catch (Exception ignored) { }
        }
    }

    /** Single image print (share path). */
    public static void printBitmap(Context ctx, Bitmap src, Progress cb) throws Exception {
        String[] urls = urls(ctx);
        int dpi = PantumSettings.dpi(ctx);
        int[] dim = PdfToPwg.mediaPixels(PantumSettings.media(ctx), dpi);
        Bitmap page = PdfToPwg.fitBitmap(src, dim[0], dim[1]);
        byte[] doc = PdfToPwg.pageToPwg(page, dim[0], dim[1], false, dpi);
        page.recycle();
        if (cb != null) cb.onStatus("发送图片…");
        send(ctx, urls[0], urls[1], "Pantum-img", doc);
    }

    private static byte[] renderPage(PdfRenderer r, int index, int tw, int th,
                                     boolean rotate180, int dpi) throws IOException {
        PdfRenderer.Page pg = r.openPage(index);
        try {
            Bitmap bm = PdfToPwg.renderPdfPage(pg, tw, th);
            byte[] doc = PdfToPwg.pageToPwg(bm, tw, th, rotate180, dpi);
            bm.recycle();
            return doc;
        } finally {
            pg.close();
        }
    }

    private static void send(Context ctx, String httpUrl, String printerUri,
                             String jobName, byte[] doc) throws IOException {
        IppClient.IppResult res = IppClient.printDocument(
                httpUrl, printerUri, "android", jobName, 1, PantumSettings.media(ctx), doc);
        if (res.httpCode != 200) throw new IOException("HTTP " + res.httpCode);
        if (res.statusCode != 0x0000 && res.statusCode != 0x0001 && res.statusCode != 0x0002)
            throw new IOException("IPP 错误: " + res.statusText);
    }

    /** Show flip-confirm dialog, block until user answers or timeout (5 min). */
    private static boolean confirmFlip(Context ctx) {
        FlipConfirmActivity.latch = new java.util.concurrent.CountDownLatch(1);
        FlipConfirmActivity.confirmed = false;
        Intent i = new Intent(ctx, FlipConfirmActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
        try {
            return FlipConfirmActivity.latch.await(300, TimeUnit.SECONDS)
                    && FlipConfirmActivity.confirmed;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static String[] urls(Context ctx) {
        String h = PantumSettings.host(ctx);
        int p = PantumSettings.port(ctx);
        String path = PantumSettings.path(ctx);
        return new String[]{
                "http://" + h + ":" + p + "/" + path,
                "ipp://" + h + ":" + p + "/" + path};
    }
}
