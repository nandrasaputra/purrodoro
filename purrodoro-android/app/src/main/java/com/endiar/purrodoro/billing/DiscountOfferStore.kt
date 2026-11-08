package com.endiar.purrodoro.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Remembers when the current discount cycle started. The file lives in app
 * files, so Auto Backup can carry the week onto a new phone.
 */
object DiscountOfferStore {
    private const val DIR = "discount-offer"
    private const val FILE = "offered-at.txt"

    fun read(filesDir: File): Long? {
        val file = file(filesDir)
        if (!file.exists()) return null
        return runCatching { file.readText().trim().toLong() }.getOrNull()
    }

    fun write(filesDir: File, offeredAtMillis: Long) {
        val file = file(filesDir)
        file.parentFile?.mkdirs()
        file.writeText(offeredAtMillis.toString())
    }

    private fun file(filesDir: File) = File(File(filesDir, DIR), FILE)
}

class DiscountOfferController(private val filesDir: File) {
    private val _offeredAt = MutableStateFlow(DiscountOfferStore.read(filesDir))
    val offeredAt: StateFlow<Long?> = _offeredAt.asStateFlow()

    /** @return true when this dismiss started a new 24-hour window. */
    fun onPaywallDismissed(nowMillis: Long, discountPriceReady: Boolean): Boolean {
        val previous = _offeredAt.value
        val next = DiscountOffer.onPaywallDismissed(previous, nowMillis, discountPriceReady)
        if (next == null || next == previous) return false
        DiscountOfferStore.write(filesDir, next)
        _offeredAt.value = next
        return true
    }
}
