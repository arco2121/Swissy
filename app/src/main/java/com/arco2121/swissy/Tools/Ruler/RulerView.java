package com.arco2121.swissy.Tools.Ruler;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import com.arco2121.swissy.R;

public class RulerView extends View {
    private final float lengthMm;
    private final float pxPerMm;
    private final float calibrationFactor;
    private final Ruler.RulerUnit unit;
    private final RulerListener listener;
    private final Paint linePaint;
    private final Paint textPaint;

    protected RulerView(Context context, float lengthMm, float pxPerMm, Ruler.RulerUnit unit, float calibrationFactor, RulerListener listener) {
        super(context);
        this.lengthMm = lengthMm;
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
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float startY = 10f;
        float baseX = getWidth() - (getWidth() - 0f);

        for (int mm = 0; mm <= lengthMm; mm++) {
            float y = startY + mm * pxPerMm * calibrationFactor;

            float height;
            boolean drawLabel = false;
            String label = "";

            switch (unit) {
                case MILLIMETER:
                    height = (mm % 10 == 0) ? 60 : (mm % 5 == 0 ? 40 : 25);
                    drawLabel = mm % 10 == 0;
                    label = mm + " mm";
                    break;

                case CENTIMETER:
                    height = (mm % 10 == 0) ? 60 : 30;
                    drawLabel = mm % 10 == 0;
                    label = (mm / 10) + " cm";
                    break;

                case INCH:
                    int inchMm = (int) (mm / 25.4f * 16);
                    height = (inchMm % 16 == 0) ? 60 : (inchMm % 8 == 0 ? 40 : 25);
                    drawLabel = inchMm % 16 == 0;
                    label = String.format("%.1f in", mm / 25.4f);
                    break;

                case PIXEL:
                    height = (mm % 10 == 0) ? 60 : 30;
                    drawLabel = mm % 10 == 0;
                    label = String.valueOf((int)(mm * pxPerMm * calibrationFactor));
                    break;

                default:
                    height = 30;
            }

            canvas.drawLine(baseX, y, baseX + height, y, linePaint);

            if (drawLabel) {
                canvas.drawText(label, baseX + 10, y + 8, textPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN ||
                event.getAction() == MotionEvent.ACTION_MOVE) {

            float lastTouchX = event.getX();
            float mm = (lastTouchX - 30f) / (pxPerMm * calibrationFactor);
            mm = Math.max(0, Math.min(mm, lengthMm));

            if (listener != null) {
                listener.onMeasure(
                        mm,
                        mm / 10f,
                        mm / 25.4f,
                        mm * pxPerMm * calibrationFactor
                );
            }
            invalidate();
            return true;
        }
        return super.onTouchEvent(event);
    }
}