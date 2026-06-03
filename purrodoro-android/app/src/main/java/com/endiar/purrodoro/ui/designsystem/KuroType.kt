package com.endiar.purrodoro.ui.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object KuroType {
    /** Timer countdown digit */
    val displayTimer = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold)

    /** Screen title, e.g. "Tasks" */
    val headingScreen = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)

    /** Large list-screen title (Tasks, Habits) */
    val headingDisplay = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp)

    /** Card section title */
    val headingCard = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

    /** Overview / metric numeric value */
    val metricValue = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)

    /** FAB glyph (+) */
    val fabGlyph = TextStyle(fontSize = 27.sp, fontWeight = FontWeight.SemiBold)

    /** Uppercase field labels in sheets */
    val labelCaption = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)

    /** Stepper center value in detail sheets */
    val stepperValue = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)

    /** Settings overlay title */
    val headingSettings = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold)

    /** Journey metric card icon letter */
    val metricHero = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)

    /** Journey metric card numeric value */
    val metricHeroValue = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Normal)

    /** Journey stat card label, e.g. "This week" */
    val labelMetric = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal)

    /** Journey stat card numeric value */
    val metricValueLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)

    /** Positive delta footnote on metric cards, e.g. "+18% vs last week" */
    val metricDeltaPositive = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

    /** Habit card streak number */
    val habitStreakValue = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold)

    /** Habit emoji tile glyph */
    val habitEmoji = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Normal)

    /** Settings row chevron, calendar nav arrows */
    val chevron = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal)

    val calendarNav = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Normal)

    /** Main list item text */
    val bodyPrimary = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)

    /** Supporting / subtitle text */
    val bodySecondary = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal)

    /** Chip and pill labels */
    val labelStrong = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

    /** Dock tab labels, badges */
    val labelMeta = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
}
