package org.jpi59.ethicqrscanner;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.util.Linkify;
import android.text.method.LinkMovementMethod;
import android.text.Layout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import android.app.AlertDialog;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

/**
 * A local-only QR and barcode scanner. Decoded text is never sent to a service
 * and web links are never opened without a second, explicit user action.
 */
public final class MainActivity extends androidx.activity.ComponentActivity {
    private static final int PADDING = 24;

    private final ActivityResultLauncher<String> cameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    showScanner();
                } else {
                    showHome();
                }
            });
    private final ActivityResultLauncher<String> photoPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), this::decodePhoto);

    private FrameLayout root;
    private PreviewView previewView;
    private ImageAnalysis analysis;
    private Camera camera;
    private DecodeAnalyzer decoder;
    private TorchButtonView torchButton;
    private PhotoButtonView photoButton;
    private final ExecutorService analysisExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(color(R.color.night));
        getWindow().setNavigationBarColor(color(R.color.night));
        requestCameraForScanning();
    }

    private void showHome() {
        if (analysis != null || camera != null) stopCamera();
        root = new FrameLayout(this);
        root.setBackgroundColor(color(R.color.night));
        root.addView(new AmbientBackground(this));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(PADDING), dp(38), dp(PADDING), dp(PADDING));
        root.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView eyebrow = text("ETHIC  /  LOCAL ONLY", 12, color(R.color.teal));
        eyebrow.setLetterSpacing(0.14f);
        content.addView(eyebrow);

        TextView title = text("QR Scanner", 45, Color.WHITE);
        title.setPadding(0, dp(8), 0, 0);
        content.addView(title);

        TextView subtitle = text("Lee un código. Decide con calma qué hacer después.", 18, color(R.color.mist));
        subtitle.setPadding(0, dp(12), 0, 0);
        content.addView(subtitle);

        View spacer = new View(this);
        content.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1));

        LinearLayout assurance = panel();
        assurance.setOrientation(LinearLayout.VERTICAL);
        assurance.setPadding(dp(20), dp(18), dp(20), dp(18));
        TextView assuranceTitle = text("Privacidad visible", 17, Color.WHITE);
        TextView assuranceBody = text("Sin cuenta. Sin anuncios. Sin red. La cámara solo se activa cuando tocas escanear.", 15, color(R.color.mist));
        assuranceBody.setPadding(0, dp(7), 0, 0);
        assurance.addView(assuranceTitle);
        assurance.addView(assuranceBody);
        content.addView(assurance, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button transparency = button("Transparencia", Color.TRANSPARENT, color(R.color.teal));
        transparency.setOnClickListener(v -> showTransparency());
        LinearLayout.LayoutParams transparencyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        transparencyParams.topMargin = dp(8);
        content.addView(transparency, transparencyParams);

        Button scan = button("Escanear código", color(R.color.teal), color(R.color.ink));
        scan.setOnClickListener(v -> requestCameraForScanning());
        LinearLayout.LayoutParams scanParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(58));
        scanParams.topMargin = dp(18);
        content.addView(scan, scanParams);

        Button photo = button("Cargar foto", Color.TRANSPARENT, color(R.color.teal));
        photo.setOnClickListener(v -> photoPicker.launch("image/*"));
        LinearLayout.LayoutParams photoParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        photoParams.topMargin = dp(6);
        content.addView(photo, photoParams);

        TextView footer = text("Los enlaces nunca se abren automáticamente.", 13, color(R.color.muted));
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(17), 0, 0);
        content.addView(footer);
        setContentView(root);
    }

    private void showTransparency() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.transparency_dialog_title)
                .setMessage(R.string.transparency_dialog_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void decodePhoto(Uri uri) {
        if (uri == null) return;
        Toast.makeText(this, getString(R.string.reading_photo), Toast.LENGTH_SHORT).show();
        analysisExecutor.execute(() -> {
            String decoded = null;
            try {
                Bitmap original = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                if (original != null) {
                    int max = 1600;
                    float scale = Math.min(1f, max / (float) Math.max(original.getWidth(), original.getHeight()));
                    Bitmap bitmap = scale < 1f ? Bitmap.createScaledBitmap(original,
                            Math.round(original.getWidth() * scale), Math.round(original.getHeight() * scale), true) : original;
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    int[] pixels = new int[width * height];
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
                    RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
                    decoded = new MultiFormatReader().decode(
                            new BinaryBitmap(new HybridBinarizer(source))).getText();
                    if (bitmap != original) bitmap.recycle();
                    original.recycle();
                }
            } catch (Exception ignored) {
                // An image without a supported code is reported in the UI below.
            }
            String result = decoded;
            runOnUiThread(() -> {
                if (result == null || result.isEmpty()) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.no_code_title)
                            .setMessage(R.string.no_code_message)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                } else {
                    showResult(result);
                }
            });
        });
    }

    private void requestCameraForScanning() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast.makeText(this, getString(R.string.camera_unavailable), Toast.LENGTH_LONG).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            showScanner();
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private void showScanner() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        previewView = new PreviewView(this);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        root.addView(previewView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(new ScannerOverlayView(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(14), dp(20), dp(14));
        header.setBackgroundColor(0xCC0C1C20);
        TextView brand = text("ETHIC QR SCANNER", 15, Color.WHITE);
        brand.setLetterSpacing(0.1f);
        header.addView(brand, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button close = button("×", Color.TRANSPARENT, Color.WHITE);
        close.setTextSize(30);
        close.setContentDescription("Cerrar cámara");
        close.setPadding(dp(10), 0, dp(10), 0);
        close.setOnClickListener(v -> showHome());
        header.addView(close, new LinearLayout.LayoutParams(dp(52), dp(48)));
        FrameLayout.LayoutParams headerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP);
        root.addView(header, headerParams);

        torchButton = new TorchButtonView(this);
        torchButton.setContentDescription("Encender o apagar la luz de la cámara trasera");
        torchButton.setOnClickListener(v -> toggleTorch());
        FrameLayout.LayoutParams torchParams = new FrameLayout.LayoutParams(
                dp(64), dp(64), Gravity.LEFT | Gravity.TOP);
        root.addView(torchButton, torchParams);
        photoButton = new PhotoButtonView(this);
        photoButton.setContentDescription("Elegir una foto guardada para leer un código");
        photoButton.setOnClickListener(v -> photoPicker.launch("image/*"));
        FrameLayout.LayoutParams photoParams = new FrameLayout.LayoutParams(
                dp(64), dp(64), Gravity.LEFT | Gravity.TOP);
        root.addView(photoButton, photoParams);
        root.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int width = right - left;
            int height = bottom - top;
            float frameSize = Math.min(width * 0.78f, height * 0.48f);
            int frameTop = Math.round((height - frameSize) * 0.45f);
            int groupLeft = (width - dp(144)) / 2;
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) torchButton.getLayoutParams();
            params.topMargin = frameTop + Math.round(frameSize) + dp(16);
            params.leftMargin = groupLeft;
            torchButton.setLayoutParams(params);
            FrameLayout.LayoutParams photoLayout = (FrameLayout.LayoutParams) photoButton.getLayoutParams();
            photoLayout.topMargin = params.topMargin;
            photoLayout.leftMargin = groupLeft + dp(80);
            photoButton.setLayoutParams(photoLayout);
        });

        setContentView(root);
        bindCamera();
    }

    private void bindCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                int rotation = getDisplay() == null ? Surface.ROTATION_0 : getDisplay().getRotation();
                Preview preview = new Preview.Builder().setTargetRotation(rotation).build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                decoder = new DecodeAnalyzer(content -> runOnUiThread(() -> showResult(content)));
                analysis = new ImageAnalysis.Builder()
                        .setTargetRotation(rotation)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                analysis.setAnalyzer(analysisExecutor, decoder);
                provider.unbindAll();
                camera = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
                camera.getCameraInfo().getTorchState().observe(this, state -> {
                    if (torchButton != null) {
                        torchButton.setTorchEnabled(state != null && state == androidx.camera.core.TorchState.ON);
                    }
                });
            } catch (Exception error) {
                showHome();
                Toast.makeText(this, "No fue posible iniciar la cámara.", Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void showResult(@NonNull String content) {
        if (decoder != null) decoder.pause();
        if (camera != null) camera.getCameraControl().enableTorch(false);
        boolean link = isSafeBrowserLink(content);
        String classification = link
                ? "Enlace detectado. Revísalo antes de abrirlo; la aplicación no comprobará su reputación en internet."
                : "Contenido detectado. Puedes copiarlo; no se enviará fuera de este teléfono.";
        String displayed = content.length() > 4000 ? content.substring(0, 4000) + "…" : content;
        LinearLayout result = new LinearLayout(this);
        result.setOrientation(LinearLayout.VERTICAL);
        TextView explanation = text(classification, 15, color(R.color.muted));
        explanation.setPadding(0, 0, 0, dp(12));
        result.addView(explanation);
        TextView value = text(displayed, 17, Color.WHITE);
        value.setTextIsSelectable(true);
        value.setHorizontallyScrolling(false);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            value.setBreakStrategy(Layout.BREAK_STRATEGY_BALANCED);
            value.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NORMAL);
        }
        Linkify.addLinks(value, Linkify.WEB_URLS);
        value.setMovementMethod(LinkMovementMethod.getInstance());
        value.setLinkTextColor(color(R.color.teal));
        ResultScrollView scroll = new ResultScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(value, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        result.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                .setTitle("Contenido detectado")
                .setView(result)
                .setNegativeButton(getString(R.string.scan_again), (d, w) -> resumeScanning())
                .setNeutralButton(getString(R.string.copy), (d, w) -> copy(content));
        if (link) {
            dialog.setPositiveButton(getString(R.string.open_browser), (d, w) -> openInBrowser(content));
        }
        dialog.setOnCancelListener(d -> resumeScanning());
        dialog.show();
    }

    private void resumeScanning() {
        if (decoder != null) decoder.resume();
    }

    private void copy(String content) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Código escaneado", content));
        Toast.makeText(this, "Copiado en este dispositivo.", Toast.LENGTH_SHORT).show();
        resumeScanning();
    }

    private void openInBrowser(String address) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(address)));
        } catch (Exception noHandler) {
            Toast.makeText(this, "No hay un navegador disponible.", Toast.LENGTH_LONG).show();
        }
        resumeScanning();
    }

    private boolean isSafeBrowserLink(String value) {
        Uri uri = Uri.parse(value.trim());
        String scheme = uri.getScheme();
        return ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                && uri.getHost() != null;
    }

    private void toggleTorch() {
        if (camera == null || !camera.getCameraInfo().hasFlashUnit()) {
            Toast.makeText(this, "Esta cámara no tiene luz auxiliar.", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean enabled = camera.getCameraInfo().getTorchState().getValue() != null
                && camera.getCameraInfo().getTorchState().getValue() == androidx.camera.core.TorchState.ON;
        camera.getCameraControl().enableTorch(!enabled);
    }

    private void stopCamera() {
        if (analysis != null) analysis.clearAnalyzer();
        ProcessCameraProvider.getInstance(this).addListener(() -> {
            try { ProcessCameraProvider.getInstance(this).get().unbindAll(); } catch (Exception ignored) { }
        }, ContextCompat.getMainExecutor(this));
        analysis = null;
        camera = null;
        decoder = null;
    }

    @Override
    protected void onDestroy() {
        stopCamera();
        analysisExecutor.shutdown();
        super.onDestroy();
    }

    private LinearLayout panel() {
        LinearLayout panel = new LinearLayout(this);
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xE6153136);
        background.setCornerRadius(dp(24));
        background.setStroke(dp(1), 0x4D8DE5D0);
        panel.setBackground(background);
        return panel;
    }

    private Button button(String label, int backgroundColor, int textColor) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextColor(textColor);
        button.setTextSize(16);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(backgroundColor);
        shape.setCornerRadius(dp(18));
        button.setBackground(shape);
        return button;
    }

    private TextView text(String value, int size, int textColor) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(textColor);
        view.setTextSize(size);
        view.setIncludeFontPadding(false);
        return view;
    }

    private int color(int resource) { return ContextCompat.getColor(this, resource); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
