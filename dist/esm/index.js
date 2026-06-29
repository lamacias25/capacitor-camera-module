import { registerPlugin } from '@capacitor/core';
const CameraModule = registerPlugin('CameraModule', {
    web: () => import('./web').then((m) => new m.CameraModuleWeb()),
});
export * from './definitions';
export { CameraModule };
//# sourceMappingURL=index.js.map