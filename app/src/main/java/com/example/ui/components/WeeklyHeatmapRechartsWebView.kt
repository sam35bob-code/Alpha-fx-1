package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.WeeklyHeatmapData
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalBorder

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WeeklyHeatmapRechartsWebView(
    data: WeeklyHeatmapData,
    modifier: Modifier = Modifier
) {
    val htmlContent = remember(data) {
        WeeklyHeatmapRechartsHtmlBuilder.buildHtml(data)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(680.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            .background(TerminalBackground)
            .testTag("weekly_heatmap_recharts_webview")
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(680.dp),
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        allowFileAccess = false
                        allowContentAccess = false
                    }
                    setBackgroundColor(0xFF080C14.toInt())
                    webViewClient = WebViewClient()
                    loadDataWithBaseURL("https://recharts.org", htmlContent, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL("https://recharts.org", htmlContent, "text/html", "UTF-8", null)
            }
        )
    }
}
