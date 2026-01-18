package com.filmtracker.app.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmtracker.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * AI 胶卷助手对话窗口
 * 
 * 固定在左侧的 AI 对话面板，提供：
 * - 对话历史显示
 * - 消息输入框
 * - 快捷操作按钮（生图、智能调色）
 * 
 * 设计特点：
 * - 奶灰蓝浅底色
 * - 毛玻璃效果
 * - 薄荷绿发送按钮
 */
@Composable
fun AIDialogPanel(
    modifier: Modifier = Modifier,
    showQuickActions: Boolean = false,  // 是否显示快捷操作按钮
    onSendMessage: (String) -> Unit = {},
    onGenerateImage: () -> Unit = {},
    onSmartColorGrade: () -> Unit = {}
) {
    var messageText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<AIMessage>() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // 初始欢迎消息
    LaunchedEffect(Unit) {
        messages.add(
            AIMessage(
                text = "你好！我是 AI 胶卷助手，可以帮你选择合适的画幅、调整参数，或者提供拍摄建议。",
                isUser = false
            )
        )
    }
    
    Surface(
        modifier = modifier,
        color = FilmMilkyBlue.copy(alpha = 0.3f),  // 奶灰蓝浅底色
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 对话历史
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    AIMessageBubble(message = message)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 快捷操作按钮（仅在预览调色页显示）
            if (showQuickActions) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 生图按钮
                    OutlinedButton(
                        onClick = onGenerateImage,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = FilmMintGreen
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = SolidColor(FilmMintGreen)
                        )
                    ) {
                        Text("生图", fontSize = 12.sp)
                    }
                    
                    // 智能调色按钮
                    OutlinedButton(
                        onClick = onSmartColorGrade,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = FilmCaramelOrange
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = SolidColor(FilmCaramelOrange)
                        )
                    ) {
                        Text("智能调色", fontSize = 12.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // 输入框
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 文本输入框
                BasicTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(FilmWhite)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    textStyle = LocalTextStyle.current.copy(
                        color = FilmInkBlack,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(FilmCaramelOrange),
                    decorationBox = { innerTextField ->
                        if (messageText.isEmpty()) {
                            Text(
                                text = "输入消息...",
                                color = FilmDarkGray,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                )
                
                // 发送按钮
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            messages.add(AIMessage(text = messageText, isUser = true))
                            onSendMessage(messageText)
                            
                            // 模拟 AI 回复
                            messages.add(
                                AIMessage(
                                    text = "收到！让我帮你分析一下...",
                                    isUser = false
                                )
                            )
                            
                            messageText = ""
                            
                            // 滚动到底部
                            coroutineScope.launch {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(FilmMintGreen),
                    enabled = messageText.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "发送",
                        tint = FilmWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * AI 消息气泡
 */
@Composable
private fun AIMessageBubble(
    message: AIMessage,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (message.isUser) FilmCaramelOrange else FilmWhite,
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (message.isUser) 12.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 12.dp
            ),
            tonalElevation = 1.dp,
            modifier = Modifier.widthIn(max = 200.dp)
        ) {
            Text(
                text = message.text,
                fontSize = 13.sp,
                color = if (message.isUser) FilmWhite else FilmInkBlack,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

/**
 * AI 消息数据类
 */
data class AIMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
