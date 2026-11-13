package com.thirtythousand.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.HttpsURLConnection;

/** Public HTTPS Date headers give approximate UTC, never millisecond accuracy. */
public final class TimeAuthority {
    private final SharedPreferences prefs;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final int boot;
    private volatile long anchorUtc, anchorElapsed, lastAttempt;
    private volatile boolean syncing;
    private volatile String status;
    private volatile long uncertainty;
    public TimeAuthority(Context context) {
        prefs = context.getSharedPreferences("clock", Context.MODE_PRIVATE);
        boot = Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
        long elapsed = SystemClock.elapsedRealtime();
        if (prefs.contains("utc") && boot >= 0 && prefs.getInt("boot", -2) == boot && elapsed >= prefs.getLong("elapsed", Long.MAX_VALUE)) {
            anchorUtc = prefs.getLong("utc", 0);
            anchorElapsed = prefs.getLong("elapsed", 0);
            uncertainty = prefs.getLong("uncertainty", 2000);
            status = "沿用上次校准 · 等待联网复核";
        } else {
            anchorUtc = System.currentTimeMillis() + prefs.getLong("offset", 0);
            anchorElapsed = elapsed;
            status = prefs.contains("utc") ? "离线估计 · 待重新校准" : "本机时间 · 尚未联网校准";
        }
    }
    public synchronized long now() { return anchorUtc + SystemClock.elapsedRealtime() - anchorElapsed; }
    public String status() { return syncing ? "正在核对网络时间…" : status; }
    public long uncertainty() { return uncertainty; }
    public boolean isSyncing() { return syncing; }
    public String lastSource() { return prefs.getString("sources", "尚无成功校准"); }
    public long lastSynced() { return prefs.getLong("utc", 0); }
    public void sync(boolean force, Runnable callback) {
        long elapsed = SystemClock.elapsedRealtime();
        if (syncing || (!force && lastAttempt != 0 && elapsed - lastAttempt < 60_000)) return;
        syncing = true;
        lastAttempt = elapsed;
        worker.execute(() -> {
            try {
                String[] hosts = {"www.baidu.com", "www.qq.com", "www.cloudflare.com"};
                List<Sample> samples = new ArrayList<>();
                Sample acceptedA = null, acceptedB = null;
                for (String host : hosts) {
                    try {
                        Sample sample = fetch(host);
                        for (Sample earlier : samples) {
                            if (Math.abs((earlier.utc - earlier.elapsed) - (sample.utc - sample.elapsed)) <= 5_000) {
                                acceptedA = earlier; acceptedB = sample; break;
                            }
                        }
                        samples.add(sample);
                        if (acceptedA != null) break;
                    } catch (Exception ignored) { /* Try independent source, no personal information sent. */ }
                }
                if (acceptedA == null) {
                    status = "校时未完成 · " + (prefs.contains("utc") ? "沿用上次估计" : "使用本机时间");
                } else {
                    long at = SystemClock.elapsedRealtime();
                    long a = acceptedA.utc + at - acceptedA.elapsed;
                    long b = acceptedB.utc + at - acceptedB.elapsed;
                    long utc = (a + b) / 2;
                    synchronized (this) { anchorUtc = utc; anchorElapsed = at; }
                    uncertainty = Math.max(acceptedA.error, acceptedB.error) + Math.abs(a - b) / 2;
                    status = "网络时间已校准 · 估计误差 ±" + Math.max(1, (uncertainty + 999) / 1000) + " 秒";
                    prefs.edit().putLong("utc", utc).putLong("elapsed", at).putInt("boot", boot)
                        .putLong("offset", utc - System.currentTimeMillis()).putLong("uncertainty", uncertainty)
                        .putString("sources", acceptedA.host + " / " + acceptedB.host).commit();
                }
            } finally {
                syncing = false;
                callback.run();
            }
        });
    }
    private Sample fetch(String host) throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection)new URL("https://" + host + "/?clock=" + SystemClock.elapsedRealtime()).openConnection();
        connection.setRequestMethod("HEAD");
        connection.setConnectTimeout(4000); connection.setReadTimeout(4000);
        connection.setUseCaches(false); connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Cache-Control", "no-cache, no-store");
        try {
            long start = SystemClock.elapsedRealtime();
            int code = connection.getResponseCode();
            long end = SystemClock.elapsedRealtime();
            long date = connection.getHeaderFieldDate("Date", -1);
            long age = connection.getHeaderFieldLong("Age", 0);
            if (code < 200 || code >= 400 || date < 1_577_836_800_000L || age > 0 || end - start > 5000) throw new IllegalStateException("Unreliable sample");
            return new Sample(host, date + 500 + (end - start) / 2, end, 1000 + (end - start) / 2);
        } finally { connection.disconnect(); }
    }
    public void close() { worker.shutdownNow(); }
    private static final class Sample {
        final String host; final long utc, elapsed, error;
        Sample(String host, long utc, long elapsed, long error) { this.host = host; this.utc = utc; this.elapsed = elapsed; this.error = error; }
    }
}
