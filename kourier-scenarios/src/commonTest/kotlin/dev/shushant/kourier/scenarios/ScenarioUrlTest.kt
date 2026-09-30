package dev.shushant.kourier.scenarios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ScenarioUrlTest {
    @Test fun normalizesOriginAndPreservesEncodedSeparators() {
        val parsed = assertNotNull(ScenarioUrl.parse("HTTPS://API.EXAMPLE.:443/a/../charging%2fsessions?b=2&a=1#screen"))
        assertEquals(ScenarioOrigin("https", "api.example", 443), parsed.origin)
        assertEquals("/charging%2Fsessions", parsed.path)
        assertEquals(listOf("b" to "2", "a" to "1"), parsed.query)
        assertEquals(parsed.origin, ScenarioUrl.parse("https://api.example/")?.origin)
        assertEquals("/", ScenarioUrl.parse("http://api.example")?.path)
        assertEquals("/a/", ScenarioUrl.parse("http://api.example/a/.")?.path)
        assertEquals(ScenarioOrigin("http", "::1", 8080), ScenarioUrl.parse("http://[::1]:8080/")?.origin)
    }

    @Test fun queryDecodingPreservesDuplicatesAndUnicode() {
        val parsed = assertNotNull(ScenarioUrl.parse("https://api.example/?id=1&id=2&%74oken=a%2Bb&name=hello+world&unicode=%E2%9C%93&emoji=😀"))
        assertEquals(listOf("id" to "1", "id" to "2", "token" to "a+b", "name" to "hello world", "unicode" to "✓", "emoji" to "😀"), parsed.query)
    }

    @Test fun invalidOrUnsupportedUrlsFailOpen() {
        for (url in listOf("ftp://api.example/a", "relative", "https://user:pass@api.example/", "https://api.example:0/", "https://api.example:65536/", "https://api.example:/", "https://api.example/%XX", "https://api.example/?x=%", "https://api.example/?x=%FF", "https://api example/", "https://api.example\\other/", "https://api.example/" + "x".repeat(2049))) {
            assertNull(ScenarioUrl.parse(url), url)
        }
    }
}
