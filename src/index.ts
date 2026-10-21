import { registerPlugin } from '@capacitor/core';

import type { CameraModulePlugin } from './definitions';

const CameraModule = registerPlugin<CameraModulePlugin>('CameraModule');

export * from './definitions';
export { CameraModule };