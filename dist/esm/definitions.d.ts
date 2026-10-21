export interface CameraModulePlugin {
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
    checkPermission(): Promise<{
        granted: boolean;
        status: string;
        details: string;
    }>;
    requestPermission(): Promise<{
        granted: boolean;
        status: string;
        details: string;
    }>;
    startPreview(): Promise<void>;
    stopPreview(): Promise<void>;
    toggleFlash(options: {
        enable: boolean;
    }): Promise<void>;
    hasFlash(): Promise<{
        hasFlash: boolean;
    }>;
    takePhotoBase64(): Promise<{
        base64: string;
        mimeType: string;
    }>;
    startBarcodeScan(): Promise<{
        rawValue: string;
        format: number;
    }>;
    stopBarcodeScan(): Promise<void>;
    classifyPhoto(): Promise<{
        primaryLabel: string;
        labels: Array<{
            label: string;
            confidence: number;
        }>;
        base64: string;
    }>;
}
