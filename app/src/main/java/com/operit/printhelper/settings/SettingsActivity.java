package com.operit.printhelper.settings;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.operit.printhelper.core.PantumSettings;

public class SettingsActivity extends Activity {
    private static final String[] MEDIAS = {
            "iso_a4_210x297mm", "na_letter_8.5x11in", "iso_a5_148x210mm"
    };

    private EditText etHost, etPort, etPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = this;
        int pad = dp(16);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Pantum Helper 设置");
        tvTitle.setTextSize(20);
        tvTitle.setTypeface(tvTitle.getTypeface(), Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, dp(12));
        root.addView(tvTitle);

        // 1-3. IP / 端口 / 路径
        root.addView(label("打印机 IP"));
        etHost = new EditText(this);
        etHost.setText(PantumSettings.host(this));
        etHost.setHint("例如 10.0.0.20");
        etHost.setInputType(InputType.TYPE_CLASS_TEXT);
        root.addView(etHost);

        root.addView(label("端口"));
        etPort = new EditText(this);
        etPort.setText(String.valueOf(PantumSettings.port(this)));
        etPort.setHint("默认 631");
        etPort.setInputType(InputType.TYPE_CLASS_NUMBER);
        root.addView(etPort);

        root.addView(label("IPP 路径"));
        etPath = new EditText(this);
        etPath.setText(PantumSettings.path(this));
        etPath.setHint("默认 ipp/print");
        etPath.setInputType(InputType.TYPE_CLASS_TEXT);
        root.addView(etPath);

        // 4. 翻页方式
        root.addView(label("翻页方式（双面打印）"));
        RadioGroup rgDuplex = radioGroup();
        rgDuplex.addView(radio("长边翻（书本）", true, !PantumSettings.shortEdge(this)));
        rgDuplex.addView(radio("短边翻（台历）", false, PantumSettings.shortEdge(this)));
        root.addView(rgDuplex);
        final RadioGroup fRgDuplex = rgDuplex;

        // 5. 进纸方向
        root.addView(label("进纸方向"));
        RadioGroup rgFeed = radioGroup();
        rgFeed.addView(radio("下进纸", true, !PantumSettings.topFeed(this)));
        rgFeed.addView(radio("上进纸", false, PantumSettings.topFeed(this)));
        root.addView(rgFeed);
        final RadioGroup fRgFeed = rgFeed;

        // 6. 纸型
        root.addView(label("纸型"));
        RadioGroup rgMedia = radioGroup();
        rgMedia.addView(radio("A4", true, true));
        rgMedia.addView(radio("Letter", false, false));
        android.widget.RadioButton rbA5 = radio("A5", false, false);
        rbA5.setId(2);
        rgMedia.addView(rbA5);
        String curMedia = PantumSettings.media(ctx);
        for (int i = 0; i < MEDIAS.length; i++) {
            if (MEDIAS[i].equals(curMedia)) {
                ((android.widget.RadioButton) rgMedia.getChildAt(i)).setChecked(true);
                if (i != 0) ((android.widget.RadioButton) rgMedia.getChildAt(0)).setChecked(false);
            }
        }
        root.addView(rgMedia);
        final RadioGroup fRgMedia = rgMedia;

        // 7. 分辨率
        root.addView(label("分辨率"));
        RadioGroup rgDpi = radioGroup();
        rgDpi.addView(radio("300 dpi", true, true));
        rgDpi.addView(radio("600 dpi", false, false));
        if (PantumSettings.dpi(ctx) >= 600) {
            ((android.widget.RadioButton) rgDpi.getChildAt(0)).setChecked(false);
            ((android.widget.RadioButton) rgDpi.getChildAt(1)).setChecked(true);
        }
        root.addView(rgDpi);
        final RadioGroup fRgDpi = rgDpi;

        // 出纸顺序
        CheckBox cbReverse = new CheckBox(this);
        cbReverse.setText("后出在上（第二面逆序发送）");
        cbReverse.setChecked(PantumSettings.reverseEven(this));
        root.addView(cbReverse);
        final CheckBox fCbReverse = cbReverse;

        // 提示文字
        TextView tvHint = new TextView(this);
        tvHint.setText("双面打印第二阶段：请把整叠纸翻面后放回进纸口，已打印内容朝下（下进纸时）。\n首次使用建议先打 2 页测试校准方向。");
        tvHint.setTextSize(13);
        tvHint.setTextColor(Color.GRAY);
        tvHint.setPadding(0, dp(12), 0, dp(12));
        root.addView(tvHint);

        // 保存按钮
        Button btnSave = new Button(this);
        btnSave.setText("保存");
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context c = SettingsActivity.this;
                PantumSettings.setHost(c, etHost.getText().toString().trim());
                int port;
                try {
                    port = Integer.parseInt(etPort.getText().toString().trim());
                } catch (NumberFormatException e) {
                    port = 631;
                }
                PantumSettings.setPort(c, port);
                String p = etPath.getText().toString().trim();
                while (p.startsWith("/")) p = p.substring(1);
                PantumSettings.setPath(c, p);
                PantumSettings.setShortEdge(c, fRgDuplex.getCheckedRadioButtonId() == 1);
                PantumSettings.setTopFeed(c, fRgFeed.getCheckedRadioButtonId() == 1);
                int mIdx = checkedIndex(fRgMedia);
                PantumSettings.setMedia(c, mIdx >= 0 ? MEDIAS[mIdx] : MEDIAS[0]);
                PantumSettings.setDpi(c, checkedIndex(fRgDpi) == 1 ? 600 : 300);
                PantumSettings.setReverseEven(c, fCbReverse.isChecked());
                Toast.makeText(c, "已保存", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        root.addView(btnSave);

        setContentView(scroll);
    }

    private int checkedIndex(RadioGroup rg) {
        for (int i = 0; i < rg.getChildCount(); i++) {
            if (((android.widget.RadioButton) rg.getChildAt(i)).isChecked()) return i;
        }
        return -1;
    }

    private TextView label(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setPadding(0, dp(12), 0, dp(4));
        return tv;
    }

    private RadioGroup radioGroup() {
        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(LinearLayout.VERTICAL);
        return rg;
    }

    private android.widget.RadioButton radio(String text, boolean first, boolean checked) {
        android.widget.RadioButton rb = new android.widget.RadioButton(this);
        rb.setText(text);
        rb.setChecked(checked);
        rb.setId(first ? 0 : 1);
        return rb;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
