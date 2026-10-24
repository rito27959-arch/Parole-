package app.parole
import app.parole.data.*
import org.junit.Assert.*
import org.junit.Test
class SessionMetricsTest {
    private val question=Question("Pourquoi ce choix ?")
    @Test fun emptySessionDoesNotInventMetrics(){val s=Session("s","p");assertNull(SessionMetrics.wordsPerMinute(s));assertNull(SessionMetrics.mean(s){it.clarity})}
    @Test fun measuredDurationIgnoresTypingOnlyAnswers(){val s=Session("s","p",answers=listOf(Answer("a",question,"Un exemple très simple",10000)));assertEquals(24,SessionMetrics.wordsPerMinute(s))}
    @Test fun clockIsStable(){assertEquals("02:05",SessionMetrics.clock(125000));assertEquals("00:00",SessionMetrics.clock(-100))}
    @Test fun wordCountHandlesWhitespace(){assertEquals(4,SessionMetrics.words("  Une réponse\ntrès claire "));assertEquals(0,SessionMetrics.words("   "))}
    @Test fun missingAssessmentDoesNotBecomeZero(){val s=Session("s","p",answers=listOf(Answer("a",question,"texte",feedback=Feedback(clarity=4)),Answer("b",question,"texte")));assertEquals(4.0,SessionMetrics.mean(s){it.clarity}!!,0.01)}
}
