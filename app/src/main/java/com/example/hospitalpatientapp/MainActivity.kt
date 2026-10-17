/**
 * =========================================================================================
 * FILE: MainActivity.kt
 * ROLE: Bệnh nhân (Patient) - Điểm khởi động ứng dụng Android
 * MÔ TẢ:
 *   - Lớp khởi chạy chính (Entry Activity) của ứng dụng Android dành cho Bệnh nhân.
 *   - Cài đặt giao diện Jetpack Compose, cấu hình theme và nhúng màn hình chính PatientMainScreen.
 * 
 * LUỒNG HOẠT ĐỘNG:
 *   1. Hệ điều hành Android khởi chạy ứng dụng -> Gọi onCreate().
 *   2. Bật chế độ tràn viền hiện đại (enableEdgeToEdge).
 *   3. Thiết lập Content với HospitalPatientAppTheme.
 *   4. Render PatientMainScreen() chứa 3 tab: Đặt khám, Vé QR, và Giám sát hàng đợi.
 * =========================================================================================
 */
package com.example.hospitalpatientapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.hospitalpatientapp.theme.HospitalPatientAppTheme
import com.example.hospitalpatientapp.ui.PatientMainScreen

class MainActivity : ComponentActivity() {
    /**
     * Phương thức vòng đời khởi tạo Activity
     * Thiết lập hiển thị toàn màn hình và nạp giao diện Compose
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HospitalPatientAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PatientMainScreen()
                }
            }
        }
    }
}
