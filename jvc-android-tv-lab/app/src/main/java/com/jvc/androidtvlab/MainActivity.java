package com.jvc.androidtvlab;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * JVC Smart UI: a lightweight, self-contained Android 7-compatible interface demo.
 * It is an app, not a replacement launcher or a second Android operating system.
 * No package disabling, system settings changes, elevated permissions, or network calls.
 */
public class MainActivity extends Activity {
    private LinearLayout page;
    private final int bg = Color.rgb(12, 17, 29);
    private final int panel = Color.rgb(27, 36, 54);
    private final int accent = Color.rgb(151, 191, 255);
    private final int white = Color.rgb(245, 247, 252);
    private final int muted = Color.rgb(177, 188, 207);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        home();
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(18, 10, 18, 10);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button action(String title, Runnable run) {
        Button b = new Button(this);
        b.setText(title);
        b.setTextSize(17);
        b.setAllCaps(false);
        b.setTextColor(white);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(panel));
        b.setMinHeight(64);
        b.setFocusable(true);
        b.setOnClickListener(v -> run.run());
        return b;
    }

    private void shell(String title, String subtitle) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        root.setPadding(26, 16, 26, 16);
        root.addView(label("JVC  •  SMART UI CONCEPT", 13, accent, true));
        root.addView(label(title, 30, white, true));
        if (subtitle != null && !subtitle.isEmpty()) root.addView(label(subtitle, 16, muted, false));
        ScrollView scroll = new ScrollView(this);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(0, 14, 0, 10);
        scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(label("نموذج واجهة داخل تطبيق — لا يغيّر نظام التلفزيون", 12, muted, false));
        setContentView(root);
    }

    private void tile(String symbol, String title, String detail, Runnable run) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundTintList(android.content.res.ColorStateList.valueOf(panel));
        card.setBackgroundColor(panel);
        card.setPadding(8, 8, 8, 8);
        TextView icon = label(symbol, 27, accent, true);
        TextView name = label(title, 20, white, true);
        TextView desc = label(detail, 14, muted, false);
        card.addView(icon);
        card.addView(name);
        card.addView(desc);
        card.setFocusable(true);
        card.setClickable(true);
        card.setPadding(10, 8, 10, 8);
        card.setOnClickListener(v -> run.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 142, 1);
        lp.setMargins(5, 5, 5, 5);
        page.addView(card, lp);
    }

    private void tileRow(String s1, String t1, String d1, Runnable r1,
                         String s2, String t2, String d2, Runnable r2) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(row);
        addTileToRow(row, s1, t1, d1, r1);
        addTileToRow(row, s2, t2, d2, r2);
    }

    private void addTileToRow(LinearLayout row, String symbol, String title, String detail, Runnable run) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(panel);
        card.setPadding(12, 8, 12, 8);
        card.addView(label(symbol, 25, accent, true));
        card.addView(label(title, 18, white, true));
        card.addView(label(detail, 13, muted, false));
        card.setFocusable(true);
        card.setClickable(true);
        card.setOnClickListener(v -> run.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 132, 1);
        lp.setMargins(5, 5, 5, 5);
        row.addView(card, lp);
    }

    private void home() {
        shell("مرحبًا", "واجهة تجريبية خفيفة مستوحاة من واجهات التلفزيون الحديثة");
        page.addView(label("مساحة العرض الرئيسية", 21, white, true));
        page.addView(label("واجهة مستقلة داخل التطبيق؛ لا تستبدل مشغّل JVC الأصلي.", 16, muted, false));
        tileRow("⚙", "الإعدادات", "صفحات إعدادات تجريبية", this::settings,
                "▦", "التطبيقات", "مركز التطبيقات", this::apps);
        tileRow("☼", "لوحة سريعة", "شبكة وصوت وشاشة (عرض فقط)", this::quickPanel,
                "ⓘ", "حول الواجهة", "الإصدار وحدود التشغيل", this::about);
        page.addView(label("التطبيقات المفضلة", 21, white, true));
        page.addView(action("▶  YouTube  — فتح التطبيق المثبت", () -> launchPackage("com.google.android.youtube.tv")));
        page.addView(action("▣  Shahid  — فتح التطبيق المثبت", () -> launchPackage("net.mbc.shahidTV")));
        page.addView(action("▣  STC TV  — فتح التطبيق المثبت", () -> launchPackage("com.intigral.jawwytv")));
        page.addView(label("إذا لم يفتح أحد التطبيقات، فهذا يعني أن اسم حزمتها مختلف أو أنها غير مثبتة/غير متاحة.", 13, muted, false));
    }

    private void settings() {
        shell("الإعدادات", "صفحات توضيحية فقط؛ لا تغيّر إعدادات Android الفعلية");
        page.addView(action("الشاشة والصورة", () -> info("الشاشة والصورة", "يمكن تطوير صفحة داخلية لاختيارات العرض. لا يملك التطبيق العادي صلاحية تغيير كل إعدادات التلفزيون.")));
        page.addView(action("الصوت", () -> info("الصوت", "هذه صفحة نموذجية. لم يتم تغيير مستوى الصوت أو إعدادات النظام.")));
        page.addView(action("الشبكة", () -> info("الشبكة", "عرض توضيحي فقط. لم يتم تعديل Wi‑Fi أو DNS أو إعدادات الراوتر.")));
        page.addView(action("التطبيقات", this::apps));
        page.addView(action("رجوع إلى الرئيسية", this::home));
    }

    private void apps() {
        shell("مركز التطبيقات", "اختصارات تفتح التطبيقات الموجودة فقط");
        page.addView(action("YouTube", () -> launchPackage("com.google.android.youtube.tv")));
        page.addView(action("Shahid", () -> launchPackage("net.mbc.shahidTV")));
        page.addView(action("Jawwy TV", () -> launchPackage("com.intigral.jawwytv")));
        page.addView(action("STC TV", () -> launchPackage("com.stc.tv")));
        page.addView(label("ملاحظة: قد يختلف اسم الحزمة من جهاز لآخر؛ لن يتم تثبيت أو تعطيل أي تطبيق تلقائيًا.", 15, muted, false));
        page.addView(action("رجوع إلى الرئيسية", this::home));
    }

    private void quickPanel() {
        shell("اللوحة السريعة", "أزرار شكلية آمنة — لا تنفذ تغييرات في النظام");
        tileRow("◉", "الشبكة", "عرض فقط", () -> info("الشبكة", "لم يتم تغيير اتصال Wi‑Fi أو DNS."),
                "♫", "الصوت", "عرض فقط", () -> info("الصوت", "لم يتم تغيير مستوى الصوت."));
        tileRow("☾", "الوضع الليلي", "محاكاة داخل التطبيق", () -> info("الوضع الليلي", "واجهة هذا النموذج داكنة بالفعل."),
                "▣", "الشاشة", "عرض فقط", () -> info("الشاشة", "لم يتم تغيير إعدادات الصورة."));
        page.addView(action("رجوع إلى الرئيسية", this::home));
    }

    private void about() {
        shell("حول الواجهة", "JVC Smart UI Concept  •  الإصدار التجريبي 0.2");
        page.addView(label("الهدف", 20, accent, true));
        page.addView(label("إنشاء واجهة تفاعلية حديثة تعمل كتطبيق مستقل فوق Android 7، مع الحفاظ على مشغّل JVC والنظام الأصلي.", 17, white, false));
        page.addView(label("ما الذي يعمل؟", 20, accent, true));
        page.addView(label("التنقل بين الصفحات، مركز الاختصارات، صفحات الإعدادات التجريبية، ومحاولة فتح التطبيقات المثبتة.", 16, white, false));
        page.addView(label("ما الذي لا يدّعيه هذا الإصدار؟", 20, accent, true));
        page.addView(label("ليس نظام Android جديدًا، ولا يشغّل تطبيقات غير متوافقة مع Android 7، ولا يغيّر واجهة النظام خارج التطبيق.", 16, white, false));
        page.addView(action("رجوع إلى الرئيسية", this::home));
    }

    private void info(String title, String body) {
        shell(title, "عرض تجريبي آمن");
        page.addView(label(body, 18, white, false));
        page.addView(action("رجوع", this::home));
    }

    private void launchPackage(String packageName) {
        try {
            android.content.Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent == null) {
                info("التطبيق غير متاح", "لم يعثر Android على تطبيق قابل للفتح باسم الحزمة: " + packageName + "\nلم يتم تثبيت أو تغيير أي شيء.");
            } else {
                startActivity(intent);
            }
        } catch (Exception e) {
            info("تعذر فتح التطبيق", "رفض النظام فتح التطبيق. لم يتم إجراء أي تغيير.");
        }
    }
}
