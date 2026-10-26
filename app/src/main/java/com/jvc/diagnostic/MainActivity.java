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
import android.graphics.Point;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private String report;
    private TextView info;
    private Button saveButton;

    private static final int REQUEST_SAVE_REPORT = 1001;

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

        info = new TextView(this);
        info.setText(report);
        info.setTextSize(14);
        info.setTextIsSelectable(true);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(info);

        layout.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        Button refresh = new Button(this);
        refresh.setText("تحديث التقرير");
        refresh.setOnClickListener(v -> {
            report = buildReport();
            info.setText(report);
            Toast.makeText(
                    this,
                    "تم تحديث التقرير",
                    Toast.LENGTH_SHORT
            ).show();
        });
        layout.addView(refresh);

        Button copy = new Button(this);
        copy.setText("نسخ التقرير الكامل");
        copy.setOnClickListener(v ->
                copyText(copy, report, "JVC Diagnostic Report"));
        layout.addView(copy);

        saveButton = new Button(this);
        saveButton.setText("حفظ التقرير TXT");
        saveButton.setOnClickListener(v -> saveReport());
        layout.addView(saveButton);

        Button share = new Button(this);
        share.setText("مشاركة التقرير");
        share.setOnClickListener(v -> shareReport());
        layout.addView(share);

        setContentView(layout);
    }

    private void copyText(Button button, String text, String label) {
        ClipboardManager cb = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);

        cb.setPrimaryClip(ClipData.newPlainText(label, text));
        button.setText("تم النسخ");

        Toast.makeText(
                this,
                "تم نسخ التقرير",
                Toast.LENGTH_SHORT
        ).show();
    }

    private String buildReport() {
        StringBuilder s = new StringBuilder();

        s.append("JVC DIAGNOSTIC REPORT\n");
        s.append("=====================\n\n");

        s.append("DEVICE INFORMATION\n");
        s.append("------------------\n");
        add(s, "Manufacturer", Build.MANUFACTURER);
        add(s, "Brand", Build.BRAND);
        add(s, "Model", Build.MODEL);
        add(s, "Device", Build.DEVICE);
        add(s, "Product", Build.PRODUCT);
        add(s, "Board", Build.BOARD);
        add(s, "Hardware", Build.HARDWARE);
        add(s, "Display Build", Build.DISPLAY);
        add(s, "Bootloader", Build.BOOTLOADER);
        add(s, "Build Type", Build.TYPE);
        add(s, "Build Tags", Build.TAGS);

        s.append("\nANDROID INFORMATION\n");
        s.append("-------------------\n");
        add(s, "Android Version", Build.VERSION.RELEASE);
        add(s, "SDK Level", String.valueOf(Build.VERSION.SDK_INT));
        add(s, "Build ID", Build.ID);
        add(s, "Fingerprint", Build.FINGERPRINT);

        if (Build.SUPPORTED_ABIS != null) {
            add(s, "Supported ABIs",
                    join(Build.SUPPORTED_ABIS));
        }

        s.append("\nSCREEN INFORMATION\n");
        s.append("------------------\n");
        appendScreenInfo(s);

        s.append("\nHOME / LAUNCHER ACTIVITIES\n");
        s.append("-------------------------\n");
        appendHomeActivities(s);

        s.append("\nLAUNCHABLE APPLICATIONS\n");
        s.append("-----------------------\n");
        appendLaunchableApps(s);

        s.append("\nSYSTEM PROPERTIES\n");
        s.append("-----------------\n");
        s.append(buildPropertiesReport());

        s.append("\nREPORT NOTES\n");
        s.append("-------------\n");
        s.append("This report includes information accessible ");
        s.append("to this application.\n");
        s.append("Some system or hardware details may be ");
        s.append("restricted by Android.\n");
        s.append("This report does not confirm firmware compatibility.\n");

        return s.toString();
    }

    private void add(StringBuilder s, String key, String value) {
        s.append(key)
         .append(": ")
         .append(value == null ? "Unavailable" : value)
         .append("\n");
    }

    private String join(String[] values) {
        StringBuilder s = new StringBuilder();

        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                s.append(", ");
            }
            s.append(values[i]);
        }

        return s.toString();
    }

    private void appendScreenInfo(StringBuilder s) {
        try {
            WindowManager wm =
                    (WindowManager) getSystemService(WINDOW_SERVICE);

            Display display = wm.getDefaultDisplay();

            DisplayMetrics metrics = new DisplayMetrics();
            display.getRealMetrics(metrics);

            Point realSize = new Point();
            display.getRealSize(realSize);

            add(s, "Screen Width Pixels",
                    String.valueOf(realSize.x));
            add(s, "Screen Height Pixels",
                    String.valueOf(realSize.y));

            add(s, "Density DPI",
                    String.valueOf(metrics.densityDpi));

            add(s, "Density Scale",
                    String.format(
                            Locale.US,
                            "%.2f",
                            metrics.density
                    ));

            add(s, "Refresh Rate Hz",
                    String.format(
                            Locale.US,
                            "%.2f",
                            display.getRefreshRate()
                    ));

            add(s, "Pixel Format",
                    String.valueOf(display.getPixelFormat()));

            add(s, "Rotation",
                    String.valueOf(display.getRotation()));

        } catch (Exception e) {
            s.append("Screen information unavailable: ")
             .append(e.getClass().getSimpleName())
             .append("\n");
        }
    }

    private void appendHomeActivities(StringBuilder s) {
        PackageManager pm = getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);

        try {
            List<ResolveInfo> homes =
                    pm.queryIntentActivities(intent, 0);

            if (homes == null || homes.isEmpty()) {
                s.append("No HOME activities returned.\n");
                return;
            }

            for (ResolveInfo r : homes) {
                s.append("- ")
                 .append(r.activityInfo.packageName)
                 .append("/")
                 .append(r.activityInfo.name)
                 .append("\n");
            }

        } catch (Exception e) {
            s.append("Unable to read HOME activities: ")
             .append(e.getClass().getSimpleName())
             .append("\n");
        }
    }

    private void appendLaunchableApps(StringBuilder s) {
        PackageManager pm = getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        try {
            List<ResolveInfo> apps =
                    pm.queryIntentActivities(intent, 0);

            if (apps == null || apps.isEmpty()) {
                s.append("No launchable applications returned.\n");
                return;
            }

            for (ResolveInfo r : apps) {
                s.append("- ")
                 .append(r.activityInfo.packageName)
                 .append(" | ")
                 .append(r.loadLabel(pm))
                 .append("\n");
            }

        } catch (Exception e) {
            s.append("Unable to read applications: ")
             .append(e.getClass().getSimpleName())
             .append("\n");
        }
    }

    private String buildPropertiesReport() {
        StringBuilder s = new StringBuilder();

        Process process = null;

        try {
            process = Runtime.getRuntime().exec(
                    new String[]{"getprop"}
            );

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            process.getInputStream(),
                            StandardCharsets.UTF_8
                    )
            );

            String line;
            int count = 0;

            while ((line = reader.readLine()) != null) {
                String lower = line.toLowerCase(Locale.ROOT);

                if (lower.contains("ro.product")
                        || lower.contains("ro.build")
                        || lower.contains("ro.hardware")
                        || lower.contains("ro.board")
                        || lower.contains("ro.boot")
                        || lower.contains("ro.mediatek")
                        || lower.contains("ro.vendor")
                        || lower.contains("ro.soc")
                        || lower.contains("ro.opengles")
                        || lower.contains("ro.sf.lcd_density")
                        || lower.contains("ro.config")) {

                    s.append(line).append("\n");
                    count++;
                }
            }

            reader.close();

            int exitCode = process.waitFor();

            if (count == 0) {
                s.append("No matching properties were returned.\n");
            }

            s.append("getprop exit code: ")
             .append(exitCode)
             .append("\n");

        } catch (Exception e) {
            s.append("Properties unavailable: ")
             .append(e.getClass().getSimpleName())
             .append("\n");

        } finally {
            if (process != null) {
                process.destroy();
            }
        }

        return s.toString();
    }

    private void saveReport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(
                Intent.EXTRA_TITLE,
                "JVC-Diagnostic-Report.txt"
        );

        try {
            startActivityForResult(intent, REQUEST_SAVE_REPORT);
        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "تعذر فتح نافذة حفظ الملف",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_SAVE_REPORT
                || resultCode != RESULT_OK
                || data == null
                || data.getData() == null) {
            return;
        }

        OutputStream output = null;
        BufferedWriter writer = null;

        try {
            output = getContentResolver().openOutputStream(
                    data.getData(),
                    "w"
            );

            if (output == null) {
                throw new IOException("Unable to open output file");
            }

            writer = new BufferedWriter(
                    new OutputStreamWriter(
                            output,
                            StandardCharsets.UTF_8
                    )
            );

            writer.write(report);
            writer.flush();

            Toast.makeText(
                    this,
                    "تم حفظ التقرير بنجاح",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "فشل حفظ التقرير: " + e.getClass().getSimpleName(),
                    Toast.LENGTH_LONG
            ).show();

        } finally {
            try {
                if (writer != null) {
                    writer.close();
                } else if (output != null) {
                    output.close();
                }
            } catch (IOException ignored) {
            }
        }
    }

    private void shareReport() {
        try {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_SUBJECT, "JVC Diagnostic Report");
            send.putExtra(Intent.EXTRA_TEXT, report);

            startActivity(Intent.createChooser(
                    send,
                    "مشاركة تقرير JVC"
            ));

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "تعذرت مشاركة التقرير",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}
