import { WebPlugin } from '@capacitor/core';
import type { CameraModulePlugin, PermissionStatus, startBarcodeScanResult } from './definitions';
export declare class CameraModuleWeb extends WebPlugin implements CameraModulePlugin {
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
    checkPermission(): Promise<PermissionStatus>;
    requestPermission(): Promise<PermissionStatus>;
    checkAndRequestPermission(): Promise<PermissionStatus>;
    getCameraCapabilities(): Promise<{
        hasCamera: boolean;
        isSecureContext: boolean;
        userAgent: string;
    }>;
    startPreview(): Promise<void>;
    stopPreview(): Promise<void>;
    hasFlash(): Promise<boolean>;
    toggleFlash(_enable: boolean): Promise<void>;

    takePhotoBase64(): Promise<any>;
    startBarcodeScan(): Promise<startBarcodeScanResult>;
    stopBarcodeScan(): Promise<void>;
}
