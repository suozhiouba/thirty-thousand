package com.thirtythousand.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int BG = 0xff101d2b, CARD = 0xff192b3c, INK = 0xfff1f5f2, MUTED = 0xff94a8b7,
        GREEN = 0xff9ce6ca, GOLD = 0xffe9c891, LINE = 0xff304454;
    private static final int EXPORT = 41, IMPORT = 42;
    private static final Locale LOCALE = Locale.US;
    private SharedPreferences prefs;
    private LedgerDb db;
    private TimeAuthority clock;
    private ZoneId zone;
    private LocalDate birth, selectedBirth;
    private LinearLayout root, page;
    private TextView balance, countdown, todaySpend, clockState, dateLabel;
    private EditText noteInput;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean active;
    private int tab = 0;
    private String shownDay = "", chosenCategory = "生活";
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (!active) return;
            updateLive();
            handler.postDelayed(this, prefs.getBoolean("milliseconds", true) ? 50 : 250);
        }
    };
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        prefs = getSharedPreferences("account", MODE_PRIVATE);
        db = new LedgerDb(this); clock = new TimeAuthority(this);
        loadProfile();
        if (saved != null) tab = saved.getInt("tab", 0);
        render();
    }
    private void loadProfile() {
        zone = ZoneId.of(prefs.getString("zone", ZoneId.systemDefault().getId()));
        String date = prefs.getString("birthday", null);
        birth = date == null ? null : LocalDate.parse(date);
    }
    @Override protected void onResume() {
        super.onResume(); active = true;
        handler.removeCallbacks(ticker); handler.post(ticker);
        sync(false);
    }
    @Override protected void onPause() { active = false; handler.removeCallbacks(ticker); super.onPause(); }
    @Override protected void onSaveInstanceState(Bundle state) { state.putInt("tab", tab); super.onSaveInstanceState(state); }
    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); clock.close(); db.close(); super.onDestroy(); }
    private int dp(float value) { return (int)(getResources().getDisplayMetrics().density * value + .5f); }
    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke); return d;
    }
    private TextView text(String value, float size, int color) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        v.setFontFeatureSettings("tnum"); v.setIncludeFontPadding(false); v.setLineSpacing(dp(3), 1); return v;
    }
    private TextView bold(String value, float size, int color) { TextView v = text(value, size, color); v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); return v; }
    private void add(LinearLayout parent, View v) { parent.addView(v, new LinearLayout.LayoutParams(-1, -2)); }
    private void gap(LinearLayout parent, int height) { View v = new View(this); parent.addView(v, new LinearLayout.LayoutParams(1, dp(height))); }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private void line(LinearLayout parent) { View v = new View(this); v.setBackgroundColor(LINE); parent.addView(v, new LinearLayout.LayoutParams(-1, dp(1))); }
    private LinearLayout card(LinearLayout parent) {
        LinearLayout l = column(); l.setPadding(dp(20), dp(20), dp(20), dp(20)); l.setBackground(shape(CARD, 22, 0)); add(parent, l); return l;
    }
    private Button button(String label, boolean primary, View.OnClickListener listener) {
        Button b = new Button(this); b.setText(label); b.setTextSize(15); b.setAllCaps(false);
        b.setTextColor(primary ? BG : INK); b.setBackground(shape(primary ? GREEN : CARD, 14, primary ? 0 : LINE));
        b.setMinHeight(dp(52)); b.setMinimumHeight(dp(52)); b.setPadding(dp(14), dp(12), dp(14), dp(12));
        b.setStateListAnimator(null); b.setOnClickListener(listener); return b;
    }
    private EditText input(String hint, boolean multiline) {
        EditText e = new EditText(this); e.setTextColor(INK); e.setHintTextColor(MUTED); e.setTextSize(15);
        e.setHint(hint); e.setPadding(dp(16), dp(16), dp(16), dp(16)); e.setBackground(shape(BG, 12, LINE));
        e.setGravity(Gravity.TOP | Gravity.START);
        e.setInputType(InputType.TYPE_CLASS_TEXT | (multiline ? InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES : 0));
        if (!multiline) e.setSingleLine(true); return e;
    }
    private void title(String eyebrow, String title, String subtitle) {
        add(page, text(eyebrow, 11, GREEN)); gap(page, 9); add(page, bold(title, 28, INK));
        if (!subtitle.isEmpty()) { gap(page, 10); add(page, text(subtitle, 13, MUTED)); } gap(page, 24);
    }
    private void render() {
        balance = countdown = todaySpend = clockState = dateLabel = null; noteInput = null;
        shownDay = LifeMath.day(clock.now(), zone).toString();
        root = column(); root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(root);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        page = column(); page.setPadding(dp(24), dp(24), dp(24), dp(24)); scroll.addView(page);
        if (birth == null) onboarding();
        else {
            switch(tab) { case 1: journal(); break; case 2: history(); break; case 3: settings(); break; default: wallet(); }
            navigation();
        }
        updateLive();
    }
    private void onboarding() {
        ImageView icon = new ImageView(this); icon.setImageResource(com.thirtythousand.app.R.drawable.ic_launcher);
        page.addView(icon, new LinearLayout.LayoutParams(dp(60), dp(60)));
        gap(page, 24); add(page, text("三 万  /  TIME ACCOUNT", 12, GREEN)); gap(page, 18);
        add(page, bold("把时间，\n当作唯一的本金。", 34, INK)); gap(page, 18);
        add(page, text("人生短短三万天。\n看见它的流逝，也留下认真生活的痕迹。", 15, MUTED)); gap(page, 30);
        LinearLayout intro = card(page);
        add(intro, text("每个人，相同的起点", 12, MUTED)); gap(intro, 12);
        TextView amount = text("30,000.00", 42, GREEN); amount.setTypeface(Typeface.create("sans-serif-light", 0)); add(intro, amount);
        gap(intro, 8); add(intro, text("时  /  1 时 = 1 天 = 86,400 秒", 12, INK)); gap(intro, 16); line(intro); gap(intro, 16);
        add(intro, text("没有充值，也不必追赶。\n把今天，花在你在乎的事上。", 14, MUTED)); gap(page, 28);
        add(page, bold("从你的生日开始", 16, INK)); gap(page, 12);
        if (selectedBirth == null) selectedBirth = LocalDate.of(2000, 1, 1);
        Button pick = button("选择出生日期", false, null);
        pick.setOnClickListener(v -> chooseDate(selectedBirth, date -> { selectedBirth = date; pick.setText(date.toString() + "  ·  点击更改"); }));
        add(page, pick); gap(page, 10);
        add(page, text("以生日当天零点起算 · 账户时区 " + zone.getId(), 12, MUTED)); gap(page, 22);
        add(page, button("开启我的时间账户  →", true, v -> {
            if (pick.getText().toString().equals("选择出生日期")) { toast("请先选择你的出生日期"); return; }
            if (!LifeMath.validBirthday(selectedBirth, zone, clock.now())) { toast("出生日期不能晚于今天"); return; }
            if (!prefs.edit().putString("birthday", selectedBirth.toString()).putString("zone", zone.getId()).commit()) { toast("保存失败，请检查手机存储空间"); return; }
            loadProfile(); tab = 0; render();
        })); gap(page, 12);
        add(page, button("从本地备份恢复", false, v -> pickImport())); gap(page, 22);
        add(page, text("三万天是一种时间刻度，不是寿命预测。\n生日与留言仅保存在本机，联网仅用于校时。", 12, MUTED));
        gap(page, 12); clockState = text(clock.status(), 11, MUTED); add(page, clockState);
    }
    private interface DateChosen { void accept(LocalDate date); }
    private void chooseDate(LocalDate initial, DateChosen chosen) {
        DatePickerDialog dialog = new DatePickerDialog(this, (picker, y, m, d) -> chosen.accept(LocalDate.of(y, m + 1, d)), initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth());
        dialog.getDatePicker().setMaxDate(clock.now());
        dialog.getDatePicker().setMinDate(LocalDate.of(1800, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
        dialog.setTitle("出生日期"); dialog.show();
    }
    private void wallet() {
        LinearLayout brand = row();
        TextView name = bold("三万", 24, INK); brand.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        TextView mark = text("时间账户  /  私人存折", 11, MUTED); brand.addView(mark); add(page, brand);
        gap(page, 8); dateLabel = text("", 12, MUTED); add(page, dateLabel); gap(page, 26);
        LinearLayout account = card(page);
        LinearLayout heading = row();
        TextView label = text("可用时间余额", 13, MUTED); heading.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        heading.addView(text("TIME / 时", 10, GREEN)); add(account, heading); gap(account, 15);
        balance = text("", 37, GREEN); balance.setTypeface(Typeface.create("sans-serif-light", 0)); balance.setSingleLine(true);
        balance.setAutoSizeTextTypeUniformWithConfiguration(22, 39, 1, android.util.TypedValue.COMPLEX_UNIT_SP); add(account, balance);
        gap(account, 8); add(account, text("1 时 = 1 天 · 每秒支出 0.00001157 时", 11, MUTED)); gap(account, 25); line(account); gap(account, 18);
        add(account, text("剩余的人生刻度", 12, MUTED)); gap(account, 10);
        countdown = text("", 17, INK); countdown.setTypeface(Typeface.MONOSPACE); add(account, countdown); gap(account, 18);
        clockState = text(clock.status(), 10, MUTED); add(account, clockState);
        gap(page, 20);
        LinearLayout stats = row();
        LinearLayout left = column(), right = column();
        add(left, text("初始本金", 12, MUTED)); gap(left, 8); add(left, bold("30,000 时", 20, INK));
        long remaining = LifeMath.remaining(birth, zone, clock.now());
        add(right, text("已经走过", 12, MUTED)); gap(right, 8);
        add(right, bold(String.format(LOCALE, "%,d 天", (LifeMath.BUDGET_MS - remaining) / LifeMath.DAY_MS), 20, INK));
        stats.addView(left, new LinearLayout.LayoutParams(0, -2, 1)); stats.addView(right, new LinearLayout.LayoutParams(0, -2, 1)); add(page, stats);
        gap(page, 26); line(page); gap(page, 23);
        LinearLayout gridHead = row(); TextView gridTitle = bold("一生的刻度", 15, INK);
        gridHead.addView(gridTitle, new LinearLayout.LayoutParams(0, -2, 1)); gridHead.addView(text("每格 100 天", 11, MUTED)); add(page, gridHead); gap(page, 14);
        page.addView(new LifeGrid(this, 1 - (double)remaining / LifeMath.BUDGET_MS), new LinearLayout.LayoutParams(-1, dp(96)));
        gap(page, 12); add(page, text("灰 · 已走过       金 · 此刻       绿 · 尚可支配", 10, MUTED)); gap(page, 25);
        LinearLayout today = card(page);
        add(today, text("今日流水", 12, MUTED)); gap(today, 8); todaySpend = bold("", 22, INK); add(today, todaySpend);
        gap(today, 6); add(today, text("今天的时间正在流动，你把它花在哪里？", 12, MUTED)); gap(today, 16);
        boolean recorded = db.get(shownDay) != null;
        add(today, button(recorded ? "查看今天的留言  ↗" : "给今天，留一笔  ↗", true, v -> changeTab(1)));
        gap(page, 22); add(page, text(remaining == 0 ? "你已走过三万天的刻度。今天，依然值得认真度过。" : "时间不会为谁停留，但此刻仍然属于你。", 12, MUTED));
    }
    private void journal() {
        title("DAILY NOTE  /  " + shownDay, "给今天，留一笔", "不必写得漂亮，只要真实。每天仅能保存一条。");
        LedgerDb.Entry existing = db.get(shownDay);
        if (existing != null) {
            LinearLayout c = card(page); add(c, text("✓  今天已入账", 13, GREEN)); gap(c, 20);
            add(c, bold(existing.category, 18, INK)); gap(c, 16); TextView body = text(existing.note, 17, INK); body.setTextIsSelectable(true); add(c, body);
            gap(c, 22); line(c); gap(c, 16); add(c, text("记录于 " + formatTime(existing.savedAt) + "\n当日时间额度 1.00 时 · 留言已封存", 12, MUTED));
            gap(page, 24); add(page, text("今天的痕迹，已经好好留下。\n明天再来，写下新的故事。", 15, MUTED)); gap(page, 22);
            add(page, button("翻阅历史账本", false, v -> changeTab(2))); return;
        }
        if (!LifeMath.canWrite(shownDay, db.latest(), false)) {
            LinearLayout c = card(page); add(c, bold("日期需要核对", 20, GOLD)); gap(c, 12);
            add(c, text("当前日期早于已经记录的日期。请联网校准时间后再写，以免把留言记入错误的一天。", 15, MUTED));
            gap(c, 20); add(c, button("重新联网校时", true, v -> sync(true))); return;
        }
        LinearLayout receipt = card(page);
        add(receipt, text("今日时间额度", 12, MUTED)); gap(receipt, 10); add(receipt, bold("1.00 时", 30, GREEN)); gap(receipt, 8);
        add(receipt, text("24 小时，用来生活，也用来成为自己。", 12, MUTED));
        gap(page, 24); add(page, bold("今天，主要花在哪里？", 16, INK)); gap(page, 12);
        chosenCategory = prefs.getString("draftCategory:" + shownDay, "生活");
        LinearLayout chips = column(); add(page, chips);
        for (int r = 0; r < 2; r++) {
            LinearLayout chipRow = row();
            for (int j = 0; j < 3; j++) {
                String category = LedgerDb.CATEGORIES[r * 3 + j];
                TextView chip = bold(category, 14, category.equals(chosenCategory) ? BG : MUTED);
                chip.setGravity(Gravity.CENTER); chip.setPadding(0, dp(13), 0, dp(13)); chip.setTag(category);
                chip.setBackground(shape(category.equals(chosenCategory) ? GREEN : CARD, 12, 0));
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1); if (j < 2) p.rightMargin = dp(8); chipRow.addView(chip, p);
                chip.setOnClickListener(v -> {
                    chosenCategory = category; prefs.edit().putString("draftCategory:" + shownDay, category).apply();
                    for (int a = 0; a < chips.getChildCount(); a++) if (chips.getChildAt(a) instanceof LinearLayout) {
                        LinearLayout rr = (LinearLayout)chips.getChildAt(a);
                        for (int b = 0; b < rr.getChildCount(); b++) {
                            TextView t = (TextView)rr.getChildAt(b); boolean selected = category.equals(t.getTag());
                            t.setTextColor(selected ? BG : MUTED); t.setBackground(shape(selected ? GREEN : CARD, 12, 0));
                        }
                    }
                });
            }
            add(chips, chipRow); if (r == 0) gap(chips, 8);
        }
        gap(page, 20);
        noteInput = input("今天做了什么？\n什么值得记住，又有什么想留给明天？", true); noteInput.setMinLines(6);
        noteInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2000)});
        noteInput.setText(prefs.getString("draft:" + shownDay, "")); add(page, noteInput); gap(page, 8);
        TextView counter = text(noteInput.length() + " / 2000 · 草稿自动保存在本机", 11, MUTED); add(page, counter);
        final String draftDay = shownDay;
        noteInput.addTextChangedListener(watcher(value -> { prefs.edit().putString("draft:" + draftDay, value).apply(); counter.setText(value.length() + " / 2000 · 草稿自动保存在本机"); }));
        gap(page, 22); add(page, button("封存今天的留言", true, v -> confirmSave())); gap(page, 14);
        add(page, text("保存后不能修改或补写。日期按账户时区划分。", 12, MUTED));
    }
    private void confirmSave() {
        String note = noteInput.getText().toString().trim();
        if (note.isEmpty()) { noteInput.setError("留下一句话，再封存今天"); return; }
        final String expectedDay = shownDay, category = chosenCategory;
        new AlertDialog.Builder(this).setTitle("封存 " + expectedDay + " 的留言？")
            .setMessage("这是今天唯一的一条留言，保存后不能修改。\n\n" + note)
            .setNegativeButton("再想一想", null).setPositiveButton("确认封存", (d, w) -> {
                long now = clock.now(); String day = LifeMath.day(now, zone).toString();
                if (!day.equals(expectedDay)) { render(); toast("日期已改变，原草稿仍保留，请核对后再保存"); return; }
                try {
                    boolean saved = db.save(new LedgerDb.Entry(day, note, category, now, LifeMath.remaining(birth, zone, now)));
                    if (saved) { prefs.edit().remove("draft:" + day).remove("draftCategory:" + day).apply(); hideKeyboard(); render(); toast("今天的留言已封存"); }
                    else { render(); toast("今天已保存，或当前日期需要重新校准"); }
                } catch (Exception e) { toast("保存失败，请检查手机存储空间，草稿仍保留"); }
            }).show();
    }
    private interface Changed { void accept(String text); }
    private TextWatcher watcher(Changed changed) { return new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) { changed.accept(s.toString()); }
        public void afterTextChanged(Editable e) {}
    }; }
    private void history() {
        title("YOUR TIME LEDGER", "时间花过的地方", "留下 " + db.count() + " 天的痕迹。每一笔，都是生活的凭证。");
        EditText search = input("搜索留言、日期或投入方向", false); add(page, search); gap(page, 20);
        LinearLayout results = column(); add(page, results);
        fillHistory(results, ""); search.addTextChangedListener(watcher(value -> fillHistory(results, value.trim())));
    }
    private void fillHistory(LinearLayout results, String query) {
        results.removeAllViews(); List<LedgerDb.Entry> entries = db.list(query);
        if (entries.isEmpty()) {
            LinearLayout c = card(results); add(c, bold(query.isEmpty() ? "你的第一笔，还没写下" : "没有找到这段回忆", 18, INK)); gap(c, 12);
            add(c, text(query.isEmpty() ? "从今天开始。哪怕只是普通的一天，也值得留下。" : "换一个关键词，或试试日期。", 14, MUTED));
            if (query.isEmpty()) { gap(c, 20); add(c, button("写下今天", true, v -> changeTab(1))); } return;
        }
        String month = "";
        for (LedgerDb.Entry entry : entries) {
            if (!month.equals(entry.day.substring(0, 7))) { month = entry.day.substring(0, 7); gap(results, 8); add(results, text(month.replace("-", " / "), 12, GREEN)); gap(results, 14); }
            LinearLayout c = card(results);
            LinearLayout first = row(); TextView date = bold(entry.day.substring(5).replace("-", " / "), 19, INK);
            first.addView(date, new LinearLayout.LayoutParams(0, -2, 1)); first.addView(text(entry.category + "  ·  1 天", 12, GREEN)); add(c, first);
            gap(c, 14); TextView excerpt = text(entry.note, 15, INK); excerpt.setMaxLines(3); excerpt.setEllipsize(android.text.TextUtils.TruncateAt.END); add(c, excerpt);
            gap(c, 14); add(c, text("写下时余额 " + String.format(LOCALE, "%,.4f 时", (double)entry.remaining / LifeMath.DAY_MS) + "  ↗", 11, MUTED));
            c.setOnClickListener(v -> showEntry(entry)); c.setContentDescription(entry.day + "，" + entry.category + "，" + entry.note + "，点击查看全文"); gap(results, 12);
        }
        if (entries.size() == 500) add(results, text("显示最近 500 条匹配记录，可搜索更早的日期。", 12, MUTED));
    }
    private void showEntry(LedgerDb.Entry entry) {
        ScrollView scroll = new ScrollView(this); LinearLayout content = column(); content.setPadding(dp(24), dp(16), dp(24), dp(16)); scroll.addView(content);
        add(content, text(entry.category + " · " + formatTime(entry.savedAt), 12, GREEN)); gap(content, 20);
        TextView note = text(entry.note, 17, INK); note.setTextIsSelectable(true); add(content, note); gap(content, 24);
        add(content, text("当日时间额度 1.00 时\n写下时余额 " + String.format(LOCALE, "%,.6f 时", (double)entry.remaining / LifeMath.DAY_MS), 12, MUTED));
        new AlertDialog.Builder(this).setTitle(entry.day + " 的留言").setView(scroll).setPositiveButton("收好这一天", null).show();
    }
    private void settings() {
        title("ACCOUNT SETTINGS", "照看你的账户", "时间的规则说清楚，记录也由你自己保管。");
        LinearLayout account = card(page);
        add(account, text("出生日期", 12, MUTED)); gap(account, 8); add(account, bold(birth.toString(), 24, INK)); gap(account, 10);
        add(account, text("账户时区  " + zone.getId() + "\n初始本金  30,000 时 · 1 时 = 24 小时", 12, MUTED)); gap(account, 16);
        add(account, button(db.count() == 0 ? "修正出生日期" : "出生日期已随账本锁定", false, v -> {
            if (db.count() > 0) { toast("已有留言后锁定生日，确保历史余额保持一致"); return; }
            chooseDate(birth, date -> new AlertDialog.Builder(this).setTitle("修正为 " + date + "？").setMessage("时间余额会根据新生日重新计算。")
                .setNegativeButton("取消", null).setPositiveButton("确认修正", (d, w) -> {
                    if (!LifeMath.validBirthday(date, zone, clock.now())) { toast("出生日期不能晚于今天"); return; }
                    if (prefs.edit().putString("birthday", date.toString()).commit()) { loadProfile(); render(); } else toast("保存失败");
                }).show());
        })); gap(page, 18);
        LinearLayout timing = card(page); add(timing, bold("网络校时", 17, INK)); gap(timing, 12);
        clockState = text(clock.status(), 12, GREEN); add(timing, clockState); gap(timing, 10);
        add(timing, text("至少两个公开 HTTPS 来源交叉核对。\n断网时继续计时，联网后自动重试。", 12, MUTED)); gap(timing, 12);
        add(timing, text("上次来源：" + clock.lastSource() + (clock.lastSynced() == 0 ? "" : "\n上次校准：" + formatTime(clock.lastSynced())), 11, MUTED)); gap(timing, 16);
        add(timing, button("立即联网校时", false, v -> sync(true))); gap(timing, 16);
        android.widget.Switch milliseconds = new android.widget.Switch(this); milliseconds.setText("显示毫秒"); milliseconds.setTextColor(INK); milliseconds.setTextSize(14); milliseconds.setChecked(prefs.getBoolean("milliseconds", true));
        milliseconds.setOnCheckedChangeListener((b, checked) -> prefs.edit().putBoolean("milliseconds", checked).apply()); add(timing, milliseconds); gap(timing, 10);
        add(timing, text("毫秒是显示刻度，网络校时仍可能存在秒级误差。关闭毫秒可减少界面刷新。", 11, MUTED)); gap(page, 18);
        LinearLayout backup = card(page); add(backup, bold("我的记录，我来保管", 17, INK)); gap(backup, 10);
        add(backup, text("已有 " + db.count() + " 条留言，仅存于本机。卸载会清除数据，换机前请导出备份。", 12, MUTED)); gap(backup, 16);
        add(backup, button("导出本地备份", false, v -> exportBackup())); gap(backup, 10);
        add(backup, button("从备份恢复", false, v -> pickImport())); gap(backup, 12);
        add(backup, text("备份是包含生日与留言的明文 JSON 文件，请妥善保存。恢复时已有日期不会覆盖。", 11, MUTED)); gap(page, 24);
        add(page, bold("人生不止一个数字", 16, GOLD)); gap(page, 12);
        add(page, text("三万天只是帮助你感知时间的统一刻度，不是寿命预测。超过刻度后，仍然可以继续记录每一天。\n\n没有 AI，没有账号，也没有广告。联网仅校时，不上传生日和留言。\n\n三万 · 时间账户  1.0.0", 12, MUTED));
    }
    private void navigation() {
        line(root); LinearLayout nav = row(); nav.setPadding(dp(10), dp(8), dp(10), dp(8));
        String[] labels = {"余额", "留一笔", "账本", "设置"}; String[] icons = {"▤", "＋", "≡", "○"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i; LinearLayout item = column(); item.setGravity(Gravity.CENTER); item.setPadding(0, dp(6), 0, dp(6));
            TextView icon = text(icons[i], 23, i == tab ? GREEN : MUTED); icon.setGravity(Gravity.CENTER); add(item, icon); gap(item, 4);
            TextView caption = text(labels[i], 11, i == tab ? GREEN : MUTED); caption.setGravity(Gravity.CENTER); add(item, caption);
            if (i == tab) item.setBackground(shape(CARD, 13, 0)); item.setOnClickListener(v -> changeTab(index));
            item.setContentDescription(labels[i] + (i == tab ? "，已选中" : "")); item.setFocusable(true);
            nav.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
        }
        add(root, nav);
    }
    private void changeTab(int index) { hideKeyboard(); tab = index; render(); }
    private void hideKeyboard() { ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(), 0); }
    private void updateLive() {
        long now = clock.now(); String actualDay = LifeMath.day(now, zone).toString();
        if (birth != null && !shownDay.equals(actualDay)) {
            String oldDay = shownDay; render();
            if (tab == 1 && prefs.contains("draft:" + oldDay)) offerOldDraft(oldDay);
            return;
        }
        if (clockState != null) clockState.setText(clock.status());
        if (birth == null) return;
        long remaining = LifeMath.remaining(birth, zone, now);
        if (balance != null) balance.setText(String.format(LOCALE, "%,.6f", Math.floor((double)remaining / LifeMath.DAY_MS * 1_000_000) / 1_000_000));
        if (countdown != null) {
            long[] p = LifeMath.split(remaining);
            countdown.setText(String.format(LOCALE, "%,d 天\n%02d 时  %02d 分  %02d 秒", p[0], p[1], p[2], p[3]) + (prefs.getBoolean("milliseconds", true) ? String.format(LOCALE, "  %03d 毫秒", p[4]) : ""));
        }
        if (todaySpend != null) todaySpend.setText(String.format(LOCALE, "− %.6f 时", LifeMath.todaySpent(now, zone)));
        if (dateLabel != null) dateLabel.setText(Instant.ofEpochMilli(now).atZone(zone).format(DateTimeFormatter.ofPattern("yyyy 年 MM 月 dd 日  EEEE", Locale.CHINA)));
    }
    private void offerOldDraft(String oldDay) {
        String draft = prefs.getString("draft:" + oldDay, "");
        if (draft.trim().isEmpty() || noteInput == null) return;
        new AlertDialog.Builder(this).setTitle("已经是新的一天了")
            .setMessage("上一天的草稿还未封存。可以复制到今天继续整理，记录日期将是今天。\n\n" + draft)
            .setNegativeButton("暂不复制", null).setPositiveButton("复制到今天", (d, w) -> { if (noteInput != null) noteInput.setText(draft); }).show();
    }
    private void sync(boolean force) {
        clock.sync(force, () -> runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            updateLive();
            if (tab == 3 && birth != null) render();
            if (force) toast(clock.status());
        })); updateLive();
    }
    private String formatTime(long at) { return Instant.ofEpochMilli(at).atZone(zone).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA)); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    private void exportBackup() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE, "三万-时间账本-" + shownDay + ".json");
        try { startActivityForResult(intent, EXPORT); } catch (Exception e) { toast("本机没有可用的文件选择器"); }
    }
    private void pickImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
        try { startActivityForResult(intent, IMPORT); } catch (Exception e) { toast("本机没有可用的文件选择器"); }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        if (request == EXPORT) {
            try {
                byte[] bytes = db.backup(birth.toString(), zone.getId(), clock.now()).toString(2).getBytes(StandardCharsets.UTF_8);
                try (OutputStream out = getContentResolver().openOutputStream(data.getData(), "wt")) {
                    if (out == null) throw new IllegalStateException(); out.write(bytes);
                }
                toast("备份已导出，共 " + db.count() + " 条留言");
            } catch (Exception e) { toast("导出失败，请检查目标位置和可用空间"); }
        } else if (request == IMPORT) {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (InputStream in = getContentResolver().openInputStream(data.getData())) {
                    if (in == null) throw new IllegalStateException();
                    byte[] chunk = new byte[8192]; int n;
                    while ((n = in.read(chunk)) != -1) { if (bytes.size() + n > 32 * 1024 * 1024) throw new IllegalArgumentException("备份文件超过 32 MB 上限"); bytes.write(chunk, 0, n); }
                }
                LedgerDb.Backup backup = LedgerDb.parseBackup(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
                if (!LifeMath.validBirthday(LocalDate.parse(backup.birthday), ZoneId.of(backup.zone), clock.now())) throw new IllegalArgumentException("备份生日晚于当前日期，请先核对时间");
                if (birth != null && (!birth.toString().equals(backup.birthday) || !zone.getId().equals(backup.zone))) throw new IllegalArgumentException("备份的生日或账户时区与当前账户不同，无法混合恢复");
                new AlertDialog.Builder(this).setTitle("恢复这份时间账本？")
                    .setMessage("生日：" + backup.birthday + "\n时区：" + backup.zone + "\n记录：" + backup.entries.size() + " 条\n\n已有日期会保留原记录，不会覆盖。")
                    .setNegativeButton("取消", null).setPositiveButton("恢复", (d, w) -> {
                        try {
                            if (birth == null && !prefs.edit().putString("birthday", backup.birthday).putString("zone", backup.zone).commit()) throw new IllegalStateException();
                            int added = db.restore(backup); loadProfile(); tab = 2; render(); toast("已恢复 " + added + " 条；跳过已有日期 " + (backup.entries.size() - added) + " 条");
                        } catch (Exception e) { toast("恢复失败，请检查手机存储空间"); }
                    }).show();
            } catch (Exception e) { new AlertDialog.Builder(this).setTitle("无法恢复备份").setMessage(e instanceof IllegalArgumentException ? e.getMessage() : "文件无法读取或格式不正确。请选择本应用导出的 JSON 备份。").setPositiveButton("知道了", null).show(); }
        }
    }
    @Override public void onBackPressed() { if (birth != null && tab != 0) changeTab(0); else super.onBackPressed(); }
}
