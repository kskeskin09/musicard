/**
 * Musicard (C) 2026, based on Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.importer

import org.junit.Assert.assertTrue
import org.junit.Test

class ImportMatcherTest {
    private fun score(wantTitle: String, wantArtist: String, gotTitle: String, vararg gotArtists: String) =
        ImportMatcher.score(wantTitle, listOf(wantArtist), gotTitle, gotArtists.toList())

    @Test
    fun `same song counts as sure`() {
        assertTrue(score("Blinding Lights", "The Weeknd", "Blinding Lights", "The Weeknd") >= ImportMatcher.SURE)
    }

    @Test
    fun `turkish letters and spelling without them match`() {
        assertTrue(score("Şımarık", "Tarkan", "Simarik", "Tarkan") >= ImportMatcher.SURE)
    }

    @Test
    fun `remaster and feat tags do not lower the match`() {
        assertTrue(score("Here Comes The Sun - Remastered 2009", "The Beatles", "Here Comes The Sun (2019 Mix)", "The Beatles") >= ImportMatcher.SURE)
        assertTrue(score("One Kiss (with Dua Lipa)", "Calvin Harris", "One Kiss", "Calvin Harris") >= ImportMatcher.SURE)
    }

    @Test
    fun `live remix and cover versions are not sure`() {
        assertTrue(score("Blinding Lights", "The Weeknd", "Blinding Lights (Live)", "The Weeknd") < ImportMatcher.SURE)
        assertTrue(score("Levitating", "Dua Lipa", "Levitating (Remix)", "Dua Lipa") < ImportMatcher.SURE)
        assertTrue(score("Gülümse", "Sezen Aksu", "Gülümse (Canlı)", "Sezen Aksu") < ImportMatcher.SURE)
    }

    @Test
    fun `live source keeps its live result`() {
        assertTrue(score("Bohemian Rhapsody - Live Aid", "Queen", "Bohemian Rhapsody (Live Aid)", "Queen") >= ImportMatcher.SURE)
    }

    @Test
    fun `other artist is not sure and unrelated song is not kept`() {
        assertTrue(score("Hello", "Adele", "Hello", "Lionel Richie") < ImportMatcher.SURE)
        assertTrue(score("Something", "Someone", "Totally Different", "Other") <= 0)
    }
}
