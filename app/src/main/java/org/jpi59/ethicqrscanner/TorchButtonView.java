package org.jpi59.ethicqrscanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Accessible, code-drawn torch control; it uses no third-party icon asset. */
final class TorchButtonView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean torchEnabled;

    TorchButtonView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);
    }

    void setTorchEnabled(boolean enabled) {
        torchEnabled = enabled;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float size = Math.min(getWidth(), getHeight());
        float center = size / 2f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(torchEnabled ? 0xFFE8C98D : 0xE6153136);
        canvas.drawCircle(center, center, size * 0.47f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2f, size * 0.042f));
        paint.setColor(torchEnabled ? 0xFF102428 : 0xFF8DE5D0);
        canvas.drawCircle(center, center, size * 0.47f, paint);

        // Original flashlight silhouette: lens, handle and three light rays.
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(size * .37f, size * .26f, size * .63f, size * .47f),
                size * .035f, size * .035f, paint);
        canvas.drawRoundRect(new RectF(size * .42f, size * .44f, size * .58f, size * .72f),
                size * .04f, size * .04f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1.5f, size * .03f));
        canvas.drawLine(size * .33f, size * .20f, size * .27f, size * .13f, paint);
        canvas.drawLine(size * .50f, size * .17f, size * .50f, size * .08f, paint);
        canvas.drawLine(size * .67f, size * .20f, size * .73f, size * .13f, paint);
    }
}
