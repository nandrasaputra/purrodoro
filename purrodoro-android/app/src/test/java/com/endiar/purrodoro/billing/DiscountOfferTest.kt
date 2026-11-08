package com.endiar.purrodoro.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscountOfferTest {

    private val start = 1_700_000_000_000L

    @Test
    fun leavingFullPriceStartsA24HourWindowWhenTheDiscountPriceIsReady() {
        val offeredAt = DiscountOffer.onPaywallDismissed(
            offeredAtMillis = null,
            nowMillis = start,
            discountPriceReady = true,
        )
        assertEquals(start, offeredAt)
        assertEquals(DiscountOffer.Phase.Active, DiscountOffer.phase(offeredAt, start))
    }

    @Test
    fun leavingFullPriceDoesNotStartTheClockWithoutADiscountPrice() {
        val offeredAt = DiscountOffer.onPaywallDismissed(
            offeredAtMillis = null,
            nowMillis = start,
            discountPriceReady = false,
        )
        assertNull(offeredAt)
    }

    @Test
    fun dismissDuringTheWindowDoesNotRestartTheClock() {
        val later = start + 23 * 60 * 60 * 1000L
        val offeredAt = DiscountOffer.onPaywallDismissed(
            offeredAtMillis = start,
            nowMillis = later,
            discountPriceReady = true,
        )
        assertEquals(start, offeredAt)
        assertEquals(DiscountOffer.Phase.Active, DiscountOffer.phase(offeredAt, later))
    }

    @Test
    fun after24HoursThePriceIsFullUntilAWeekHasPassed() {
        val afterDay = start + DiscountOffer.ACTIVE_WINDOW_MS
        assertEquals(DiscountOffer.Phase.Cooldown, DiscountOffer.phase(start, afterDay))
        val stillCooling = DiscountOffer.onPaywallDismissed(
            offeredAtMillis = start,
            nowMillis = afterDay,
            discountPriceReady = true,
        )
        assertEquals(start, stillCooling)

        val justBeforeWeek = start + DiscountOffer.CYCLE_MS - 1
        assertEquals(DiscountOffer.Phase.Cooldown, DiscountOffer.phase(start, justBeforeWeek))
    }

    @Test
    fun aWeekAfterTheOfferStartedTheSameLeaveCanStartItAgain() {
        val weekLater = start + DiscountOffer.CYCLE_MS
        assertEquals(DiscountOffer.Phase.Idle, DiscountOffer.phase(start, weekLater))
        val offeredAt = DiscountOffer.onPaywallDismissed(
            offeredAtMillis = start,
            nowMillis = weekLater,
            discountPriceReady = true,
        )
        assertEquals(weekLater, offeredAt)
        assertEquals(DiscountOffer.Phase.Active, DiscountOffer.phase(offeredAt, weekLater))
    }

    @Test
    fun countdownFormatsHoursMinutesAndSeconds() {
        assertEquals("00:01:30", DiscountOffer.formatRemaining(90_000L))
        assertEquals("23:41:08", DiscountOffer.formatRemaining(
            23 * 3_600_000L + 41 * 60_000L + 8_000L,
        ))
        assertEquals("00:00:00", DiscountOffer.formatRemaining(0L))
        assertEquals("00:00:00", DiscountOffer.formatRemaining(-1L))
    }

    @Test
    fun storeRemembersTheOfferStartAndIgnoresACorruptFile() {
        val dir = java.nio.file.Files.createTempDirectory("discount-offer").toFile()
        assertNull(DiscountOfferStore.read(dir))
        DiscountOfferStore.write(dir, start)
        assertEquals(start, DiscountOfferStore.read(dir))
        val file = java.io.File(java.io.File(dir, "discount-offer"), "offered-at.txt")
        file.writeText("nope")
        assertNull(DiscountOfferStore.read(dir))
    }
}
