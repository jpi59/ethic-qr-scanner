package org.jpi59.ethicqrscanner;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Decodes the luminance plane locally; no image is written to storage or transmitted. */
final class DecodeAnalyzer implements ImageAnalysis.Analyzer {
    interface Callback { void onDecoded(@NonNull String content); }

    private final Callback callback;
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);

    DecodeAnalyzer(Callback callback) {
        this.callback = callback;
        hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(
                BarcodeFormat.QR_CODE, BarcodeFormat.AZTEC, BarcodeFormat.DATA_MATRIX,
                BarcodeFormat.PDF_417, BarcodeFormat.CODE_128, BarcodeFormat.CODE_39,
                BarcodeFormat.CODE_93, BarcodeFormat.EAN_13, BarcodeFormat.EAN_8,
                BarcodeFormat.UPC_A, BarcodeFormat.UPC_E, BarcodeFormat.ITF));
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    }

    void pause() { paused.set(true); }
    void resume() { paused.set(false); }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        if (paused.get()) {
            image.close();
            return;
        }
        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] luma = new byte[buffer.remaining()];
            buffer.get(luma);
            int rowStride = image.getPlanes()[0].getRowStride();
            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                    luma, rowStride, image.getHeight(), 0, 0,
                    image.getWidth(), image.getHeight(), false);
            Result result = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(source)), hints);
            if (result != null && result.getText() != null && paused.compareAndSet(false, true)) {
                callback.onDecoded(result.getText());
            }
        } catch (Exception ignored) {
            // Most camera frames do not contain a code. This is expected.
        } finally {
            image.close();
        }
    }
}
