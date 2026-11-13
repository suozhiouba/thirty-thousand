import com.thirtythousand.app.LifeMath;
import java.time.*;
import java.util.Arrays;

public final class LifeMathTest {
    private static int checks;
    private static void check(boolean condition, String name) { if (!condition) throw new AssertionError(name); checks++; }
    public static void main(String[] args) {
        ZoneId zone = ZoneId.of("Asia/Shanghai"); LocalDate birth = LocalDate.of(2000, 2, 29);
        long start = LifeMath.birthMillis(birth, zone);
        check(LifeMath.remaining(birth, zone, start) == LifeMath.BUDGET_MS, "birthday starts at full budget");
        check(LifeMath.remaining(birth, zone, start + LifeMath.DAY_MS) == LifeMath.BUDGET_MS - LifeMath.DAY_MS, "one day debit");
        check(LifeMath.remaining(birth, zone, start + LifeMath.BUDGET_MS) == 0, "exact zero");
        check(LifeMath.remaining(birth, zone, start + LifeMath.BUDGET_MS + 5000) == 0, "never negative");
        check(LifeMath.remaining(birth, zone, start - 5000) == LifeMath.BUDGET_MS, "never exceed principal");
        check(Arrays.equals(LifeMath.split(2 * LifeMath.DAY_MS + 3_600_000 + 120_000 + 3_004), new long[]{2,1,2,3,4}), "all units");
        check(Arrays.equals(LifeMath.split(1000), new long[]{0,0,0,1,0}), "second rollover");
        check(Arrays.equals(LifeMath.split(999), new long[]{0,0,0,0,999}), "millisecond rollover");
        check(Arrays.equals(LifeMath.split(-1), new long[]{0,0,0,0,0}), "negative split");
        long late = LocalDate.of(2026,11,13).atTime(23,59,59,999_000_000).atZone(zone).toInstant().toEpochMilli();
        check(LifeMath.day(late, zone).toString().equals("2026-11-13"), "before midnight");
        check(LifeMath.day(late + 1, zone).toString().equals("2026-11-14"), "after midnight");
        check(LifeMath.todaySpent(late+1, zone) == 0, "new day spend reset");
        long noon = LocalDate.of(2026,11,13).atTime(12,0).atZone(zone).toInstant().toEpochMilli();
        check(LifeMath.todaySpent(noon, zone) == .5, "half day spend");
        check(!LifeMath.validBirthday(LocalDate.of(2026,11,14), zone, noon), "future birth rejected");
        check(LifeMath.validBirthday(LocalDate.of(2026,11,13), zone, noon), "today birth accepted");
        check(!LifeMath.validBirthday(LocalDate.of(1799,12,31), zone, noon), "minimum birthday");
        check(LifeMath.canWrite("2026-11-13", null, false), "first entry");
        check(!LifeMath.canWrite("2026-11-13", "2026-11-13", true), "same day blocked");
        check(!LifeMath.canWrite("2026-11-12", "2026-11-13", false), "clock rollback blocked");
        check(LifeMath.canWrite("2026-11-14", "2026-11-13", false), "next day accepted");
        ZoneId ny = ZoneId.of("America/New_York"); LocalDate dst = LocalDate.of(2026,3,8);
        long dstStart = LifeMath.birthMillis(dst, ny), dstEnd = dst.plusDays(1).atStartOfDay(ny).toInstant().toEpochMilli();
        check(dstEnd-dstStart == 23*3_600_000L, "DST fixture");
        check(LifeMath.remaining(dst, ny, dstStart + LifeMath.DAY_MS) == LifeMath.BUDGET_MS-LifeMath.DAY_MS, "budget uses exact 24h");
        check(LifeMath.todaySpent(dstEnd-1, ny) < 1, "DST day bounded");
        System.out.println("PASS: " + checks + " date, countdown, DST and daily-lock checks");
    }
}
