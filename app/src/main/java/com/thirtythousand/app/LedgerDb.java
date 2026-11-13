package com.thirtythousand.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class LedgerDb extends SQLiteOpenHelper {
    public static final String[] CATEGORIES = {"工作", "学习", "陪伴", "生活", "休息", "探索"};
    public LedgerDb(Context context) { super(context, "time-ledger.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE entries (day TEXT PRIMARY KEY NOT NULL, note TEXT NOT NULL CHECK(length(note)>0), category TEXT NOT NULL, saved_at INTEGER NOT NULL, remaining INTEGER NOT NULL CHECK(remaining>=0))");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { throw new IllegalStateException("Unsupported migration"); }
    public static final class Entry {
        public final String day, note, category;
        public final long savedAt, remaining;
        public Entry(String day, String note, String category, long savedAt, long remaining) {
            this.day = day; this.note = note; this.category = category; this.savedAt = savedAt; this.remaining = remaining;
        }
        ContentValues values() {
            ContentValues v = new ContentValues(); v.put("day", day); v.put("note", note); v.put("category", category); v.put("saved_at", savedAt); v.put("remaining", remaining); return v;
        }
        JSONObject json() throws JSONException {
            return new JSONObject().put("day", day).put("note", note).put("category", category).put("savedAt", savedAt).put("remainingMs", remaining);
        }
    }
    public String latest() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT MAX(day) FROM entries", null)) { return c.moveToFirst() && !c.isNull(0) ? c.getString(0) : null; }
    }
    public int count() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM entries", null)) { c.moveToFirst(); return c.getInt(0); }
    }
    public Entry get(String day) {
        try (Cursor c = getReadableDatabase().query("entries", null, "day=?", new String[]{day}, null, null, null)) { return c.moveToFirst() ? from(c) : null; }
    }
    public List<Entry> list(String query) {
        List<Entry> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("entries", null, "instr(note,?)>0 OR instr(day,?)>0 OR instr(category,?)>0", new String[]{query, query, query}, null, null, "day DESC", "500")) {
            while (c.moveToNext()) result.add(from(c));
        }
        return result;
    }
    private Entry from(Cursor c) {
        return new Entry(c.getString(c.getColumnIndexOrThrow("day")), c.getString(c.getColumnIndexOrThrow("note")), c.getString(c.getColumnIndexOrThrow("category")), c.getLong(c.getColumnIndexOrThrow("saved_at")), c.getLong(c.getColumnIndexOrThrow("remaining")));
    }
    public boolean save(Entry entry) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (!LifeMath.canWrite(entry.day, latest(), get(entry.day) != null)) return false;
            long row = db.insertWithOnConflict("entries", null, entry.values(), SQLiteDatabase.CONFLICT_IGNORE);
            db.setTransactionSuccessful(); return row != -1;
        } finally { db.endTransaction(); }
    }
    public JSONObject backup(String birthday, String zone, long now) throws JSONException {
        JSONArray rows = new JSONArray();
        try (Cursor c = getReadableDatabase().query("entries", null, null, null, null, null, "day ASC")) { while (c.moveToNext()) rows.put(from(c).json()); }
        return new JSONObject().put("format", "thirty-thousand-backup").put("version", 1).put("birthday", birthday).put("zone", zone).put("exportedAt", now).put("entries", rows);
    }
    public static final class Backup {
        final String birthday, zone; final List<Entry> entries;
        Backup(String birthday, String zone, List<Entry> entries) { this.birthday = birthday; this.zone = zone; this.entries = entries; }
    }
    public static Backup parseBackup(String text) throws Exception {
        JSONObject root = new JSONObject(text);
        if (!"thirty-thousand-backup".equals(root.getString("format")) || root.getInt("version") != 1) throw new IllegalArgumentException("不支持的备份格式或版本");
        String birthday = root.getString("birthday"), zone = root.getString("zone");
        LocalDate birth = LocalDate.parse(birthday); ZoneId zoneId = ZoneId.of(zone);
        if (!birth.toString().equals(birthday) || birth.isBefore(LocalDate.of(1800, 1, 1))) throw new IllegalArgumentException("生日格式不正确");
        JSONArray rows = root.getJSONArray("entries");
        if (rows.length() > 100000) throw new IllegalArgumentException("记录数量超过上限");
        List<Entry> entries = new ArrayList<>();
        java.util.HashSet<String> dates = new java.util.HashSet<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String day = row.getString("day"), note = row.getString("note"), category = row.getString("category");
            long savedAt = row.getLong("savedAt"), remaining = row.getLong("remainingMs");
            if (!LocalDate.parse(day).toString().equals(day) || !dates.add(day) || note.trim().isEmpty() || note.length() > 2000 || !Arrays.asList(CATEGORIES).contains(category)
                || remaining < 0 || remaining > LifeMath.BUDGET_MS || !LifeMath.day(savedAt, zoneId).toString().equals(day)
                || remaining != LifeMath.remaining(birth, zoneId, savedAt)) throw new IllegalArgumentException("备份中有无效或重复的记录，第 " + (i + 1) + " 条");
            entries.add(new Entry(day, note, category, savedAt, remaining));
        }
        return new Backup(birthday, zone, entries);
    }
    public int restore(Backup backup) {
        SQLiteDatabase db = getWritableDatabase(); int added = 0;
        db.beginTransaction();
        try {
            for (Entry e : backup.entries) if (db.insertWithOnConflict("entries", null, e.values(), SQLiteDatabase.CONFLICT_IGNORE) != -1) added++;
            db.setTransactionSuccessful(); return added;
        } finally { db.endTransaction(); }
    }
}
