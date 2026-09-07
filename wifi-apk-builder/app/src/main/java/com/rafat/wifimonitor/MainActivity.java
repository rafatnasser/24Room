package com.rafat.wifimonitor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {

    private final Handler main = new Handler(Looper.getMainLooper());
    private TextView networkInfo;
    private TextView statusText;
    private ProgressBar progressBar;
    private LinearLayout resultsBox;
    private Button scanButton;
    private Button shareButton;
    private final List<DeviceInfo> lastResults = Collections.synchronizedList(new ArrayList<>());

    private static final int[][] COMMON_PORTS = {
            {22, 1}, {53, 2}, {80, 3}, {139, 4}, {443, 5}, {445, 6}, {3389, 7}, {8080, 8}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        refreshNetworkInfo();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(14));
        root.setBackgroundColor(Color.rgb(247, 249, 252));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = new TextView(this);
        title.setText("مراقب شبكة Wi‑Fi");
        title.setTextSize(26);
        title.setTextColor(Color.rgb(25, 45, 75));
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, 1);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = new TextView(this);
        subtitle.setText("فحص الأجهزة المتصلة بالشبكة المحلية التي تملكها");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(4), 0, dp(14));
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));

        networkInfo = new TextView(this);
        networkInfo.setTextSize(15);
        networkInfo.setTextColor(Color.rgb(35, 50, 70));
        networkInfo.setPadding(dp(14), dp(12), dp(14), dp(12));
        networkInfo.setBackground(rounded(Color.WHITE, 14));
        root.addView(networkInfo, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(12), 0, dp(8));

        scanButton = new Button(this);
        scanButton.setText("فحص الشبكة");
        scanButton.setOnClickListener(v -> startScan());
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(0, dp(50), 1f);
        actionParams.setMarginEnd(dp(6));
        actions.addView(scanButton, actionParams);

        shareButton = new Button(this);
        shareButton.setText("مشاركة النتائج");
        shareButton.setEnabled(false);
        shareButton.setOnClickListener(v -> shareResults());
        LinearLayout.LayoutParams shareParams = new LinearLayout.LayoutParams(0, dp(50), 1f);
        shareParams.setMarginStart(dp(6));
        actions.addView(shareButton, shareParams);

        root.addView(actions, new LinearLayout.LayoutParams(-1, -2));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(254);
        progressBar.setProgress(0);
        root.addView(progressBar, new LinearLayout.LayoutParams(-1, dp(10)));

        statusText = new TextView(this);
        statusText.setText("جاهز للفحص");
        statusText.setTextSize(14);
        statusText.setTextColor(Color.DKGRAY);
        statusText.setPadding(0, dp(8), 0, dp(8));
        root.addView(statusText, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        resultsBox = new LinearLayout(this);
        resultsBox.setOrientation(LinearLayout.VERTICAL);
        resultsBox.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(resultsBox, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        TextView footer = new TextView(this);
        footer.setText("لا يقوم التطبيق باستخراج كلمات المرور أو تجاوز الحماية.");
        footer.setTextSize(12);
        footer.setTextColor(Color.GRAY);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(8), 0, 0);
        root.addView(footer, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
    }

    private void refreshNetworkInfo() {
        NetworkData data = getNetworkData();
        if (data == null) {
            networkInfo.setText("اتصل بشبكة Wi‑Fi أولاً ثم افتح التطبيق.");
            scanButton.setEnabled(false);
            return;
        }
        scanButton.setEnabled(true);
        networkInfo.setText("IP الهاتف: " + data.localIp + "\nنطاق الفحص: " + data.basePrefix + ".1 – " + data.basePrefix + ".254");
    }

    private NetworkData getNetworkData() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            Network active = cm.getActiveNetwork();
            if (active == null) return null;
            NetworkCapabilities caps = cm.getNetworkCapabilities(active);
            if (caps == null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null;
            LinkProperties lp = cm.getLinkProperties(active);
            if (lp == null) return null;

            for (LinkAddress linkAddress : lp.getLinkAddresses()) {
                InetAddress a = linkAddress.getAddress();
                if (a instanceof Inet4Address && !a.isLoopbackAddress()) {
                    String ip = a.getHostAddress();
                    if (ip == null) continue;
                    String[] p = ip.split("\\.");
                    if (p.length == 4) {
                        return new NetworkData(ip, p[0] + "." + p[1] + "." + p[2]);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void startScan() {
        NetworkData data = getNetworkData();
        if (data == null) {
            Toast.makeText(this, "يجب الاتصال بشبكة Wi‑Fi أولاً", Toast.LENGTH_LONG).show();
            refreshNetworkInfo();
            return;
        }

        lastResults.clear();
        resultsBox.removeAllViews();
        progressBar.setProgress(0);
        scanButton.setEnabled(false);
        shareButton.setEnabled(false);
        statusText.setText("جارٍ فحص الشبكة...");

        Thread supervisor = new Thread(() -> {
            ExecutorService pool = Executors.newFixedThreadPool(48);
            CountDownLatch latch = new CountDownLatch(254);
            AtomicInteger completed = new AtomicInteger(0);

            for (int i = 1; i <= 254; i++) {
                final String ip = data.basePrefix + "." + i;
                pool.execute(() -> {
                    try {
                        DeviceInfo device = inspectHost(ip, data.localIp);
                        if (device != null) lastResults.add(device);
                    } finally {
                        int done = completed.incrementAndGet();
                        main.post(() -> {
                            progressBar.setProgress(done);
                            statusText.setText("جارٍ الفحص: " + done + " / 254");
                        });
                        latch.countDown();
                    }
                });
            }

            try {
                latch.await();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            pool.shutdownNow();

            Map<String, String> arp = readArpTable();
            synchronized (lastResults) {
                for (DeviceInfo d : lastResults) {
                    if (d.mac.length() == 0 && arp.containsKey(d.ip)) d.mac = arp.get(d.ip);
                }
                Collections.sort(lastResults, Comparator.comparingInt(d -> ipLastOctet(d.ip)));
            }

            main.post(this::renderResults);
        });
        supervisor.start();
    }

    private DeviceInfo inspectHost(String ip, String localIp) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            boolean reachable = ip.equals(localIp);
            try {
                reachable = reachable || address.isReachable(260);
            } catch (Exception ignored) {
            }

            List<String> open = new ArrayList<>();
            for (int[] p : COMMON_PORTS) {
                if (isPortOpen(ip, p[0], 120)) {
                    reachable = true;
                    open.add(portLabel(p[0]));
                }
            }

            if (!reachable) return null;

            String host = "غير معروف";
            try {
                String h = address.getCanonicalHostName();
                if (h != null && !h.equals(ip)) host = h;
            } catch (Exception ignored) {
            }

            String ports = open.isEmpty() ? "لا توجد منافذ شائعة ظاهرة" : String.join("، ", open);
            return new DeviceInfo(ip, "", host, ports, ip.equals(localIp));
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isPortOpen(String host, int port, int timeoutMs) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception ignored) {
            return false;
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private Map<String, String> readArpTable() {
        Map<String, String> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] f = line.trim().split("\\s+");
                if (f.length >= 4 && f[0].matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && f[3].contains(":")) {
                    map.put(f[0], f[3].toUpperCase(Locale.US));
                }
            }
        } catch (Exception ignored) {
        }
        return map;
    }

    private void renderResults() {
        resultsBox.removeAllViews();
        scanButton.setEnabled(true);
        shareButton.setEnabled(!lastResults.isEmpty());

        if (lastResults.isEmpty()) {
            statusText.setText("اكتمل الفحص ولم تظهر أجهزة. قد يمنع الراوتر أو بعض الأجهزة الاستجابة للفحص.");
            addInfoCard("لم يتم اكتشاف أجهزة قابلة للوصول.");
            return;
        }

        statusText.setText("اكتمل الفحص — تم اكتشاف " + lastResults.size() + " جهاز/أجهزة");
        synchronized (lastResults) {
            for (DeviceInfo d : lastResults) {
                StringBuilder text = new StringBuilder();
                text.append(d.local ? "📱 هذا الهاتف\n" : "🌐 جهاز على الشبكة\n");
                text.append("IP: ").append(d.ip).append("\n");
                text.append("الاسم: ").append(d.host).append("\n");
                if (!d.mac.isEmpty() && !d.mac.equals("00:00:00:00:00:00")) text.append("MAC: ").append(d.mac).append("\n");
                text.append("المنافذ: ").append(d.ports);
                addDeviceCard(text.toString(), d.local);
            }
        }
    }

    private void addDeviceCard(String text, boolean localDevice) {
        TextView card = new TextView(this);
        card.setText(text);
        card.setTextSize(15);
        card.setTextColor(Color.rgb(30, 42, 58));
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setLineSpacing(0, 1.18f);
        card.setBackground(rounded(localDevice ? Color.rgb(229, 244, 255) : Color.WHITE, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(10));
        resultsBox.addView(card, lp);
    }

    private void addInfoCard(String text) {
        TextView card = new TextView(this);
        card.setText(text);
        card.setGravity(Gravity.CENTER);
        card.setTextSize(15);
        card.setPadding(dp(16), dp(24), dp(16), dp(24));
        card.setBackground(rounded(Color.WHITE, 14));
        resultsBox.addView(card, new LinearLayout.LayoutParams(-1, -2));
    }

    private void shareResults() {
        StringBuilder out = new StringBuilder("نتائج WiFi Network Monitor\n\n");
        synchronized (lastResults) {
            for (DeviceInfo d : lastResults) {
                out.append(d.local ? "[هذا الهاتف] " : "[جهاز] ")
                        .append(d.ip).append("\n")
                        .append("الاسم: ").append(d.host).append("\n")
                        .append("MAC: ").append(d.mac.isEmpty() ? "غير متاح" : d.mac).append("\n")
                        .append("المنافذ: ").append(d.ports).append("\n\n");
            }
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, out.toString());
        startActivity(Intent.createChooser(send, "مشاركة نتائج الفحص"));
    }

    private String portLabel(int port) {
        switch (port) {
            case 22: return "22/SSH";
            case 53: return "53/DNS";
            case 80: return "80/HTTP";
            case 139: return "139/NetBIOS";
            case 443: return "443/HTTPS";
            case 445: return "445/SMB";
            case 3389: return "3389/RDP";
            case 8080: return "8080/HTTP";
            default: return String.valueOf(port);
        }
    }

    private int ipLastOctet(String ip) {
        try {
            String[] p = ip.split("\\.");
            return Integer.parseInt(p[3]);
        } catch (Exception e) {
            return 999;
        }
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class NetworkData {
        final String localIp;
        final String basePrefix;
        NetworkData(String localIp, String basePrefix) {
            this.localIp = localIp;
            this.basePrefix = basePrefix;
        }
    }

    private static class DeviceInfo {
        final String ip;
        String mac;
        final String host;
        final String ports;
        final boolean local;
        DeviceInfo(String ip, String mac, String host, String ports, boolean local) {
            this.ip = ip;
            this.mac = mac;
            this.host = host;
            this.ports = ports;
            this.local = local;
        }
    }
}
