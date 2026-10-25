package com.jvc.diagnostic;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Typeface;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

public class MainActivity extends Activity {

private String report;
private String propertiesReport;

@Override
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(24, 24, 24, 24);

    TextView title = new TextView(this);
    title.setText("JVC Diagnostic");
    title.setTextSize(26);
    title.setTypeface(null, Typeface.BOLD);
    layout.addView(title);

    report = buildReport();
    propertiesReport = buildPropertiesReport();

    TextView info = new TextView(this);
    info.setText(report);
    info.setTextSize(16);
    info.setTextIsSelectable(true);

    ScrollView scroll = new ScrollView(this);
    scroll.addView(info);
    layout.addView(scroll, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

    Button copy = new Button(this);
    copy.setText("نسخ التقرير");
    copy.setOnClickListener(v -> copyText(copy, report, "JVC Report"));
    layout.addView(copy);

    Button copyProperties = new Button(this);
    copyProperties.setText("نسخ خصائص النظام");
    copyProperties.setOnClickListener(v ->
            copyText(copyProperties, propertiesReport, "System Properties"));
    layout.addView(copyProperties);

    setContentView(layout);
}

private void copyText(Button button, String text, String label) {
    ClipboardManager cb = (ClipboardManager)
            getSystemService(Context.CLIPBOARD_SERVICE);
    cb.setPrimaryClip(ClipData.newPlainText(label, text));
    button.setText("تم النسخ");
}

private String buildReport() {
    StringBuilder s = new StringBuilder();

    s.append("JVC Diagnostic Report\n\n");
    s.append("Manufacturer: ").append(Build.MANUFACTURER).append("\n");
    s.append("Brand: ").append(Build.BRAND).append("\n");
    s.append("Model: ").append(Build.MODEL).append("\n");
    s.append("Device: ").append(Build.DEVICE).append("\n");
    s.append("Product: ").append(Build.PRODUCT).append("\n");
    s.append("Board: ").append(Build.BOARD).append("\n");
    s.append("Hardware: ").append(Build.HARDWARE).append("\n");
    s.append("Display: ").append(Build.DISPLAY).append("\n");
    s.append("Bootloader: ").append(Build.BOOTLOADER).append("\n");
    s.append("Build Type: ").append(Build.TYPE).append("\n");
    s.append("Build Tags: ").append(Build.TAGS).append("\n");
    s.append("Android: ").append(Build.VERSION.RELEASE).append("\n");
    s.append("SDK: ").append(Build.VERSION.SDK_INT).append("\n");
    s.append("Build ID: ").append(Build.ID).append("\n");
    s.append("Fingerprint: ").append(Build.FINGERPRINT).append("\n");
    s.append("CPU ABI: ").append(Build.SUPPORTED_ABIS[0]).append("\n\n");

    s.append("HOME / Launcher activities:\n");
    PackageManager pm = getPackageManager();
    Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_HOME);

    List<ResolveInfo> homes = pm.queryIntentActivities(intent, 0);
    for (ResolveInfo r : homes) {
        s.append("- ")
         .append(r.activityInfo.packageName)
         .append(" / ")
         .append(r.activityInfo.name)
         .append("\n");
    }

    s.append("\nInstalled apps visible to this app:\n");
    Intent launchIntent = new Intent(Intent.ACTION_MAIN);
    launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
    List<ResolveInfo> apps = pm.queryIntentActivities(launchIntent, 0);

    for (ResolveInfo r : apps) {
        s.append("- ")
         .append(r.activityInfo.packageName)
         .append(" (")
         .append(r.loadLabel(pm))
         .append(")\n");
    }

    s.append("\nNote: This report cannot identify every hardware component "
            + "or confirm firmware compatibility.");

    return s.toString();
}

private String buildPropertiesReport() {
    StringBuilder s = new StringBuilder();
    s.append("JVC System Properties\n\n");

    try {
        Process process = Runtime.getRuntime().exec(new String[]{"getprop"});
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            String lower = line.toLowerCase();
            if (lower.contains("ro.product")
                    || lower.contains("ro.build")
                    || lower.contains("ro.hardware")
                    || lower.contains("ro.board")
                    || lower.contains("ro.boot")
                    || lower.contains("ro.mediatek")
                    || lower.contains("ro.vendor")) {
                s.append(line).append("\n");
            }
        }

        reader.close();
        process.waitFor();
    } catch (Exception e) {
        s.append("Could not read properties: ")
         .append(e.getClass().getSimpleName())
         .append("\n");
    }

    return s.toString();
}

}
