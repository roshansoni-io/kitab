package io.github.roshansoni_io.kitab.feature.reader.epub

import org.junit.Assert.assertEquals
import org.junit.Test

class EpubSessionTest {
    @Test
    fun buildChapterRanges_missingMiddleChapterInheritsPreviousStartWithoutJumping() {
        val ranges =
            buildChapterRanges(
                hrefKeys = listOf("cover.xhtml", "toc.xhtml", "ch1.xhtml", "ch2.xhtml"),
                positionStarts = listOf(0.0, null, 0.05, 0.6),
            )

        assertEquals(4, ranges.size)
        assertEquals(0.0, ranges[1].startProgression, 1e-6)
        assertEquals(0.05, ranges[2].startProgression, 1e-6)
        assertEquals(0.6, ranges[3].startProgression, 1e-6)
        assertEquals(1.0, ranges[3].endProgression, 1e-6)
    }

    @Test
    fun buildChapterRanges_fallsBackEvenlyWhenNoTotalProgressionsExist() {
        val ranges =
            buildChapterRanges(
                hrefKeys = listOf("a.xhtml", "b.xhtml"),
                positionStarts = listOf(null, null),
            )

        assertEquals(0.0, ranges[0].startProgression, 1e-6)
        assertEquals(0.5, ranges[0].endProgression, 1e-6)
        assertEquals(0.5, ranges[1].startProgression, 1e-6)
        assertEquals(1.0, ranges[1].endProgression, 1e-6)
    }

    @Test
    fun smoothChapterFraction_keepsNonFinalChapterBelowNextChapterStart() {
        val lastPageNonFinal =
            smoothChapterFraction(
                pageIndex = 9,
                totalPages = 10,
                isLastChapter = false,
                locatorProgression = 0.9,
            )!!
        val lastPageFinal =
            smoothChapterFraction(
                pageIndex = 9,
                totalPages = 10,
                isLastChapter = true,
                locatorProgression = 0.9,
            )!!
        val singlePage =
            smoothChapterFraction(
                pageIndex = 0,
                totalPages = 1,
                isLastChapter = false,
                locatorProgression = 0.5,
            )!!

        assertEquals(0.9, lastPageNonFinal, 1e-6)
        assertEquals(1.0, lastPageFinal, 1e-6)
        assertEquals(0.0, singlePage, 1e-6)
    }
}
