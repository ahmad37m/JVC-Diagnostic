
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

import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private String report;

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
        copy.setOnClickListener(v -> {
            ClipboardManager cb = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);
            cb.setPrimaryClip(ClipData.newPlainText("JVC Report", report));
            copy.setText("تم نسخ التقرير");
        });
        layout.addView(copy);

        setContentView(layout);
    }

    private String buildReport() {
        StringBuilder s = new StringBuilder();

        s.append("JVC Diagnostic Report\n\n");
        s.append("Manufacturer: ").append(Build.MANUFACTURER).append("\n");
        s.append("Brand: ").append(Build.BRAND).append("\n");
        s.append("Model: ").append(Build.MODEL).append("\n");
        s.append("Device: ").append(Build.DEVICE).append("\n");
        s.append("Product: ").append(Build.PRODUCT).append("\n");
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
}
