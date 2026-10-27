package com.jvc.androidtvlab;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Safe first-stage shell for JVC Android TV Lab.
 * It never changes the launcher, disables packages, writes system settings,
 * or attempts to boot/virtualize another OS.
 */
public class MainActivity extends Activity {
    private LinearLayout content;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        showHome();
    }

    private TextView text(String value, float size) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(Color.WHITE);
        v.setPadding(18, 12, 18, 12);
        return v;
    }

    private Button button(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(18);
        b.setFocusable(true);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void base(String title) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(15, 22, 35));
        root.setPadding(26, 18, 26, 18);
        root.addView(text("JVC ANDROID TV LAB", 14));
        root.addView(text(title, 28));
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void showHome() {
        base("مختبر التلفزيون");
        content.addView(text("واجهة تجريبية آمنة تعمل كتطبيق داخل النظام الأصلي.", 19));
        content.addView(text("لا تغيّر المشغّل الأصلي ولا تعطّل التطبيقات ولا تعدّل إعدادات النظام.", 16));
        content.addView(button("فحص توافق الجهاز", this::showCompatibility));
        content.addView(button("حدود تشغيل نظام Android مستقل", this::showLimits));
        content.addView(button("الرجوع إلى الواجهة الرئيسية", this::showHome));
    }

    private void showCompatibility() {
        base("تقرير التوافق");
        content.addView(text("إصدار Android: " + Build.VERSION.RELEASE +
            " (API " + Build.VERSION.SDK_INT + ")", 19));
        content.addView(text("الشركة: " + Build.MANUFACTURER, 17));
        content.addView(text("الجهاز: " + Build.MODEL, 17));
        content.addView(text("المنتج: " + Build.PRODUCT, 17));
        content.addView(text("المعمارية: " + (Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "غير معروفة"), 17));
        content.addView(text("الذاكرة الكلية التقريبية: " +
            (getSystemService(ACTIVITY_SERVICE) != null ? "راجع تقرير التشخيص المرفق" : "غير متاحة"), 16));
        content.addView(text("هذا الفحص يقرأ معلومات عامة فقط؛ لا يغيّر إعدادات التلفزيون.", 16));
        content.addView(button("عودة", this::showHome));
    }

    private void showLimits() {
        base("تشغيل نظام مستقل");
        content.addView(text("التطبيق العادي لا يستطيع تشغيل نظام Android ثانٍ بمفرده.", 20));
        content.addView(text("يلزم محرك افتراضي أو بيئة تشغيل متوافقة مع النواة والصلاحيات. في هذا التلفزيون: Android 7، وذاكرة تقارب 0.78 GB، ومعمارية 32-bit، ومحمل إقلاع مقفل. لذلك لم تثبت إمكانية تشغيل نظام ضيف حتى الآن.", 17));
        content.addView(text("هذه النسخة لا تحاول الروت أو فتح محمل الإقلاع أو تثبيت نظام أو تعديل أقسام الجهاز.", 17));
        content.addView(button("عودة", this::showHome));
    }
}
