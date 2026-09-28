package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.TradeAction
import com.example.ui.theme.*
import com.example.ui.viewmodel.ForexViewModel

@Composable
fun Mt5ScannerScreen(
    viewModel: ForexViewModel,
    onNavigateToRiskCalc: () -> Unit,
    onNavigateToJournal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeBitmap by viewModel.activeMt5Bitmap.collectAsState()
    val scanResult by viewModel.activeMt5ScanResult.collectAsState()
    val isScanning by viewModel.isScanningMt5.collectAsState()
    val scanError by viewModel.scanError.collectAsState()

    var customInquiryText by remember { mutableStateOf("") }
    var showFullMarkdown by remember { mutableStateOf(false) }
    var showJournalConfirmDialog by remember { mutableStateOf(false) }

    // Android zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        viewModel.setMt5Screenshot(bitmap)
                    } else {
                        Toast.makeText(context, "Could not decode chart image", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Clipboard Paste Helper
    fun pasteFromClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val item = clip.getItemAt(0)
            val uri = item.uri
            if (uri != null) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        if (bitmap != null) {
                            viewModel.setMt5Screenshot(bitmap)
                            Toast.makeText(context, "Pasted MT5 screenshot from clipboard!", Toast.LENGTH_SHORT).show()
                            return
                        }
                    }
                } catch (e: Exception) {
                    // Fallthrough to text check
                }
            }
        }
        Toast.makeText(context, "No image found on clipboard. Tap 'Upload Screenshot' to pick an MT5 chart.", Toast.LENGTH_LONG).show()
    }

    // Load bundled sample MT5 chart
    fun loadSampleMt5Chart() {
        try {
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.mt5_chart_sample)
            if (bitmap != null) {
                viewModel.setMt5Screenshot(bitmap)
                Toast.makeText(context, "Loaded sample EUR/USD MT5 chart for scanning!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Sample chart loaded", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .padding(horizontal = 14.dp)
            .testTag("mt5_scanner_screen_column"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(CyanAccent.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "MT5 Scanner Icon",
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "MT5 AI CHART SCANNER",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = "Computer Vision Structural Price Action Audit",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                if (activeBitmap != null) {
                    IconButton(
                        onClick = { viewModel.clearMt5Screenshot() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(TerminalSurfaceVariant, CircleShape)
                            .testTag("clear_mt5_chart_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear Chart",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Intake Options Card (Upload, Paste, Sample)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
                    .testTag("mt5_upload_options_card"),
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. INTAKE MT5 SCREENSHOT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        color = CyanAccent
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Gallery / File Picker Button
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("pick_mt5_screenshot_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanAccent,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Paste from Clipboard Button
                        OutlinedButton(
                            onClick = { pasteFromClipboard() },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("paste_clipboard_button"),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, TerminalBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = CyanAccent
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Demo Chart Button
                        FilledTonalButton(
                            onClick = { loadSampleMt5Chart() },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("load_sample_mt5_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = TerminalSurfaceVariant,
                                contentColor = TextPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = GoldAccent
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Demo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "Accepts direct mobile or desktop MT5 chart screenshots showing candles, EMAs, and indicators.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Image Preview & Scanner Trigger Card
        if (activeBitmap != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .testTag("mt5_preview_card"),
                    colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TerminalBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = activeBitmap!!.asImageBitmap(),
                                contentDescription = "Active MT5 Screenshot",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Status Overlay Tag
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = TerminalSurface.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, TerminalBorder),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = if (isScanning) "SCANNING WITH AI..." else "CHART READY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isScanning) GoldAccent else BullishGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Scan Action Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.scanMt5Chart() },
                                enabled = !isScanning,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("trigger_ai_scan_btn"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanAccent,
                                    contentColor = Color.Black
                                )
                            ) {
                                if (isScanning) {
                                    CircularProgressIndicator(
                                        color = Color.Black,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Auditing Structure...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scan MT5 Chart with AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            FilledTonalIconButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.size(44.dp).testTag("replace_chart_button"),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = TerminalSurfaceVariant
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Replace Image",
                                    tint = CyanAccent
                                )
                            }
                        }
                    }
                }
            }
        }

        // Scanning Error Notice
        if (scanError != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BearishRed.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, BearishRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = scanError ?: "",
                        fontSize = 11.sp,
                        color = BearishRed,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // VERDICT & TRADE DECISION DISPLAY (Whether you can take a trade or not)
        if (scanResult != null) {
            val result = scanResult!!
            val canTrade = result.canTakeTrade
            val action = result.tradeAction

            // 1. Primary Verdict Banner
            item {
                val verdictColor = if (canTrade) {
                    if (action == TradeAction.BUY) BullishGreen else BearishRed
                } else GoldAccent

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, verdictColor, RoundedCornerShape(12.dp))
                        .testTag("mt5_verdict_banner"),
                    colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(verdictColor.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (canTrade) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                                        contentDescription = null,
                                        tint = verdictColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "CAN YOU TAKE THIS TRADE?",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.8.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = if (canTrade) "YES • ${action.label}" else "NO • PRESERVATION OF CAPITAL",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = verdictColor
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = TerminalBackground,
                                border = BorderStroke(1.dp, TerminalBorder)
                            ) {
                                Text(
                                    text = "${result.detectedPair} • ${result.detectedTimeframe}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = result.verdictSummary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = TextPrimary,
                            modifier = Modifier
                                .background(TerminalBackground, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }

            // 2. Strict Checklist Rule Compliance
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
                        .testTag("mt5_rules_checklist_card"),
                    colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "DISCIPLINE CHECKLIST AUDIT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = CyanAccent
                        )

                        result.checklist.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(TerminalBackground, RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (item.isMet) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                                    contentDescription = null,
                                    tint = if (item.isMet) BullishGreen else GoldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${index + 1}. ${item.title}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = item.valueLabel,
                                        fontSize = 10.sp,
                                        color = if (item.isMet) BullishGreen else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Trade Execution Specifications (Entry, SL, TP1, TP2, R:R)
            if (result.entryPrice != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .testTag("mt5_execution_levels_card"),
                        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SUGGESTED TRADE EXECUTION",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp,
                                    color = CyanAccent
                                )
                                Text(
                                    text = "Min 1:2.0 R:R Verified",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BullishGreen
                                )
                            }

                            // Price Level Grid
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(TerminalBackground, RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Entry", fontSize = 10.sp, color = TextSecondary)
                                    Text(
                                        "${result.entryPrice}",
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }

                                Column {
                                    Text("Stop Loss", fontSize = 10.sp, color = TextSecondary)
                                    Text(
                                        "${result.stopLossPrice ?: "--"}",
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = BearishRed
                                    )
                                }

                                Column {
                                    Text("TP1 (1:2)", fontSize = 10.sp, color = TextSecondary)
                                    Text(
                                        "${result.takeProfit1Price ?: "--"}",
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = BullishGreen
                                    )
                                }

                                Column {
                                    Text("TP2 (1:3+)", fontSize = 10.sp, color = TextSecondary)
                                    Text(
                                        "${result.takeProfit2Price ?: "--"}",
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = BullishGreen
                                    )
                                }
                            }

                            // Quick Bridge Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.applyMt5ScanToRiskCalculator()
                                        onNavigateToRiskCalc()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("transfer_to_risk_calc_btn"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyanAccent,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Calculate,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Size This Trade", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { showJournalConfirmDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("save_mt5_to_journal_btn"),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, TerminalBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = CyanAccent
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Log to Journal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Detailed Technical Audit (Expandable)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
                        .clickable { showFullMarkdown = !showFullMarkdown }
                        .testTag("mt5_full_analysis_toggle_card"),
                    colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FULL TECHNICAL SCAN AUDIT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = CyanAccent
                            )
                            Icon(
                                imageVector = if (showFullMarkdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }

                        if (showFullMarkdown) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = result.fullAnalysisMarkdown,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = TextPrimary,
                                modifier = Modifier
                                    .background(TerminalBackground, RoundedCornerShape(6.dp))
                                    .padding(10.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // Follow-up Inquiry Card
        if (activeBitmap != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TerminalSurface,
                    border = BorderStroke(1.dp, TerminalBorder),
                    modifier = Modifier.fillMaxWidth().testTag("custom_mt5_query_surface")
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customInquiryText,
                            onValueChange = { customInquiryText = it },
                            placeholder = { Text("Ask AI about this MT5 screenshot...", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).testTag("mt5_query_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = TerminalBorder
                            )
                        )

                        IconButton(
                            onClick = {
                                if (customInquiryText.isNotBlank()) {
                                    viewModel.scanMt5Chart(customInquiryText)
                                    customInquiryText = ""
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(CyanAccent, RoundedCornerShape(8.dp))
                                .testTag("send_mt5_query_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send MT5 Question",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    if (showJournalConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showJournalConfirmDialog = false },
            title = { Text("Log MT5 Setup", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    "Save this verified MT5 chart setup to your local Room database journal for performance tracking?",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveMt5ScanToJournal()
                        showJournalConfirmDialog = false
                        Toast.makeText(context, "Logged to Trade Journal!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJournalConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = TerminalSurface
        )
    }
}
