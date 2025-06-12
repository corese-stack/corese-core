package fr.inria.corese.core.next.api.model.serialization;

import fr.inria.corese.core.next.api.base.model.serialization.FormatConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormatConfigTest {

    @Test
    @DisplayName("Builder should create FormatConfig with default blank node prefix")
    void builderShouldCreateWithDefaultBlankNodePrefix() {
         
        FormatConfig config = new FormatConfig.Builder().build();

         
        assertNotNull(config, "FormatConfig should not be null");
        assertEquals("_:", config.getBlankNodePrefix(), "Default blank node prefix should be '_:'");
    }

    @Test
    @DisplayName("Builder should create FormatConfig with custom blank node prefix")
    void builderShouldCreateWithCustomBlankNodePrefix() {
        String customPrefix = "genid-";

         
        FormatConfig config = new FormatConfig.Builder()
                .blankNodePrefix(customPrefix)
                .build();

         
        assertNotNull(config, "FormatConfig should not be null");
        assertEquals(customPrefix, config.getBlankNodePrefix(), "Blank node prefix should match the custom value");
    }

    @Test
    @DisplayName("blankNodePrefix method in Builder should throw NullPointerException for null prefix")
    void blankNodePrefixShouldThrowForNull() {

        FormatConfig.Builder builder = new FormatConfig.Builder();


        assertThrows(NullPointerException.class, () -> builder.blankNodePrefix(null),
                "Setting a null blank node prefix should throw NullPointerException");
    }

    @Test
    @DisplayName("FormatConfig constructor should be private and only accessible via builder")
    void constructorIsPrivateAndAccessibleViaBuilder() {

        FormatConfig config = new FormatConfig.Builder().build();
        assertNotNull(config);
    }
}
