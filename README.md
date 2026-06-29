# capacitor-camera-module

Plugin to request permissiones view camera take phots

## Install

```bash
npm install capacitor-camera-module
npx cap sync
```

## API

<docgen-index>

* [`echo(...)`](#echo)
* [`checkPermission()`](#checkpermission)
* [`requestPermission()`](#requestpermission)
* [`checkAndRequestPermission()`](#checkandrequestpermission)
* [`getCameraCapabilities()`](#getcameracapabilities)
* [`startPreview()`](#startpreview)
* [`stopPreview()`](#stoppreview)
* [`toggleFlash(...)`](#toggleflash)
* [`hasFlash()`](#hasflash)
* [`checkGalleryPermission()`](#checkgallerypermission)
* [`requestGalleryPermission()`](#requestgallerypermission)
* [`checkAndRequestGalleryPermission()`](#checkandrequestgallerypermission)
* [`pickImageBase64()`](#pickimagebase64)
* [`getLastGalleryImage()`](#getlastgalleryimage)
* [`takePhotoBase64()`](#takephotobase64)
* [`startBarcodeScan()`](#startbarcodescan)
* [`stopBarcodeScan()`](#stopbarcodescan)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### echo(...)

```typescript
echo(options: { value: string; }) => Promise<{ value: string; }>
```

| Param         | Type                            |
| ------------- | ------------------------------- |
| **`options`** | <code>{ value: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### checkPermission()

```typescript
checkPermission() => Promise<PermissionStatus>
```

**Returns:** <code>Promise&lt;<a href="#permissionstatus">PermissionStatus</a>&gt;</code>

--------------------


### requestPermission()

```typescript
requestPermission() => Promise<PermissionStatus>
```

**Returns:** <code>Promise&lt;<a href="#permissionstatus">PermissionStatus</a>&gt;</code>

--------------------


### checkAndRequestPermission()

```typescript
checkAndRequestPermission() => Promise<PermissionStatus>
```

**Returns:** <code>Promise&lt;<a href="#permissionstatus">PermissionStatus</a>&gt;</code>

--------------------


### getCameraCapabilities()

```typescript
getCameraCapabilities() => Promise<CameraCapabilities>
```

**Returns:** <code>Promise&lt;<a href="#cameracapabilities">CameraCapabilities</a>&gt;</code>

--------------------


### startPreview()

```typescript
startPreview() => Promise<void>
```

--------------------


### stopPreview()

```typescript
stopPreview() => Promise<void>
```

--------------------


### toggleFlash(...)

```typescript
toggleFlash(enable: boolean) => Promise<void>
```

| Param        | Type                 |
| ------------ | -------------------- |
| **`enable`** | <code>boolean</code> |

--------------------


### hasFlash()

```typescript
hasFlash() => Promise<boolean>
```

**Returns:** <code>Promise&lt;boolean&gt;</code>

--------------------


### checkGalleryPermission()

```typescript
checkGalleryPermission() => Promise<PermissionResult>
```

**Returns:** <code>Promise&lt;<a href="#permissionresult">PermissionResult</a>&gt;</code>

--------------------


### requestGalleryPermission()

```typescript
requestGalleryPermission() => Promise<PermissionResult>
```

**Returns:** <code>Promise&lt;<a href="#permissionresult">PermissionResult</a>&gt;</code>

--------------------


### checkAndRequestGalleryPermission()

```typescript
checkAndRequestGalleryPermission() => Promise<PermissionResult>
```

**Returns:** <code>Promise&lt;<a href="#permissionresult">PermissionResult</a>&gt;</code>

--------------------


### pickImageBase64()

```typescript
pickImageBase64() => Promise<PickImageBase64Result>
```

**Returns:** <code>Promise&lt;<a href="#pickimagebase64result">PickImageBase64Result</a>&gt;</code>

--------------------


### getLastGalleryImage()

```typescript
getLastGalleryImage() => Promise<LastGalleryImageResult>
```

**Returns:** <code>Promise&lt;<a href="#lastgalleryimageresult">LastGalleryImageResult</a>&gt;</code>

--------------------


### takePhotoBase64()

```typescript
takePhotoBase64() => Promise<takephotoBase64Result>
```

**Returns:** <code>Promise&lt;<a href="#takephotobase64result">takephotoBase64Result</a>&gt;</code>

--------------------


### startBarcodeScan()

```typescript
startBarcodeScan() => Promise<startBarcodeScanResult>
```

**Returns:** <code>Promise&lt;<a href="#startbarcodescanresult">startBarcodeScanResult</a>&gt;</code>

--------------------


### stopBarcodeScan()

```typescript
stopBarcodeScan() => Promise<void>
```

--------------------


### Interfaces


#### PermissionStatus

| Prop          | Type                                                                                   |
| ------------- | -------------------------------------------------------------------------------------- |
| **`granted`** | <code>boolean</code>                                                                   |
| **`status`**  | <code>'granted' \| 'denied' \| 'prompt' \| 'prompt-with-rationale' \| 'limited'</code> |
| **`details`** | <code>string</code>                                                                    |


#### CameraCapabilities

| Prop                  | Type                 |
| --------------------- | -------------------- |
| **`hasCamera`**       | <code>boolean</code> |
| **`isSecureContext`** | <code>boolean</code> |
| **`userAgent`**       | <code>string</code>  |


#### PermissionResult

| Prop          | Type                                           |
| ------------- | ---------------------------------------------- |
| **`granted`** | <code>boolean</code>                           |
| **`status`**  | <code>'granted' \| 'denied' \| 'prompt'</code> |
| **`details`** | <code>string</code>                            |


#### PickImageBase64Result

| Prop           | Type                |
| -------------- | ------------------- |
| **`base64`**   | <code>string</code> |
| **`mimeType`** | <code>string</code> |


#### LastGalleryImageResult

| Prop         | Type                |
| ------------ | ------------------- |
| **`base64`** | <code>string</code> |


#### takephotoBase64Result

| Prop           | Type                |
| -------------- | ------------------- |
| **`base64`**   | <code>string</code> |
| **`mimeType`** | <code>string</code> |


#### startBarcodeScanResult

| Prop           | Type                |
| -------------- | ------------------- |
| **`rawValue`** | <code>string</code> |
| **`format`**   | <code>number</code> |

</docgen-api>
