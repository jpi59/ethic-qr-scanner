package org.jpi59.ethicqrscanner;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/** Keeps very long QR payloads readable without allowing the result dialog to overflow. */
final class ResultScrollView extends ScrollView {
    ResultScrollView(Context context) { super(context); }
    ResultScrollView(Context context, AttributeSet attrs) { super(context, attrs); }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int maxHeight = Math.round(260 * getResources().getDisplayMetrics().density);
        if (getMeasuredHeight() > maxHeight) {
            setMeasuredDimension(getMeasuredWidth(), maxHeight);
        }
    }
}
