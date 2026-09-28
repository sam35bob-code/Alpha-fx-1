package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.AccuracyDashboardData

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RechartsWebView(
    dashboardData: AccuracyDashboardData,
    modifier: Modifier = Modifier
) {
    val htmlContent = remember(dashboardData) {
        RechartsHtmlBuilder.buildHtml(dashboardData)
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.parseColor("#080c14"))
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                }
                webViewClient = WebViewClient()
                loadDataWithBaseURL("https://recharts.local", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://recharts.local", htmlContent, "text/html", "UTF-8", null)
        },
        modifier = modifier
            .fillMaxWidth()
            .height(490.dp)
            .testTag("recharts_webview")
    )
}
