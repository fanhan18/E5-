package com.operit.printhelper.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import java.util.concurrent.CountDownLatch;

import com.operit.printhelper.core.PantumSettings;

/**
 * Dialog-style activity shown between duplex phases.
 * PrintCore blocks on the static latch until the user answers.
 */
public class FlipConfirmActivity extends Activity {
    public static volatile CountDownLatch latch;
    public static volatile boolean confirmed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String feed = PantumSettings.topFeed(this) ? "上进纸口" : "下进纸口";
        new AlertDialog.Builder(this)
                .setTitle("请翻纸")
                .setMessage("第一面打印完成。\n\n" +
                        "请把整叠纸翻面后放回" + feed + "，\n" +
                        "已打印内容朝下。\n\n" +
                        "放好后点「继续」打印第二面。")
                .setPositiveButton("继续", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) { done(true); }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) { done(false); }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface d) { done(false); }
                })
                .show();
    }

    private void done(boolean ok) {
        confirmed = ok;
        if (latch != null) latch.countDown();
        finish();
    }

    @Override
    protected void onDestroy() {
        if (latch != null && latch.getCount() > 0) {
            confirmed = false;
            latch.countDown();
        }
        super.onDestroy();
    }
}
