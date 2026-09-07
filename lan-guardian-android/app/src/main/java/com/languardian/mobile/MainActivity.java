package com.languardian.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextView statusText;
    private TextView networkText;
    private LinearLayout resultsContainer;
    private Button scanButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(28));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("LAN Guardian");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setPadding(0, dp(8), 0, dp(4));
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("مراقبة الأجهزة المتصلة بالشبكة المحلية");
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        subtitle.setPadding(0, 0, 0, dp(18));
        root.addView(subtitle);

        networkText = new TextView(this);
        networkText.setTextSize(15);
        networkText.setPadding(0, dp(8), 0, dp(8));
        root.addView(networkText);

        scanButton = new Button(this);
        scanButton.setText("فحص الشبكة الآن");
        scanButton.setAllCaps(false);
        scanButton.setOnClickListener(v -> startScan());
        root.addView(scanButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        progressParams.gravity = Gravity.CENTER_HORIZONTAL;
        progressParams.topMargin = dp(12);
        root.addView(progressBar, progressParams);

        statusText = new TextView(this);
        statusText.setText("جاهز للفحص");
        statusText.setTextSize(15);
        statusText.setGravity(Gravity.CENTER_HORIZONTAL);
        statusText.setPadding(0, dp(12), 0, dp(12));
        root.addView(statusText);

        resultsContainer = new LinearLayout(this);
        resultsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(resultsContainer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView note = new TextView(this);
        note.setText("هذه الأداة مخصصة لفحص الشبكات والأجهزة التي تملكها أو لديك تصريح بإدارتها. لا تقوم باستخراج كلمات المرور أو تجاوز المصادقة.");
        note.setTextSize(12);
        note.setPadding(0, dp(24), 0, 0);
        root.addView(note);

        setContentView(scrollView);
        updateNetworkInfo();
    }

    private void updateNetworkInfo() {
        String localIp = getLocalIpv4();
        if (localIp == null) {
            networkText.setText("لم يتم العثور على اتصال Wi‑Fi/شبكة محلية IPv4.");
        } else {
            String prefix = subnetPrefix(localIp);
            networkText.setText("عنوان هذا الجهاز: " + localIp + "\nنطاق الفحص: " + prefix + "1 - " + prefix + "254");
        }
    }

    private void startScan() {
        final String localIp = getLocalIpv4();
        if (localIp == null) {
            statusText.setText("تعذر تحديد عنوان الشبكة المحلية. تأكد من اتصال الجهاز بالـ Wi‑Fi.");
            return;
        }

        scanButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        resultsContainer.removeAllViews();
        statusText.setText("جارٍ فحص الشبكة...");

        Thread coordinator = new Thread(() -> {
            String prefix = subnetPrefix(localIp);
            ExecutorService pool = Executors.newFixedThreadPool(40);
            List<DeviceInfo> devices = Collections.synchronizedList(new ArrayList<>());

            for (int i = 1; i <= 254; i++) {
                final String ip = prefix + i;
                pool.submit(() -> {
                    DeviceInfo info = inspectHost(ip);
                    if (info != null) {
                        devices.add(info);
                    }
                });
            }

            pool.shutdown();
            try {
                pool.awaitTermination(55, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

            Map<String, String> arp = readArpTable();
            for (DeviceInfo d : devices) {
                String mac = arp.get(d.ip);
                if (mac != null && !mac.isEmpty()) d.mac = mac;
            }

            Collections.sort(devices, Comparator.comparingInt(d -> lastOctet(d.ip)));
            mainHandler.post(() -> showResults(devices));
        });
        coordinator.start();
    }

    private DeviceInfo inspectHost(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            boolean online = address.isReachable(260);
            List<Integer> openPorts = new ArrayList<>();
            int[] ports = {80, 443, 22, 445, 53, 8080};

            for (int port : ports) {
                if (isPortOpen(ip, port, 130)) {
                    openPorts.add(port);
                    online = true;
                }
            }

            if (!online) return null;

            String host = "";
            try {
                host = address.getCanonicalHostName();
                if (ip.equals(host)) host = "";
            } catch (Exception ignored) {}

            return new DeviceInfo(ip, host, openPorts);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isPortOpen(String ip, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, String> readArpTable() {
        Map<String, String> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 4 && parts[0].matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                    String mac = parts[3].toUpperCase(Locale.US);
                    if (!"00:00:00:00:00:00".equals(mac)) map.put(parts[0], mac);
                }
            }
        } catch (Exception ignored) {}
        return map;
    }

    private void showResults(List<DeviceInfo> devices) {
        scanButton.setEnabled(true);
        progressBar.setVisibility(View.GONE);
        statusText.setText("اكتمل الفحص — تم العثور على " + devices.size() + " جهاز/أجهزة ظاهرة");

        if (devices.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("لم تظهر أجهزة أخرى. بعض الأجهزة أو أجهزة الراوتر قد تمنع Ping أو فحص المنافذ.");
            empty.setTextSize(15);
            empty.setPadding(dp(12), dp(18), dp(12), dp(18));
            resultsContainer.addView(empty);
            return;
        }

        for (DeviceInfo d : devices) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(12), dp(14), dp(12));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cp.topMargin = dp(8);
            card.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);

            TextView ip = new TextView(this);
            ip.setText(d.ip);
            ip.setTextSize(19);
            card.addView(ip);

            TextView details = new TextView(this);
            StringBuilder sb = new StringBuilder();
            if (!TextUtils.isEmpty(d.hostName)) sb.append("اسم الجهاز: ").append(d.hostName).append("\n");
            sb.append("MAC: ").append(TextUtils.isEmpty(d.mac) ? "غير متاح من Android" : d.mac).append("\n");
            sb.append("المنافذ الشائعة المفتوحة: ");
            if (d.openPorts.isEmpty()) sb.append("لم يُكتشف شيء");
            else sb.append(joinPorts(d.openPorts));
            details.setText(sb.toString());
            details.setTextSize(14);
            details.setPadding(0, dp(5), 0, 0);
            card.addView(details);

            resultsContainer.addView(card, cp);
        }
    }

    private String joinPorts(List<Integer> ports) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ports.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(ports.get(i));
        }
        return sb.toString();
    }

    private String getLocalIpv4() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress a = addresses.nextElement();
                    String ip = a.getHostAddress();
                    if (ip != null && ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && isPrivateIpv4(ip)) {
                        return ip;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private boolean isPrivateIpv4(String ip) {
        return ip.startsWith("10.") || ip.startsWith("192.168.") ||
                ip.matches("172\\.(1[6-9]|2\\d|3[0-1])\\..*");
    }

    private String subnetPrefix(String ip) {
        int p = ip.lastIndexOf('.');
        return ip.substring(0, p + 1);
    }

    private int lastOctet(String ip) {
        try { return Integer.parseInt(ip.substring(ip.lastIndexOf('.') + 1)); }
        catch (Exception e) { return 0; }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class DeviceInfo {
        final String ip;
        final String hostName;
        final List<Integer> openPorts;
        String mac = "";

        DeviceInfo(String ip, String hostName, List<Integer> openPorts) {
            this.ip = ip;
            this.hostName = hostName;
            this.openPorts = openPorts;
        }
    }
}
