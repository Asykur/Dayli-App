package com.example

import com.example.core.model.calculateLevelInfo
import com.example.core.model.calculateWeeklyScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase5ConsistencyEngineTest {

    @Test
    fun calculateLevelInfo_zeroXp_isLevelOneNoviceBuilder() {
        val info = calculateLevelInfo(0L)
        assertEquals(1, info.level)
        assertEquals("Novice Builder", info.rankTitle)
        assertEquals(0L, info.currentLevelMinXp)
        assertEquals(100L, info.nextLevelMinXp)
        assertEquals(0L, info.xpInLevel)
        assertEquals(100L, info.xpRequiredForNextLevel)
        assertEquals(0f, info.progressRatio, 0.001f)
    }

    @Test
    fun calculateLevelInfo_levelTwoProgression() {
        // Level 1: 0 - 99
        // Level 2: 100 - 249 (150 required)
        val info = calculateLevelInfo(175L)
        assertEquals(2, info.level)
        assertEquals("Consistent Seeker", info.rankTitle)
        assertEquals(100L, info.currentLevelMinXp)
        assertEquals(250L, info.nextLevelMinXp)
        assertEquals(75L, info.xpInLevel)
        assertEquals(150L, info.xpRequiredForNextLevel)
        assertEquals(0.50f, info.progressRatio, 0.001f)
    }

    @Test
    fun calculateLevelInfo_higherLevelsHaveDistinctRanks() {
        val l3 = calculateLevelInfo(300L)
        assertEquals(3, l3.level)
        assertEquals("Momentum Builder", l3.rankTitle)

        val l4 = calculateLevelInfo(500L)
        assertEquals(4, l4.level)
        assertEquals("Habit Artisan", l4.rankTitle)

        val l5 = calculateLevelInfo(800L)
        assertEquals(5, l5.level)
        assertEquals("Master of Focus", l5.rankTitle)
    }

    @Test
    fun calculateWeeklyScore_deterministicPercentage() {
        assertEquals(0, calculateWeeklyScore(0, 0))
        assertEquals(100, calculateWeeklyScore(14, 14))
        assertEquals(75, calculateWeeklyScore(20, 15))
        assertEquals(50, calculateWeeklyScore(10, 5))
    }
}
