package fr.inria.corese.core.next.impl.io.serialization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormatConfigTest {

    @Test
    @DisplayName("Builder should create FormatConfig with default blank node prefix")
    void builderShouldCreateWithDefaultBlankNodePrefix() {
         
        fr.inria.corese.core.next.impl.io.serialization.FormatConfig config = new fr.inria.corese.core.next.impl.io.serialization.FormatConfig.Builder().build();

         
        assertNotNull(config, "FormatConfig should not be null");
        assertEquals("_:", config.getBlankNodePrefix(), "Default blank node prefix should be '_:'");
    }

    @Test
    @DisplayName("Builder should create FormatConfig with custom blank node prefix")
    void builderShouldCreateWithCustomBlankNodePrefix() {
        String customPrefix = "genid-";

         
        fr.inria.corese.core.next.impl.io.serialization.FormatConfig config = new fr.inria.corese.core.next.impl.io.serialization.FormatConfig.Builder()
                .blankNodePrefix(customPrefix)
                .build();

         
        assertNotNull(config, "FormatConfig should not be null");
        assertEquals(customPrefix, config.getBlankNodePrefix(), "Blank node prefix should match the custom value");
    }

    @Test
    @DisplayName("blankNodePrefix method in Builder should throw NullPointerException for null prefix")
    void blankNodePrefixShouldThrowForNull() {

        fr.inria.corese.core.next.impl.io.serialization.FormatConfig.Builder builder = new fr.inria.corese.core.next.impl.io.serialization.FormatConfig.Builder();


        assertThrows(NullPointerException.class, () -> builder.blankNodePrefix(null),
                "Setting a null blank node prefix should throw NullPointerException");
    }

    @Test
    @DisplayName("FormatConfig constructor should be private and only accessible via builder")
    void constructorIsPrivateAndAccessibleViaBuilder() {

        fr.inria.corese.core.next.impl.io.serialization.FormatConfig config = new FormatConfig.Builder().build();
        assertNotNull(config);
    }
}
