package com.jvc.diagnostic;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int SAVE_REPORT = 1001;
    private String report = "";
    private TextView reportView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 20, 20, 20);

        TextView title = new TextView(this);
        title.setText("JVC Diagnostic - تقرير الشاشة");
        title.setTextSize(23);
        root.addView(title);

        reportView = new TextView(this);
        reportView.setTextSize(13);
        reportView.setTextIsSelectable(true);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(reportView);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        addButton(root, "تحديث التقرير", v -> refreshReport());

        addButton(root, "نسخ التقرير", v -> {
            ClipboardManager cb = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);
            cb.setPrimaryClip(ClipData.newPlainText("JVC Report", report));
            Toast.makeText(this, "تم نسخ التقرير", Toast.LENGTH_SHORT).show();
        });

        addButton(root, "حفظ التقرير TXT", v -> saveReport());
        addButton(root, "مشاركة التقرير", v -> shareReport());

        setContentView(root);
        refreshReport();
    }

    private void addButton(LinearLayout root, String text,
                           android.view.View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setOnClickListener(listener);
        root.addView(b);
    }

    private void refreshReport() {
        report = sanitizeReport(buildReport());
        reportView.setText(report);
        Toast.makeText(this, "تم تحديث التقرير", Toast.LENGTH_SHORT).show();
    }

    /*
     * إزالة الرقم التسلسلي وأي قيمة مطابقة له من التقرير كله.
     * لا يغيّر الرقم التسلسلي في النظام نفسه.
     */
    private String sanitizeReport(String text) {
        StringBuilder result = new StringBuilder();

        for (String line : text.split("\n", -1)) {
            String lower = line.toLowerCase(Locale.US);

            if (lower.contains("serialno")
                    || lower.contains("serial_number")) {
                result.append("[الرقم التسلسلي مخفي]");
            } else {
                result.append(line);
            }

            result.append("\n");
        }

        return result.toString();
    }

    private void section(StringBuilder s, String name) {
        s.append("\n\n========== ").append(name).append(" ==========\n");
    }

    private void add(StringBuilder s, String key, Object value) {
        s.append(key).append(": ")
                .append(value == null ? "غير متاح" : value)
                .append("\n");
    }

    private String buildReport() {
        StringBuilder s = new StringBuilder();

        s.append("JVC DIAGNOSTIC REPORT\n");
        s.append("وقت التقرير: ")
                .append(new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss", Locale.US)
                        .format(new Date())).append("\n");
        s.append("النطاق: شاشة التلفزيون فقط\n");
        s.append("الرقم التسلسلي: مخفي من التقرير\n");

        section(s, "DEVICE AND ANDROID");
        add(s, "Manufacturer", Build.MANUFACTURER);
        add(s, "Brand", Build.BRAND);
        add(s, "Model", Build.MODEL);
        add(s, "Device", Build.DEVICE);
        add(s, "Product", Build.PRODUCT);
        add(s, "Board", Build.BOARD);
        add(s, "Hardware", Build.HARDWARE);
        add(s, "Bootloader", Build.BOOTLOADER);
        add(s, "Android", Build.VERSION.RELEASE);
        add(s, "SDK", Build.VERSION.SDK_INT);
        add(s, "Build ID", Build.ID);
        add(s, "Build display", Build.DISPLAY);
        add(s, "Build type", Build.TYPE);
        add(s, "Build tags", Build.TAGS);
        add(s, "Fingerprint", Build.FINGERPRINT);
        add(s, "ABIs", join(Build.SUPPORTED_ABIS));

        section(s, "DISPLAY");
        try {
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            Display d = wm.getDefaultDisplay();
            DisplayMetrics m = new DisplayMetrics();
            Point p = new Point();
            d.getRealMetrics(m);
            d.getRealSize(p);
            add(s, "Resolution", p.x + " x " + p.y);
            add(s, "Density DPI", m.densityDpi);
            add(s, "Density scale", m.density);
            add(s, "Refresh rate Hz", d.getRefreshRate());
            add(s, "Rotation", rotation(d.getRotation()));
        } catch (Exception e) {
            add(s, "Display error", e.toString());
        }

        section(s, "RAM");
        try {
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            add(s, "Total RAM", bytes(mi.totalMem));
            add(s, "Available RAM", bytes(mi.availMem));
            add(s, "Low memory", mi.lowMemory);
            add(s, "Low memory threshold", bytes(mi.threshold));
            add(s, "/proc/meminfo", readFile("/proc/meminfo", 80));
        } catch (Exception e) {
            add(s, "RAM error", e.toString());
        }

        section(s, "STORAGE");
        storage(s, "Data", Environment.getDataDirectory());
        storage(s, "System", Environment.getRootDirectory());
        storage(s, "Shared storage", Environment.getExternalStorageDirectory());

        section(s, "PARTITIONS AND MOUNTS");
        add(s, "/proc/partitions", readFile("/proc/partitions", 200));
        add(s, "/proc/mounts", readFile("/proc/mounts", 250));
        add(s, "/proc/mtd", readFile("/proc/mtd", 100));
        add(s, "/proc/cmdline", readFile("/proc/cmdline", 20));

        section(s, "CPU AND KERNEL");
        add(s, "Available processors", Runtime.getRuntime().availableProcessors());
        add(s, "Java VM", System.getProperty("java.vm.name"));
        add(s, "OS version", System.getProperty("os.version"));
        add(s, "/proc/version", readFile("/proc/version", 30));
        add(s, "/proc/cpuinfo", readFile("/proc/cpuinfo", 160));

        // تم تصحيح هذا الجزء بحيث يتم استدعاء الدالة مباشرة دون تكرار العنوان
        addCompatibilityInfo(s);

        section(s, "SENSORS");
        try {
            SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
            List<Sensor> sensors = sm.getSensorList(Sensor.TYPE_ALL);
            add(s, "Sensor count", sensors.size());
            for (Sensor sensor : sensors) {
                s.append("- ").append(sensor.getName())
                        .append(" | vendor=").append(sensor.getVendor())
                        .append(" | type=").append(sensor.getType())
                        .append(" | version=").append(sensor.getVersion())
                        .append("\n");
            }
        } catch (Exception e) {
            add(s, "Sensor error", e.toString());
        }

        section(s, "LAUNCHABLE APPLICATIONS");
        try {
            PackageManager pm = getPackageManager();
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> apps = pm.queryIntentActivities(i, 0);
            add(s, "Visible launchable entries", apps.size());

            for (ResolveInfo r : apps) {
                String pkg = r.activityInfo.packageName;
                String version = "unknown";
                String type = "unknown";
                try {
                    PackageInfo pi = pm.getPackageInfo(pkg, 0);
                    version = pi.versionName;
                    ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                    type = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                            ? "SYSTEM" : "USER";
                } catch (Exception ignored) { }

                s.append("- ").append(r.loadLabel(pm))
                        .append(" | package=").append(pkg)
                        .append(" | type=").append(type)
                        .append(" | version=").append(version)
                        .append(" | activity=").append(r.activityInfo.name)
                        .append("\n");
            }
        } catch (Exception e) {
            add(s, "App list error", e.toString());
        }

        section(s, "ALL INSTALLED PACKAGES - FULL INVENTORY");
        s.append("يشمل الحزم التي يعرضها PackageManager، بما فيها حزم النظام التي لا تظهر في الشاشة الرئيسية.\n");
        s.append("لا يتطلب هذا الفحص تعطيل التطبيقات أو تغيير إعدادات التلفزيون.\n");
        try {
            PackageManager pm = getPackageManager();
            List<PackageInfo> packages = pm.getInstalledPackages(0);
            int enabledCount = 0;
            int disabledCount = 0;
            int systemCount = 0;
            int userCount = 0;
            add(s, "Installed package count", packages.size());

            for (PackageInfo pi : packages) {
                ApplicationInfo ai = pi.applicationInfo;
                if (ai == null) continue;

                boolean isSystem = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                boolean isUpdatedSystem = (ai.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
                String type = isSystem
                        ? (isUpdatedSystem ? "SYSTEM (UPDATED)" : "SYSTEM")
                        : "USER";

                int enabledSetting;
                String enabledState;
                try {
                    enabledSetting = pm.getApplicationEnabledSetting(pi.packageName);
                    if (enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                            || enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                            || enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED) {
                        enabledState = "DISABLED";
                    } else if (enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                        enabledState = "ENABLED";
                    } else {
                        enabledState = ai.enabled ? "ENABLED (DEFAULT)" : "DISABLED (DEFAULT)";
                    }
                } catch (Exception ignored) {
                    enabledState = ai.enabled ? "ENABLED (APP INFO)" : "DISABLED (APP INFO)";
                }

                if (enabledState.startsWith("ENABLED")) enabledCount++;
                else disabledCount++;
                if (isSystem) systemCount++; else userCount++;

                String label;
                try {
                    CharSequence appLabel = pm.getApplicationLabel(ai);
                    label = appLabel == null ? "(no label)" : appLabel.toString();
                } catch (Exception ignored) {
                    label = "(label unavailable)";
                }

                s.append("- name=").append(label.replace("\n", " ").replace("\\r", " "))
                        .append(" | package=").append(pi.packageName)
                        .append(" | version=").append(pi.versionName == null ? "unknown" : pi.versionName)
                        .append(" | versionCode=").append(pi.versionCode)
                        .append(" | type=").append(type)
                        .append(" | state=").append(enabledState)
                        .append("\n");
            }

            add(s, "System packages", systemCount);
            add(s, "User packages", userCount);
            add(s, "Enabled packages", enabledCount);
            add(s, "Disabled packages", disabledCount);
        } catch (Exception e) {
            add(s, "Installed package inventory error", e.getClass().getSimpleName()
                    + ": " + e.getMessage());
        }

        addTargetPackageDetails(s, "com.example.user.myapplication");

        section(s, "HOME ACTIVITIES");
        try {
            PackageManager pm = getPackageManager();
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            List<ResolveInfo> homes = pm.queryIntentActivities(i, 0);
            for (ResolveInfo r : homes) {
                s.append("- ").append(r.activityInfo.packageName)
                        .append("/").append(r.activityInfo.name).append("\n");
            }
        } catch (Exception e) {
            add(s, "HOME query error", e.toString());
        }

        section(s, "NETWORK - THIS TV ONLY");
        try {
            for (NetworkInterface ni :
                    Collections.list(NetworkInterface.getNetworkInterfaces())) {
                s.append("Interface: ").append(ni.getName())
                        .append(" | up=").append(ni.isUp())
                        .append(" | loopback=").append(ni.isLoopback())
                        .append(" | mtu=").append(ni.getMTU()).append("\n");

                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    s.append("  Address: ").append(a.getHostAddress()).append("\n");
                }
            }
        } catch (Exception e) {
            add(s, "Interface info", "غير متاح: " + e.getMessage());
        }

        try {
            ConnectivityManager cm = (ConnectivityManager)
                    getSystemService(CONNECTIVITY_SERVICE);
            Network active = cm.getActiveNetwork();
            if (active == null) {
                add(s, "Active network", "غير متاح أو محجوب");
            } else {
                NetworkCapabilities c = cm.getNetworkCapabilities(active);
                if (c != null) {
                    add(s, "Wi-Fi transport",
                            c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
                    add(s, "Ethernet transport",
                            c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
                    add(s, "Internet capability",
                            c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
                    add(s, "Validated internet",
                            c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
                }
                LinkProperties lp = cm.getLinkProperties(active);
                if (lp != null) {
                    add(s, "Interface", lp.getInterfaceName());
                    add(s, "Link addresses", lp.getLinkAddresses());
                    add(s, "Routes", lp.getRoutes());
                    add(s, "DNS servers", lp.getDnsServers());
                }
            }
        } catch (Exception e) {
            add(s, "Network details", "غير متاح: " + e.getMessage());
        }

        section(s, "LOCAL TCP / UDP TABLES");
        s.append("هذه جداول الشاشة نفسها، وليست فحصًا للأجهزة الأخرى.\n");
        portTable(s, "/proc/net/tcp");
        portTable(s, "/proc/net/tcp6");
        portTable(s, "/proc/net/udp");
        portTable(s, "/proc/net/udp6");

        section(s, "SYSTEM PROPERTIES");
        try {
            Process p = Runtime.getRuntime().exec("getprop");
            BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
            String line;
            int count = 0;
            while ((line = r.readLine()) != null && count < 600) {
                String l = line.toLowerCase(Locale.US);

                // تجاهل خاصية الرقم التسلسلي بشكل صريح.
                if (l.contains("ro.boot.serialno")
                        || l.contains("serialno")
                        || l.contains("serial_number")) {
                    continue;
                }

                if (l.contains("ro.product") || l.contains("ro.build")
                        || l.contains("ro.hardware") || l.contains("ro.board")
                        || l.contains("ro.boot") || l.contains("ro.mediatek")
                        || l.contains("ro.vendor") || l.contains("ro.soc")
                        || l.contains("ro.opengles") || l.contains("ro.sf")
                        || l.contains("wifi") || l.contains("bluetooth")) {
                    s.append(line).append("\n");
                    count++;
                }
            }
            r.close();
            add(s, "Properties returned", count);
        } catch (Exception e) {
            add(s, "getprop error", e.toString());
        }

        section(s, "UPDATE AND RECOVERY - READ ONLY");
        addUpdateRecoveryInfo(s);

        section(s, "LIMITATIONS");
        s.append("تطبيق عادي؛ قد يمنع Android قراءة بعض الملفات أو تفاصيل الأجهزة.\n");
        s.append("جداول المنافذ قد تكون محجوبة، وقد لا تتوفر ملكية العملية.\n");
        s.append("لم يتم فحص أي جهاز خارجي أو تغيير إعدادات النظام.\n");
        s.append("التقرير لا يثبت توافق أي نظام بديل.\n");

        return s.toString();
    }

    /*
     * تفاصيل قراءة فقط للحزمة محل الفحص. لا يتم تشغيلها أو تعطيلها أو تعديلها.
     */
    private void addTargetPackageDetails(StringBuilder s, String packageName) {
        section(s, "TARGET PACKAGE DETAILS - READ ONLY");
        add(s, "Target package", packageName);
        try {
            PackageManager pm = getPackageManager();
            PackageInfo pi = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            ApplicationInfo ai = pi.applicationInfo;

            String label;
            try {
                CharSequence appLabel = pm.getApplicationLabel(ai);
                label = appLabel == null ? "(no label)" : appLabel.toString();
            } catch (Exception e) {
                label = "(label unavailable)";
            }

            add(s, "Application label", label);
            add(s, "Version name", pi.versionName == null ? "unknown" : pi.versionName);
            add(s, "Version code", pi.versionCode);
            add(s, "System app flag", (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
            add(s, "Updated system app flag",
                    (ai.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);
            add(s, "Application enabled", ai.enabled);
            add(s, "Enabled setting", pm.getApplicationEnabledSetting(packageName));
            add(s, "Install source directory", ai.sourceDir);
            add(s, "Public source directory", ai.publicSourceDir);
            add(s, "Native library directory", ai.nativeLibraryDir);
            add(s, "Data directory", ai.dataDir);
            add(s, "UID", ai.uid);
            add(s, "First install time", new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(pi.firstInstallTime)));
            add(s, "Last update time", new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(pi.lastUpdateTime)));

            if (Build.VERSION.SDK_INT >= 30) {
                try {
                    android.content.pm.InstallSourceInfo source = pm.getInstallSourceInfo(packageName);
                    add(s, "Installing package", source.getInstallingPackageName());
                    add(s, "Initiating package", source.getInitiatingPackageName());
                    add(s, "Originating package", source.getOriginatingPackageName());
                } catch (Exception e) {
                    add(s, "Install source details", "غير متاح: " + e.getClass().getSimpleName());
                }
            } else {
                add(s, "Installer package", pm.getInstallerPackageName(packageName));
            }

            String[] requested = pi.requestedPermissions;
            add(s, "Requested permission count", requested == null ? 0 : requested.length);
            if (requested != null) {
                for (String permission : requested) {
                    s.append("- requested-permission=").append(permission).append("\n");
                }
            }

            try {
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.setPackage(packageName);
                List<ResolveInfo> homeActivities = pm.queryIntentActivities(homeIntent, 0);
                add(s, "HOME activities in target package", homeActivities.size());
                for (ResolveInfo r : homeActivities) {
                    s.append("- HOME activity=").append(r.activityInfo.name).append("\n");
                }
            } catch (Exception e) {
                add(s, "Target HOME activity check", e.getClass().getSimpleName());
            }

            s.append("ملاحظة: مسار system app وحده لا يثبت أن التطبيق آمن أو ضار.\n");
        } catch (PackageManager.NameNotFoundException e) {
            add(s, "Target package result", "الحزمة غير موجودة أو غير ظاهرة لـ PackageManager");
        } catch (Exception e) {
            add(s, "Target package inspection error",
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private String join(String[] values) {
        StringBuilder b = new StringBuilder();
        if (values != null) {
            for (String v : values) {
                if (b.length() > 0) b.append(", ");
                b.append(v);
            }
        }
        return b.toString();
    }

    private String bytes(long n) {
        if (n < 0) return "unknown";
        return String.format(Locale.US, "%.2f GB",
                n / (1024.0 * 1024.0 * 1024.0));
    }

    private void storage(StringBuilder s, String label, File f) {
        try {
            StatFs st = new StatFs(f.getAbsolutePath());
            long total = st.getBlockCountLong() * st.getBlockSizeLong();
            long free = st.getAvailableBlocksLong() * st.getBlockSizeLong();
            s.append(label).append(": path=").append(f.getAbsolutePath())
                    .append(" | total=").append(bytes(total))
                    .append(" | available=").append(bytes(free)).append("\n");
        } catch (Exception e) {
            add(s, label, "غير متاح: " + e.getMessage());
        }
    }

    private String readFile(String path, int maxLines) {
        try {
            File f = new File(path);
            if (!f.canRead()) return "Permission denied";
            BufferedReader r = new BufferedReader(
                    new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8));
            StringBuilder b = new StringBuilder();
            String line;
            int n = 0;
            while ((line = r.readLine()) != null && n++ < maxLines) {
                b.append(line).append("\n");
            }
            r.close();
            return b.length() == 0 ? "Empty" : b.toString();
        } catch (Exception e) {
            return "غير متاح: " + e.getClass().getSimpleName();
        }
    }

    private void portTable(StringBuilder s, String path) {
        try {
            File f = new File(path);
            if (!f.canRead()) {
                s.append(path).append(": permission denied\n");
                return;
            }
            BufferedReader r = new BufferedReader(
                    new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8));
            String line;
            int n = 0;
            while ((line = r.readLine()) != null && n++ < 250) {
                s.append(path).append(": ").append(line).append("\n");
            }
            r.close();
            if (n == 0) s.append(path).append(": empty\n");
        } catch (Exception e) {
            s.append(path).append(": unavailable (")
                    .append(e.getClass().getSimpleName()).append(")\n");
        }
    }

    private String rotation(int r) {
        if (r == Surface.ROTATION_0) return "0";
        if (r == Surface.ROTATION_90) return "90";
        if (r == Surface.ROTATION_180) return "180";
        if (r == Surface.ROTATION_270) return "270";
        return String.valueOf(r);
    }

    private void saveReport() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TITLE, "JVC-Diagnostic-Report.txt");
        try {
            startActivityForResult(i, SAVE_REPORT);
        } catch (Exception e) {
            Toast.makeText(this, "استخدم خيار مشاركة التقرير",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != SAVE_REPORT || result != RESULT_OK
                || data == null || data.getData() == null) return;

        try {
            OutputStream out = getContentResolver().openOutputStream(
                    data.getData(), "w");
            if (out == null) throw new Exception("Output unavailable");
            out.write(report.getBytes(StandardCharsets.UTF_8));
            out.close();
            Toast.makeText(this, "تم حفظ التقرير",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "تعذر الحفظ؛ استخدم المشاركة",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void shareReport() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "JVC Diagnostic Report");
        i.putExtra(Intent.EXTRA_TEXT, report);
        startActivity(Intent.createChooser(i, "مشاركة تقرير JVC"));
    }

    private void addCompatibilityInfo(StringBuilder s) {
        section(s, "ANDROID COMPATIBILITY");

        add(s, "Android SDK", Build.VERSION.SDK_INT);
        add(s, "Android release", Build.VERSION.RELEASE);
        add(s, "Supported ABIs", join(Build.SUPPORTED_ABIS));

        try {
            ActivityManager am = (ActivityManager)
                    getSystemService(ACTIVITY_SERVICE);

            ConfigurationInfo config =
                    am.getDeviceConfigurationInfo();

            if (config != null) {
                add(s, "OpenGL ES version", config.getGlEsVersion());
                add(s, "OpenGL ES required by app",
                        config.reqGlEsVersion);
            }
        } catch (Exception e) {
            add(s, "Graphics compatibility",
                    "غير متاح: " + e.getClass().getSimpleName());
        }

        try {
            PackageManager pm = getPackageManager();

            add(s, "Touchscreen feature",
                    pm.hasSystemFeature(
                            PackageManager.FEATURE_TOUCHSCREEN));

            add(s, "Wi-Fi feature",
                    pm.hasSystemFeature(
                            PackageManager.FEATURE_WIFI));

            add(s, "Ethernet feature",
                    pm.hasSystemFeature(
                            PackageManager.FEATURE_ETHERNET));

            add(s, "Leanback TV feature",
                    pm.hasSystemFeature(
                            PackageManager.FEATURE_LEANBACK));

            add(s, "Low RAM device",
                    pm.hasSystemFeature(
                            PackageManager.FEATURE_RAM_LOW));

            add(s, "Available system features",
                    pm.getSystemAvailableFeatures().length);

        } catch (Exception e) {
            add(s, "System features",
                    "غير متاح: " + e.getClass().getSimpleName());
        }

        s.append("ملاحظة: هذه المعلومات لا تثبت توافق نظام بديل ")
                .append("مع التلفزيون.\n");
    }

    private void addUpdateRecoveryInfo(StringBuilder s) {
        s.append("فحص قراءة فقط؛ لا يتم تنفيذ تحديث أو استرداد.\n");

        // خصائص النظام المرتبطة بالتحديث والإقلاع
        try {
            Process p = Runtime.getRuntime().exec("getprop");
            BufferedReader r = new BufferedReader(
                    new InputStreamReader(
                            p.getInputStream(),
                            StandardCharsets.UTF_8));

            String line;
            int count = 0;

            while ((line = r.readLine()) != null && count < 100) {
                String lower = line.toLowerCase(Locale.US);

                if ((lower.contains("ota")
                        || lower.contains("recovery")
                        || lower.contains("update")
                        || lower.contains("slot_suffix")
                        || lower.contains("ab_update"))
                        && !lower.contains("serial")) {
                    s.append(line).append("\n");
                    count++;
                }
            }

            r.close();
            p.destroy();

            add(s, "Update-related properties", count);
        } catch (Exception e) {
            add(s, "Update properties error",
                    e.getClass().getSimpleName());
        }

        // التطبيقات التي قد ترتبط بالتحديث أو الاسترداد
        try {
            PackageManager pm = getPackageManager();
            List<PackageInfo> packages = pm.getInstalledPackages(0);
            int found = 0;

            for (PackageInfo pi : packages) {
                String name = pi.packageName.toLowerCase(Locale.US);

                if (name.contains("update")
                        || name.contains("recovery")
                        || name.contains("firmware")
                        || name.contains("ota")
                        || name.contains("upgrade")) {

                    s.append("- package=").append(pi.packageName)
                            .append(" | version=")
                            .append(pi.versionName == null
                                    ? "unknown" : pi.versionName)
                            .append("\n");
                    found++;
                }
            }

            add(s, "Matching installed packages", found);
        } catch (Exception e) {
            add(s, "Package inspection error",
                    e.getClass().getSimpleName());
        }

        // فحص وجود الملفات فقط، دون قراءتها أو تشغيلها
        String[] paths = {
            "/system/bin/recovery",
            "/system/bin/update_engine",
            "/system/bin/install-recovery.sh",
            "/system/recovery-from-boot.p",
            "/system/etc/security/otacerts.zip",
            "/cache/recovery/command",
            "/cache/recovery/last_log"
        };

        for (String path : paths) {
            try {
                File f = new File(path);
                add(s, path,
                        "exists=" + f.exists()
                        + ", readable=" + f.canRead());
            } catch (Exception e) {
                add(s, path, "غير متاح");
            }
        }

        s.append("ملاحظة: وجود الملفات أو التطبيقات لا يثبت ")
                .append("أن التحديث متاح أو متوافق مع هذا التلفزيون.\n");
    }
}
