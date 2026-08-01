//
//  HomeSceneTests.swift
//  PurrodoroTests
//
//  Mirrors Android HomeSceneTest + FocusModeMotionTest.
//

import Foundation
import Testing
@testable import Purrodoro

@Suite struct HomeSceneTests {
    @Test func resolveRoomDay() {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(secondsFromGMT: 0)!
        let date = cal.date(from: DateComponents(year: 2026, month: 6, day: 1, hour: 10))!
        #expect(
            resolveHomeHeroImageName(scene: .room, isDarkTheme: false, date: date, calendar: cal)
            == "CatHomeHeaderDay"
        )
    }

    @Test func resolveCafeNightWhenDark() {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(secondsFromGMT: 0)!
        let date = cal.date(from: DateComponents(year: 2026, month: 6, day: 1, hour: 10))!
        #expect(
            resolveHomeHeroImageName(scene: .cafe, isDarkTheme: true, date: date, calendar: cal)
            == "CatHomeCafeNight"
        )
    }

    @Test func resolveLibraryDay() {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(secondsFromGMT: 0)!
        let date = cal.date(from: DateComponents(year: 2026, month: 6, day: 1, hour: 10))!
        #expect(
            resolveHomeHeroImageName(scene: .library, isDarkTheme: false, date: date, calendar: cal)
            == "CatHomeLibraryDay"
        )
    }

    @Test func resolveParkNight() {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(secondsFromGMT: 0)!
        let date = cal.date(from: DateComponents(year: 2026, month: 6, day: 1, hour: 10))!
        #expect(
            resolveHomeHeroImageName(scene: .park, isDarkTheme: true, date: date, calendar: cal)
            == "CatHomeParkNight"
        )
    }

    @Test func nightByLocalHour() {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(secondsFromGMT: 0)!
        let evening = cal.date(from: DateComponents(year: 2026, month: 6, day: 1, hour: 20))!
        #expect(isHomeHeroNightTime(isDarkTheme: false, date: evening, calendar: cal))
        let morning = cal.date(from: DateComponents(year: 2026, month: 6, day: 1, hour: 8))!
        #expect(!isHomeHeroNightTime(isDarkTheme: false, date: morning, calendar: cal))
    }

    @Test func effectiveHomeSceneFallsBackForFreeUser() {
        #expect(effectiveHomeScene(selected: .cafe, isPremium: false) == .room)
        #expect(effectiveHomeScene(selected: .cafe, isPremium: true) == .cafe)
        #expect(effectiveHomeScene(selected: .room, isPremium: false) == .room)
    }

    @Test func canSelectHomeScene() {
        #expect(canSelectHomeScene(.room, isPremium: false))
        #expect(!canSelectHomeScene(.cafe, isPremium: false))
        #expect(canSelectHomeScene(.cafe, isPremium: true))
        #expect(canSelectHomeScene(.library, isPremium: true))
        #expect(canSelectHomeScene(.park, isPremium: true))
    }

    @Test func premiumScenesArePremiumOnly() {
        #expect(HomeScene.cafe.isPremiumOnly)
        #expect(HomeScene.library.isPremiumOnly)
        #expect(HomeScene.park.isPremiumOnly)
        #expect(!HomeScene.room.isPremiumOnly)
    }

    @Test func fromStorageName() {
        #expect(HomeScene.fromStorageName("cafe") == .cafe)
        #expect(HomeScene.fromStorageName("Room") == .room)
        #expect(HomeScene.fromStorageName("Mars") == .room)
        #expect(HomeScene.fromStorageName(nil) == .room)
    }
}

@Suite struct FocusModeMotionTests {
    @Test func enterLongerThanExit() {
        #expect(FocusModeMotion.enterDuration > FocusModeMotion.exitDuration)
    }

    @Test func durationsMatchAndroid() {
        #expect(FocusModeMotion.enterDuration == 0.70)
        #expect(FocusModeMotion.exitDuration == 0.40)
        #expect(FocusModeMotion.breathDelay == 0.80)
        #expect(FocusModeMotion.breathDuration == 1.20)
        #expect(FocusModeMotion.breathScalePeak == 1.02)
        #expect(FocusModeMotion.scrimAlphaDashboard == 0.1)
    }

    @Test func breathWaitsUntilAfterEnter() {
        #expect(FocusModeMotion.breathDelay >= FocusModeMotion.enterDuration)
    }

    @Test func focusSessionInFlight() {
        #expect(focusSessionInFlight(timerState: .playing))
        #expect(focusSessionInFlight(timerState: .paused))
        #expect(!focusSessionInFlight(timerState: .idle))
    }

    @Test func focusModeActiveRequiresBoth() {
        #expect(focusModeActive(isFocusMode: true, timerState: .playing))
        #expect(focusModeActive(isFocusMode: true, timerState: .paused))
        #expect(!focusModeActive(isFocusMode: true, timerState: .idle))
        #expect(!focusModeActive(isFocusMode: false, timerState: .playing))
    }

    @Test func mascotStaysPhoneSizedOnNarrowBand() {
        let size = focusModeMascotMaxSize(bandWidth: 360, bandHeight: 260)
        #expect(abs(size.width - 280) < 0.1)
        #expect(abs(size.height - 200) < 0.1)
    }

    @Test func mascotGrowsOnTabletBand() {
        let size = focusModeMascotMaxSize(bandWidth: 900, bandHeight: 480)
        #expect(size.width > 280)
        #expect(size.height > 200)
        #expect(abs(size.width - 440) < 0.1)
        #expect(abs(size.height - 314.28) < 0.5)
    }

    @Test func mascotRespectsShortBandHeight() {
        let size = focusModeMascotMaxSize(bandWidth: 900, bandHeight: 220)
        #expect(abs(size.width - 280) < 0.1)
        #expect(abs(size.height - 200) < 0.1)
    }
}
