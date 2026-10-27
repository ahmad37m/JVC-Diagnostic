package com.jvc.androidtvlab;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.util.Locale;

/**
 * Safe diagnostics for JVC Android TV.
 * Does not change the launcher, disable packages, write system settings,
 * request elevated permissions, or attempt to boot/virtualize another OS.
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

    private String gib(long bytes) {
        return String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private void showHome() {
        base("مختبر التلفزيون");
        content.addView(text("المرحلة التالية: فحص الموارد وإمكانات تشغيل التطبيقات، لا إعادة تقرير Android الأساسي.", 19));
        content.addView(text("هذا التطبيق لا يغيّر النظام أو المشغّل، ولا يعطّل التطبيقات، ولا يحتاج إلى ADB.", 16));
        content.addView(button("فحص الموارد وإمكانات التشغيل", this::showResources));
        content.addView(button("حدود تشغيل نظام Android مستقل", this::showLimits));
        content.addView(button("الرجوع إلى الواجهة الرئيسية", this::showHome));
    }

    private void showResources() {
        base("فحص الموارد");
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            content.addView(text("RAM الكلية: " + gib(mi.totalMem), 18));
            content.addView(text("RAM المتاحة الآن: " + gib(mi.availMem), 18));
            content.addView(text("عتبة انخفاض الذاكرة: " + gib(mi.threshold), 16));
            content.addView(text("ذاكرة التطبيق القصوى التقريبية: " + am.getMemoryClass() + " MB", 16));
            content.addView(text("النظام يعتبر الذاكرة منخفضة: " + (mi.lowMemory ? "نعم" : "لا"), 16));
            ConfigurationInfo ci = am.getDeviceConfigurationInfo();
            if (ci != null) {
                int gl = ci.reqGlEsVersion;
                content.addView(text(String.format(Locale.US, "OpenGL ES المعلن: %d.%d", (gl >> 16), (gl & 0xffff)), 16));
            } else {
                content.addView(text("OpenGL ES: لم يعلن النظام عن القيمة", 16));
            }
        } else {
            content.addView(text("تعذر قراءة خدمة إدارة الذاكرة.", 16));
        }

        File data = Environment.getDataDirectory();
        try {
            StatFs s = new StatFs(data.getAbsolutePath());
            long total = s.getBlockCountLong() * s.getBlockSizeLong();
            long free = s.getAvailableBlocksLong() * s.getBlockSizeLong();
            content.addView(text("مساحة قسم البيانات: " + gib(total), 18));
            content.addView(text("المساحة المتاحة للمستخدم: " + gib(free), 18));
        } catch (Exception e) {
            content.addView(text("تعذر قراءة مساحة التخزين: " + e.getClass().getSimpleName(), 16));
        }

        content.addView(text("عدد أنوية المعالج التي يعرضها النظام: " + Runtime.getRuntime().availableProcessors(), 16));
        content.addView(text("معماريات النظام المدعومة: " + join(Build.SUPPORTED_ABIS), 16));
        content.addView(text("إصدار Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")", 16));
        content.addView(text("هذا الفحص يقرأ موارد النظام فقط. لا يثبت إمكانية تشغيل Android ثانٍ؛ نحتاج لاحقًا إلى دليل على دعم محرك افتراضي متوافق مع الجهاز.", 17));
        content.addView(button("عودة", this::showHome));
    }

    private String join(String[] values) {
        if (values == null || values.length == 0) return "غير معروفة";
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (out.length() > 0) out.append(", ");
            out.append(value);
        }
        return out.toString();
    }

    private void showLimits() {
        base("تشغيل نظام مستقل");
        content.addView(text("التطبيق العادي لا يستطيع تشغيل نظام Android ثانٍ بمفرده.", 20));
        content.addView(text("يلزم محرك افتراضي أو بيئة تشغيل متوافقة مع النواة والصلاحيات. معلومات الجهاز الحالية تشير إلى Android 7 وذاكرة محدودة ومحمل إقلاع مقفل؛ لذلك لم تثبت إمكانية تشغيل نظام ضيف حتى الآن.", 17));
        content.addView(text("هذه النسخة لا تحاول الروت أو فتح محمل الإقلاع أو تثبيت نظام أو تعديل أقسام الجهاز.", 17));
        content.addView(button("عودة", this::showHome));
    }
}
