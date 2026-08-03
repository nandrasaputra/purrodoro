//
//  FocusShieldTests.swift
//  PurrodoroTests
//
//  Ports Android `FocusShieldTest` arming / bypass duration rules.
//

import Foundation
import Testing
@testable import Purrodoro

struct FocusShieldTests {

    @Test func armedOnlyDuringFocusWhenPrefOnSelectionAuthorized() {
        #expect(
            FocusShield.isArmed(
                preferenceEnabled: true,
                hasSelection: true,
                authorized: true,
                timerState: .playing,
                currentIsFocus: true
            )
        )
        #expect(
            FocusShield.isArmed(
                preferenceEnabled: true,
                hasSelection: true,
                authorized: true,
                timerState: .paused,
                currentIsFocus: true
            )
        )
    }

    @Test func notArmedOnBreakEvenIfPlaying() {
        #expect(
            !FocusShield.isArmed(
                preferenceEnabled: true,
                hasSelection: true,
                authorized: true,
                timerState: .playing,
                currentIsFocus: false
            )
        )
    }

    @Test func notArmedWhenPrefOffEmptySelectionUnauthorizedOrIdle() {
        #expect(
            !FocusShield.isArmed(
                preferenceEnabled: false,
                hasSelection: true,
                authorized: true,
                timerState: .playing,
                currentIsFocus: true
            )
        )
        #expect(
            !FocusShield.isArmed(
                preferenceEnabled: true,
                hasSelection: false,
                authorized: true,
                timerState: .playing,
                currentIsFocus: true
            )
        )
        #expect(
            !FocusShield.isArmed(
                preferenceEnabled: true,
                hasSelection: true,
                authorized: false,
                timerState: .playing,
                currentIsFocus: true
            )
        )
        #expect(
            !FocusShield.isArmed(
                preferenceEnabled: true,
                hasSelection: true,
                authorized: true,
                timerState: .idle,
                currentIsFocus: true
            )
        )
    }

    @Test func bypassDurationClampsBetweenOneMinuteAndTwoHours() {
        #expect(FocusShield.bypassDuration(remainingSeconds: 10) == 60)
        #expect(FocusShield.bypassDuration(remainingSeconds: 90) == 90)
        #expect(FocusShield.bypassDuration(remainingSeconds: 10_000) == 2 * 60 * 60)
    }
}
