package io.github.gutsmcd.appguide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {
    private val page = """
        <html><head><meta name="description" property="og:description" content="Private by default. Less &amp; less tracking &#39;here&#39;.">
        </head><body>
        <img src="https://play-lh.googleusercontent.com/ICON=s48" alt="Icon image">
        <img src="https://play-lh.googleusercontent.com/AAA=w526-h296" srcset="x 2x" class="T" alt="Screenshot image" data-screenshot-index="0">
        <img src="https://play-lh.googleusercontent.com/BBB=w526-h296" alt="スクリーンショットの画像" data-screenshot-index="1">
        <img src="https://play-lh.googleusercontent.com/AAA=w526-h296" alt="Screenshot image" data-screenshot-index="0">
        <img src="https://play-lh.googleusercontent.com/CCC=w526" data-screenshot-index="2">
        <img src="https://play-lh.googleusercontent.com/DDD=w526" data-screenshot-index="3">
        <img src="https://play-lh.googleusercontent.com/EEE=w526" data-screenshot-index="4">
        </body></html>
    """.trimIndent()

    @Test fun parsesDescriptionAndScreenshots() {
        val info = PlayParser.parse(page)!!
        assertEquals("Private by default. Less & less tracking 'here'.", info.description)
        assertEquals(listOf(
            "https://play-lh.googleusercontent.com/AAA=w720",
            "https://play-lh.googleusercontent.com/BBB=w720",
            "https://play-lh.googleusercontent.com/CCC=w720",
            "https://play-lh.googleusercontent.com/DDD=w720"), info.screenshots)
    }

    @Test fun noDescriptionMeansNotAnAppPage() {
        assertNull(PlayParser.parse("<html><body>Not found</body></html>"))
    }

    @Test fun playUrlLanguage() {
        assertEquals("https://play.google.com/store/apps/details?id=a.b&hl=ja", PlayParser.playUrl("a.b", "ja"))
        assertEquals("https://play.google.com/store/apps/details?id=a.b&hl=en", PlayParser.playUrl("a.b", "en"))
    }

    @Test fun systemCategoryMapping() {
        assertEquals("games", Categories.fromSystem(0))
        assertEquals("photo", Categories.fromSystem(3))
        assertEquals("travel", Categories.fromSystem(6))
        assertEquals("work", Categories.fromSystem(7))
        assertEquals("other", Categories.fromSystem(-1))
        assertEquals(10, Categories.DEFAULTS.size)
    }

    @Test fun searchMatchesAllWords() {
        assertTrue(Text.matches("", "x"))
        assertTrue(Text.matches("pay bill", "Denki", "pays the electricity bill"))
        assertFalse(Text.matches("pay bus", "Denki", "pays the electricity bill"))
        assertTrue(Text.matches("ＬＩＮＥ".lowercase(), "ＬＩＮＥ"))
    }

    @Test fun sizesAndNames() {
        assertEquals("—", Text.size(0))
        assertEquals("1 KB", Text.size(1000))
        assertEquals("1.5 MB", Text.size(1572864))
        assertEquals("My Cat", Text.cleanCategory("  My   Cat "))
    }
}
