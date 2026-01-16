package com.filmtracker.app.processing

import com.filmtracker.app.data.BasicAdjustmentParams
import net.jqwik.api.*
import net.jqwik.api.constraints.FloatRange
import org.junit.Assert.*

/**
 * ParameterChangeDetector 的属性测试
 * 
 * **Property: 参数变化检测正确性**
 * *For any* 两组参数，检测器应正确识别变化的阶段
 * 
 * **Validates: Requirements 3.2, 4.1**
 * 
 * Feature: modular-incremental-rendering, Property 1: Parameter change detection correctness
 */
class ParameterChangeDetectorPropertyTest {
    
    private val detector = ParameterChangeDetector()
    
    // ==================== Arbitrary Generators ====================
    
    /**
     * 生成随机的 BasicAdjustmentParams
     * 使用嵌套 combine 来处理超过 8 个参数的情况
     */
    @Provide
    fun basicAdjustmentParams(): Arbitrary<BasicAdjustmentParams> {
        // 第一组参数 (TONE_BASE)
        val toneBaseArb = Combinators.combine(
            Arbitraries.floats().between(-5f, 5f),      // globalExposure
            Arbitraries.floats().between(0.5f, 2f),    // contrast
            Arbitraries.floats().between(-100f, 100f), // highlights
            Arbitraries.floats().between(-100f, 100f), // shadows
            Arbitraries.floats().between(-100f, 100f), // whites
            Arbitraries.floats().between(-100f, 100f)  // blacks
        ).`as` { exp: Float, con: Float, hi: Float, sh: Float, wh: Float, bl: Float ->
            listOf(exp, con, hi, sh, wh, bl)
        }
        
        // 第二组参数 (COLOR + EFFECTS + DETAILS)
        val otherArb = Combinators.combine(
            Arbitraries.floats().between(-100f, 100f), // temperature
            Arbitraries.floats().between(-100f, 100f), // tint
            Arbitraries.floats().between(0f, 2f),      // saturation
            Arbitraries.floats().between(-100f, 100f), // vibrance
            Arbitraries.floats().between(-100f, 100f), // clarity
            Arbitraries.floats().between(-100f, 100f), // texture
            Arbitraries.floats().between(0f, 100f),    // sharpening
            Arbitraries.floats().between(0f, 100f)     // noiseReduction
        ).`as` { temp: Float, tint: Float, sat: Float, vib: Float, cla: Float, tex: Float, sharp: Float, noise: Float ->
            listOf(temp, tint, sat, vib, cla, tex, sharp, noise)
        }
        
        // 组合两组参数
        return Combinators.combine(toneBaseArb, otherArb)
            .`as` { toneBase: List<Float>, other: List<Float> ->
                BasicAdjustmentParams(
                    globalExposure = toneBase[0],
                    contrast = toneBase[1],
                    highlights = toneBase[2],
                    shadows = toneBase[3],
                    whites = toneBase[4],
                    blacks = toneBase[5],
                    temperature = other[0],
                    tint = other[1],
                    saturation = other[2],
                    vibrance = other[3],
                    clarity = other[4],
                    texture = other[5],
                    sharpening = other[6],
                    noiseReduction = other[7]
                )
            }
    }
    
    // ==================== Property Tests ====================
    
    /**
     * Property 1: 相同参数应该检测为无变化
     * 
     * *For any* 参数集合 params，detectChanges(params, params) 应返回无变化
     */
    @Property(tries = 100)
    fun `identical parameters should detect no changes`(
        @ForAll("basicAdjustmentParams") params: BasicAdjustmentParams
    ) {
        val result = detector.detectChanges(params, params)
        
        assertFalse("Identical params should have no changes", result.hasChanges)
        assertTrue("Changed parameters should be empty", result.changedParameters.isEmpty())
        assertTrue("Affected stages should be empty", result.affectedStages.isEmpty())
        assertNull("Start stage should be null", result.startStage)
        assertTrue("Stages to recompute should be empty", result.stagesToRecompute.isEmpty())
    }
    
    /**
     * Property 2: null 旧参数应该触发完整重新计算
     * 
     * *For any* 参数集合 params，detectChanges(null, params) 应返回完整重新计算
     */
    @Property(tries = 100)
    fun `null old params should trigger full recompute`(
        @ForAll("basicAdjustmentParams") params: BasicAdjustmentParams
    ) {
        val result = detector.detectChanges(null, params)
        
        assertTrue("Should have changes", result.hasChanges)
        assertEquals("Start stage should be TONE_BASE", ProcessingStage.TONE_BASE, result.startStage)
        assertEquals("Should recompute all 5 stages", 5, result.stagesToRecompute.size)
        assertEquals("All parameters should be marked as changed", 
            StageParameterMapping.getAllParameters().size, 
            result.changedParameters.size)
    }
    
    /**
     * Property 3: 检测到的变化参数应该正确映射到阶段
     * 
     * *For any* 两组不同参数，检测到的变化参数应该属于报告的受影响阶段
     */
    @Property(tries = 100)
    fun `changed parameters should map to affected stages`(
        @ForAll("basicAdjustmentParams") oldParams: BasicAdjustmentParams,
        @ForAll("basicAdjustmentParams") newParams: BasicAdjustmentParams
    ) {
        val result = detector.detectChanges(oldParams, newParams)
        
        // 每个变化的参数都应该属于某个受影响的阶段
        result.changedParameters.forEach { param ->
            val paramStage = StageParameterMapping.getStageForParameter(param)
            assertTrue(
                "Changed parameter $param should be in affected stage $paramStage",
                result.affectedStages.contains(paramStage)
            )
        }
    }
    
    /**
     * Property 4: 起始阶段应该是最早受影响的阶段
     * 
     * *For any* 两组参数，起始阶段的 order 应该 <= 所有受影响阶段的 order
     */
    @Property(tries = 100)
    fun `start stage should be earliest affected stage`(
        @ForAll("basicAdjustmentParams") oldParams: BasicAdjustmentParams,
        @ForAll("basicAdjustmentParams") newParams: BasicAdjustmentParams
    ) {
        val result = detector.detectChanges(oldParams, newParams)
        
        if (result.hasChanges && result.startStage != null) {
            result.affectedStages.forEach { stage ->
                assertTrue(
                    "Start stage ${result.startStage} should have order <= ${stage.order}",
                    result.startStage!!.order <= stage.order
                )
            }
        }
    }
    
    /**
     * Property 5: 需要重新计算的阶段应该从起始阶段连续到最后
     * 
     * *For any* 两组参数，stagesToRecompute 应该是从 startStage 到 DETAILS 的连续阶段
     */
    @Property(tries = 100)
    fun `stages to recompute should be contiguous from start to end`(
        @ForAll("basicAdjustmentParams") oldParams: BasicAdjustmentParams,
        @ForAll("basicAdjustmentParams") newParams: BasicAdjustmentParams
    ) {
        val result = detector.detectChanges(oldParams, newParams)
        
        if (result.hasChanges && result.startStage != null) {
            // 验证阶段是连续的
            val expectedStages = ProcessingStage.getOrderedStages()
                .filter { it.order >= result.startStage!!.order }
            
            assertEquals(
                "Stages to recompute should match expected contiguous stages",
                expectedStages,
                result.stagesToRecompute
            )
        }
    }
    
    /**
     * Property 6: 可缓存阶段和需要重新计算阶段应该互补
     * 
     * *For any* 两组参数，getCacheableStages() + stagesToRecompute 应该等于所有阶段
     */
    @Property(tries = 100)
    fun `cacheable and recompute stages should be complementary`(
        @ForAll("basicAdjustmentParams") oldParams: BasicAdjustmentParams,
        @ForAll("basicAdjustmentParams") newParams: BasicAdjustmentParams
    ) {
        val result = detector.detectChanges(oldParams, newParams)
        
        val cacheableStages = result.getCacheableStages()
        val recomputeStages = result.stagesToRecompute
        
        // 两个集合不应该有交集
        val intersection = cacheableStages.intersect(recomputeStages.toSet())
        assertTrue("Cacheable and recompute stages should not overlap", intersection.isEmpty())
        
        // 两个集合的并集应该等于所有阶段
        val union = (cacheableStages + recomputeStages).toSet()
        assertEquals("Union should equal all stages", ProcessingStage.entries.toSet(), union)
    }
    
    /**
     * Property 7: hasAnyChange 应该与 detectChanges.hasChanges 一致
     * 
     * *For any* 两组参数，hasAnyChange 的结果应该与 detectChanges().hasChanges 一致
     */
    @Property(tries = 100)
    fun `hasAnyChange should be consistent with detectChanges`(
        @ForAll("basicAdjustmentParams") oldParams: BasicAdjustmentParams,
        @ForAll("basicAdjustmentParams") newParams: BasicAdjustmentParams
    ) {
        val hasChange = detector.hasAnyChange(oldParams, newParams)
        val result = detector.detectChanges(oldParams, newParams)
        
        assertEquals(
            "hasAnyChange should match detectChanges.hasChanges",
            hasChange,
            result.hasChanges
        )
    }
    
    /**
     * Property 8: 单参数变化应该只影响该参数所属的阶段及后续阶段
     * 
     * 测试单个参数变化时的行为
     */
    @Property(tries = 100)
    fun `single parameter change should affect correct stages`(
        @ForAll("basicAdjustmentParams") baseParams: BasicAdjustmentParams,
        @ForAll @FloatRange(min = -100f, max = 100f) newValue: Float
    ) {
        // 只改变 temperature 参数（属于 COLOR 阶段）
        val modifiedParams = baseParams.copy(temperature = newValue)
        
        // 如果值相同，跳过测试
        if (baseParams.temperature == newValue) return
        
        val result = detector.detectChanges(baseParams, modifiedParams)
        
        assertTrue("Should have changes", result.hasChanges)
        assertTrue(
            "Changed parameters should contain TEMPERATURE",
            result.changedParameters.contains(StageParameterMapping.ParameterName.TEMPERATURE)
        )
        assertEquals(
            "Start stage should be COLOR for temperature change",
            ProcessingStage.COLOR,
            result.startStage
        )
        
        // COLOR 阶段及之后的阶段应该需要重新计算
        assertTrue(result.stagesToRecompute.contains(ProcessingStage.COLOR))
        assertTrue(result.stagesToRecompute.contains(ProcessingStage.EFFECTS))
        assertTrue(result.stagesToRecompute.contains(ProcessingStage.DETAILS))
        
        // TONE_BASE 和 CURVES 不应该需要重新计算
        assertFalse(result.stagesToRecompute.contains(ProcessingStage.TONE_BASE))
        assertFalse(result.stagesToRecompute.contains(ProcessingStage.CURVES))
    }
}
