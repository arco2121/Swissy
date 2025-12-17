package com.arco2121.swissy.Tools.Ruler;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;

import com.arco2121.swissy.R;

public class RulerView extends View {
    private final float pxPerMm;
    private final float calibrationFactor;
    public final Ruler.RulerUnit unit;
    private final RulerListener listener;
    private final Paint linePaint;
    private final Paint textPaint;
    private final float lengthMm;
    private static final float START_Y = 10f;
    private static final float TEXT_X = 90f;
    private static final float TEXT_Y_OFFSET = 10f;

    public RulerView(Context context, float lengthM, float pxPerMm, Ruler.RulerUnit unit, float calibrationFactor, RulerListener listener) {
        super(context);
        this.lengthMm = lengthM;
        this.pxPerMm = pxPerMm;
        this.unit = unit;
        this.calibrationFactor = calibrationFactor;
        this.listener = listener;

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStrokeWidth(3f);
        linePaint.setColor(context.getColor(R.color.font_color));

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(26f);
        textPaint.setColor(context.getColor(R.color.font_color));

        Typeface tf = ResourcesCompat.getFont(context, R.font.font);
        if (tf != null) textPaint.setTypeface(tf);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float baseX = 0f;
        if (unit == Ruler.RulerUnit.INCH) {
            drawInches(canvas, baseX);
        } else {
            drawMetric(canvas, baseX);
        }
    }

    private void drawMetric(Canvas canvas, float baseX) {
        int totalMm = Math.round(lengthMm);
        for (int mm = 0; mm <= totalMm; mm++) {
            float y = START_Y + mm * pxPerMm * calibrationFactor;

            float height = 25f;
            boolean drawLabel = false;
            String label = null;

            switch (unit) {
                case MILLIMETER:
                    if (mm % 10 == 0) {
                        height = 60;
                        drawLabel = true;
                        label = mm + " mm";
                    } else if (mm % 5 == 0) {
                        height = 40;
                    }
                    break;
                case CENTIMETER:
                    if (mm % 10 == 0) {
                        height = 60;
                        drawLabel = true;
                        label = (mm / 10) + " cm";
                    } else {
                        height = 30;
                    }
                    break;
                case PIXEL:
                    if (mm % 10 == 0) {
                        height = 60;
                        drawLabel = true;
                        label = String.valueOf(Math.round(mm * pxPerMm * calibrationFactor));
                    } else {
                        height = 30;
                    }
                    break;
            }

            canvas.drawLine(baseX, y, baseX + height, y, linePaint);
            if (drawLabel && label != null) {
                canvas.drawText(label, baseX + TEXT_X, y + TEXT_Y_OFFSET, textPaint);
            }
        }
    }

    private void drawInches(Canvas canvas, float baseX) {
        int totalSixteenths = Math.round(lengthMm / 25.4f * 16f);
        for (int s = 0; s <= totalSixteenths; s++) {
            float mm = (s / 16f) * 25.4f;
            if (mm > lengthMm) break;

            float y = START_Y + mm * pxPerMm * calibrationFactor;
            float height;
            boolean drawLabel = false;
            String label = null;

            if (s % 16 == 0) {
                height = 60;
                drawLabel = true;
                label = (s / 16) + " in";
            } else if (s % 8 == 0) {
                height = 40;
            } else {
                height = 25;
            }

            canvas.drawLine(baseX, y, baseX + height, y, linePaint);
            if (drawLabel && label != null) {
                canvas.drawText(label, baseX + TEXT_X, y + TEXT_Y_OFFSET, textPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN ||
                event.getAction() == MotionEvent.ACTION_MOVE) {

            float y = event.getY();
            float mm = (y - START_Y) / (pxPerMm * calibrationFactor);
            mm = Math.max(0f, mm);

            if (listener != null) {
                listener.onMeasure(
                        mm,
                        mm / 10f,
                        mm / 25.4f,
                        mm * pxPerMm * calibrationFactor
                );
            }
            return true;
        }
        return super.onTouchEvent(event);
    }
}