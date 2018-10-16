package com.voysis.sdk
import org.junit.Assert.assertTrue
import com.voysis.generateRFCDate
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class UtilsTest : ClientTest() {

    @Test
    fun testParseRFCToLocalTimeZone() {
        val dateString = getExpiry(25)
        assertTrue(dateString.contains("Z"))
        val date = generateRFCDate(dateString)
        assertFalse(date.toString().contains("UTC"))
    }
}