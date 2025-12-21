package com.example.thescreenshotbrain.presentation.overlay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thescreenshotbrain.data.local.entity.ScreenshotEntity

@Preview(showBackground = true)
@Composable
fun FloatingBubblePreview() {
    floatingBubble(
        text = "https://www.google.com",
        type = ScreenshotEntity.TYPE_URL,
        onOpenAction = {},
        onDismiss = {}
    )
}

@Composable
fun floatingBubble(
    text: String,
    type: String,
    onOpenAction: () -> Unit,
    onDismiss: () -> Unit
){
    val clipboardManager = LocalClipboardManager.current

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.padding(16.dp).widthIn(max = 300.dp)
    ){
        Column(
            modifier = Modifier.padding(12.dp)
        ){
            //Type
            Text(
                text = "Đã phát hiện ${getTypeDisplayName(type)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            //Text is extracted OCR
            Text(
                text = text.take(100) + if(text.length > 100) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(4.dp))

            //Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
            ){
                //Button copy
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(text))
                        onDismiss()
                    }
                ){
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                }

                //Button action for url, phone, map, and event
                if(type == ScreenshotEntity.TYPE_URL || type == ScreenshotEntity.TYPE_PHONE ||
                   type == ScreenshotEntity.TYPE_MAP || type == ScreenshotEntity.TYPE_EVENT){
                    FilledTonalButton(
                        onClick = onOpenAction,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ){
                        Icon(
                            when(type) {
                                ScreenshotEntity.TYPE_URL -> Icons.Default.OpenInNew
                                ScreenshotEntity.TYPE_PHONE -> Icons.Default.OpenInNew
                                ScreenshotEntity.TYPE_MAP -> Icons.Default.LocationOn
                                ScreenshotEntity.TYPE_EVENT -> Icons.Default.DateRange
                                else -> Icons.Default.OpenInNew
                            },
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when(type) {
                                ScreenshotEntity.TYPE_URL -> "Mở trong trình duyệt"
                                ScreenshotEntity.TYPE_PHONE -> "Gọi điện thoại"
                                ScreenshotEntity.TYPE_MAP -> "Chỉ đường"
                                ScreenshotEntity.TYPE_EVENT -> "Thêm vào lịch"
                                else -> "Mở"
                            },
                        )
                    }
                }

                //Button close
                IconButton(
                    onClick = onDismiss
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }

            }

        }
    }
}

private fun getTypeDisplayName(type: String): String {
    return when (type) {
        ScreenshotEntity.TYPE_BANK -> "BANK"
        ScreenshotEntity.TYPE_URL -> "URL"
        ScreenshotEntity.TYPE_PHONE -> "SĐT"
        ScreenshotEntity.TYPE_EVENT -> "LỊCH"
        ScreenshotEntity.TYPE_MAP -> "MAP"
        ScreenshotEntity.TYPE_NOTE -> "NOTE"
        ScreenshotEntity.TYPE_OTHER -> "KHÁC"
        else -> type
    }
}