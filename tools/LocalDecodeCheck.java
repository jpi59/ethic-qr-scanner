import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.qrcode.QRCodeWriter;

/** Small host-side check for the exact local ZXing dependency declared by the app. */
public final class LocalDecodeCheck {
    public static void main(String[] args) throws Exception {
        String expected = "https://example.org/ethic-qr-scanner-test";
        BitMatrix matrix = new QRCodeWriter().encode(expected, BarcodeFormat.QR_CODE, 300, 300);
        byte[] pixels = new byte[300 * 300];
        for (int y = 0; y < 300; y++) {
            for (int x = 0; x < 300; x++) {
                pixels[y * 300 + x] = (byte) (matrix.get(x, y) ? 0 : 0xFF);
            }
        }
        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                pixels, 300, 300, 0, 0, 300, 300, false);
        Result decoded = new MultiFormatReader().decode(
                new BinaryBitmap(new HybridBinarizer(source)));
        if (!expected.equals(decoded.getText())) {
            throw new AssertionError("Decoded content differs from generated QR content");
        }
        System.out.println("ZXing local encode/decode check passed.");
    }
}
