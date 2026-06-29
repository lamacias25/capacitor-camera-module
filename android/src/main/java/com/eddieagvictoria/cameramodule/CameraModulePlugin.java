package com.eddieagvictoria.cameramodule;

import android.Manifest;
import android.os.Build;
import android.content.pm.PackageManager;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.PermissionState;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.core.content.ContextCompat;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;

import com.google.common.util.concurrent.ListenableFuture;

import androidx.camera.core.Camera;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.database.Cursor;
import android.content.ContentUris;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;

import androidx.camera.core.ImageProxy;
import android.graphics.ImageFormat;
import android.media.Image;

import java.nio.ByteBuffer;
import androidx.annotation.NonNull;

import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.camera.core.ImageAnalysis;

import android.content.Context;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;

import android.graphics.YuvImage;
import android.graphics.Rect;
import java.util.concurrent.Executors;

import java.util.concurrent.ExecutorService;
import java.io.File;

import androidx.exifinterface.media.ExifInterface;
import java.io.FileInputStream;

@CapacitorPlugin(
    name = "CameraModule",
    permissions = {
        @Permission(
            alias = "camera",
            strings = { Manifest.permission.CAMERA }
        )
    }
)

public class CameraModulePlugin extends Plugin {

    private PreviewView previewView;
    private ProcessCameraProvider cameraProvider;

    private Camera camera;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private PluginCall savedCall;

    private ImageCapture imageCapture;

    private ImageAnalysis imageAnalysis;
    private boolean isScanning = false;
    private PluginCall scanCall;

    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

    private BarcodeScanner barcodeScanner;

    private final ExecutorService barcodeExecutor = Executors.newSingleThreadExecutor();



    @Override
    public void load() {
        super.load();
        getBridge().getWebView().setBackgroundColor(0x00000000);

        galleryLauncher =
                bridge.registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (savedCall == null) return;

                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();

                            try {
                                String base64 = uriToBase64(uri);
                                String mimeType = getContext()
                                    .getContentResolver()
                                    .getType(uri);

                                JSObject ret = new JSObject();
                                ret.put("base64", base64);
                                ret.put("mimeType", mimeType);

                                savedCall.resolve(ret);
                            } catch (Exception e) {
                                savedCall.reject("Error reading image", e);
                            }
                        } else {
                            savedCall.reject("Image selection canceled");
                        }

                        savedCall = null;
                    }
                );
    }

    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value", "");
        JSObject ret = new JSObject();
        ret.put("value", value);
        call.resolve(ret);
    }

    @PluginMethod
    public void checkPermission(PluginCall call) {
        JSObject ret = new JSObject();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            ret.put("granted", true);
            ret.put("status", "granted");
            ret.put("details", "Pre-Marshmallow, granted at install");
            call.resolve(ret);
            return;
        }

        boolean granted =
            ContextCompat.checkSelfPermission(
                getContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED;

        ret.put("granted", granted);
        ret.put("status", granted ? "granted" : "prompt");
        ret.put("details", "Android SDK " + Build.VERSION.SDK_INT);

        call.resolve(ret);
    }

    @PluginMethod
    public void requestPermission(PluginCall call) {
        if (getPermissionState("camera") != PermissionState.GRANTED) {
            requestPermissionForAlias("camera", call, "cameraPermissionCallback");
        } else {
            JSObject ret = new JSObject();
            ret.put("granted", true);
            ret.put("status", "granted");
            ret.put("details", "Already granted");
            call.resolve(ret);
        }
    }

    @PermissionCallback
    private void cameraPermissionCallback(PluginCall call) {
        JSObject ret = new JSObject();
        boolean granted = getPermissionState("camera") == PermissionState.GRANTED;

        ret.put("granted", granted);
        ret.put("status", granted ? "granted" : "denied");
        ret.put("details", "Permission request completed");

        call.resolve(ret);
    }

    @PluginMethod
    public void checkAndRequestPermission(PluginCall call) {
        if (getPermissionState("camera") == PermissionState.GRANTED) {
            JSObject ret = new JSObject();
            ret.put("granted", true);
            ret.put("status", "granted");
            ret.put("details", "Already granted");
            call.resolve(ret);
        } else {
            requestPermission(call);
        }
    }

    @PluginMethod
    public void getCameraCapabilities(PluginCall call) {
        JSObject ret = new JSObject();

        boolean hasCamera = getContext()
            .getPackageManager()
            .hasSystemFeature("android.hardware.camera.any");

        ret.put("hasCamera", hasCamera);
        ret.put("isSecureContext", true);
        ret.put("userAgent", "Android");

        call.resolve(ret);
    }


    //Camera
    @PluginMethod
    public void startPreview(PluginCall call) {
        getActivity().runOnUiThread(() -> {

            if (previewView != null) {
                call.resolve();
                return;
            }

            previewView = new PreviewView(getContext());
            previewView.setLayoutParams(
                new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            );

            ViewGroup rootView = (ViewGroup) getActivity().getWindow().getDecorView();
            rootView.addView(previewView, 0);

            startCamera();

            call.resolve();
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
            ProcessCameraProvider.getInstance(getContext());

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture =
                    new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();



                cameraProvider.unbindAll();

                camera = cameraProvider.bindToLifecycle(
                    getActivity(),
                    cameraSelector,
                    preview,
                    imageCapture
                );

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }


    @PluginMethod
    public void stopPreview(PluginCall call) {
        getActivity().runOnUiThread(() -> {

            if (camera != null) {
                    camera.getCameraControl().enableTorch(false);
                    camera = null;
            }

            if (cameraProvider != null) {
                cameraProvider.unbindAll();
            }

            if (previewView != null) {
                ViewGroup rootView = (ViewGroup) previewView.getParent();
                rootView.removeView(previewView);
                previewView = null;
            }

            if (barcodeScanner != null) {
                barcodeScanner.close();
                barcodeScanner = null;
            }

            call.resolve();
        });
    }

    @PluginMethod
    public void toggleFlash(PluginCall call) {
        Boolean enable = call.getBoolean("enable");

        if (enable == null) {
            call.reject("enable parameter is required");
            return;
        }

        if (camera == null) {
            call.reject("Camera not initialized");
            return;
        }

        camera.getCameraControl().enableTorch(enable);
        call.resolve();
    }

    @PluginMethod
    public void hasFlash(PluginCall call) {
        boolean hasFlash = getContext()
            .getPackageManager()
            .hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FLASH
            );

        JSObject ret = new JSObject();
        ret.put("hasFlash", hasFlash);
        call.resolve(ret);
    }


    @PluginMethod
    public void pickImageBase64(PluginCall call) {

        String alias =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? "gallery_13"
                : "gallery_pre13";

        if (getPermissionState(alias) != PermissionState.GRANTED) {
            call.reject("Gallery permission not granted");
            return;
        }

        if (savedCall != null) {
            call.reject("Gallery already open");
            return;
        }
        savedCall = call;

        Intent intent = new Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        );
        intent.setType("image/*");

        galleryLauncher.launch(intent);
    }



    private String uriToBase64(Uri uri) throws Exception {
        InputStream inputStream =
            getContext().getContentResolver().openInputStream(uri);

        if (inputStream == null) {
            throw new Exception("Unable to open input stream");
        }

        Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
        inputStream.close();

        if (originalBitmap == null) {
            throw new Exception("Unable to decode bitmap");
        }

        Bitmap resizedBitmap = resizeBitmap(originalBitmap, 1024);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);

        byte[] imageBytes = outputStream.toByteArray();

        originalBitmap.recycle();
        resizedBitmap.recycle();

        return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
    }


    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float ratio = (float) width / height;

        if (width > height) {
            if (width <= maxSize && height <= maxSize) {
                Bitmap.Config config =
                    bitmap.getConfig() != null ? bitmap.getConfig() : Bitmap.Config.ARGB_8888;
                return bitmap.copy(config, false);
            }
            width = maxSize;
            height = Math.round(width / ratio);
        } else {
            if (height <= maxSize && width <= maxSize) {
                Bitmap.Config config =
                    bitmap.getConfig() != null ? bitmap.getConfig() : Bitmap.Config.ARGB_8888;
                return bitmap.copy(config, false);
            }
            height = maxSize;
            width = Math.round(height * ratio);
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    //Plugin request permission gallery
    @PluginMethod
    public void checkGalleryPermission(PluginCall call) {
        JSObject ret = new JSObject();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            ret.put("granted", true);
            ret.put("status", "granted");
            ret.put("details", "Pre-Marshmallow, granted at install");
            call.resolve(ret);
            return;
        }

        boolean granted;

        granted = true;

        ret.put("granted", granted);
        ret.put("status", granted ? "granted" : "prompt");
        ret.put("details", "Android SDK " + Build.VERSION.SDK_INT);

        call.resolve(ret);
    }

    @PluginMethod
    public void requestGalleryPermission(PluginCall call) {
        String alias = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? "gallery_13" : "gallery_pre13";

        if (getPermissionState(alias) != PermissionState.GRANTED) {
            requestPermissionForAlias(alias, call, "galleryPermissionCallback");
        } else {
            JSObject ret = new JSObject();
            ret.put("granted", true);
            ret.put("status", "granted");
            ret.put("details", "Already granted");
            call.resolve(ret);
        }
    }

    @PermissionCallback
    private void galleryPermissionCallback(PluginCall call) {
        boolean granted;
        String alias = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? "gallery_13" : "gallery_pre13";
        granted = getPermissionState(alias) == PermissionState.GRANTED;

        JSObject ret = new JSObject();
        ret.put("granted", granted);
        ret.put("status", granted ? "granted" : "denied");
        ret.put("details", "Permission request completed");
        call.resolve(ret);
    }



    @PluginMethod
    public void checkAndRequestGalleryPermission(PluginCall call) {
        String alias =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? "gallery_13"
                : "gallery_pre13";

        if (getPermissionState(alias) == PermissionState.GRANTED) {
            JSObject ret = new JSObject();
            ret.put("granted", true);
            ret.put("status", "granted");
            ret.put("details", "Already granted");
            call.resolve(ret);
        } else {
            requestGalleryPermission(call);
        }
    }

    @PluginMethod
    public void getLastGalleryImage(PluginCall call) {

        String alias = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? "gallery_13" : "gallery_pre13";

        if (getPermissionState(alias) != PermissionState.GRANTED) {
            call.reject("Gallery permission not granted");
            return;
        }

        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = { MediaStore.Images.Media._ID };
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = getContext().getContentResolver().query(
                uri,
                projection,
                null,
                null,
                sortOrder
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                Uri imageUri = ContentUris.withAppendedId(uri, id);

                String base64 = uriToBase64(imageUri);

                JSObject ret = new JSObject();
                ret.put("base64", base64);
                call.resolve(ret);
            } else {
                call.reject("No images found");
            }
        } catch (Exception e) {
            call.reject("Error fetching last image", e);
        }
    }


    @PluginMethod
    public void takePhotoBase64(PluginCall call) {

        if (imageCapture == null) {
            call.reject("Camera not initialized");
            return;
        }

        try {
            File photoFile = File.createTempFile(
                "photo_",
                ".jpg",
                getContext().getCacheDir()
            );

            ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

            imageCapture.takePicture(
                options,
                ContextCompat.getMainExecutor(getContext()),
                new ImageCapture.OnImageSavedCallback() {

                    @Override
                    public void onImageSaved(
                        @NonNull ImageCapture.OutputFileResults output
                    ) {
                        try {
                            Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                            bitmap = rotateBitmapIfRequired(bitmap, photoFile);
                            Bitmap resized = resizeBitmap(bitmap, 1024);

                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            resized.compress(Bitmap.CompressFormat.JPEG, 80, out);

                            String base64 = Base64.encodeToString(
                                out.toByteArray(),
                                Base64.NO_WRAP
                            );

                            bitmap.recycle();
                            resized.recycle();
                            photoFile.delete();

                            JSObject ret = new JSObject();
                            ret.put("base64", base64);
                            ret.put("mimeType", "image/jpeg");
                            call.resolve(ret);

                        } catch (Exception e) {
                            call.reject("Error processing image", e);
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        call.reject("Capture failed", exception);
                    }
                }
            );

        } catch (Exception e) {
            call.reject("Error creating file", e);
        }
    }


    @PluginMethod
    public void startBarcodeScan(PluginCall call) {

        getActivity().runOnUiThread(() -> {

            if (previewView == null) {
                call.reject("Preview not started");
                return;
            }

            if (getPermissionState("camera") != PermissionState.GRANTED) {
                call.reject("Camera permission not granted");
                return;
            }

            if (cameraProvider == null) {
                call.reject("Camera not initialized");
                return;
            }

            if (isScanning) {
                call.reject("Already scanning");
                return;
            }

            barcodeScanner = BarcodeScanning.getClient();

            call.setKeepAlive(true);
            scanCall = call;
            isScanning = true;

            imageAnalysis =
                new ImageAnalysis.Builder()
                    .setBackpressureStrategy(
                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    )
                    .build();

            imageAnalysis.setAnalyzer(
                barcodeExecutor,
                this::processBarcode
            );

            cameraProvider.unbindAll();

            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());

            imageCapture =
                new ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build();

            camera = cameraProvider.bindToLifecycle(
                getActivity(),
                cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
            );

        });
    }



    private void processBarcode(ImageProxy imageProxy) {

        if (!isScanning) {
            imageProxy.close();
            return;
        }

        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.getImageInfo().getRotationDegrees()
        );

        if (barcodeScanner == null) {
            imageProxy.close();
            return;
        }
        barcodeScanner.process(image)
            .addOnSuccessListener(barcodes -> {

                if (!isScanning) {
                    imageProxy.close();
                    return;
                }

                if (!barcodes.isEmpty()) {

                    isScanning = false;
                    vibrateOnce();

                    Barcode barcode = barcodes.get(0);

                    JSObject ret = new JSObject();
                    ret.put("rawValue", barcode.getRawValue());
                    ret.put("format", barcode.getFormat());

                    if (scanCall != null) {
                        scanCall.resolve(ret);
                        scanCall.setKeepAlive(false);
                        scanCall = null;

                    }


                    if (barcodeScanner != null) {
                            barcodeScanner.close();
                            barcodeScanner = null;
                        }


                    stopBarcodeScanInternal();
                }

                imageProxy.close();
            })
            .addOnFailureListener(e -> {
                imageProxy.close();
            });
    }


    private void stopBarcodeScanInternal() {
        getActivity().runOnUiThread(() -> {
            if (cameraProvider != null) {
                cameraProvider.unbindAll();
                startCamera();
            }
        });
    }



    @PluginMethod
    public void stopBarcodeScan(PluginCall call) {
        isScanning = false;
        imageAnalysis = null;

        if (scanCall != null) {
            scanCall.setKeepAlive(false);
            scanCall = null;
        }

        if (barcodeScanner != null) {
            barcodeScanner.close();
            barcodeScanner = null;
        }

        stopBarcodeScanInternal();

        call.resolve();
    }



    private void vibrateOnce() {
        Vibrator vibrator =
            (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);

        if (vibrator == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    120,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            );
        } else {
            vibrator.vibrate(120);
        }
    }

    private void restartNormalPreview() {
        if (cameraProvider == null) return;

        cameraProvider.unbindAll();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build();

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        cameraProvider.bindToLifecycle(
            getActivity(),
            cameraSelector,
            preview,
            imageCapture
        );
    }

    private Bitmap rotateBitmapIfRequired(Bitmap bitmap, File photoFile) throws Exception {

        ExifInterface exif = new ExifInterface(photoFile.getAbsolutePath());

        int orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        );

        Matrix matrix = new Matrix();

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return bitmap;
        }

        Bitmap rotated = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.getWidth(),
            bitmap.getHeight(),
            matrix,
            true
        );

        bitmap.recycle();
        return rotated;
    }




}
