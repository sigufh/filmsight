package com.filmtracker.app.ai

class RAGKnowledgeBase {
    private val knowledgeItems = mutableListOf<KnowledgeItem>()
    
    init {
        loadDefaultKnowledge()
    }
    
    fun search(query: String, topK: Int = 3): List<KnowledgeItem> {
        val queryLower = query.lowercase()
        return knowledgeItems
            .map { it to calculateRelevance(queryLower, it) }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
    }
    
    fun addKnowledge(item: KnowledgeItem) {
        knowledgeItems.add(item)
    }
    
    private fun calculateRelevance(query: String, item: KnowledgeItem): Double {
        val keywords = item.keywords.map { it.lowercase() }
        val contentLower = item.content.lowercase()
        
        var score = 0.0
        keywords.forEach { keyword ->
            if (query.contains(keyword)) score += 2.0
        }
        if (contentLower.contains(query)) score += 1.0
        if (query.contains(item.category.lowercase())) score += 0.5
        
        return score
    }
    
    private fun loadDefaultKnowledge() {
        // === 曝光控制 ===
        knowledgeItems.add(KnowledgeItem(
            category = "曝光",
            content = "曝光调整影响整体亮度。增加曝光提亮画面，减少曝光压暗画面。建议先调整曝光，再微调高光和阴影。每档曝光相当于光圈或快门的一档变化。",
            keywords = listOf("曝光", "亮度", "提亮", "压暗", "EV", "档位")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "曝光",
            content = "高光控制画面最亮部分，降低高光可恢复过曝细节，建议-0.3到-0.5。阴影控制最暗部分，提升阴影可显现暗部细节，建议+0.2到+0.4。",
            keywords = listOf("高光", "阴影", "过曝", "欠曝", "细节", "恢复")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "曝光",
            content = "白色和黑色控制色阶端点。降低白色(-0.2)可压制高光溢出，提升黑色(+0.2)可制造灰雾感和电影感。两者配合使用可精确控制画面反差。",
            keywords = listOf("白色", "黑色", "色阶", "反差", "灰雾", "端点")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "曝光",
            content = "宽容度是指传感器记录从最暗到最亮细节的能力。RAW格式通常有12-14档宽容度，可以在后期大幅调整曝光而不损失画质。",
            keywords = listOf("宽容度", "动态范围", "RAW", "传感器", "后期")
        ))

        
        // === 色彩理论 ===
        knowledgeItems.add(KnowledgeItem(
            category = "色彩",
            content = "色温控制画面冷暖：2000-3000K极暖（烛光），3000-4000K暖（日出日落），5500K中性（日光），6000-7000K冷（阴天），8000K+极冷（蓝天）。",
            keywords = listOf("色温", "冷暖", "白平衡", "开尔文", "K", "日光")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "色彩",
            content = "色调(Tint)控制绿-洋红轴。正值偏洋红，负值偏绿。常用于修正荧光灯下的绿色偏色，或为人像添加健康的洋红色调。",
            keywords = listOf("色调", "tint", "绿色", "洋红", "偏色", "荧光灯")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "色彩",
            content = "饱和度影响所有颜色强度，过高会导致颜色溢出。自然饱和度(Vibrance)主要影响不饱和的颜色，保护肤色和已饱和的颜色，更适合人像。",
            keywords = listOf("饱和度", "自然饱和度", "vibrance", "人像", "肤色", "颜色强度")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "色彩",
            content = "HSL调整：色相(Hue)改变颜色本身，饱和度(Saturation)控制颜色鲜艳度，明度(Luminance)控制颜色亮度。可单独调整8种颜色。",
            keywords = listOf("HSL", "色相", "饱和度", "明度", "单色调整", "选择性")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "色彩",
            content = "互补色理论：红-青、绿-洋红、蓝-黄互为补色。调整一种颜色会影响其补色。例如降低蓝色饱和度会让黄色更突出。",
            keywords = listOf("互补色", "色轮", "对比色", "红青", "绿洋红", "蓝黄")
        ))
        
        // === 胶片理论 ===
        knowledgeItems.add(KnowledgeItem(
            category = "胶片理论",
            content = "负片(Negative Film)：宽容度高，色彩柔和自然，适合人像。特点是高光柔和过渡，阴影保留细节，色彩饱和度适中。",
            keywords = listOf("负片", "彩色负片", "宽容度", "柔和", "人像", "过渡")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "胶片理论",
            content = "反转片(Slide Film)：对比度高，色彩饱和鲜艳，适合风光。特点是高光易溢出，暗部易死黑，色彩浓郁，颗粒细腻。",
            keywords = listOf("反转片", "幻灯片", "高对比", "高饱和", "风光", "鲜艳")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "胶片理论",
            content = "银盐颗粒：胶片的特征纹理，高ISO胶片颗粒更明显。数字模拟时建议颗粒大小2-5%，配合轻微降低清晰度(-0.1)营造胶片质感。",
            keywords = listOf("颗粒", "银盐", "纹理", "ISO", "胶片质感", "模拟")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "胶片理论",
            content = "胶片曲线：S型曲线是胶片的典型特征。高光压缩(toe)和阴影提升(shoulder)创造柔和过渡，中间调对比增强。",
            keywords = listOf("胶片曲线", "S曲线", "toe", "shoulder", "过渡", "对比")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "胶片理论",
            content = "色彩科学：胶片通过染料层记录色彩，每层对特定波长敏感。这造成独特的色彩响应曲线，特别是在肤色和天空的渲染上。",
            keywords = listOf("色彩科学", "染料", "波长", "色彩响应", "肤色", "天空")
        ))
        
        // === 经典胶片风格 ===
        knowledgeItems.add(KnowledgeItem(
            category = "胶片",
            content = "富士Velvia 50：最著名的风光反转片。超高饱和度(+0.4)，高对比度(+0.3)，偏冷色温(-300K)，绿色和蓝色特别鲜艳。",
            keywords = listOf("富士", "velvia", "velvia50", "风光", "反转片", "高饱和")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "胶片",
            content = "富士Provia 100F：中性反转片，色彩准确。适中饱和度(+0.2)，适中对比度(+0.1)，色温准确，适合需要真实色彩的场景。",
            keywords = listOf("富士", "provia", "中性", "准确", "真实", "反转片")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "胶片",
            content = "柯达Portra 400：专业人像负片。柔和对比度(-0.1)，温暖色温(+300K)，自然饱和度(+0.2)，肤色渲染极佳，高光柔和。",
            keywords = listOf("柯达", "portra", "portra400", "人像", "负片", "肤色")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "胶片",
            content = "柯达Ektar 100：高饱和负片。饱和度(+0.35)接近反转片，但保持负片的宽容度，适合风光和静物，色彩浓郁细腻。",
            keywords = listOf("柯达", "ektar", "ektar100", "高饱和", "负片", "风光")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "胶片",
            content = "柯达Gold 200：经济型负片，温暖偏黄。色温+400K，饱和度+0.15，轻微颗粒感，适合日常记录和复古感。",
            keywords = listOf("柯达", "gold", "gold200", "温暖", "偏黄", "复古")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "胶片",
            content = "富士Pro 400H：专业人像负片，已停产但备受喜爱。色温偏冷(-100K)，对比度低(-0.15)，高光呈现独特的青绿色调。",
            keywords = listOf("富士", "pro400h", "人像", "青绿", "高光", "停产")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "胶片",
            content = "Cinestill 800T：电影胶片改制，钨丝灯平衡。色温3200K，高ISO颗粒感，灯光周围有独特的红色光晕(halation)。",
            keywords = listOf("cinestill", "800t", "电影", "钨丝灯", "光晕", "夜景")
        ))

        
        // === 黑白胶片 ===
        knowledgeItems.add(KnowledgeItem(
            category = "黑白",
            content = "Ilford HP5 Plus：经典黑白负片。中等对比度(+0.2)，细腻颗粒，宽容度极佳，适合街拍和纪实。转黑白时保留细节。",
            keywords = listOf("黑白", "ilford", "hp5", "街拍", "纪实", "颗粒")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "黑白",
            content = "Kodak Tri-X 400：传奇黑白胶片。高对比度(+0.3)，明显颗粒感，深黑浓郁，适合高反差场景和人文摄影。",
            keywords = listOf("黑白", "柯达", "trix", "高对比", "人文", "深黑")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "黑白",
            content = "黑白滤镜效果：红色滤镜压暗天空突出云层，黄色滤镜增强对比，绿色滤镜柔化肤色，橙色滤镜平衡效果。",
            keywords = listOf("黑白", "滤镜", "红色", "黄色", "绿色", "天空")
        ))
        
        // === 人像调色 ===
        knowledgeItems.add(KnowledgeItem(
            category = "人像",
            content = "亚洲肤色：色温3800-4200K，色调+5到+10(轻微洋红)，降低橙色饱和度(-0.1)避免过黄，提升橙色明度(+0.1)提亮肤色。",
            keywords = listOf("人像", "亚洲", "肤色", "橙色", "黄色", "提亮")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "人像",
            content = "欧美肤色：色温4000-4500K，色调+3到+8，降低红色饱和度(-0.05)避免过红，保持肤色自然通透。",
            keywords = listOf("人像", "欧美", "白人", "肤色", "红色", "通透")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "人像",
            content = "皮肤柔化：降低清晰度(-0.1到-0.2)，轻微降噪，避免过度锐化。可用低半径高强度的模糊配合蒙版实现磨皮效果。",
            keywords = listOf("人像", "皮肤", "柔化", "磨皮", "清晰度", "降噪")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "人像",
            content = "眼睛增强：局部提升曝光(+0.2)和清晰度(+0.3)，轻微增加饱和度(+0.1)，使眼睛更有神采。注意不要过度。",
            keywords = listOf("人像", "眼睛", "眼神", "增强", "局部", "神采")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "人像",
            content = "嘴唇增强：提升红色饱和度(+0.1到+0.2)和明度(+0.05)，使嘴唇更红润。女性可适当增强，男性保持自然。",
            keywords = listOf("人像", "嘴唇", "红润", "红色", "女性", "男性")
        ))
        
        // === 风光调色 ===
        knowledgeItems.add(KnowledgeItem(
            category = "风光",
            content = "天空增强：降低蓝色明度(-0.2)加深天空，提升蓝色饱和度(+0.2)增强色彩。配合渐变滤镜压暗天空，平衡曝光。",
            keywords = listOf("风光", "天空", "蓝色", "渐变", "压暗", "平衡")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "风光",
            content = "植被增强：提升绿色和黄色饱和度(+0.2)，调整绿色色相偏青(-5到-10)使植被更翠绿，提升黄色明度(+0.1)。",
            keywords = listOf("风光", "植被", "绿色", "黄色", "翠绿", "森林")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "风光",
            content = "日出日落：色温2800-3500K强化暖色，提升橙色和红色饱和度(+0.3)，降低蓝色饱和度(-0.2)，增加对比度(+0.2)。",
            keywords = listOf("风光", "日出", "日落", "金色", "暖色", "橙色")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "风光",
            content = "海景调色：提升青色和蓝色饱和度(+0.25)，调整青色色相偏蓝(+5到+10)使海水更蓝，增加清晰度(+0.3)突出细节。",
            keywords = listOf("风光", "海景", "海水", "青色", "蓝色", "清晰")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "风光",
            content = "雪景调色：提升曝光(+0.3到+0.5)恢复雪的亮度，降低色温(-200K)保持冷感，轻微提升蓝色饱和度(+0.1)。",
            keywords = listOf("风光", "雪景", "冬天", "白色", "冷色", "蓝色")
        ))
        
        // === 特殊场景 ===
        knowledgeItems.add(KnowledgeItem(
            category = "夜景",
            content = "城市夜景：提升阴影(+0.3)显现暗部，降低高光(-0.4)保留灯光，增加饱和度(+0.2)增强霓虹灯色彩，降噪(0.3-0.5)。",
            keywords = listOf("夜景", "城市", "霓虹", "灯光", "暗部", "降噪")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "夜景",
            content = "星空银河：大幅提升阴影(+0.5到+0.7)显现银河，降低高光(-0.3)压制星点溢出，提升蓝色和青色饱和度(+0.3)。",
            keywords = listOf("夜景", "星空", "银河", "天文", "星点", "深空")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "建筑",
            content = "建筑摄影：确保垂直线条笔直，提升清晰度(+0.3)突出细节，适中对比度(+0.1)，降低饱和度(-0.1)保持专业感。",
            keywords = listOf("建筑", "垂直", "透视", "清晰", "专业", "细节")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "美食",
            content = "美食摄影：温暖色温(+200到+400K)增加食欲，提升橙色和红色饱和度(+0.2)，增加自然饱和度(+0.2)，轻微提亮(+0.2)。",
            keywords = listOf("美食", "食物", "暖色", "食欲", "橙色", "红色")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "街拍",
            content = "街头摄影：适中对比度(+0.15)增强视觉冲击，保持自然色彩，增加清晰度(+0.2)，可添加轻微暗角(-10到-15)聚焦主体。",
            keywords = listOf("街拍", "街头", "纪实", "对比", "暗角", "主体")
        ))

        
        // === 风格化调色 ===
        knowledgeItems.add(KnowledgeItem(
            category = "风格",
            content = "日系清新：提高曝光(+0.3到+0.5)，降低对比度(-0.2到-0.3)，降低饱和度(-0.1到-0.2)，色温+200K，营造通透轻盈感。",
            keywords = listOf("日系", "清新", "通透", "小清新", "轻盈", "明亮")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "风格",
            content = "电影感调色：降低高光(-0.3到-0.4)，提升黑色(+0.2到+0.3)制造灰雾感，使用S曲线增加中间调对比，色温偏冷或偏暖营造氛围。",
            keywords = listOf("电影", "cinematic", "灰雾", "氛围", "S曲线", "中间调")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "风格",
            content = "赛博朋克：高饱和度(+0.4)，高对比度(+0.3)，强化青色和洋红色，降低黄色和绿色，添加蓝色和洋红色分离。",
            keywords = listOf("赛博朋克", "cyberpunk", "霓虹", "青色", "洋红", "未来")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "风格",
            content = "复古胶片：降低对比度(-0.1)，提升黑色(+0.15)制造褪色感，降低饱和度(-0.15)，添加颗粒(3-5%)，色温偏暖(+200K)。",
            keywords = listOf("复古", "vintage", "褪色", "怀旧", "颗粒", "老照片")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "风格",
            content = "INS风格：提高曝光(+0.2)，降低对比度(-0.15)，提升阴影(+0.3)，降低高光(-0.2)，增加自然饱和度(+0.2)，色温偏暖。",
            keywords = listOf("ins", "instagram", "社交", "滤镜", "流行", "网红")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "风格",
            content = "莫兰迪色：大幅降低饱和度(-0.3到-0.4)，降低对比度(-0.2)，提升黑色(+0.2)，色调偏灰，营造高级克制的色彩。",
            keywords = listOf("莫兰迪", "morandi", "高级", "克制", "灰调", "性冷淡")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "风格",
            content = "暗黑哥特：降低曝光(-0.2到-0.3)，提高对比度(+0.3)，降低饱和度(-0.2)，强化黑色和深色调，色温偏冷(-300K)。",
            keywords = listOf("暗黑", "哥特", "gothic", "黑暗", "冷色", "神秘")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "风格",
            content = "糖果色：高饱和度(+0.4到+0.5)，提高曝光(+0.2)，降低对比度(-0.1)，强化粉色、蓝色、黄色等明快色彩。",
            keywords = listOf("糖果", "candy", "明快", "鲜艳", "少女", "甜美")
        ))
        
        // === 高级技巧 ===
        knowledgeItems.add(KnowledgeItem(
            category = "高级",
            content = "色彩分离：在高光和阴影中添加不同色调。常见组合：高光偏暖(橙黄)阴影偏冷(青蓝)，或高光偏冷阴影偏暖，营造电影感。",
            keywords = listOf("色彩分离", "split toning", "高光", "阴影", "电影", "色调")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "高级",
            content = "曲线调整：RGB曲线控制整体对比和亮度，单色曲线可精确调整特定颜色。S曲线增加对比，反S曲线降低对比。",
            keywords = listOf("曲线", "curve", "RGB", "S曲线", "对比", "精确")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "高级",
            content = "局部调整：使用渐变滤镜、径向滤镜或画笔工具对特定区域调整。常用于压暗天空、提亮主体、局部调色等。",
            keywords = listOf("局部", "渐变", "径向", "画笔", "蒙版", "选择性")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "高级",
            content = "色彩校准：微调相机色彩配置文件，调整红绿蓝三原色的色相和饱和度，可实现独特的色彩风格。",
            keywords = listOf("色彩校准", "calibration", "配置文件", "三原色", "微调")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "高级",
            content = "锐化技巧：半径0.8-1.2适合细节，2.0-3.0适合边缘。蒙版值80-90可避免锐化平滑区域。过度锐化会产生光晕。",
            keywords = listOf("锐化", "sharpening", "半径", "蒙版", "细节", "光晕")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "高级",
            content = "降噪策略：明度降噪去除亮度噪点，色彩降噪去除彩色噪点。高ISO照片建议明度40-60，色彩30-50。过度降噪损失细节。",
            keywords = listOf("降噪", "noise reduction", "明度", "色彩", "ISO", "细节")
        ))
        
        // === 工作流程 ===
        knowledgeItems.add(KnowledgeItem(
            category = "工作流",
            content = "标准调色流程：1.裁剪构图 2.调整曝光 3.恢复高光阴影 4.调整白平衡 5.调整对比度 6.调整色彩 7.局部调整 8.锐化降噪 9.最终微调。",
            keywords = listOf("工作流", "流程", "步骤", "顺序", "标准", "调色")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "工作流",
            content = "非破坏性编辑：使用RAW格式保留最大信息，所有调整保存为参数而非修改原图，可随时回退或重新调整。",
            keywords = listOf("非破坏", "RAW", "参数", "回退", "可逆", "保护")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "工作流",
            content = "预设使用：预设是参数组合的快照，可快速应用风格。但应根据具体照片微调，不要完全依赖预设。",
            keywords = listOf("预设", "preset", "快照", "风格", "微调", "模板")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "工作流",
            content = "批量处理：对同一场景的多张照片，调整好一张后同步设置到其他照片，再单独微调。提高效率保持一致性。",
            keywords = listOf("批量", "同步", "效率", "一致性", "场景", "系列")
        ))
        
        // === 常见问题 ===
        knowledgeItems.add(KnowledgeItem(
            category = "问题",
            content = "照片发灰：原因是对比度不足或黑色不够深。解决：提高对比度(+0.2)，降低黑色(-0.2)，使用S曲线增加反差。",
            keywords = listOf("发灰", "灰蒙蒙", "对比度", "黑色", "反差", "解决")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "问题",
            content = "颜色不自然：原因是饱和度过高或色温不准。解决：降低饱和度，调整白平衡，使用自然饱和度代替饱和度。",
            keywords = listOf("不自然", "过饱和", "艳丽", "假", "白平衡", "解决")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "问题",
            content = "细节丢失：原因是过度降噪或锐化不足。解决：降低降噪强度，适当增加锐化，提升清晰度(+0.1到+0.2)。",
            keywords = listOf("细节", "模糊", "丢失", "降噪", "锐化", "解决")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "问题",
            content = "肤色偏色：黄色偏色降低色温和橙色饱和度，红色偏色降低红色饱和度，绿色偏色增加色调(洋红)。",
            keywords = listOf("肤色", "偏色", "黄", "红", "绿", "人像", "解决")
        ))
        
        knowledgeItems.add(KnowledgeItem(
            category = "问题",
            content = "噪点过多：高ISO拍摄的照片噪点明显。解决：使用明度降噪(40-60)，保留细节降噪，适当降低清晰度，添加轻微颗粒模拟胶片。",
            keywords = listOf("噪点", "噪声", "高ISO", "降噪", "颗粒", "解决")
        ))
    }
}

data class KnowledgeItem(
    val category: String,
    val content: String,
    val keywords: List<String>
)
