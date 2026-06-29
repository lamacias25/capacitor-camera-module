export interface CameraModulePlugin {
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
    checkPermission(): Promise<PermissionStatus>;
    requestPermission(): Promise<PermissionStatus>;
    checkAndRequestPermission(): Promise<PermissionStatus>;
    getCameraCapabilities(): Promise<CameraCapabilities>;
    startPreview(): Promise<void>;
    stopPreview(): Promise<void>;
    toggleFlash(enable: boolean): Promise<void>;
    hasFlash(): Promise<boolean>;
    checkGalleryPermission(): Promise<PermissionResult>;
    requestGalleryPermission(): Promise<PermissionResult>;
    checkAndRequestGalleryPermission(): Promise<PermissionResult>;
    pickImageBase64(): Promise<PickImageBase64Result>;
    getLastGalleryImage(): Promise<LastGalleryImageResult>;
    takePhotoBase64(): Promise<takephotoBase64Result>;
    startBarcodeScan(): Promise<startBarcodeScanResult>;
    stopBarcodeScan(): Promise<void>;
}
export interface PermissionStatus {
    granted: boolean;
    status: 'granted' | 'denied' | 'prompt' | 'prompt-with-rationale' | 'limited';
    details?: string;
}
export interface CameraCapabilities {
    hasCamera: boolean;
    isSecureContext: boolean;
    userAgent: string;
}
export interface PermissionResult {
    granted: boolean;
    status: 'granted' | 'denied' | 'prompt';
    details?: string;
}
export interface PickImageBase64Result {
    base64: string;
    mimeType?: string;
}
export interface LastGalleryImageResult {
    base64: string;
}
export interface startBarcodeScanResult {
    rawValue: string;
    format: number;
}
export interface takephotoBase64Result {
    base64: string;
    mimeType: string;
}
