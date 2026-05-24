package com.endiar.kuropomodoro.ui.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object KuroType {
    /** Timer countdown digit */
    val displayTimer = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold)

    /** Screen title, e.g. "Tasks" */
    val headingScreen = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)

    /** Card section title */
    val headingCard = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

    /** Main list item text */
    val bodyPrimary = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)

    /** Supporting / subtitle text */
    val bodySecondary = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal)

    /** Chip and pill labels */
    val labelStrong = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

    /** Dock tab labels, badges */
    val labelMeta = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
}
