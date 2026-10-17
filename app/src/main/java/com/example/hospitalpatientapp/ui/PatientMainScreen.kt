/**
 * =========================================================================================
 * FILE: PatientMainScreen.kt
 * ROLE: Bệnh nhân (Patient) - Giao diện chính đa chức năng (Jetpack Compose)
 * MÔ TẢ:
 *   - Màn hình trung tâm của ứng dụng di động dành cho Bệnh nhân trong hệ thống bệnh viện thông minh.
 *   - Cung cấp thanh điều hướng dưới đáy (Bottom Navigation Bar) chia làm 3 phân hệ cốt lõi:
 * 
 * CÁC PHÂN HỆ VÀ LUỒNG HOẠT ĐỘNG:
 *   1. TAB 1: ĐẶT LỊCH KHÁM (BookingTab)
 *      - Chọn chuyên khoa -> Lọc bác sĩ phụ trách.
 *      - Chọn ngày khám, khung giờ (Buổi sáng / Buổi chiều).
 *      - Nhập triệu chứng & Xác nhận -> Gửi API lên /api/patient/book.
 *      - Nhận mã đặt khám và tự động tạo mã QR Check-in.
 * 
 *   2. TAB 2: VÉ KHÁM & MÃ QR (MyTicketsTab)
 *      - Hiển thị danh sách vé khám đã đăng ký của bệnh nhân.
 *      - Sinh mã QR động chuẩn ISO từ QR Token để quét tại quầy Lễ tân hoặc Kiosk.
 *      - Trực quan hóa trạng thái: Đã đặt, Đã check-in, Đang khám, Đã hoàn tất.
 * 
 *   3. TAB 3: THEO DÕI HÀNG ĐỢI TRỰC TIẾP (QueueTrackerTab - Smart Queue)
 *      - Cập nhật số thứ tự đang khám tại phòng khám theo thời gian thực (Real-time).
 *      - Hiển thị STT của tôi, STT đang gọi, và số người còn lại phía trước.
 *      - Đưa ra cảnh báo thông minh: Khi còn <= 2 người, nhắc nhở bệnh nhân đến trước cửa phòng khám.
 * =========================================================================================
 */
package com.example.hospitalpatientapp.ui

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hospitalpatientapp.data.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientMainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val patientPhone = "0912345678" // Số điện thoại mẫu của bệnh nhân Nguyễn Văn Hùng
    val patientName = "Nguyễn Văn Hùng"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MedQueue Bệnh Nhân",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "$patientName • $patientPhone",
                            fontSize = 12.sp,
                            color = Color(0xFFCCFBF1)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F766E) // Teal 700
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Đặt lịch") },
                    label = { Text("Đặt lịch") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0F766E),
                        selectedTextColor = Color(0xFF0F766E),
                        indicatorColor = Color(0xFFCCFBF1)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.QrCode, contentDescription = "Mã QR") },
                    label = { Text("Vé & Mã QR") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0F766E),
                        selectedTextColor = Color(0xFF0F766E),
                        indicatorColor = Color(0xFFCCFBF1)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.AccessTime, contentDescription = "Hàng đợi") },
                    label = { Text("Hàng đợi") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0F766E),
                        selectedTextColor = Color(0xFF0F766E),
                        indicatorColor = Color(0xFFCCFBF1)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            when (selectedTab) {
                0 -> BookingTab(
                    patientName = patientName,
                    patientPhone = patientPhone,
                    onBookSuccess = { selectedTab = 1 }
                )
                1 -> QrTicketsTab(patientPhone = patientPhone)
                2 -> LiveQueueTab(patientPhone = patientPhone)
            }
        }
    }
}

// ==========================================
// TAB 1: ĐẶT LỊCH KHÁM (BOOKING)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingTab(
    patientName: String,
    patientPhone: String,
    onBookSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var departments by remember { mutableStateOf<List<Department>>(emptyList()) }
    var selectedDept by remember { mutableStateOf<Department?>(null) }
    var selectedDoctor by remember { mutableStateOf<Doctor?>(null) }
    var deptExpanded by remember { mutableStateOf(false) }
    var doctorExpanded by remember { mutableStateOf(false) }

    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    var selectedTimeSlot by remember { mutableStateOf("09:00") }
    var reasonText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val timeSlots = listOf("08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "14:00", "14:30")

    LaunchedEffect(Unit) {
        try {
            val list = NetworkClient.getDepartments()
            departments = list
            if (list.isNotEmpty()) {
                selectedDept = list.first()
                if (list.first().doctors.isNotEmpty()) {
                    selectedDoctor = list.first().doctors.first()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Đặt lịch khám thông minh",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Chọn chuyên khoa và giờ khám phù hợp. Hệ thống sẽ tự động cấp mã QR check-in tại viện.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    // Chọn Chuyên khoa
                    Text(text = "Chuyên khoa khám *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    ExposedDropdownMenuBox(
                        expanded = deptExpanded,
                        onExpandedChange = { deptExpanded = !deptExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedDept?.name ?: "Đang tải danh mục...",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = deptExpanded,
                            onDismissRequest = { deptExpanded = false }
                        ) {
                            departments.forEach { dept ->
                                DropdownMenuItem(
                                    text = { Text(dept.name) },
                                    onClick = {
                                        selectedDept = dept
                                        selectedDoctor = dept.doctors.firstOrNull()
                                        deptExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Chọn Bác sĩ
                    Text(text = "Bác sĩ phụ trách *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    ExposedDropdownMenuBox(
                        expanded = doctorExpanded,
                        onExpandedChange = { doctorExpanded = !doctorExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedDoctor?.let { "${it.name} (${it.title ?: "BS"})" } ?: "Chọn bác sĩ",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = doctorExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = doctorExpanded,
                            onDismissRequest = { doctorExpanded = false }
                        ) {
                            selectedDept?.doctors?.forEach { doc ->
                                DropdownMenuItem(
                                    text = { Text("${doc.name} - ${doc.room ?: "Phòng khám"}") },
                                    onClick = {
                                        selectedDoctor = doc
                                        doctorExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Khung giờ
                    Text(text = "Khung giờ hẹn khám *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        timeSlots.take(4).forEach { slot ->
                            FilterChip(
                                selected = selectedTimeSlot == slot,
                                onClick = { selectedTimeSlot = slot },
                                label = { Text(slot, fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        timeSlots.drop(4).forEach { slot ->
                            FilterChip(
                                selected = selectedTimeSlot == slot,
                                onClick = { selectedTimeSlot = slot },
                                label = { Text(slot, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Lý do khám
                    Text(text = "Lý do khám / Triệu chứng", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = { reasonText = it },
                        placeholder = { Text("Mô tả sơ bộ triệu chứng (VD: Ho đờm, rát họng...)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val docId = selectedDoctor?.id ?: 1
                            isLoading = true
                            scope.launch {
                                try {
                                    val res = NetworkClient.bookAppointment(
                                        patientName = patientName,
                                        phone = patientPhone,
                                        doctorId = docId,
                                        date = todayDate,
                                        timeSlot = selectedTimeSlot,
                                        reason = reasonText
                                    )
                                    isLoading = false
                                    if (res.success) {
                                        Toast.makeText(context, "Đặt lịch thành công!", Toast.LENGTH_SHORT).show()
                                        onBookSuccess()
                                    } else {
                                        Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    isLoading = false
                                    Toast.makeText(context, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("XÁC NHẬN ĐẶT LỊCH KHÁM", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 2: VÉ KHÁM & MÃ QR (QR TICKETS)
// ==========================================
@Composable
fun QrTicketsTab(patientPhone: String) {
    var appointments by remember { mutableStateOf<List<PatientAppointment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            appointments = NetworkClient.getMyAppointments(patientPhone)
            isLoading = false
        } catch (e: Exception) {
            e.printStackTrace()
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF0F766E))
        }
    } else if (appointments.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Bạn chưa có lịch hẹn nào.", color = Color.Gray, fontSize = 14.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(appointments) { apt ->
                QrTicketCard(appointment = apt)
            }
        }
    }
}

@Composable
fun QrTicketCard(appointment: PatientAppointment) {
    val qrBitmap: Bitmap? = remember(appointment.qrToken) {
        QrCodeGenerator.generateQrBitmap(appointment.qrToken, 400)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Ticket
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = appointment.department,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F766E)
                    )
                    Text(
                        text = appointment.room ?: "Phòng 204",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (appointment.status == "DACHECKIN") Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                ) {
                    Text(
                        text = if (appointment.status == "DACHECKIN") "ĐÃ CHECK-IN" else "ĐÃ XÁC NHẬN",
                        color = if (appointment.status == "DACHECKIN") Color(0xFF166534) else Color(0xFF92400E),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // QR Code Image
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(Color.White)
                    .border(2.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                qrBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Mã QR",
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: CircularProgressIndicator()
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mã QR Code Text
            Text(
                text = appointment.qrToken,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "Đưa mã này trước máy quét Kiosk hoặc Lễ tân để check-in",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFF1F5F9))

            // Chi tiết lịch
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Bác sĩ:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text(appointment.doctorName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Thời gian hẹn:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text("${appointment.timeSlot} • ${appointment.date}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                }
            }
        }
    }
}

// ==========================================
// TAB 3: THEO DÕI HÀNG ĐỢI THÔNG MINH (QUEUE)
// ==========================================
@Composable
fun LiveQueueTab(patientPhone: String) {
    var tracker by remember { mutableStateOf<QueueTrackerResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    fun refreshQueue() {
        isLoading = true
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            try {
                tracker = NetworkClient.getQueueTracker(patientPhone)
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshQueue()
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF0F766E))
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "VỊ TRÍ HÀNG ĐỢI CỦA BẠN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Số phiếu của bạn
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F766E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tracker?.myTicketNumber ?: "---",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = tracker?.department ?: "Khoa Tai Mũi Họng",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "${tracker?.room ?: "Phòng 204"} • ${tracker?.doctorName ?: "Bác sĩ phụ trách"}",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Thống kê nhanh hàng đợi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Đang khám ca", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(
                                text = tracker?.currentServingNumber ?: "Chưa có",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Đứng trước bạn", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(
                                text = "${tracker?.peopleAhead ?: 0} người",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Dự kiến chờ", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(
                                text = "~${tracker?.estimatedWaitMinutes ?: 0} phút",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        }
                    }
                }
            }

            // Nút làm mới
            OutlinedButton(
                onClick = { refreshQueue() },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("LÀM MỚI TRẠNG THÁI HÀNG ĐỢI")
            }
        }
    }
}
