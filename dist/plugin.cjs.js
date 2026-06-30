'use strict';

var core = require('@capacitor/core');

const CameraModule = core.registerPlugin('CameraModule', {
    web: () => Promise.resolve().then(function () { return web; }).then((m) => new m.CameraModuleWeb()),
});

class CameraModuleWeb extends core.WebPlugin {
    async echo(options) {
        console.log('ECHO', options);
        return options;
    }
    async checkPermission() {
        var _a, _b;
        // Verificar disponibilidad de APIs - usando prefijo _ para variables no usadas
        const hasMediaDevices = !!((_a = navigator.mediaDevices) === null || _a === void 0 ? void 0 : _a.enumerateDevices);
        const _hasGetUserMedia = !!((_b = navigator.mediaDevices) === null || _b === void 0 ? void 0 : _b.getUserMedia); // Prefijo _
        if (!hasMediaDevices || !_hasGetUserMedia) {
            throw this.unavailable('Camera API no disponible en este navegador');
        }
        try {
            const devices = await navigator.mediaDevices.enumerateDevices();
            const videoDevices = devices.filter((device) => device.kind === 'videoinput');
            // No hay dispositivos de cámara
            if (videoDevices.length === 0) {
                return {
                    granted: false,
                    status: 'denied',
                    details: 'No camera devices found',
                };
            }
            // Verificar si algún dispositivo tiene label (indica permiso)
            const hasPermission = videoDevices.some((device) => device.label && device.label.trim() !== '');
            // Determinar estado más preciso
            let status = 'denied';
            if (hasPermission) {
                status = 'granted';
            }
            else {
                // En web, sin intentar getUserMedia, asumimos que es la primera vez
                status = 'prompt';
            }
            return {
                granted: hasPermission,
                status,
                details: `Found ${videoDevices.length} camera device(s)`,
            };
        }
        catch (error) {
            console.error('Error checking camera permission:', error);
            return {
                granted: false,
                status: 'denied',
                details: error.message || 'Unknown error',
            };
        }
    }
    async requestPermission() {
        var _a;
        // Verificar getUserMedia específicamente para este método
        if (!((_a = navigator.mediaDevices) === null || _a === void 0 ? void 0 : _a.getUserMedia)) {
            throw this.unavailable('Camera API no disponible en este navegador');
        }
        try {
            // Intentar acceder a la cámara con configuración mínima
            const stream = await navigator.mediaDevices.getUserMedia({
                video: {
                    width: { ideal: 1280 },
                    height: { ideal: 720 },
                    facingMode: { ideal: 'environment' },
                },
                audio: false,
            });
            // Limpiar recursos inmediatamente
            stream.getTracks().forEach((track) => {
                track.stop();
            });
            return {
                granted: true,
                status: 'granted',
                details: 'Permission granted successfully',
            };
        }
        catch (error) {
            console.error('Error requesting camera permission:', error);
            // Manejo específico de errores
            let status = 'denied';
            let details = error.message || 'Unknown error';
            if (error.name === 'NotAllowedError') {
                status = 'denied';
                details = 'User denied camera access';
            }
            else if (error.name === 'NotFoundError') {
                status = 'denied';
                details = 'No camera found';
            }
            else if (error.name === 'NotReadableError') {
                status = 'denied';
                details = 'Camera is already in use';
            }
            else if (error.name === 'OverconstrainedError') {
                status = 'denied';
                details = 'Camera constraints cannot be met';
            }
            else if (error.name === 'SecurityError') {
                status = 'denied';
                details = 'Camera access blocked by browser security settings';
            }
            else if (error.name === 'TypeError') {
                status = 'denied';
                details = 'Invalid constraints specified';
            }
            return {
                granted: false,
                status,
                details,
            };
        }
    }
    async checkAndRequestPermission() {
        try {
            // Primero verificar el estado actual
            const currentStatus = await this.checkPermission();
            // Si ya tiene permiso, retornar inmediatamente
            if (currentStatus.granted) {
                return currentStatus;
            }
            // Si no tiene permiso, solicitar
            return await this.requestPermission();
        }
        catch (error) {
            console.error('Error in checkAndRequestPermission:', error);
            return {
                granted: false,
                status: 'denied',
                details: 'Failed to check or request permission',
            };
        }
    }
    // Método adicional para verificar soporte de características
    async getCameraCapabilities() {
        var _a, _b;
        const hasMediaDevices = !!((_a = navigator.mediaDevices) === null || _a === void 0 ? void 0 : _a.enumerateDevices);
        const hasGetUserMedia = !!((_b = navigator.mediaDevices) === null || _b === void 0 ? void 0 : _b.getUserMedia);
        let hasCamera = false;
        if (hasMediaDevices || hasGetUserMedia) {
            try {
                const devices = await navigator.mediaDevices.enumerateDevices();
                hasCamera = devices.some((device) => device.kind === 'videoinput');
            }
            catch (_c) {
                hasCamera = false;
            }
        }
        return {
            hasCamera,
            isSecureContext: window.isSecureContext,
            userAgent: navigator.userAgent,
        };
    }
    //Camera Preview
    async startPreview() {
        console.warn('[CameraModule] startPreview is not supported on web');
    }
    async stopPreview() {
        console.warn('[CameraModule] stopPreview is not supported on web');
    }
    async hasFlash() {
        return false;
    }
    async toggleFlash(_enable) {
        console.log(_enable);
        console.warn('[CameraModule] toggleFlash is not supported on web ');
    }
    async checkGalleryPermission() {
        return {
            granted: true,
            status: 'granted',
            details: 'Web does not require gallery permission',
        };
    }
    async requestGalleryPermission() {
        return {
            granted: true,
            status: 'granted',
            details: 'Web does not require gallery permission',
        };
    }
    async checkAndRequestGalleryPermission() {
        return this.checkGalleryPermission();
    }
    /* Imagen */
    async pickImageBase64() {
        throw new Error('[CameraModule] pickImageBase64 is not supported on web');
    }
    async getLastGalleryImage() {
        throw new Error('[CameraModule] getLastGalleryImage is not supported on web');
    }
    async takePhotoBase64() {
        throw this.unavailable('Camera not available on web');
    }
    async startBarcodeScan() {
        throw this.unavailable('Barcode scanning not available on web');
    }
    async stopBarcodeScan() {
        return;
    }
}

var web = /*#__PURE__*/Object.freeze({
    __proto__: null,
    CameraModuleWeb: CameraModuleWeb
});

exports.CameraModule = CameraModule;
//# sourceMappingURL=plugin.cjs.js.map
