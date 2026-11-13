package com.thirtythousand.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** 300 cells: one cell is 100 days. */
public final class LifeGrid extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private double spent;
    public LifeGrid(Context context, double spent) { super(context); this.spent = spent; setContentDescription("三万天刻度，每格一百天，已使用 " + Math.round(spent * 100) + "%"); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int columns = 30, rows = 10;
        float gap = getResources().getDisplayMetrics().density * 3;
        float w = (getWidth() - gap * (columns - 1)) / columns;
        float h = (getHeight() - gap * (rows - 1)) / rows;
        int used = (int)(spent * 300);
        for (int i = 0; i < 300; i++) {
            paint.setColor(i < used ? 0xff334657 : i == used && used < 300 ? 0xffe9c891 : 0xff9ce6ca);
            float x = (i % columns) * (w + gap), y = (i / columns) * (h + gap);
            canvas.drawRoundRect(new RectF(x, y, x + w, y + h), 2, 2, paint);
        }
    }
}
