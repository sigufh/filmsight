package com.filmtracker.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.filmtracker.app.ai.BeautyAIAnalyzer
import com.filmtracker.app.ai.BeautySuggestion
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * 美颜面板
 */
@Composable
fun BeautyPanel(
    imageUri: String,
    onApplyBeauty: (BeautySuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    var isAnalyzing by remember { mutableStateOf(false) }
    var beautySuggestion by remember { mutableStateOf<BeautySuggestion?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val beautyAnalyzer = remember { BeautyAIAnalyzer() }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "AI 美颜",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isAnalyzing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("分析中...")
                }
            } else if (beautySuggestion != null) {
                // 显示建议
                val suggestion = beautySuggestion!!
                
                Text(
                    text = "检测到 ${suggestion.faceRegions.size} 张人脸",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 建议项
                SuggestionItem("皮肤平滑", "${(suggestion.params.skinSmoothing * 100).toInt()}%")
                SuggestionItem("肤色修正", if (suggestion.params.skinToneWarmth > 0) "暖色" else "冷色")
                SuggestionItem("眼部增强", "亮度 +${(suggestion.params.eyeBrightness * 100).toInt()}%")
                SuggestionItem("嘴唇增强", "饱和度 +${(suggestion.params.lipSaturation * 100).toInt()}%")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { beautySuggestion = null }
                    ) {
                        Text("取消")
                    }
                    
                    Button(
                        onClick = { 
                            onApplyBeauty(suggestion)
                        }
                    ) {
                        Text("一键应用")
                    }
                }
            } else {
                Button(
                    onClick = {
                        isAnalyzing = true
                        coroutineScope.launch {
                            // TODO: 加载实际图像并分析
                            // 当前使用占位
                            val suggestion = beautyAnalyzer.analyzeBeauty(
                                image = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888),
                                iso = 400.0f
                            )
                            beautySuggestion = suggestion
                            isAnalyzing = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("分析照片")
                }
            }
        }
    }
}

/**
 * Local suggestion item for BeautyPanel
 */
@Composable
private fun SuggestionItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
