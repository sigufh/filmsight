package com.filmtracker.app.ui.screens.panels.color

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.ui.screens.components.AdjustmentSlider
import com.filmtracker.app.ui.components.HistogramView
import com.filmtracker.app.util.ExifHelper
import com.filmtracker.app.util.HistogramInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ColorGradingScreen(
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit,
    onBack: () -> Unit,
    currentBitmap: Bitmap? = null
) {
    // 计算直方图
    var histogramInfo by remember { mutableStateOf<HistogramInfo?>(null) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(currentBitmap) {
        currentBitmap?.let { bitmap ->
            scope.launch {
                histogramInfo = withContext(Dispatchers.Default) {
                    ExifHelper.calculateHistogram(bitmap)
                }
            }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2C2C2E))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }
            Text(
                text = "分级",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.width(48.dp))
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 直方图显示
            HistogramView(
                histogramInfo = histogramInfo,
                modifier = Modifier.fillMaxWidth()
            )
            
            Divider(color = Color.Gray.copy(alpha = 0.3f))
            
            Text("高光", color = Color.White, style = MaterialTheme.typography.titleSmall)
            AdjustmentSlider("色温", params.gradingHighlightsTemp, 
                { onParamsChange(params.copy(gradingHighlightsTemp = it)) }, -100f..100f)
            AdjustmentSlider("色调", params.gradingHighlightsTint, 
                { onParamsChange(params.copy(gradingHighlightsTint = it)) }, -100f..100f)
            
            Divider(color = Color.Gray.copy(alpha = 0.3f))
            
            Text("中间调", color = Color.White, style = MaterialTheme.typography.titleSmall)
            AdjustmentSlider("色温", params.gradingMidtonesTemp, 
                { onParamsChange(params.copy(gradingMidtonesTemp = it)) }, -100f..100f)
            AdjustmentSlider("色调", params.gradingMidtonesTint, 
                { onParamsChange(params.copy(gradingMidtonesTint = it)) }, -100f..100f)
            
            Divider(color = Color.Gray.copy(alpha = 0.3f))
            
            Text("阴影", color = Color.White, style = MaterialTheme.typography.titleSmall)
            AdjustmentSlider("色温", params.gradingShadowsTemp, 
                { onParamsChange(params.copy(gradingShadowsTemp = it)) }, -100f..100f)
            AdjustmentSlider("色调", params.gradingShadowsTint, 
                { onParamsChange(params.copy(gradingShadowsTint = it)) }, -100f..100f)
            
            Divider(color = Color.Gray.copy(alpha = 0.3f))
            
            Text("全局", color = Color.White, style = MaterialTheme.typography.titleSmall)
            AdjustmentSlider("混合", params.gradingBlending, 
                { onParamsChange(params.copy(gradingBlending = it)) }, 0f..100f)
            AdjustmentSlider("平衡", params.gradingBalance, 
                { onParamsChange(params.copy(gradingBalance = it)) }, -100f..100f)
        }
    }
}
