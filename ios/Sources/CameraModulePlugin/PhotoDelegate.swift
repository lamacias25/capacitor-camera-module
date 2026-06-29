import AVFoundation
import UIKit

class PhotoDelegate: NSObject, AVCapturePhotoCaptureDelegate {


    private let callback: (String) -> Void

    init(callback: @escaping (String) -> Void) {
        self.callback = callback
    }

    func photoOutput(_ output: AVCapturePhotoOutput,
                     didFinishProcessingPhoto photo: AVCapturePhoto,
                     error: Error?) {

        guard let data = photo.fileDataRepresentation(),
              let image = UIImage(data: data),
              let jpeg = image.jpegData(compressionQuality: 0.8)
        else { return }

        let base64 = jpeg.base64EncodedString()
        callback(base64)
    }
}
