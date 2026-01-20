package com.filmtracker.app.ui.components

import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin

/**
 * Markdown 文本渲染组件
 * 使用 Markwon 库渲染 Markdown
 * 
 * 自动隐藏包含 "parameters" 的 JSON 代码块（用于 AI 参数传递）
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current
) {
    val context = LocalContext.current
    // 使用传入的样式颜色，如果没有则使用深灰色作为默认值
    val textColor = (style.color.takeIf { it != Color.Unspecified } ?: Color(0xFF2C2C2C)).toArgb()
    
    // 过滤掉包含 "parameters" 的 JSON 代码块
    val filteredMarkdown = remember(markdown) {
        filterParameterJsonBlocks(markdown)
    }
    
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .build()
    }
    
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(textColor)
                textSize = style.fontSize.value
            }
        },
        update = { textView ->
            textView.setTextColor(textColor)
            textView.textSize = style.fontSize.value
            markwon.setMarkdown(textView, filteredMarkdown)
        }
    )
}

/**
 * 过滤掉包含 "parameters" 的 JSON 代码块
 * 这些代码块用于参数传递，不需要显示给用户
 */
private fun filterParameterJsonBlocks(markdown: String): String {
    // 匹配 ```json ... ``` 代码块
    val jsonBlockPattern = Regex("""```json\s*([\s\S]*?)\s*```""")
    
    return jsonBlockPattern.replace(markdown) { matchResult ->
        val jsonContent = matchResult.groupValues[1]
        // 如果 JSON 内容包含 "parameters" 字段，则移除整个代码块
        if (jsonContent.contains("\"parameters\"")) {
            "" // 返回空字符串，移除该代码块
        } else {
            matchResult.value // 保留其他 JSON 代码块
        }
    }
}
