package com.operit.printhelper.share;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import com.operit.printhelper.core.PantumSettings;
import com.operit.printhelper.core.PdfToPwg;
import com.operit.printhelper.core.PrintCore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class ShareEntryActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String action = intent.getAction();
        final ArrayList<Uri> uris = new ArrayList<Uri>();
        if (Intent.ACTION_SEND.equals(action)) {
            Uri u = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (u != null) uris.add(u);
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            ArrayList<Uri> list = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (list != null) uris.addAll(list);
        }
        if (uris.isEmpty()) {
            toast("未收到文件");
            finish();
            return;
        }

        String target = PantumSettings.host(this) + " (" + uris.size() + " 个文件)";
        new AlertDialog.Builder(this)
                .setTitle("Pantum Helper 打印")
                .setMessage("目标打印机: " + target + "\n\n选择打印方式：")
                .setPositiveButton("双面·长边翻", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) { start(uris, true, false); }
                })
                .setNegativeButton("双面·短边翻", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) { start(uris, true, true); }
                })
                .setNeutralButton("单面", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) { start(uris, false, false); }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface d) { finish(); }
                })
                .show();
    }

    private void start(final ArrayList<Uri> uris, final boolean duplex, final boolean shortEdge) {
        toast("开始打印…");
        final Activity self = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                String err = null;
                try {
                    for (Uri u : uris) printOne(self, u, duplex, shortEdge);
                } catch (Exception e) {
                    err = e.getMessage() == null ? e.toString() : e.getMessage();
                }
                final String e2 = err;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (e2 == null) toast("打印完成");
                        else toast("打印失败: " + e2);
                        self.finish();
                    }
                });
            }
        }).start();
    }

    private void printOne(Activity self, Uri u, boolean duplex, boolean shortEdge) throws Exception {
        String mime = self.getContentResolver().getType(u);
        if (mime != null && mime.startsWith("image/")) {
            int dpi = PantumSettings.dpi(self);
            int[] dim = PdfToPwg.mediaPixels(PantumSettings.media(self), dpi);
            Bitmap bm = PdfToPwg.renderImage(self, u, dim[0], dim[1]);
            PrintCore.printBitmap(self, bm, null);
            bm.recycle();
            return;
        }
        // treat everything else as PDF
        File f = copyToCache(self, u);
        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(
                f, ParcelFileDescriptor.MODE_READ_ONLY);
        PrintCore.printPdf(self, pfd, duplex, shortEdge, null);
        f.delete();
    }

    private File copyToCache(Activity self, Uri u) throws Exception {
        File f = new File(self.getCacheDir(), "share_" + System.currentTimeMillis() + ".pdf");
        InputStream in = self.getContentResolver().openInputStream(u);
        if (in == null) throw new Exception("无法读取文件");
        OutputStream out = new FileOutputStream(f);
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        out.close();
        in.close();
        return f;
    }

    private void toast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ShareEntryActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }
}

