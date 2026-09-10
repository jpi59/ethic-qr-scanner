package org.jpi59.ethicqrscanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.view.View;

/** Original camera framing overlay. */
final class ScannerOverlayView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    ScannerOverlayView(Context context) {
        super(context);
        // CLEAR gives the camera a single, unambiguous scanning window.
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float size = Math.min(w * 0.78f, getHeight() * 0.48f);
        float left = (w - size) / 2f;
        float top = (getHeight() - size) * 0.45f;
        RectF frame = new RectF(left, top, left + size, top + size);

        int save = canvas.saveLayer(0, 0, w, getHeight(), null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xA6000000);
        canvas.drawRect(0, 0, w, getHeight(), paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawRoundRect(frame, size * 0.08f, size * 0.08f, paint);
        paint.setXfermode(null);
        canvas.restoreToCount(save);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(Math.max(4f, w / 92f));
        paint.setColor(0xFF8DE5D0);
        float corner = size * 0.16f;
        canvas.drawLine(left, top + corner, left, top, paint);
        canvas.drawLine(left, top, left + corner, top, paint);
        canvas.drawLine(left + size - corner, top, left + size, top, paint);
        canvas.drawLine(left + size, top, left + size, top + corner, paint);
        canvas.drawLine(left, top + size - corner, left, top + size, paint);
        canvas.drawLine(left, top + size, left + corner, top + size, paint);
        canvas.drawLine(left + size - corner, top + size, left + size, top + size, paint);
        canvas.drawLine(left + size, top + size - corner, left + size, top + size, paint);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setColor(0xBFE8C98D);
        canvas.drawLine(left + size * 0.16f, top + size / 2f, left + size * 0.84f, top + size / 2f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(Math.max(14f, w / 24f));
        canvas.drawText("ENFOQUE LOCAL", w / 2f, top - size * 0.08f, paint);
    }
}
