package com.example.thescreenshotbrain.presentation.screens.history.components

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.thescreenshotbrain.data.local.entity.ScreenshotEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ScreenshotItem(
    item: ScreenshotEntity,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Ảnh thumbnail
                AsyncImage(
                    model = item.uri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                // 2. Nội dung Text chính
                Column(modifier = Modifier.weight(1f)) {
                    // Badge loại (Hiển thị màu sắc theo loại)
                    TypeBadge(type = item.type)

                    Spacer(modifier = Modifier.height(4.dp))

                    // Text trích xuất
                    Text(
                        text = item.extractedText.ifBlank { "Không có nội dung" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Thời gian
                    Text(
                        text = convertTimestamp(item.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- 3. HÀNG NÚT HÀNH ĐỘNG (ACTION ROW) ---
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Nút COPY (Luôn hiện)
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(item.extractedText))
                        Toast.makeText(context, "Đã sao chép!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sao chép")
                }

                // Nút SMART ACTION (Logic thông minh cho: URL, PHONE, EVENT, MAP)
                if (isSmartActionAvailable(item.type)) {
                    TextButton(
                        onClick = { handleSmartAction(context, item) }
                    ) {
                        val (icon, label) = getActionInfo(item.type)
                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
                    }
                }

                // Nút XÓA
                TextButton(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Xóa")
                }
            }
        }
    }
}

// --- CÁC HÀM HỖ TRỢ LOGIC ---

// Kiểm tra xem loại này có nút hành động đặc biệt không
fun isSmartActionAvailable(type: String): Boolean {
    return type == ScreenshotEntity.TYPE_URL ||
            type == ScreenshotEntity.TYPE_PHONE ||
            type == ScreenshotEntity.TYPE_EVENT ||
            type == ScreenshotEntity.TYPE_MAP
}

// Lấy Icon và Tên nút dựa trên loại
@Composable
fun getActionInfo(type: String): Pair<ImageVector, String> {
    return when (type) {
        ScreenshotEntity.TYPE_URL -> Icons.Default.OpenInNew to "Mở Link"
        ScreenshotEntity.TYPE_PHONE -> Icons.Default.Phone to "Gọi ngay"
        ScreenshotEntity.TYPE_EVENT -> Icons.Default.DateRange to "Thêm Lịch"
        ScreenshotEntity.TYPE_MAP -> Icons.Default.LocationOn to "Chỉ đường"
        else -> Icons.Default.OpenInNew to "Mở"
    }
}

// Xử lý sự kiện khi bấm nút Smart Action
private fun handleSmartAction(context: Context, item: ScreenshotEntity) {
    val text = item.extractedText
    try {
        when (item.type) {
            ScreenshotEntity.TYPE_URL -> {
                // Logic xử lý Link thông minh (Search hoặc Open URL)
                if (text.contains("...") || text.contains("…")) {
                    val query = text
                        .replace("https://", "")
                        .replace("http://", "")
                        .replace("...", " ")
                        .replace("/", " ")
                        .replace("-", " ")

                    val searchIntent = Intent(Intent.ACTION_WEB_SEARCH)
                    searchIntent.putExtra(SearchManager.QUERY, query)
                    searchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(searchIntent)
                } else {
                    var finalUrl = text.trim()
                    if (!finalUrl.startsWith("http")) finalUrl = "https://$finalUrl"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }
            ScreenshotEntity.TYPE_PHONE -> {
                // Gọi điện
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:$text")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            ScreenshotEntity.TYPE_EVENT -> {
                // Mở ứng dụng Lịch (Calendar Intent)
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    putExtra(CalendarContract.Events.TITLE, "Sự kiện từ Screenshot")
                    putExtra(CalendarContract.Events.DESCRIPTION, "Nội dung:\n$text")
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            ScreenshotEntity.TYPE_MAP -> {
                // Mở Google Maps
                val mapUri = Uri.parse("geo:0,0?q=${Uri.encode(text)}")
                val intent = Intent(Intent.ACTION_VIEW, mapUri)
                intent.setPackage("com.google.android.apps.maps") // Ưu tiên mở bằng App Maps
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Nếu không có App Maps thì mở bằng trình duyệt (Link chuẩn Google Maps)
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(text)}"))
                    webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(webIntent)
                }
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Không thể thực hiện hành động: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// Badge hiển thị màu sắc đẹp mắt cho từng loại
@Composable
fun TypeBadge(type: String) {
    val (color, icon) = when (type) {
        ScreenshotEntity.TYPE_URL -> MaterialTheme.colorScheme.primary to Icons.Default.Link
        ScreenshotEntity.TYPE_PHONE -> MaterialTheme.colorScheme.tertiary to Icons.Default.Phone
        ScreenshotEntity.TYPE_BANK -> Color(0xFF9C27B0) to Icons.Default.AccountBalanceWallet // Tím
        ScreenshotEntity.TYPE_EVENT -> Color(0xFFE91E63) to Icons.Default.Event // Hồng (Lịch)
        ScreenshotEntity.TYPE_MAP -> Color(0xFFF44336) to Icons.Default.Map // Đỏ (Bản đồ)
        ScreenshotEntity.TYPE_NOTE -> Color(0xFFFF9800) to Icons.Default.Description // Cam
        else -> MaterialTheme.colorScheme.secondary to Icons.Default.Photo
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = color)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = type.removePrefix("TYPE_"),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 10.sp
        )
    }
}

fun convertTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}