package org.jpi59.ethicqrscanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Original, code-drawn texture; it contains no copied imagery or external assets. */
final class AmbientBackground extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    AmbientBackground(Context context) { super(context); }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1, w / 260f));
        paint.setColor(0x1F8DE5D0);
        for (int i = 0; i < 7; i++) {
            float inset = w * (0.18f + i * 0.09f);
            canvas.drawRoundRect(inset, h * 0.18f + i * 11, w - inset, h * 0.82f - i * 11,
                    w * 0.08f, w * 0.08f, paint);
        }
    }
}
