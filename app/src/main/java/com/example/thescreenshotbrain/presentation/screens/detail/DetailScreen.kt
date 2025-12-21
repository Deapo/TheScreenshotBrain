package com.example.thescreenshotbrain.presentation.screens.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.thescreenshotbrain.core.common.BlockParser
import com.example.thescreenshotbrain.data.local.entity.ScreenshotEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val screenshot by viewModel.screenshot.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết ảnh") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    //button share image
                    IconButton(onClick = {
                        screenshot?.let { item ->
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/*"
                                putExtra(Intent.EXTRA_STREAM, Uri.parse(item.uri))
                                putExtra(Intent.EXTRA_TEXT, item.extractedText)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Chia sẻ ảnh"))
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { paddingValues ->
        val item = screenshot
        if (item != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                //1.show image
                AsyncImage(
                    model = item.uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.LightGray),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(16.dp))

                //2.Title
                if (item.title.isNotBlank()) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                //3.info tag, date,month
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(item.type.uppercase()) },
                        leadingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    )

                    Text(
                        text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(item.timestamp)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                //4.BlockParser: Tách khối và show Action Chips
                val blocks = BlockParser.parseBlocks(
                    item.rawText,
                    item.extractedText,
                    item.type
                )
                
                blocks.forEach { block ->
                    BlockCard(
                        block = block,
                        context = context
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        } else {
            // Loading or error
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}


@Composable
fun BlockCard(
    block: BlockParser.TextBlock,
    context: android.content.Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            //show content
            SelectionContainer {
                Text(
                    text = block.content,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            //Action Chip base on block type
            when (block.type) {
                BlockParser.BlockType.URL_LINK -> {
                    ActionChip(
                        label = "Mở Link",
                        icon = Icons.Default.OpenInNew,
                        onClick = {
                            handleBlockAction(context, block)
                        }
                    )
                }
                BlockParser.BlockType.PHONE_NUMBER -> {
                    ActionChip(
                        label = "Gọi ngay",
                        icon = Icons.Default.Phone,
                        onClick = {
                            handleBlockAction(context, block)
                        }
                    )
                }
                BlockParser.BlockType.MAP_LOCATION -> {
                    ActionChip(
                        label = "Chỉ đường",
                        icon = Icons.Default.LocationOn,
                        onClick = {
                            handleBlockAction(context, block)
                        }
                    )
                }
                BlockParser.BlockType.BANK_INFO -> {
                    ActionChip(
                        label = "Sao chép thông tin",
                        icon = Icons.Default.AccountBalanceWallet,
                        onClick = {
                            handleBlockAction(context, block)
                        }
                    )
                }
                BlockParser.BlockType.QR_CODE -> {
                    ActionChip(
                        label = "Xem QR Code",
                        icon = Icons.Default.Share,
                        onClick = {
                            handleBlockAction(context, block)
                        }
                    )
                }
                BlockParser.BlockType.TEXT -> {
                }
            }
        }
    }
}

@Composable
fun ActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    )
}

private fun handleBlockAction(context: android.content.Context, block: BlockParser.TextBlock) {
    try {
        when (block.type) {
            BlockParser.BlockType.URL_LINK -> {
                var url = block.content.trim()
                if (!url.startsWith("http")) url = "https://$url"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            BlockParser.BlockType.PHONE_NUMBER -> {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:${block.content}")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            BlockParser.BlockType.MAP_LOCATION -> {
                val mapUri = Uri.parse("geo:0,0?q=${Uri.encode(block.content)}")
                val intent = Intent(Intent.ACTION_VIEW, mapUri)
                intent.setPackage("com.google.android.apps.maps")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(block.content)}"))
                    webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(webIntent)
                }
            }
            BlockParser.BlockType.BANK_INFO, BlockParser.BlockType.TEXT -> {
                // Copy to clipboard
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Text", block.content)
                clipboard.setPrimaryClip(clip)
                android.widget.Toast.makeText(context, "Đã sao chép!", android.widget.Toast.LENGTH_SHORT).show()
            }
            BlockParser.BlockType.QR_CODE -> {
                // Copy QR content
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("QR Code", block.content)
                clipboard.setPrimaryClip(clip)
                android.widget.Toast.makeText(context, "Đã sao chép QR Code!", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Lỗi: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}