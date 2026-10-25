package com.pctutu.adb;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.os.Environment;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.StrictMode;
import java.io.File;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class SuperADBActivity extends Activity {
    private static final int BG = 0xFF121820;
    private static final int SURFACE = 0xFF26323D;
    private static final int SURFACE_ALT = 0xFF1B2731;
    private static final int ORANGE = 0xFFFF8A00;
    private static final int GREEN = 0xFF54D68A;
    private static final int WHITE = 0xFFF5F7F8;
    private static final int MUTED = 0xFFBAC4CC;
    private LinearLayout root;
    private FrameLayout screen;
    private int systemModulesReturnPage;
    private int pageGeneration;
    private final Handler moduleHandler = new Handler(Looper.getMainLooper());
    private java.util.List<SettingModule> activationQueue;
    private int activationIndex;
    private boolean activationRunning;
    private boolean activationAwaitingReturn;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(createView());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (activationRunning && activationIndex > 0) activationAwaitingReturn = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (activationRunning && activationAwaitingReturn) {
            activationAwaitingReturn = false;
            moduleHandler.postDelayed(this::activateNextModule, 5000);
        }
    }

    private View createView() {
        screen = new FrameLayout(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.TOP);
        root.setPadding(dp(32), dp(24), dp(32), dp(24));
        scroll.addView(root);
        screen.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        addBrandFooter();

        showHome();
        return screen;
    }

    private void addBrandFooter() {
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(dp(7), dp(6), dp(13), dp(6));
        footer.setBackground(makePanelBackground());
        footer.setFocusable(false);
        if (Build.VERSION.SDK_INT >= 16) {
            footer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        ImageView logo = new ImageView(this);
        Bitmap source;
        try {
            source = BitmapFactory.decodeStream(getAssets().open("pctutu_icon.jpg"));
        } catch (java.io.IOException ignored) {
            source = null;
        }
        if (source != null) {
            logo.setImageBitmap(source);
            logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            logo.setBackgroundColor(0xFFFFFFFF);
            logo.setPadding(dp(3), dp(3), dp(3), dp(3));
            footer.addView(logo, params(dp(50), dp(50)));
        }

        TextView brand = text("PCTUTU.com", 14, WHITE, true);
        LinearLayout.LayoutParams brandParams = params(-2, -2);
        brandParams.leftMargin = dp(9);
        footer.addView(brand, brandParams);

        FrameLayout.LayoutParams footerParams = new FrameLayout.LayoutParams(-2, -2, Gravity.RIGHT | Gravity.BOTTOM);
        footerParams.setMargins(0, 0, dp(18), dp(18));
        screen.addView(footer, footerParams);
    }

    private void showHome() {
        pageGeneration++;
        systemModulesReturnPage = 0;
        root.removeAllViews();
        root.addView(text("ANDROID TV  /  ADB SETUP", 14, ORANGE, true));
        TextView title = text("超级ADB", 34, WHITE, true);
        LinearLayout.LayoutParams titleParams = params(-2, -2);
        titleParams.topMargin = dp(8);
        root.addView(title, titleParams);
        TextView intro = text("选择要处理的系统能力。ADB 调试与 ROOT 权限是两类不同操作，取得 ROOT 通常需要解锁或刷入适配固件。", 16, MUTED, false);
        intro.setMaxWidth(contentWidth());
        LinearLayout.LayoutParams introParams = params(contentWidth(), -2);
        introParams.topMargin = dp(8);
        root.addView(intro, introParams);

        addDeviceCard();

        addTvHomeAction();

        addButton(root, "开启 ADB 调试", this::showAdbPage);
        addButton(root, "开启 ROOT 权限", this::showRootPage);
        addButton(root, "重启系统", this::restartSystem);
    }

    private void addDeviceCard() {
        String manufacturer = safeDeviceValue(Build.MANUFACTURER, "未知厂商");
        String model = safeDeviceValue(Build.MODEL, "未知型号");
        String version = safeDeviceValue(Build.VERSION.RELEASE, "未知");
        String ipAddress = getLocalIpv4Address();
        String state = Build.VERSION.SDK_INT >= 30
                ? "可尝试 Wireless debugging"
                : "请优先检查 USB 调试或厂商网络调试";

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(14), dp(18), dp(14));
        card.setBackground(makePanelBackground());
        LinearLayout.LayoutParams cardParams = params(contentWidth(), -2);
        cardParams.topMargin = dp(18);
        cardParams.bottomMargin = dp(8);
        root.addView(card, cardParams);

        TextView caption = text("当前设备", 13, ORANGE, true);
        card.addView(caption);
        TextView identity = text(manufacturer + "  " + model, 18, WHITE, true);
        LinearLayout.LayoutParams identityParams = params(-1, -2);
        identityParams.topMargin = dp(4);
        card.addView(identity, identityParams);
        TextView details = text("Android " + version + "  ·  API " + Build.VERSION.SDK_INT
                + "\n" + state, 14, MUTED, false);
        LinearLayout.LayoutParams detailsParams = params(-1, -2);
        detailsParams.topMargin = dp(5);
        card.addView(details, detailsParams);
        TextView ip = text("当前电视 IP：" + ipAddress, 15, WHITE, true);
        LinearLayout.LayoutParams ipParams = params(-1, -2);
        ipParams.topMargin = dp(7);
        card.addView(ip, ipParams);
        addCapabilityStatus(card);
    }

    private String getLocalIpv4Address() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback()) continue;
                Enumeration<java.net.InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
            // Some vendor builds restrict network interface enumeration.
        }
        try {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wifi != null && wifi.getConnectionInfo() != null) {
                int address = wifi.getConnectionInfo().getIpAddress();
                if (address != 0) {
                    return (address & 0xFF) + "." + ((address >> 8) & 0xFF) + "."
                            + ((address >> 16) & 0xFF) + "." + ((address >> 24) & 0xFF);
                }
            }
        } catch (Exception ignored) {
            // Wi-Fi information may be unavailable on Ethernet-only TVs.
        }
        return "无法确认";
    }

    private void addCapabilityStatus(LinearLayout card) {
        boolean developerEnabled = isDeveloperModeEnabled();
        AdbdStatus adbdStatus = getAdbdStatus();
        boolean rootDetected = isRootDetected();
        LinearLayout status = new LinearLayout(this);
        status.setOrientation(LinearLayout.HORIZONTAL);
        status.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams statusParams = params(-1, -2);
        statusParams.topMargin = dp(12);
        card.addView(status, statusParams);
        addStatusItem(status, "开发者模式", developerEnabled ? "已开启" : "未开启", developerEnabled);
        addStatusItem(status, "adbd", adbdStatus.label, adbdStatus.running);
        addStatusItem(status, "ROOT", rootDetected ? "已检测" : "未检测", rootDetected);
    }

    private enum AdbdStatus {
        RUNNING("运行中", true),
        STOPPED("未运行", false),
        UNKNOWN("无法确认", false);

        final String label;
        final boolean running;

        AdbdStatus(String label, boolean running) {
            this.label = label;
            this.running = running;
        }
    }

    /**
     * init.svc.adbd is the service state and is independent of the developer
     * settings flag. Some vendor TV firmware starts adbd without enabling that
     * flag, so it must be shown as a separate capability.
     */
    private AdbdStatus getAdbdStatus() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"getprop", "init.svc.adbd"});
            String output = readProcessOutput(process).trim();
            int exitCode = process.waitFor();
            if (exitCode != 0 || output.length() == 0) return AdbdStatus.UNKNOWN;
            if ("running".equalsIgnoreCase(output)) return AdbdStatus.RUNNING;
            if ("stopped".equalsIgnoreCase(output) || "stopping".equalsIgnoreCase(output)
                    || "restarting".equalsIgnoreCase(output)) return AdbdStatus.STOPPED;
        } catch (Exception ignored) {
            // Older or restricted vendor builds may not expose getprop.
        }
        return AdbdStatus.UNKNOWN;
    }

    private void addStatusItem(LinearLayout parent, String name, String value, boolean active) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(dp(12), dp(8), dp(12), dp(8));
        item.setBackground(makeStatusBackground(active));
        LinearLayout.LayoutParams itemParams = params(0, -2);
        itemParams.weight = 1;
        itemParams.rightMargin = dp(8);
        parent.addView(item, itemParams);
        item.addView(text(name, 12, MUTED, false));
        item.addView(text(value, 15, active ? GREEN : ORANGE, true));
    }

    private GradientDrawable makeStatusBackground(boolean active) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(active ? 0xFF18372C : 0xFF382A1B);
        shape.setCornerRadius(dp(5));
        shape.setStroke(dp(1), active ? 0xFF286D4A : 0xFF7A5125);
        return shape;
    }

    private boolean isDeveloperModeEnabled() {
        try {
            return Settings.Global.getInt(getContentResolver(), "development_settings_enabled", 0) == 1;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isRootDetected() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            String output = readProcessOutput(process);
            return process.waitFor() == 0 && output.contains("uid=0");
        } catch (Exception ignored) {
            return false;
        }
    }

    private String safeDeviceValue(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private static final String TV_HOME_PACKAGE = "com.pctutu.tvhome";
    private static final String TV_HOME_ACTIVITY = "com.pctutu.tvhome/.MainActivity";

    private void addTvHomeAction() {
        File apk = findTvHomeApk();
        if (isTvHomeInstalled() || apk != null) {
            addSectionTitle("超级桌面");
            addButton(root, "安装超级桌面", () -> handleTvHome(apk));
            addDescription(isTvHomeInstalled()
                    ? "检测到超级桌面已安装。点击后将尝试重新启用桌面、自启动和默认 HOME 设置。"
                    : "检测到 pctutucomtvhome.apk。点击后打开系统安装流程；安装完成后可再次点击激活桌面设置。");
        }
    }

    private File findTvHomeApk() {
        String[] paths = {
                "/sdcard/pctutucomtvhome.apk",
                "/sdcard/Download/pctutucomtvhome.apk",
                "/storage/emulated/0/pctutucomtvhome.apk",
                "/storage/emulated/0/Download/pctutucomtvhome.apk"
        };
        for (String path : paths) {
            File candidate = new File(path);
            if (candidate.isFile() && candidate.canRead()) return candidate;
        }
        return null;
    }

    private boolean isTvHomeInstalled() {
        try {
            getPackageManager().getApplicationInfo(TV_HOME_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    private void handleTvHome(File apk) {
        if (!isTvHomeInstalled()) {
            if (apk == null) {
                toast("未找到 pctutucomtvhome.apk，请放入电视 Download 目录后重试。");
                return;
            }
            installTvHome(apk);
            return;
        }
        activateTvHome();
    }

    private void installTvHome(File apk) {
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
            toast("已打开安装流程，请在系统界面确认安装。");
        } catch (Exception exception) {
            toast("无法打开安装流程，请在文件管理器中手动安装 pctutucomtvhome.apk。");
        }
    }

    private void activateTvHome() {
        new Thread(() -> {
            if (isRootDetected()) {
                String command = "pm enable " + TV_HOME_PACKAGE + "; "
                        + "(cmd package set-home-activity " + TV_HOME_ACTIVITY + " || true); "
                        + "(appops set " + TV_HOME_PACKAGE + " AUTO_START allow || true)";
                boolean ok = runRootCommand(command);
                runOnUiThread(() -> toast(ok
                        ? "已用 ROOT 重新激活超级桌面并设置为 HOME。"
                        : "ROOT 命令执行失败，改用手动设置默认桌面。"));
            } else {
                runOnUiThread(this::openHomeChooser);
            }
        }, "check-tv-home-root").start();
    }

    private void openHomeChooser() {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.addCategory(Intent.CATEGORY_DEFAULT);
            try {
                startActivity(home);
                toast("请选择“超级桌面”，再选择始终使用；无需 ROOT。");
            } catch (Exception exception) {
                toast("请到系统设置 → 应用 → 默认应用 → 主屏幕，选择超级桌面。");
            }
    }

    private boolean runRootCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            return process.waitFor() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void showAdbPage() {
        pageGeneration++;
        systemModulesReturnPage = 1;
        root.removeAllViews();
        addButton(root, "返回分类", this::showHome);
        addSectionTitle("开启 ADB 调试");
        addDescription("从 ADB 尚未开启开始：优先在电视本机打开开发者选项和 USB 调试，或使用厂商网络调试入口。普通 APK 无法越权启动 adbd。");
        addButton(root, "自动开启 ADB", this::enableTcpAdbWithRoot);
        addSectionTitle("自动打开设置");
        addButton(root, "打开开发者选项", this::openDeveloperSettings);
        addButton(root, "打开系统设置 / 关于", this::openSystemSettings);
        addButton(root, "Android 11+：打开无线调试设置", this::openWirelessSettings);
        addSectionTitle("ADB 调试工具");
        addButton(root, "打开系统模块", this::showSystemModules);
        addButton(root, "自助激活模块", this::showAutoActivation);
        addSectionTitle("需要电视操作");
        addButton(root, "按电视品牌查看开启步骤", this::showBrandGuides);
        addSectionTitle("ADB 已建立后的工具");
        addButton(root, "Android 11+：复制配对/连接命令", this::copyPairCommands);
        addButton(root, "USB OTG 控制条件与参考", this::copyOtgGuide);
        addButton(root, "Android 14+ 配对后自恢复说明", this::copyRecoveryGuide);
        addButton(root, "USB ADB 已连通后：复制转无线命令", this::copyLegacyCommands);
        addButton(root, "复制 GitHub ADB 参考项目", this::copyAdbGithubReferences);
        TextView guide = text(
                "无 ADB 起点的可行入口\n"
                + "1. 所有版本：在电视本机打开开发者选项和 USB 调试。Android 4-10 要电脑首次连入，还必须有 USB device/OTG 调试口；电视常见 USB-A Host 口不行。\n"
                + "2. 厂商网络调试：如果电视本机菜单提供 ADB over network，直接开启并记录屏幕显示的 IP/端口。\n"
                + "3. Android 11+：在电视本机打开 Wireless debugging，电脑用 adb pair 配对码，再连设置页显示的连接端口。\n"
                + "4. USB OTG 控制：另一台 Android 可作为控制端，但目标电视仍须先开 USB debugging、有 USB Gadget/device 口并完成首次授权；普通电视 USB-A Host 口不适用。\n"
                + "5. 配对后自恢复：Android 11+ 可在用户先开启 Wireless debugging 并配对后，用本机 ADB TLS 凭据恢复连接；这不是首次开启，自动常驻开放网络 ADB 有安全风险。\n"
                + "第二阶段：USB ADB 已连通后，Android 4-10 可由电脑执行 adb tcpip 5555 转无线。ADB 未开启时若无电视端设置、厂商入口或可用 USB Gadget，就没有通用的远程开启办法。",
                15, MUTED, false);
        guide.setMaxWidth(contentWidth());
        LinearLayout.LayoutParams guideParams = params(contentWidth(), -2);
        guideParams.topMargin = dp(20);
        guide.setBackground(makePanelBackground());
        guide.setPadding(dp(18), dp(16), dp(18), dp(16));
        root.addView(guide, guideParams);
    }

    private void showRootPage() {
        pageGeneration++;
        systemModulesReturnPage = 2;
        root.removeAllViews();
        addButton(root, "返回分类", this::showHome);
        addSectionTitle("开启 ROOT 权限");
        addButton(root, "打开系统模块", this::showSystemModules);
        addDescription("ROOT 是系统超级用户权限。普通 APK、ADB 设置页或一次命令都不能自行授予 ROOT。请先确认设备型号、固件和可恢复方式，再决定是否解锁或刷机。");

        addSectionTitle("设备已经 ROOT");
        addDescription("若设备上已有 Magisk、KernelSU、SuperSU 等 root 管理器，可由用户在授权提示中授予本工具或终端 su 权限。以下命令用于电视本机 root 终端开启 TCP ADB，不会帮未 root 的设备取得 ROOT。");
        addButton(root, "ROOT 自动开启 TCP ADB", this::enableTcpAdbWithRoot);
        addButton(root, "复制已 ROOT 设备的 ADB 命令", this::copyRootCommands);
        addButton(root, "复制 root 无线 ADB 项目参考", this::copyRootGithubReferences);

        addSectionTitle("设备尚未 ROOT");
        addDescription("可行途径取决于具体芯片、Bootloader 和固件：查找厂商正式解锁流程或该型号可信的解锁/刷机方案。解锁常会清除数据、影响 DRM/保修，并存在变砖风险；不要使用其他型号的 boot、recovery 或刷机包。LSPosed 也要求设备已 root，不能用于首次提权。");
        addButton(root, "复制 ROOT / LSPosed 参考项目", this::copyRootGithubReferences);
    }

    private void addSectionTitle(String title) {
        TextView heading = text(title, 23, ORANGE, true);
        LinearLayout.LayoutParams headingParams = params(-2, -2);
        headingParams.topMargin = dp(16);
        root.addView(heading, headingParams);
    }

    private void addDescription(String value) {
        TextView description = text(value, 15, MUTED, false);
        description.setMaxWidth(contentWidth());
        LinearLayout.LayoutParams descriptionParams = params(contentWidth(), -2);
        descriptionParams.topMargin = dp(8);
        descriptionParams.bottomMargin = dp(4);
        root.addView(description, descriptionParams);
    }

    private GradientDrawable makePanelBackground() {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(SURFACE_ALT);
        shape.setCornerRadius(dp(6));
        shape.setStroke(dp(1), 0xFF34434F);
        return shape;
    }

    private void showSystemModules() {
        final int scanGeneration = ++pageGeneration;
        root.removeAllViews();
        addButton(root, "返回", this::returnFromSystemModules);
        addSectionTitle("系统模块");
        addDescription("正在检索系统包中导出的 Activity…");
        new Thread(() -> {
            java.util.List<SystemModule> modules = scanSystemModules();
            new Handler(Looper.getMainLooper()).post(() -> {
                if (scanGeneration == pageGeneration) displaySystemModules(modules);
            });
        }, "system-module-scan").start();
    }

    private void showAutoActivation() {
        stopActivation(false);
        root.removeAllViews();
        addButton(root, "返回 ADB 页面", this::showAdbPage);
        addSectionTitle("手动激活模块");
        addDescription("逐个浏览电视中可用的设置模块。只有点击“激活”才会打开当前模块，不会自动跳转。");
        if (activationQueue == null) activationQueue = buildSettingModules();
        if (activationQueue.isEmpty()) {
            activationIndex = 0;
        } else if (activationIndex >= activationQueue.size()) {
            activationIndex = activationQueue.size() - 1;
        }
        activationRunning = false;
        addButton(root, "上一个", this::selectPreviousModule);
        Button nextButton = addButton(root, "下一个", this::selectNextModule);
        addButton(root, "激活", this::activateSelectedModule);
        updateSelectedModuleLabel();
        nextButton.requestFocus();
        nextButton.post(() -> nextButton.requestFocus());
    }

    private void updateSelectedModuleLabel() {
        if (activationQueue == null || activationQueue.isEmpty()) {
            addDescription("当前模块：没有发现可启动的设置模块");
            return;
        }
        SettingModule module = activationQueue.get(activationIndex);
        addDescription("当前模块：" + module.label + "\n" + (activationIndex + 1)
                + " / " + activationQueue.size());
    }

    private void selectPreviousModule() {
        if (activationQueue == null || activationQueue.isEmpty()) return;
        activationIndex = (activationIndex - 1 + activationQueue.size()) % activationQueue.size();
        showAutoActivation();
    }

    private void selectNextModule() {
        if (activationQueue == null || activationQueue.isEmpty()) return;
        activationIndex = (activationIndex + 1) % activationQueue.size();
        showAutoActivation();
    }

    private void activateSelectedModule() {
        if (activationQueue == null || activationQueue.isEmpty()) {
            toast("没有可激活的设置模块。");
            return;
        }
        SettingModule module = activationQueue.get(activationIndex);
        launchSettingModule(module);
    }

    private void launchSettingModule(SettingModule module) {
        Intent intent = module.packageName == null
                ? new Intent(module.action)
                : new Intent().setComponent(new ComponentName(module.packageName, module.className));
        if (intent.resolveActivity(getPackageManager()) == null) {
            toast("当前模块无法启动：" + module.label);
            return;
        }
        try {
            toast("正在激活：" + module.label);
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException exception) {
            toast("无权限或模块不可用：" + module.label);
        }
    }

    private void startActivation() {
        showAutoActivation();
    }

    private void activateNextManually() {
        selectNextModule();
    }

    private void activateNextModule() {
        if (!activationRunning || activationQueue == null) return;
        if (activationIndex >= activationQueue.size()) {
            stopActivation(true);
            toast("设置模块队列已完成。");
            return;
        }
        SettingModule module = activationQueue.get(activationIndex++);
        Intent intent = module.packageName == null
                ? new Intent(module.action)
                : new Intent().setComponent(new ComponentName(module.packageName, module.className));
        if (intent.resolveActivity(getPackageManager()) == null) {
            activateNextModule();
            return;
        }
        try {
            toast("正在激活：" + module.label);
            startActivity(intent);
            activationAwaitingReturn = true;
        } catch (ActivityNotFoundException | SecurityException exception) {
            toast("跳过：" + module.label);
            moduleHandler.postDelayed(this::activateNextModule, 100);
        }
    }

    private void stopActivation(boolean showMessage) {
        activationRunning = false;
        activationAwaitingReturn = false;
        moduleHandler.removeCallbacksAndMessages(null);
        if (showMessage) toast("已停止自动激活。");
    }

    private java.util.List<SettingModule> buildSettingModules() {
        String[][] definitions = {
                {"系统设置", Settings.ACTION_SETTINGS},
                {"无线网络", Settings.ACTION_WIRELESS_SETTINGS},
                {"Wi-Fi", Settings.ACTION_WIFI_SETTINGS},
                {"蓝牙", Settings.ACTION_BLUETOOTH_SETTINGS},
                {"显示", Settings.ACTION_DISPLAY_SETTINGS},
                {"声音", Settings.ACTION_SOUND_SETTINGS},
                {"日期和时间", Settings.ACTION_DATE_SETTINGS},
                {"语言和输入法", Settings.ACTION_INPUT_METHOD_SETTINGS},
                {"语言", Settings.ACTION_LOCALE_SETTINGS},
                {"存储", Settings.ACTION_INTERNAL_STORAGE_SETTINGS},
                {"应用管理", Settings.ACTION_APPLICATION_SETTINGS},
                {"已安装应用", Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS},
                {"应用详情", Settings.ACTION_APPLICATION_DETAILS_SETTINGS},
                {"安全设置", Settings.ACTION_SECURITY_SETTINGS},
                {"位置信息", Settings.ACTION_LOCATION_SOURCE_SETTINGS},
                {"无障碍", Settings.ACTION_ACCESSIBILITY_SETTINGS},
                {"开发者选项", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS},
                {"设备信息", Settings.ACTION_DEVICE_INFO_SETTINGS},
                {"电池", Settings.ACTION_BATTERY_SAVER_SETTINGS},
                {"省电模式", Settings.ACTION_BATTERY_SAVER_SETTINGS},
                {"通知", "android.settings.NOTIFICATION_SETTINGS"},
                {"隐私", "android.settings.PRIVACY_SETTINGS"},
                {"账户", Settings.ACTION_SYNC_SETTINGS},
                {"无线调试", "android.settings.WIRELESS_DEBUGGING_SETTINGS"},
                {"投屏", "android.settings.CAST_SETTINGS"},
                {"网络和互联网", "android.settings.WIFI_SETTINGS"}
        };
        java.util.ArrayList<SettingModule> result = new java.util.ArrayList<>();
        java.util.HashSet<String> actions = new java.util.HashSet<>();
        for (String[] definition : definitions) {
            Intent intent = new Intent(definition[1]);
            if (actions.add(definition[1]) && intent.resolveActivity(getPackageManager()) != null) {
                result.add(new SettingModule(definition[0], definition[1]));
            }
        }

        // Vendor TVs often hide settings behind private exported Activities.
        // Add only likely settings entries so the queue does not launch every
        // unrelated system screen such as the launcher or media player.
        PackageManager pm = getPackageManager();
        int appFlags = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
        String[] keywords = {"setting", "settings", "display", "sound", "audio", "network",
                "wifi", "bluetooth", "date", "time", "language", "locale", "storage",
                "security", "developer", "system", "device", "screen", "wireless",
                "显示", "声音", "网络", "无线", "时间", "语言", "存储", "系统", "设置"};
        java.util.HashSet<String> components = new java.util.HashSet<>();
        for (SettingModule module : result) {
            components.add(module.action);
        }
        for (ApplicationInfo app : pm.getInstalledApplications(0)) {
            if ((app.flags & appFlags) == 0) continue;
            String packageName = app.packageName.toLowerCase(java.util.Locale.ROOT);
            String appLabel = String.valueOf(app.loadLabel(pm)).toLowerCase(java.util.Locale.ROOT);
            boolean settingsPackage = packageName.contains("settings") || appLabel.contains("设置")
                    || appLabel.contains("setting");
            try {
                PackageInfo info = pm.getPackageInfo(app.packageName, PackageManager.GET_ACTIVITIES);
                if (info.activities == null) continue;
                for (ActivityInfo activity : info.activities) {
                    if (!activity.exported || !activity.enabled) continue;
                    String name = activity.name.toLowerCase(java.util.Locale.ROOT);
                    String label = String.valueOf(activity.loadLabel(pm)).toLowerCase(java.util.Locale.ROOT);
                    boolean matches = settingsPackage;
                    if (!matches) {
                        for (String keyword : keywords) {
                            if (name.contains(keyword) || label.contains(keyword)) {
                                matches = true;
                                break;
                            }
                        }
                    }
                    if (!matches) continue;
                    String component = activity.packageName + "/" + activity.name;
                    if (components.add(component)) {
                        String displayLabel = label.trim().isEmpty() || label.equals(name)
                                ? String.valueOf(app.loadLabel(pm)) : String.valueOf(activity.loadLabel(pm));
                        result.add(new SettingModule(displayLabel, activity.packageName, activity.name));
                    }
                }
            } catch (PackageManager.NameNotFoundException | RuntimeException ignored) {
                // Vendor packages may disappear or reject inspection while scanning.
            }
        }
        return result;
    }

    private void returnFromSystemModules() {
        if (systemModulesReturnPage == 1) showAdbPage();
        else if (systemModulesReturnPage == 2) showRootPage();
        else showHome();
    }

    private java.util.List<SystemModule> scanSystemModules() {
        PackageManager pm = getPackageManager();
        java.util.LinkedHashMap<String, SystemModule> unique = new java.util.LinkedHashMap<>();
        int appFlags = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
        for (ApplicationInfo app : pm.getInstalledApplications(0)) {
            if ((app.flags & appFlags) == 0) continue;
            try {
                PackageInfo packageInfo = pm.getPackageInfo(app.packageName, PackageManager.GET_ACTIVITIES);
                if (packageInfo.activities == null) continue;
                for (ActivityInfo activity : packageInfo.activities) {
                    if (!activity.exported || !activity.enabled) continue;
                    String key = activity.packageName + "/" + activity.name;
                    CharSequence appLabel = app.loadLabel(pm);
                    CharSequence activityLabel = activity.loadLabel(pm);
                    String label = String.valueOf(activityLabel);
                    if (label.trim().isEmpty() || label.equals(activity.name)) label = String.valueOf(appLabel);
                    unique.put(key, new SystemModule(label, activity.packageName, activity.name));
                }
            } catch (PackageManager.NameNotFoundException | RuntimeException ignored) {
                // A package may disappear or be restricted while the device is being scanned.
            }
        }
        java.util.ArrayList<SystemModule> result = new java.util.ArrayList<>(unique.values());
        java.util.Collections.sort(result, (a, b) -> a.label.compareToIgnoreCase(b.label));
        return result;
    }

    private void displaySystemModules(java.util.List<SystemModule> modules) {
        if (isFinishing()) return;
        if (modules.isEmpty()) {
            root.addView(text("没有发现系统包中可由第三方启动的导出 Activity。未导出的系统内部模块受 Android 权限保护，不能由普通应用启动。", 16, MUTED, false));
            return;
        }
        addDescription("找到 " + modules.size() + " 个系统导出 Activity。点击一项尝试以原始组件打开；部分模块仍可能要求系统权限。");
        ListView list = new ListView(this);
        list.setItemsCanFocus(true);
        list.setFocusable(true);
        list.setFocusableInTouchMode(true);
        list.setDivider(null);
        list.setDividerHeight(dp(5));
        list.setSelector(android.R.color.transparent);
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        for (SystemModule module : modules) labels.add(module.label + "\n" + module.packageName + " / " + module.className);
        list.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, labels) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                TextView row = (TextView) super.getView(position, convertView, parent);
                row.setTextColor(WHITE);
                row.setTextSize(15);
                row.setSingleLine(false);
                row.setMinHeight(dp(64));
                row.setPadding(dp(16), dp(10), dp(16), dp(10));
                row.setFocusable(true);
                row.setFocusableInTouchMode(true);
                row.setBackground(makeButtonBackground(false));
                row.setOnFocusChangeListener((view, focused) -> view.setBackground(makeButtonBackground(focused)));
                row.setOnClickListener(view -> launchSystemModule(modules, position));
                row.setOnKeyListener((view, keyCode, event) -> {
                    if (event.getAction() == KeyEvent.ACTION_UP
                            && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                            || keyCode == KeyEvent.KEYCODE_ENTER
                            || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                        launchSystemModule(modules, position);
                        return true;
                    }
                    return false;
                });
                return row;
            }
        });
        list.setOnItemClickListener((parent, view, position, id) -> {
            launchSystemModule(modules, position);
        });
        root.addView(list, params(contentWidth(), dp(440)));
    }

    private void launchSystemModule(java.util.List<SystemModule> modules, int position) {
        if (position < 0 || position >= modules.size()) {
            toast("模块选择无效，请重新选择。");
            return;
        }
        SystemModule selected = modules.get(position);
        toast("正在打开：" + selected.label);
        Intent launch = new Intent();
        launch.setComponent(new ComponentName(selected.packageName, selected.className));
        if (!tryStart(launch)) {
            toast("打开失败：此模块拒绝外部启动或当前固件不允许打开。");
        }
    }

    private static final class SystemModule {
        final String label;
        final String packageName;
        final String className;

        SystemModule(String label, String packageName, String className) {
            this.label = label;
            this.packageName = packageName;
            this.className = className;
        }
    }

    private static final class SettingModule {
        final String label;
        final String action;
        final String packageName;
        final String className;

        SettingModule(String label, String action) {
            this.label = label;
            this.action = action;
            this.packageName = null;
            this.className = null;
        }

        SettingModule(String label, String packageName, String className) {
            this.label = label;
            this.action = null;
            this.packageName = packageName;
            this.className = className;
        }
    }

    private Button addButton(LinearLayout root, String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(17);
        button.setAllCaps(false);
        button.setTextColor(WHITE);
        button.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        button.setPadding(dp(20), dp(8), dp(20), dp(8));
        button.setFocusable(true);
        button.setFocusableInTouchMode(true);
        button.setBackground(makeButtonBackground(false));
        button.setOnFocusChangeListener((view, focused) -> view.setBackground(makeButtonBackground(focused)));
        button.setOnClickListener(view -> action.run());
        button.setMinHeight(dp(56));
        LinearLayout.LayoutParams buttonParams = params(Math.min(dp(680), contentWidth()), dp(56));
        buttonParams.topMargin = dp(7);
        root.addView(button, buttonParams);
        return button;
    }

    private GradientDrawable makeButtonBackground(boolean focused) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(focused ? ORANGE : SURFACE);
        shape.setCornerRadius(dp(5));
        if (focused) shape.setStroke(dp(3), 0xFFFFD18F);
        return shape;
    }

    private void openDeveloperSettings() {
        openFirst(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS), new Intent(Settings.ACTION_SETTINGS));
    }

    private void restartSystem() {
        new AlertDialog.Builder(this)
                .setTitle("重启系统")
                .setMessage("重启会立即关闭当前应用和电视上的其他应用。普通 APK 没有直接重启系统的权限，将先尝试设备已有的 ROOT 权限。")
                .setPositiveButton("立即重启", (dialog, which) -> executeRootReboot())
                .setNegativeButton("取消", null)
                .show();
    }

    private void executeRootReboot() {
        new Thread(() -> {
            boolean started = false;
            try {
                Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot"});
                started = process.waitFor() == 0;
            } catch (Exception ignored) {
                // Normal APKs do not have permission to invoke su.
            }
            final boolean result = started;
            runOnUiThread(() -> {
                if (!result) {
                    new AlertDialog.Builder(this)
                            .setTitle("无法直接重启")
                            .setMessage("当前应用未获得 ROOT 权限。若电脑已连接 ADB，可执行：\n\nadb reboot\n\n若设备已有 ROOT，可在电视终端执行：\nsu\nreboot")
                            .setPositiveButton("复制命令", (dialog, which) -> {
                                copy("adb reboot\n\n# 电视本机 ROOT 终端：\nsu\nreboot");
                                toast("重启命令已复制。普通 APK 不能代替 ADB 或 ROOT。");
                            })
                            .setNegativeButton("关闭", null)
                            .show();
                }
            });
        }, "system-reboot").start();
    }

    private void openSystemSettings() {
        Intent info = new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS);
        if (tryStart(info)) return;
        openFirst(new Intent(Settings.ACTION_SETTINGS), explicit("com.android.settings.Settings"));
    }

    private void openWirelessSettings() {
        if (Build.VERSION.SDK_INT < 30) {
            toast("此入口主要适用于 Android 11 及以上；旧系统请使用 USB ADB 或厂商网络调试。 ");
            openDeveloperSettings();
            return;
        }
        openFirst(new Intent("android.settings.WIRELESS_DEBUGGING_SETTINGS"),
                new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
    }

    private void showBrandGuides() {
        final String[] brands = {
                "自动识别本机",
                "TCL / 雷鸟",
                "小米 / Redmi",
                "OPPO",
                "海尔 / 模卡",
                "海信 / VIDAA",
                "创维 / 酷开",
                "Sony / 原生 Android TV",
                "Sber / SaluteTV",
                "Amazon Fire TV",
                "其他 Android TV"
        };
        new AlertDialog.Builder(this)
                .setTitle("选择电视品牌")
                .setItems(brands, (dialog, which) -> {
                    if (which == 0) showDetectedBrandGuide();
                    else showBrandGuide(brands[which]);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showDetectedBrandGuide() {
        String manufacturer = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER;
        String brand = Build.BRAND == null ? "" : Build.BRAND;
        String model = Build.MODEL == null ? "未知型号" : Build.MODEL;
        String identity = (manufacturer + " " + brand).toLowerCase(java.util.Locale.ROOT);
        String target;
        if (identity.contains("tcl") || identity.contains("雷鸟")) target = "TCL / 雷鸟";
        else if (identity.contains("xiaomi") || identity.contains("redmi") || identity.contains("小米")) target = "小米 / Redmi";
        else if (identity.contains("oppo")) target = "OPPO";
        else if (identity.contains("hisense") || identity.contains("海信")) target = "海信 / VIDAA";
        else if (identity.contains("sony")) target = "Sony / 原生 Android TV";
        else if (identity.contains("amazon")) target = "Amazon Fire TV";
        else target = "其他 Android TV";
        showBrandGuide(target, manufacturer + " / " + brand + " / " + model);
    }

    private void showBrandGuide(String brand) {
        showBrandGuide(brand, Build.MANUFACTURER + " / " + Build.BRAND + " / " + Build.MODEL);
    }

    private void showBrandGuide(String brand, String device) {
        String guide;
        String level;
        if ("TCL / 雷鸟".equals(brand)) {
            guide = "设置 → 关于 → 本机信息；用遥控器依次按：上、下、左、右，进入“更多”后手动打开 ADB。不同型号/固件可能不同。";
            level = "GitHub 资料：TCL T7K Android 11 有实测；其他型号未必适用。";
        } else if ("小米 / Redmi".equals(brand)) {
            guide = "设置 → 关于 → 对“产品型号”连续按确认，直到进入开发者模式；再到设置 → 账号与安全，手动打开 ADB 调试。";
            level = "社区反馈，未由本工具验证；菜单名称因型号和固件而异。";
        } else if ("OPPO".equals(brand)) {
            guide = "设置 → 系统 → 关于本机 → 设备型号；遥控器按上、上、右、右并确认。随后到开发者选项手动开启 USB/ADB 调试。";
            level = "用户反馈路径，未由本工具验证；机型差异较大。";
        } else if ("海尔 / 模卡".equals(brand)) {
            guide = "按电视机身信号源键打开信号源菜单，再用遥控器按左、左、上、右进入工厂菜单，寻找 ADB Debug 开关。只改 ADB 项，不要操作工厂复位或校准选项。";
            level = "社区机型反馈，未验证。工厂菜单包含可能导致画面/调谐异常的项目，请谨慎。";
        } else if ("海信 / VIDAA".equals(brand)) {
            guide = "先在设置 → 关于中检查版本号/内部版本号，连续确认约 7 次，再查看开发者选项。部分机型可能需工厂/工程菜单；请勿尝试未经该机型验证的工程码。";
            level = "机型差异大，未验证。VIDAA 非 Android 机型不适用本工具。";
        } else if ("创维 / 酷开".equals(brand)) {
            guide = "设置 → 关于 → 对版本号/内部版本号连续确认约 7 次，再查看开发者选项；部分固件可能另有工厂菜单。";
            level = "社区/通用路径，未验证；不要修改工厂菜单中的其他选项。";
        } else if ("Sony / 原生 Android TV".equals(brand)) {
            guide = "设置 → 设备偏好设置 → 关于 → 对内部版本号连续确认约 7 次；返回开发者选项手动开启 USB 调试。Android 11+ 另检查无线调试。";
            level = "通用 Android TV 路径，具体菜单随系统版本变化。";
        } else if ("Sber / SaluteTV".equals(brand)) {
            guide = "可尝试 SaluteDevHelper 打开隐藏开发者设置并查看 ADB/SSH 状态。该工具针对 SaluteTV，不是通用入口；其仓库未明确许可证，安装前请审查来源。";
            level = "GitHub 项目：github.com/qqqwv72/SaluteDevHelper；未作为本应用代码依赖。";
        } else if ("Amazon Fire TV".equals(brand)) {
            guide = "设置 → 我的 Fire TV → 关于，连续选择设备名称/版本号以显示开发者选项，再手动开启 ADB Debugging。";
            level = "Fire OS 版本与机型可能不同；不等同于标准 Android TV。";
        } else {
            guide = "设置 → 设备偏好设置 → 关于 → 对内部版本号（Build number，不是 Android 版本）连续确认约 7 次；返回开发者选项手动开启 USB 调试。旧版 Android TV 菜单名称可能不同。";
            level = "Android 通用操作路径；无法保证厂商固件保留对应入口。";
        }
        TextView message = text("当前设备：" + device + "\n\n" + guide + "\n\n" + level + "\n\n本工具不会模拟遥控器操作；需要你在电视上完成确认。", 16, WHITE, false);
        message.setPadding(dp(22), dp(12), dp(22), dp(12));
        ScrollView messageScroll = new ScrollView(this);
        messageScroll.addView(message);
        new AlertDialog.Builder(this)
                .setTitle(brand + " 开启 ADB")
                .setView(messageScroll)
                .setPositiveButton("打开开发者设置", (dialog, which) -> openDeveloperSettings())
                .setNeutralButton("打开系统设置", (dialog, which) -> openSystemSettings())
                .setNegativeButton("关闭", null)
                .show();
    }

    private void openFirst(Intent first, Intent fallback) {
        if (!tryStart(first) && !tryStart(fallback)) toast("此固件没有可启动的对应设置页面，请从电视菜单手动进入。");
    }

    private Intent explicit(String className) {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", className);
        return intent;
    }

    private boolean tryStart(Intent intent) {
        try {
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException exception) {
            return false;
        }
    }

    private void copyLegacyCommands() {
        copy("adb devices\nadb -s <USB_SERIAL> tcpip 5555\nadb connect <TV_IP>:5555");
        toast("已复制。前提：USB ADB 已连接并在电视上确认授权；电视与电脑需在同一网络。");
    }

    private void copyRootCommands() {
        copy("su\nsetprop service.adb.tcp.port 5555\nstop adbd\nstart adbd\ngetprop service.adb.tcp.port");
        toast("这些命令在电视本机的 root 终端执行。随后在电脑另执行 adb connect <TV_IP>:5555；普通 APK 不能代替 su。");
    }

    private void enableTcpAdbWithRoot() {
        new AlertDialog.Builder(this)
                .setTitle("自动开启 ADB")
                .setMessage("将请求 ROOT 授权，并依次执行 adb_enabled、TCP 5555 和 adbd 重启命令。当前 ADB 连接可能会暂时中断。未知厂商私有属性不会盲目写入。")
                .setPositiveButton("执行", (dialog, which) -> runRootTcpAdb())
                .setNegativeButton("取消", null)
                .show();
    }

    private void runRootTcpAdb() {
        new Thread(() -> {
            if (!isRootDetected()) {
                runOnUiThread(() -> toast("未获得 ROOT 授权，TCP ADB 未开启。"));
                return;
            }
            String command = "settings put global adb_enabled 1; "
                    + "setprop service.adb.tcp.port 5555; "
                    + "setprop ctl.stop adbd; "
                    + "setprop ctl.start adbd; "
                    + "(stop adbd 2>/dev/null || true); "
                    + "(start adbd 2>/dev/null || true); "
                    + "echo ADB_ENABLED=$(settings get global adb_enabled); "
                    + "echo TCP_PORT=$(getprop service.adb.tcp.port); "
                    + "echo ADBD_STATE=$(getprop init.svc.adbd)";
            RootResult result = executeRootCommand(command);
            runOnUiThread(() -> {
                if (!result.authorized) {
                    toast("未获得 ROOT 授权，TCP ADB 未开启。");
                } else {
                    toast("ADB 开启命令已执行：" + result.output.trim() + "。请从电脑执行 adb connect <TV_IP>:5555 验证。");
                }
            });
        }, "root-enable-tcp-adb").start();
    }

    private RootResult executeRootCommand(String command) {
        StringBuilder output = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            output.append(readProcessOutput(process));
            boolean authorized = process.waitFor() == 0;
            return new RootResult(authorized, output.toString());
        } catch (Exception ignored) {
            return new RootResult(false, output.toString());
        }
    }

    private String readProcessOutput(Process process) throws java.io.IOException {
        java.io.InputStream input = process.getInputStream();
        byte[] buffer = new byte[1024];
        StringBuilder output = new StringBuilder();
        int count;
        while ((count = input.read(buffer)) != -1) output.append(new String(buffer, 0, count, "UTF-8"));
        return output.toString();
    }

    private static final class RootResult {
        final boolean authorized;
        final String output;

        RootResult(boolean authorized, String output) {
            this.authorized = authorized;
            this.output = output;
        }
    }

    private void copyPairCommands() {
        copy("adb pair <TV_IP>:<PAIRING_PORT> <PAIRING_CODE>\nadb connect <TV_IP>:<CONNECT_PORT>\nadb devices -l");
        toast("仅适用于 Android 11+ Wireless debugging；配对端口与连接端口不同，请从电视设置页读取。");
    }

    private void copyOtgGuide() {
        copy("USB OTG 控制端参考：https://github.com/twaik/pocket-adb-tcpip\n前提：目标电视本机已开启 USB debugging；目标机有 USB device/Gadget 调试口并完成首次授权。多数电视 USB-A 是 Host 口，不可用于此方法。\n此方法只把电脑换成另一台 Android 控制端，不是从零开启目标机 ADB。");
        toast("说明已复制。目标电视仍需先开启 USB 调试并具备 USB Gadget/device 口。");
    }

    private void copyRecoveryGuide() {
        copy("Android 14+ 配对后自恢复参考：https://github.com/mouldybread/adb-auto-enable\n首次仍须用户在 Wireless debugging 页面手动开启并配对；之后才可能用保存的 ADB TLS 凭据本机回连并恢复设置。\n这不是首次开启 ADB 的方法。持续开放网络 ADB 会增加风险，不建议在不可信网络使用。");
        toast("已复制。此方案依赖预先开启并配对的 Android 11+ Wireless debugging。");
    }

    private void copyAdbGithubReferences() {
        copy("USB OTG 控制另一设备（目标端仍需先开 USB 调试）：https://github.com/twaik/pocket-adb-tcpip\nAndroid 14+ 配对后自恢复（不是首次开启）：https://github.com/mouldybread/adb-auto-enable\nLSPosed 无 Wi-Fi 无线调试模块（需 root/LSPosed）：https://github.com/iflyabd/nowifi-adb");
        toast("已复制 ADB 相关参考项目；每项都有设备和授权前提。");
    }

    private void copyRootGithubReferences() {
        copy("ROOT 无线 ADB 开关（明确要求 ROOT）：https://github.com/PhenixStar/Simple-wireless-ADB\nLSPosed 无 Wi-Fi 无线调试模块（需要设备已 root 并安装 LSPosed）：https://github.com/iflyabd/nowifi-adb\n注意：这些项目使用已有的 root/框架权限，不会替未 root 设备取得 root。");
        toast("已复制 ROOT/LSPosed 项目参考；它们均不能替代设备解锁或 root。");
    }

    private void copy(String value) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("SuperADB commands", value));
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setLineSpacing(dp(2), 1.0f);
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
    }

    private LinearLayout.LayoutParams params(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int contentWidth() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        return Math.max(dp(280), screenWidth - dp(64));
    }
}
