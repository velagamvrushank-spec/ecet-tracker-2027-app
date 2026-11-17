package com.example

import com.example.data.model.Subject
import com.example.data.model.TopicPriority
import com.example.data.model.TopicStatus
import com.example.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun dateUtilsEpochDayRoundTrip() {
        val day = DateUtils.civilToEpochDay(2027, 5, 18)
        val civil = DateUtils.epochDayToCivil(day)
        assertEquals(2027, civil.year)
        assertEquals(5, civil.month)
        assertEquals(18, civil.day)
    }

    @Test
    fun dateUtilsFormattingAndDurations() {
        assertEquals("25:00", DateUtils.formatDurationHms(25 * 60_000L))
        assertEquals("01:05:09", DateUtils.formatDurationHms((3600 + 300 + 9) * 1000L))
        assertEquals("4h", DateUtils.formatHoursCompact(240))
        assertEquals("2h 30m", DateUtils.formatHoursCompact(150))
        assertEquals(Subject.ECE, Subject.fromName("ECE"))
        assertEquals(TopicStatus.COMPLETED, TopicStatus.IN_PROGRESS.next())
        assertEquals(TopicPriority.HIGH, TopicPriority.fromName("HIGH"))
    }
}
