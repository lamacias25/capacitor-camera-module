import Capacitor
import AVFoundation
import Vision
import UIKit

@objc(CameraModule)
public class CameraModulePlugin: CAPPlugin, CAPBridgedPlugin, AVCaptureVideoDataOutputSampleBufferDelegate {

    public let identifier = "CameraModule"
    public let jsName = "CameraModule"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "checkPermission", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestPermission", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startPreview", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopPreview", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "toggleFlash", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "hasFlash", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "takePhotoBase64", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startBarcodeScan", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopBarcodeScan", returnType: CAPPluginReturnPromise)
    ]

    private var previewView: UIView?
    private var captureSession: AVCaptureSession?
    private var videoPreviewLayer: AVCaptureVideoPreviewLayer?
    private let photoOutput = AVCapturePhotoOutput()
    private var photoDelegate: PhotoDelegate?

    // MARK: - Barcode

    private var isScanning = false
    private var scanCall: CAPPluginCall?
    private var barcodeRequest: VNDetectBarcodesRequest?
    private var videoDataOutput: AVCaptureVideoDataOutput?
    private var lastScannedValue: String?
    private var lastScanTime: Date?
    private let scanDebounceInterval: TimeInterval = 0.5


    // MARK: - Lifecycle
    public override func load() {
        print("📱 CameraModulePlugin cargado")
    }

    // MARK: - Camera Permission

    @objc func checkPermission(_ call: CAPPluginCall) {
        let status = AVCaptureDevice.authorizationStatus(for: .video)
        call.resolve([
            "granted": status == .authorized,
            "status": status == .authorized ? "granted" : "prompt"
        ])
    }

    @objc func requestPermission(_ call: CAPPluginCall) {
        AVCaptureDevice.requestAccess(for: .video) { granted in
            DispatchQueue.main.async {
                call.resolve([
                    "granted": granted,
                    "status": granted ? "granted" : "denied"
                ])
            }
        }
    }


    // MARK: - Camera Preview

    @objc func startPreview(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            if self.previewView != nil {
                call.resolve()
                return
            }

            guard AVCaptureDevice.authorizationStatus(for: .video) == .authorized else {
                call.reject("Camera permission not granted")
                return
            }

            let session = AVCaptureSession()

            guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
                  let input = try? AVCaptureDeviceInput(device: device) else {
                call.reject("Camera not available")
                return
            }

            if session.canAddInput(input) {
                session.addInput(input)
            }

            if session.canAddOutput(self.photoOutput) {
                session.addOutput(self.photoOutput)
            }

            guard let container = self.bridge?.viewController?.view else {
                call.reject("No container view")
                return
            }

            guard let webView = self.bridge?.webView as? WKWebView else {
                call.reject("No webView")
                return
            }

            // 1. Hacer el webView transparente
            webView.isOpaque = false
            webView.backgroundColor = .clear
            webView.scrollView.backgroundColor = .clear
            webView.scrollView.isOpaque = false

            // 2. Crear previewView
            let previewView = UIView()
            previewView.translatesAutoresizingMaskIntoConstraints = false
            previewView.backgroundColor = .clear // Transparente desde el inicio
            previewView.isUserInteractionEnabled = false // No intercepta toques

            // 3. Insertar previewView DETRÁS de todo
            container.insertSubview(previewView, at: 0)

            // 4. Configurar constraints para toda la pantalla
            NSLayoutConstraint.activate([
                previewView.topAnchor.constraint(equalTo: container.topAnchor),
                previewView.bottomAnchor.constraint(equalTo: container.bottomAnchor),
                previewView.leadingAnchor.constraint(equalTo: container.leadingAnchor),
                previewView.trailingAnchor.constraint(equalTo: container.trailingAnchor)
            ])

            // 5. Asegurar orden de capas
            previewView.layer.zPosition = -1 // Detrás de todo
            webView.layer.zPosition = 0 // Normal

            // 6. Forzar layout
            container.layoutIfNeeded()

            // 7. Crear previewLayer
            let previewLayer = AVCaptureVideoPreviewLayer(session: session)
            previewLayer.videoGravity = .resizeAspectFill
            previewLayer.frame = previewView.bounds
            previewLayer.masksToBounds = true

            previewView.layer.insertSublayer(previewLayer, at: 0)

            // 8. Guardar referencias
            self.previewView = previewView
            self.videoPreviewLayer = previewLayer
            self.captureSession = session

            // 9. Configurar sesión
            session.sessionPreset = .photo

            // 10. Iniciar sesión en background
            DispatchQueue.global(qos: .userInitiated).async {
                session.startRunning()

                DispatchQueue.main.async {
                    print("Cámara iniciada correctamente")
                    print("Preview visible: \(previewView.window != nil ? "SÍ" : "NO")")
                }
            }

            call.resolve([
                "success": true,
                "message": "Preview de cámara iniciado"
            ])
        }
    }

    @objc func stopPreview(_ call: CAPPluginCall) {
        DispatchQueue.main.async {

            if self.isScanning {
                self.isScanning = false
                self.scanCall?.keepAlive = false
                self.scanCall = nil
                self.barcodeRequest = nil
                self.lastScannedValue = nil
                self.lastScanTime = nil

                if let output = self.videoDataOutput,
                   let session = self.captureSession {
                    session.removeOutput(output)
                }
                self.videoDataOutput = nil
            }

            // 🛑 Detener cámara
            self.captureSession?.stopRunning()
            self.captureSession = nil

            // 🧹 Limpiar preview
            self.videoPreviewLayer?.removeFromSuperlayer()
            self.previewView?.removeFromSuperview()
            self.videoPreviewLayer?.session = nil
            self.videoPreviewLayer = nil
            self.previewView = nil

            // 🔁 Restaurar WebView
            if let webView = self.bridge?.webView as? WKWebView {
                webView.isOpaque = true
                webView.backgroundColor = .white
                webView.scrollView.backgroundColor = .white
                webView.scrollView.isOpaque = true
            }

            print("Preview y escaneo detenidos correctamente")
            call.resolve()
        }
    }

    // MARK: - Flash

    @objc func toggleFlash(_ call: CAPPluginCall) {
        guard let enable = call.getBool("enable"),
              let device = AVCaptureDevice.default(for: .video),
              device.hasTorch else {
            call.reject("Flash not available")
            return
        }

        do {
            try device.lockForConfiguration()
            device.torchMode = enable ? .on : .off
            device.unlockForConfiguration()
            call.resolve()
        } catch {
            call.reject("Flash error")
        }
    }

    @objc func hasFlash(_ call: CAPPluginCall) {
        call.resolve([
            "hasFlash": AVCaptureDevice.default(for: .video)?.hasTorch ?? false
        ])
    }


    // MARK: - Photo Capture

    @objc func takePhotoBase64(_ call: CAPPluginCall) {
        guard captureSession?.isRunning == true else {
            call.reject("Camera not started")
            return
        }

        let settings = AVCapturePhotoSettings()
        settings.flashMode = .off

        photoDelegate = PhotoDelegate { [weak self] base64 in
            call.resolve([
                "base64": base64,
                "mimeType": "image/jpeg"
            ])
            self?.photoDelegate = nil
        }

        photoOutput.capturePhoto(with: settings, delegate: photoDelegate!)
    }

    // MARK: - Barcode Scan

    @objc func startBarcodeScan(_ call: CAPPluginCall) {
        guard let session = captureSession else {
            call.reject("Preview not started")
            return
        }

        if isScanning {
            call.reject("Already scanning")
            return
        }

        isScanning = true
        scanCall = call
        call.keepAlive = true

        barcodeRequest = VNDetectBarcodesRequest { request, _ in
            guard let barcode = (request.results as? [VNBarcodeObservation])?.first,
                  let value = barcode.payloadStringValue else { return }

            let now = Date()
            if self.lastScannedValue == value,
               let lastTime = self.lastScanTime,
               now.timeIntervalSince(lastTime) < self.scanDebounceInterval {
                return
            }

            self.lastScannedValue = value
            self.lastScanTime = now
            self.isScanning = false

            if let output = self.videoDataOutput,
               let session = self.captureSession {
                session.removeOutput(output)
            }
            self.videoDataOutput = nil

            self.scanCall?.resolve([
                "rawValue": value,
                "format": barcode.symbology.rawValue
            ])
            self.scanCall?.keepAlive = false
            self.scanCall = nil

        }

        if videoDataOutput == nil {
            let output = AVCaptureVideoDataOutput()
            output.videoSettings = [
                kCVPixelBufferPixelFormatTypeKey as String:
                    kCVPixelFormatType_32BGRA
            ]
            output.alwaysDiscardsLateVideoFrames = true
            output.setSampleBufferDelegate(self, queue: DispatchQueue(label: "barcode.queue"))
            if session.canAddOutput(output) {
                session.addOutput(output)
            }
            videoDataOutput = output
        }
    }

    @objc func stopBarcodeScan(_ call: CAPPluginCall) {
        isScanning = false
        if let output = videoDataOutput, let session = captureSession {
            session.removeOutput(output)
        }
        videoDataOutput = nil
        scanCall?.keepAlive = false
        scanCall = nil
        barcodeRequest = nil
        lastScannedValue = nil
        lastScanTime = nil
        call.resolve()
    }

    public func captureOutput(_ output: AVCaptureOutput,
                              didOutput sampleBuffer: CMSampleBuffer,
                              from connection: AVCaptureConnection) {

        guard isScanning,
              captureSession?.isRunning == true,
              let request = barcodeRequest,
              let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer)
        else { return }

        let handler = VNImageRequestHandler(
            cvPixelBuffer: pixelBuffer,
            orientation: .right,
            options: [:]
        )

        try? handler.perform([request])
    }

    // MARK: - PhotoDelegate Class

    private class PhotoDelegate: NSObject, AVCapturePhotoCaptureDelegate {
        private let completion: (String) -> Void

        init(completion: @escaping (String) -> Void) {
            self.completion = completion
        }

        func photoOutput(_ output: AVCapturePhotoOutput,
                        didFinishProcessingPhoto photo: AVCapturePhoto,
                        error: Error?) {
            if let error = error {
                print("Error capturando foto: \(error)")
                return
            }

            guard let data = photo.fileDataRepresentation(),
                  let base64 = data.base64EncodedString() as? String else {
                print("No se pudo convertir la imagen a base64")
                return
            }

            completion(base64)
        }
    }
}


// MARK: - UIImage Resize
extension UIImage {
    func resized(maxSize: CGFloat) -> UIImage? {
        let maxSide = max(size.width, size.height)
        if maxSide <= maxSize { return self }

        let scale = maxSize / maxSide
        let newSize = CGSize(width: size.width * scale, height: size.height * scale)

        UIGraphicsBeginImageContextWithOptions(newSize, false, 0)
        draw(in: CGRect(origin: .zero, size: newSize))
        let img = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return img
    }
}