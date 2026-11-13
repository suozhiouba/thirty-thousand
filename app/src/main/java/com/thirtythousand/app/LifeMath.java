package com.thirtythousand.app;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public final class LifeMath {
    public static final long DAY_MS = 86_400_000L;
    public static final long BUDGET_MS = 30_000L * DAY_MS;
    private LifeMath() {}
    public static long birthMillis(LocalDate birth, ZoneId zone) {
        return birth.atStartOfDay(zone).toInstant().toEpochMilli();
    }
    public static long remaining(LocalDate birth, ZoneId zone, long now) {
        return Math.max(0L, Math.min(BUDGET_MS, birthMillis(birth, zone) + BUDGET_MS - now));
    }
    public static long[] split(long ms) {
        ms = Math.max(0, ms);
        return new long[]{ms / DAY_MS, ms / 3_600_000 % 24, ms / 60_000 % 60, ms / 1000 % 60, ms % 1000};
    }
    public static LocalDate day(long now, ZoneId zone) { return Instant.ofEpochMilli(now).atZone(zone).toLocalDate(); }
    public static double todaySpent(long now, ZoneId zone) {
        LocalDate today = day(now, zone);
        long start = today.atStartOfDay(zone).toInstant().toEpochMilli();
        long end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        return Math.max(0, Math.min(1, (double)(now - start) / (end - start)));
    }
    public static boolean validBirthday(LocalDate birthday, ZoneId zone, long now) {
        return !birthday.isBefore(LocalDate.of(1800, 1, 1)) && !birthday.isAfter(day(now, zone));
    }
    public static boolean canWrite(String today, String latest, boolean exists) {
        return !exists && (latest == null || today.compareTo(latest) >= 0);
    }
}
