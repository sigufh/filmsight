package com.filmtracker.app.processing

import com.filmtracker.app.data.BasicAdjustmentParams
import net.jqwik.api.*
import net.jqwik.api.constraints.FloatRange
import org.junit.Assert.*

/**
 * IncrementalRenderingEngine 的属性测试
 * 
 * **Property: 增量计算等价性**
 * *For any* 参数变化，增量计算的结果应与完整重新计算的结果相同
 * 
 * **Validates: Requirements 4.1, 4.4**
 * 
 * Feature: modular-incremental-rendering, Property 2: Incremental computation equivalence
 * 
 * 注意：由于 Android Bitmap 在单元测试中不可用，此测试验证增量计算的逻辑正确性，
 * 包括：
 * 1. 处理计划的正确性
 * 2. 参数变化检测的一致性
 * 3. 阶段执行顺序的正确性
 */
class IncrementalRenderingEnginePropertyTest {
    
    private val changeDetector = ParameterChangeDetector()
    
    // ==================== Arbitrary Generators ====================
    
    /**
     * 生成随机的 BasicAdjustmentParams
     */
    @Provide
    fun basicAdjustmentParams(): Arbitrary<BasicAdjustmentParams> {
        val toneBaseArb = Combinators.combine(
            Arbitraries.floats().between(-5f, 5f),
            Arbitraries.floats().between(0.5f, 2f),
            Arbitraries.floats().between(-100f, 100f),
            Arbitraries.floats().between(-100f, 100f),
            Arbitraries.floats().between(-100f, 100f),
            Arbitraries.floats().between(-100f, 100f)
        ).`as` { exp, con, hi, sh, wh, bl -> listOf(exp, con, hi, sh, wh, bl) }
        
        val otherArb = Combinators.combine(
            Arbitraries.floats().between(-100f, 100f),
            Arbitraries.floats().between(-100f, 100f),
            Arbitraries.floats().between(0f, 2f),
            Arbitraries.floats().between(-100f, 100f),
            Arbitraries.floats().between(-100f, 100f),
            Arbitraries.floats().between(-100f, 100f),
            Arbitraries.floats().between(0f, 100f),
            Arbitraries.floats().between(0f, 100f)
        ).`as` { temp: Float, tint: Float, sat: Float, vib: Float, cla: Float, tex: Float, sharp: Float, noise: Float ->
            listOf(temp, tint, sat, vib, cla, tex, sharp, noise)
        }
        
        return Combinators.combine(toneBaseArb, otherArb)
            .`as` { toneBase, other ->
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
     * Property 1: 处理计划应该覆盖所有受影响的阶段
     * 
     * *For any* 两组参数，处理计划中的 stagesToExecute 应该包含所有受影响的阶段
     */
    @Property(tries = 100)
    fun `processing plan should cover all affected stages`(
        @ForAll("basicAdjustmentParams") oldParams: BasicAdjustmentParams,
        @ForAll("basicAdjustmentParams") newParams: BasicAdjustmentParams
    ) {
        val changedParams = StageParameterMapping.detectChangedParameters(oldParams, newParams)
        val plan = ProcessingPlan.incrementalProcessing(changedParams)
        
        if (changedParams.isEmpty()) {
            assertTrue("No stages to execute when no changes", plan.stagesToExecute.isEmpty())
            return
        }
        
        // 所有受影响的阶段都应该在执行计划中
        val affectedStages = changedParams.map { StageParameterMapping.getStageForParameter(it) }.toSet()
        affectedStages.forEach { stage ->
            assertTrue(
                "Affected stage $stage should be in execution plan",
                plan.stagesToExecute.contains(stage)
            )
        }
    }

    
    /**
     * Property 2: 增量计算和完整计算应该产生相同的处理计划（对于相同的参数变化）
     * 
     * *For any* 参数变化，从相同起始状态开始的增量计算应该执行相同的阶段
     */
    @Property(tries = 100)
    fun `incremental and full compute should have consistent stage coverage`(
        @ForAll("basicAdjustmentParams") params: BasicAdjustmentParams
    ) {
        // 完整计算应该执行所有阶段
        val fullPlan = ProcessingPlan.fullProcessing()
        assertEquals("Full processing should execute all 5 stages", 5, fullPlan.stagesToExecute.size)
        assertEquals("Full processing should start from TONE_BASE", 
            ProcessingStage.TONE_BASE, fullPlan.startStage)
        
        // 增量计算（从 null 开始）也应该执行所有阶段
        val changedParams = StageParameterMapping.detectChangedParameters(null, params)
        val incrementalPlan = ProcessingPlan.incrementalProcessing(changedParams)
        
        assertEquals("Incremental from null should execute all stages",
            fullPlan.stagesToExecute.size, incrementalPlan.stagesToExecute.size)
    }
    
    /**
     * Property 3: 阶段执行顺序应该保持一致
     * 
     * *For any* 处理计划，阶段应该按照 order 顺序执行
     */
    @Property(tries = 100)
    fun `stage execution order should be consistent`(
        @ForAll("basicAdjustmentParams") oldParams: BasicAdjustmentParams,
        @ForAll("basicAdjustmentParams") newParams: BasicAdjustmentParams
    ) {
        val changedParams = StageParameterMapping.detectChangedParameters(oldParams, newParams)
        val plan = ProcessingPlan.incrementalProcessing(changedParams)
        
        // 验证阶段按顺序排列
        for (i in 0 until plan.stagesToExecute.size - 1) {
            val current = plan.stagesToExecute[i]
            val next = plan.stagesToExecute[i + 1]
            assertTrue(
                "Stage ${current.name} (order ${current.order}) should come before ${next.name} (order ${next.order})",
                current.order < next.order
            )
        }
    }
    
    /**
     * Property 4: 跳过的阶段和执行的阶段应该互补
     * 
     * *For any* 处理计划，getSkippedStages() + stagesToExecute 应该等于所有阶段
     */
    @Property(tries = 100)
    fun `skipped and executed stages should be complementary`(
        @ForAll("basicAdjustmentParams") oldParams: BasicAdjustmentParams,
        @ForAll("basicAdjustmentParams") newParams: BasicAdjustmentParams
    ) {
        val changedParams = StageParameterMapping.detectChangedParameters(oldParams, newParams)
        val plan = ProcessingPlan.incrementalProcessing(changedParams)
        
        if (!plan.hasWork()) return
        
        val skipped = plan.getSkippedStages()
        val executed = plan.stagesToExecute
        
        // 不应该有交集
        val intersection = skipped.intersect(executed.toSet())
        assertTrue("Skipped and executed stages should not overlap", intersection.isEmpty())
        
        // 并集应该等于所有阶段
        val union = (skipped + executed).toSet()
        assertEquals("Union should equal all stages", ProcessingStage.entries.toSet(), union)
    }

    
    /**
     * Property 5: 预估时间应该与执行阶段数量成正比
     * 
     * *For any* 处理计划，预估时间应该等于所有执行阶段的预估时间之和
     */
    @Property(tries = 100)
    fun `estimated time should match sum of stage times`(
        @ForAll("basicAdjustmentParams") oldParams: BasicAdjustmentParams,
        @ForAll("basicAdjustmentParams") newParams: BasicAdjustmentParams
    ) {
        val changedParams = StageParameterMapping.detectChangedParameters(oldParams, newParams)
        val plan = ProcessingPlan.incrementalProcessing(changedParams)
        
        val expectedTime = plan.stagesToExecute.sumOf { 
            StageConfig.getConfig(it).estimatedTimeMs 
        }
        
        assertEquals("Estimated time should match sum of stage times",
            expectedTime, plan.estimatedTotalTimeMs)
    }
    
    /**
     * Property 6: 缓存阶段应该只包含 shouldCache=true 的阶段
     * 
     * *For any* 处理计划，stagesToCache 应该只包含需要缓存的阶段
     */
    @Property(tries = 100)
    fun `cache stages should only include cacheable stages`(
        @ForAll("basicAdjustmentParams") oldParams: BasicAdjustmentParams,
        @ForAll("basicAdjustmentParams") newParams: BasicAdjustmentParams
    ) {
        val changedParams = StageParameterMapping.detectChangedParameters(oldParams, newParams)
        val plan = ProcessingPlan.incrementalProcessing(changedParams)
        
        plan.stagesToCache.forEach { stage ->
            assertTrue(
                "Stage $stage in stagesToCache should have shouldCache=true",
                stage.shouldCache
            )
        }
        
        // 所有需要缓存且在执行列表中的阶段都应该在 stagesToCache 中
        plan.stagesToExecute.filter { it.shouldCache }.forEach { stage ->
            assertTrue(
                "Cacheable stage $stage in execution list should be in stagesToCache",
                plan.stagesToCache.contains(stage)
            )
        }
    }
    
    /**
     * Property 7: 参数哈希应该对相同参数产生相同结果
     * 
     * *For any* 参数和阶段，相同参数应该产生相同的哈希值
     */
    @Property(tries = 100)
    fun `parameter hash should be deterministic`(
        @ForAll("basicAdjustmentParams") params: BasicAdjustmentParams
    ) {
        ProcessingStage.entries.forEach { stage ->
            val hash1 = ParameterHasher.hashParameters(stage, params)
            val hash2 = ParameterHasher.hashParameters(stage, params)
            
            assertEquals(
                "Hash for stage $stage should be deterministic",
                hash1, hash2
            )
        }
    }
    
    /**
     * Property 8: 不同参数应该产生不同的哈希值（大概率）
     * 
     * *For any* 两组不同参数，它们的哈希值应该不同（除非碰撞）
     */
    @Property(tries = 100)
    fun `different parameters should produce different hashes`(
        @ForAll("basicAdjustmentParams") params1: BasicAdjustmentParams,
        @ForAll("basicAdjustmentParams") params2: BasicAdjustmentParams
    ) {
        // 如果参数相同，跳过测试
        if (params1 == params2) return
        
        // 检查至少有一个阶段的哈希不同
        val anyDifferent = ProcessingStage.entries.any { stage ->
            val hash1 = ParameterHasher.hashParameters(stage, params1)
            val hash2 = ParameterHasher.hashParameters(stage, params2)
            hash1 != hash2
        }
        
        assertTrue(
            "Different parameters should produce at least one different hash",
            anyDifferent
        )
    }

    
    /**
     * Property 9: 增量计算的起始阶段应该与参数变化检测一致
     * 
     * *For any* 参数变化，处理计划的起始阶段应该与变化检测结果一致
     */
    @Property(tries = 100)
    fun `incremental start stage should match change detection`(
        @ForAll("basicAdjustmentParams") oldParams: BasicAdjustmentParams,
        @ForAll("basicAdjustmentParams") newParams: BasicAdjustmentParams
    ) {
        val changeResult = changeDetector.detectChanges(oldParams, newParams)
        val changedParams = StageParameterMapping.detectChangedParameters(oldParams, newParams)
        val plan = ProcessingPlan.incrementalProcessing(changedParams)
        
        if (changeResult.hasChanges) {
            assertEquals(
                "Plan start stage should match change detection start stage",
                changeResult.startStage, plan.startStage
            )
        }
    }
    
    /**
     * Property 10: 单阶段参数变化应该只从该阶段开始重新计算
     * 
     * 测试单个阶段的参数变化时的增量计算行为
     */
    @Property(tries = 100)
    fun `single stage parameter change should start from that stage`(
        @ForAll("basicAdjustmentParams") baseParams: BasicAdjustmentParams,
        @ForAll @FloatRange(min = 0f, max = 100f) newSharpening: Float
    ) {
        // 只改变 DETAILS 阶段的参数
        val modifiedParams = baseParams.copy(sharpening = newSharpening)
        
        if (baseParams.sharpening == newSharpening) return
        
        val changedParams = StageParameterMapping.detectChangedParameters(baseParams, modifiedParams)
        val plan = ProcessingPlan.incrementalProcessing(changedParams)
        
        assertEquals(
            "Changing DETAILS param should start from DETAILS stage",
            ProcessingStage.DETAILS, plan.startStage
        )
        
        // 只有 DETAILS 阶段需要执行
        assertEquals("Only DETAILS stage should execute", 1, plan.stagesToExecute.size)
        assertTrue(plan.stagesToExecute.contains(ProcessingStage.DETAILS))
        
        // 前面的阶段应该被跳过
        val skipped = plan.getSkippedStages()
        assertTrue(skipped.contains(ProcessingStage.TONE_BASE))
        assertTrue(skipped.contains(ProcessingStage.CURVES))
        assertTrue(skipped.contains(ProcessingStage.COLOR))
        assertTrue(skipped.contains(ProcessingStage.EFFECTS))
    }
    
    /**
     * Property 11: 多阶段参数变化应该从最早的阶段开始
     * 
     * 测试多个阶段的参数同时变化时的增量计算行为
     */
    @Property(tries = 100)
    fun `multi stage parameter change should start from earliest`(
        @ForAll("basicAdjustmentParams") baseParams: BasicAdjustmentParams,
        @ForAll @FloatRange(min = -5f, max = 5f) newExposure: Float,
        @ForAll @FloatRange(min = 0f, max = 100f) newSharpening: Float
    ) {
        // 同时改变 TONE_BASE 和 DETAILS 阶段的参数
        val modifiedParams = baseParams.copy(
            globalExposure = newExposure,
            sharpening = newSharpening
        )
        
        if (baseParams.globalExposure == newExposure && baseParams.sharpening == newSharpening) return
        
        val changedParams = StageParameterMapping.detectChangedParameters(baseParams, modifiedParams)
        val plan = ProcessingPlan.incrementalProcessing(changedParams)
        
        // 如果只有一个参数变化，检查对应的阶段
        if (baseParams.globalExposure != newExposure) {
            assertEquals(
                "Should start from TONE_BASE when exposure changes",
                ProcessingStage.TONE_BASE, plan.startStage
            )
            // 所有阶段都应该执行
            assertEquals("All stages should execute", 5, plan.stagesToExecute.size)
        } else if (baseParams.sharpening != newSharpening) {
            assertEquals(
                "Should start from DETAILS when only sharpening changes",
                ProcessingStage.DETAILS, plan.startStage
            )
        }
    }
}
