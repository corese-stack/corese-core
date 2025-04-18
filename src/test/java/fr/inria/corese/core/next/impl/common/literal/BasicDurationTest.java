package fr.inria.corese.core.next.impl.common.literal;

import fr.inria.corese.core.next.impl.exception.IncorrectFormatException;
import fr.inria.corese.core.next.impl.exception.IncorrectOperationException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalTime;
import java.time.Period;
import java.util.Optional;

import static org.junit.Assert.*;

public class BasicDurationTest {

    private static Logger logger = LoggerFactory.getLogger(BasicDurationTest.class);

    @Test
    public void testParse() {
        String positiveExample1 = "P1Y2M3D";
        String positiveExample2 = "PT1H2M3.4S";
        String positiveExample3 = "P1347Y";
        String positiveExample4 = "P1347M";
        String positiveExample5 = "P1Y2MT2H";
        String positiveExample6 = "P0Y1347M";
        String positiveExample7 = "P0Y1347M0D";
        String positiveExample8 = "-P1348M";

        String negativeExample1 = "P-1347M";
        String negativeExample2 = "P1Y2MT";

        BasicDuration durationP1 = BasicDuration.parse(positiveExample1);
        BasicDuration durationP2 = BasicDuration.parse(positiveExample2);
        BasicDuration durationP3 = BasicDuration.parse(positiveExample3);
        BasicDuration durationP4 = BasicDuration.parse(positiveExample4);
        BasicDuration durationP5 = BasicDuration.parse(positiveExample5);
        BasicDuration durationP6 = BasicDuration.parse(positiveExample6);
        BasicDuration durationP7 = BasicDuration.parse(positiveExample7);
        BasicDuration durationP8 = BasicDuration.parse(positiveExample8);

        assertEquals(Period.of(1, 2, 3).toString(), durationP1.getLabel());
        assertEquals(Duration.ofHours(1).plusMinutes(2).plusSeconds(3).plusNanos(400000000).toString(), durationP2.getLabel());
        assertEquals(Period.of(1347, 0, 0).toString(), durationP3.getLabel());
        assertEquals(Period.of(0, 1347, 0).toString(), durationP4.getLabel());
        assertEquals(positiveExample5, durationP5.getLabel());
        assertEquals(Period.of(0, 1347, 0).toString(), durationP6.getLabel());
        assertEquals(Period.of(0, 1347, 0).toString(), durationP7.getLabel());
        assertEquals(positiveExample8, durationP8.getLabel());

        assertThrows(IncorrectFormatException.class, () -> BasicDuration.parse(negativeExample1));
        assertThrows(IncorrectFormatException.class, () -> BasicDuration.parse(negativeExample2));
    }

    @Test
    public void constructorString() {
        BasicDuration basicDuration = BasicDuration.parse("P2DT3H4M");
        assertNotNull(basicDuration);
    }

    @Test
    public void isBNode() {
        BasicDuration basicDuration = BasicDuration.parse("P2DT3H4M");
        assertFalse(basicDuration.isBNode());
    }

    @Test
    public void isIRI() {
        BasicDuration basicDuration = BasicDuration.parse("P2DT3H4M");
        assertFalse(basicDuration.isIRI());
    }

    @Test
    public void isResource() {
        BasicDuration basicDuration = BasicDuration.parse("P2DT3H4M");
        assertFalse(basicDuration.isResource());
    }

    @Test
    public void isLiteral() {
        BasicDuration basicDuration = BasicDuration.parse("P2DT3H4M");
        assertTrue(basicDuration.isLiteral());
    }

    @Test
    public void isTriple() {
        BasicDuration basicDuration = BasicDuration.parse("P2DT3H4M");
        assertFalse(basicDuration.isTriple());
    }

    @Test
    public void stringValue() {
        BasicDuration basicDuration = BasicDuration.parse("P2DT3H4M");
        assertEquals("\"P2DT3H4M\"^^<http://www.w3.org/2001/XMLSchema#duration>", basicDuration.stringValue());
    }

    @Test
    public void getLabel() {
        BasicDuration basicDuration = BasicDuration.parse("P2DT3H4M");
        assertEquals("P2DT3H4M", basicDuration.getLabel());
    }

    @Test
    public void getLanguage() {
        BasicDuration basicDuration = BasicDuration.parse("P2DT3H4M");
        assertEquals(Optional.empty(), basicDuration.getLanguage());
    }

    @Test
    public void getDatatype() {
        BasicDuration basicDuration = BasicDuration.parse("P2DT3H4M");
        assertEquals(XSD.DURATION.getIRI().stringValue(), basicDuration.getDatatype().stringValue());
    }

    @Test
    public void booleanValue() {
        assertThrows(IncorrectOperationException.class, () -> {
            BasicDuration basicDuration = BasicDuration.parse("P2DT3H4M");
            basicDuration.booleanValue();
        });
    }

    @Test
    public void byteValue() {
        assertThrows(IncorrectOperationException.class, () -> {
            BasicDuration basicDuration = BasicDuration.parse("P2DT3H4M");
            basicDuration.byteValue();
        });
    }

    @Test
    public void shortValue() {
        assertThrows(IncorrectOperationException.class, () -> {
            BasicDuration basicDuration = BasicDuration.parse("P2DT3H4M");
            basicDuration.shortValue();
        });
    }

    @Test
    public void intValue() {
        assertThrows(IncorrectOperationException.class, () -> {
            BasicDuration basicDuration = BasicDuration.parse("P2DT3H4M");
            basicDuration.intValue();
        });
    }

    @Test
    public void longValue() {
        assertThrows(IncorrectOperationException.class, () -> {
            BasicDuration basicDuration = BasicDuration.parse("P2DT3H4M");
            basicDuration.longValue();
        });
    }

    @Test
    public void integerValue() {
        assertThrows(IncorrectOperationException.class, () -> {
            BasicDuration basicDuration = BasicDuration.parse("P2DT3H4M");
            basicDuration.integerValue();
        });
    }

    @Test
    public void decimalValue() {
        assertThrows(IncorrectOperationException.class, () -> {
            BasicDuration basicDuration = BasicDuration.parse("P2DT3H4M");
            basicDuration.decimalValue();
        });
    }

    @Test
    public void floatValue() {
        assertThrows(IncorrectOperationException.class, () -> {
            BasicDuration basicDuration = BasicDuration.parse("P2DT3H4M");
            basicDuration.floatValue();
        });
    }

    @Test
    public void doubleValue() {
        assertThrows(IncorrectOperationException.class, () -> {
            BasicDuration basicDuration = BasicDuration.parse("P2DT3H4M");
            basicDuration.doubleValue();
        });
    }

    @Test
    public void temporalAmountValue() {
        BasicDuration basicDuration = BasicDuration.parse("PT2H");
        assertNotNull(basicDuration.temporalAmountValue());
        assertEquals(Duration.ofHours(2).toString(),
                basicDuration.temporalAmountValue().toString());
    }

    @Test
    public void calendarValue() {
        assertThrows(IncorrectOperationException.class, () -> {
            BasicDuration basicDuration = BasicDuration.parse("P2DT3H4M");
            basicDuration.calendarValue();
        });
    }

    @Test
    public void getCoreDatatype() {
        BasicDuration basicDuration = BasicDuration.parse("P2D");
        assertEquals(XSD.DURATION, basicDuration.getCoreDatatype());
    }

    @Test
    public void setCoreDatatype() {
        assertThrows(IncorrectOperationException.class, () -> {
            BasicDuration basicDuration = BasicDuration.parse("P2DT3H4M");
            basicDuration.setCoreDatatype(XSD.INTEGER);
        });
    }

    @Test
    public void testTemporalAmountValueAddition() {
        LocalTime time = LocalTime.of(12,0);
        BasicDuration basicPositiveDuration = BasicDuration.parse("PT3H");
        BasicDuration basicNegativeDuration = BasicDuration.parse("-PT5H30M");
        BasicDuration tooBigDuration = BasicDuration.parse("P3DT6H");
        LocalTime expectedTeaTime = time.plusHours(3);
        LocalTime expectedWakeUpTime = time.minusHours(5).minusMinutes(30);
        LocalTime actualTeaTime = time.plus(basicPositiveDuration.temporalAmountValue());
        LocalTime actualWakeUpTime = time.plus(basicNegativeDuration.temporalAmountValue());

        assertEquals(expectedTeaTime, actualTeaTime);
        assertEquals(expectedWakeUpTime, actualWakeUpTime);
        assertThrows(IncorrectOperationException.class, () -> time.plus(tooBigDuration.temporalAmountValue()));
    }

    @Test
    public void testTemporalAmountValueSubtraction() {
        LocalTime time = LocalTime.of(12,0);
        BasicDuration basicPositiveDuration = BasicDuration.parse("PT5H30M");
        BasicDuration basicNegativeDuration = BasicDuration.parse("-PT3H");
        BasicDuration tooBigDuration = BasicDuration.parse("P3DT6H");
        LocalTime expectedTeaTime = time.plusHours(3);
        LocalTime expectedWakeUpTime = time.minusHours(5).minusMinutes(30);
        LocalTime actualTeaTime = time.minus(basicNegativeDuration.temporalAmountValue());
        LocalTime actualWakeUpTime = time.minus(basicPositiveDuration.temporalAmountValue());

        assertEquals(expectedTeaTime, actualTeaTime);
        assertEquals(expectedWakeUpTime, actualWakeUpTime);
        assertThrows(IncorrectOperationException.class, () -> time.minus(tooBigDuration.temporalAmountValue()));
    }
}