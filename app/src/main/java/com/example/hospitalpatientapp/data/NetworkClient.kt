/**
 * =========================================================================================
 * FILE: NetworkClient.kt
 * ROLE: Bệnh nhân (Patient) - Lớp giao tiếp mạng (HTTP API Client)
 * MÔ TẢ:
 *   - Quản lý toàn bộ các lời gọi API RESTful từ ứng dụng Android lên Node.js Backend.
 *   - Sử dụng OkHttpClient kết hợp Coroutines (withContext Dispatchers.IO) để chạy ngầm không block UI.
 *   - Chuyển đổi dữ liệu JSON sang Kotlin Data Classes bằng thư viện Gson.
 * 
 * CÁC CHỨC NĂNG CHÍNH:
 *   1. getDepartments(): Lấy danh mục Chuyên khoa và danh sách Bác sĩ phụ trách.
 *   2. bookAppointment(): Gửi yêu cầu đặt lịch khám trực tuyến, sinh mã QR Token.
 *   3. getMyAppointments(): Truy vấn lịch sử khám và vé QR theo số điện thoại bệnh nhân.
 *   4. getQueueTracker(): Giám sát thứ tự hàng đợi trực tiếp tại phòng khám (Smart Queue Tracking).
 * =========================================================================================
 */
package com.example.hospitalpatientapp.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// Model dữ liệu
data class Department(
    val id: Int,
    val name: String,
    val description: String?,
    val doctors: List<Doctor>
)

data class Doctor(
    val id: Int,
    val name: String,
    val title: String?,
    val room: String?
)

data class BookResponse(
    val success: Boolean,
    val message: String,
    val appointment: AppointmentDetail?
)

data class AppointmentDetail(
    val appointmentId: Int,
    val qrToken: String,
    val date: String,
    val timeSlot: String,
    val patientName: String,
    val doctorName: String,
    val department: String,
    val room: String?
)

data class QueueTrackerResponse(
    val hasTicket: Boolean,
    val myTicketNumber: String?,
    val queueStatus: String?,
    val department: String?,
    val room: String?,
    val doctorName: String?,
    val currentServingNumber: String?,
    val peopleAhead: Int,
    val estimatedWaitMinutes: Int
)

data class PatientAppointment(
    val id: Int,
    val qrToken: String,
    val date: String,
    val timeSlot: String,
    val status: String,
    val doctorName: String,
    val department: String,
    val room: String?,
    val ticketNumber: Int?,
    val queueStatus: String?
)

object NetworkClient {
    // 10.0.2.2 là địa chỉ IP localhost của máy tính khi chạy trong máy ảo Android Emulator
    var BASE_URL = "http://10.0.2.2:3000"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    suspend fun getDepartments(): List<Department> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$BASE_URL/api/patient/departments").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            val type = object : TypeToken<List<Department>>() {}.type
            gson.fromJson(body, type)
        }
    }

    suspend fun bookAppointment(
        patientName: String,
        phone: String,
        doctorId: Int,
        date: String,
        timeSlot: String,
        reason: String
    ): BookResponse = withContext(Dispatchers.IO) {
        val jsonPayload = gson.toJson(mapOf(
            "patientName" to patientName,
            "phone" to phone,
            "doctorId" to doctorId,
            "date" to date,
            "timeSlot" to timeSlot,
            "reason" to reason
        ))

        val request = Request.Builder()
            .url("$BASE_URL/api/patient/book")
            .post(jsonPayload.toRequestBody(JSON))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: return@withContext BookResponse(false, "Lỗi phản hồi rỗng", null)
            gson.fromJson(body, BookResponse::class.java)
        }
    }

    suspend fun getMyAppointments(phone: String): List<PatientAppointment> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$BASE_URL/api/patient/appointments/$phone").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            val type = object : TypeToken<List<PatientAppointment>>() {}.type
            gson.fromJson(body, type)
        }
    }

    suspend fun getQueueTracker(phone: String): QueueTrackerResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$BASE_URL/api/patient/queue-tracker/$phone").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext QueueTrackerResponse(false, null, null, null, null, null, null, 0, 0)
            }
            val body = response.body?.string() ?: return@withContext QueueTrackerResponse(false, null, null, null, null, null, null, 0, 0)
            gson.fromJson(body, QueueTrackerResponse::class.java)
        }
    }
}
