package org.jpi59.ethicqrscanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Original gallery/photo icon drawn in code, paired with the torch control. */
final class PhotoButtonView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    PhotoButtonView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float size = Math.min(getWidth(), getHeight());
        float center = size / 2f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xE6153136);
        canvas.drawCircle(center, center, size * 0.47f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2f, size * 0.042f));
        paint.setColor(0xFF8DE5D0);
        canvas.drawCircle(center, center, size * 0.47f, paint);
        RectF frame = new RectF(size * .27f, size * .31f, size * .73f, size * .69f);
        canvas.drawRoundRect(frame, size * .04f, size * .04f, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(size * .62f, size * .43f, size * .055f, paint);
        Path mountains = new Path();
        mountains.moveTo(size * .31f, size * .64f);
        mountains.lineTo(size * .46f, size * .49f);
        mountains.lineTo(size * .56f, size * .59f);
        mountains.lineTo(size * .64f, size * .51f);
        mountains.lineTo(size * .71f, size * .64f);
        mountains.close();
        canvas.drawPath(mountains, paint);
    }
}
