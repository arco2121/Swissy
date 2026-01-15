package com.arco2121.swissy.Tools.ScannerEye;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.Image;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.arco2121.swissy.Tools.ToolListener;
import com.arco2121.swissy.Tools.ToolStructure;
import com.arco2121.swissy.Utility.LogPrinter;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.objects.DetectedObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScannerEye implements ToolStructure {
    private ScannerEyeListener listener;
    private ExecutorService cameraExecutor;
    public final Preview preview;
    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;
    private BarcodeScanner barcodeScanner;
    private ObjectDetector objectDetector;
    public boolean scannerMode = true;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private boolean isStarted = false;

    public ScannerEye(Context c) {
        preview = new Preview.Builder().build();
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(c);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                LogPrinter.printToast(c, "Error initializing camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(c));
    }

    @Override
    public void startSensors(Context c) {
        if (!(c instanceof LifecycleOwner)) {
            LogPrinter.printToast(c, "Context must be LifecycleOwner");
            return;
        }

        if (cameraProvider == null) {
            LogPrinter.printToast(c, "Camera not ready, please wait");
            return;
        }

        if (isStarted) {
            return; // Already started
        }

        try {
            // Unbind all use cases before rebinding
            cameraProvider.unbindAll();

            // Create executor if needed
            if (cameraExecutor == null || cameraExecutor.isShutdown()) {
                cameraExecutor = Executors.newSingleThreadExecutor();
            }

            // Initialize scanners
            if (barcodeScanner == null) {
                barcodeScanner = BarcodeScanning.getClient();
            }

            if (objectDetector == null) {
                ObjectDetectorOptions options = new ObjectDetectorOptions.Builder()
                        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                        .enableClassification()
                        .build();
                objectDetector = ObjectDetection.getClient(options);
            }

            // Build ImageAnalysis with proper configuration
            imageAnalysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();

            imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                // Prevent concurrent processing
                if (!isProcessing.compareAndSet(false, true)) {
                    imageProxy.close();
                    return;
                }

                try {
                    if (scannerMode) {
                        analyzeCode(imageProxy);
                    } else {
                        analyzeObject(imageProxy);
                    }
                } finally {
                    isProcessing.set(false);
                }
            });

            // Bind use cases to lifecycle
            cameraProvider.bindToLifecycle(
                    (LifecycleOwner) c,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
            );

            isStarted = true;

        } catch (Exception e) {
            LogPrinter.printToast(c, "Camera error: " + e.getMessage());
            isStarted = false;
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyzeCode(ImageProxy imageProxy) {
        try {
            Image mediaImage = imageProxy.getImage();
            if (mediaImage == null) {
                imageProxy.close();
                return;
            }

            InputImage image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            barcodeScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            if (listener != null && barcode.getRawValue() != null) {
                                listener.onScanCode(barcode.getRawValue());
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Silent fail for analysis errors
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        } catch (Exception e) {
            imageProxy.close();
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyzeObject(ImageProxy imageProxy) {
        try {
            Image mediaImage = imageProxy.getImage();
            if (mediaImage == null) {
                imageProxy.close();
                return;
            }

            InputImage image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            objectDetector.process(image)
                    .addOnSuccessListener(results -> {
                        for (DetectedObject object : results) {
                            for (DetectedObject.Label label : object.getLabels()) {
                                if (listener != null) {
                                    listener.onScanObject(
                                            label.getText(),
                                            label.getConfidence() * 100
                                    );
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Silent fail for analysis errors
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        } catch (Exception e) {
            imageProxy.close();
        }
    }

    public void changeAnalisis(Context c) {
        this.scannerMode = !this.scannerMode;
        // Restart sensors to apply new mode
        stopSensors();
        startSensors(c);
    }

    @Override
    public void stopSensors() {
        isStarted = false;
        isProcessing.set(false);

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        if (imageAnalysis != null) {
            imageAnalysis.clearAnalyzer();
            imageAnalysis = null;
        }

        if (cameraExecutor != null && !cameraExecutor.isShutdown()) {
            cameraExecutor.shutdown();
            cameraExecutor = null;
        }

        if (barcodeScanner != null) {
            barcodeScanner.close();
            barcodeScanner = null;
        }

        if (objectDetector != null) {
            objectDetector.close();
            objectDetector = null;
        }
    }

    @Override
    public void setListener(ToolListener listener) {
        this.listener = (ScannerEyeListener) listener;
    }
}