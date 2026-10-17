/**
 * =========================================================================================
 * FILE: QrCodeGenerator.kt
 * ROLE: Bệnh nhân (Patient) - Tiện ích tạo hình ảnh mã QR (QR Code Generator)
 * MÔ TẢ:
 *   - Chuyển đổi chuỗi token vé khám bệnh (QR Token) thành ảnh Bitmap hiển thị trên màn hình.
 *   - Sử dụng thư viện đồ họa ZXing (QRCodeWriter).
 *   - Ảnh QR này được bệnh nhân dùng để Check-in tại quầy Lễ tân hoặc Kiosk tự động.
 * 
 * PHƯƠNG THỨC CHÍNH:
 *   - generateQrBitmap(content, size): Nhận chuỗi token, mã hóa ma trận bit 2D và tạo Bitmap RGB_565.
 * =========================================================================================
 */
package com.example.hospitalpatientapp.ui

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeGenerator {
    /**
     * Tạo Bitmap QR Code từ chuỗi văn bản/token
     * @param content Nội dung chuỗi token (VD: QR-APP-...)
     * @param size Kích thước pixel vuông (mặc định 512x512)
     * @return Bitmap QR Code hoặc null nếu có lỗi
     */
    fun generateQrBitmap(content: String, size: Int = 512): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
