package app.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AttachmentController utility methods.
 */
class AttachmentControllerTest {

    @Test
    void sanitizeFilename_removesNonAsciiCharacters() {
        AttachmentController controller = new AttachmentController(null, null);
        
        String result = controller.sanitizeFilename("файл.pdf");
        assertFalse(result.contains("ф"));
        assertTrue(result.endsWith(".pdf"));
    }

    @Test
    void sanitizeFilename_removesQuotes() {
        AttachmentController controller = new AttachmentController(null, null);
        
        String result = controller.sanitizeFilename("file\"with\"quotes.pdf");
        assertFalse(result.contains("\""));
    }

    @Test
    void sanitizeFilename_removesNewlinesAndCarriageReturns() {
        AttachmentController controller = new AttachmentController(null, null);
        
        String result = controller.sanitizeFilename("file\nwith\rbreaks.pdf");
        assertFalse(result.contains("\n"));
        assertFalse(result.contains("\r"));
    }

    @Test
    void sanitizeFilename_handlesNullInput() {
        AttachmentController controller = new AttachmentController(null, null);
        
        String result = controller.sanitizeFilename(null);
        assertEquals("download", result);
    }

    @Test
    void sanitizeFilename_preservesValidAsciiCharacters() {
        AttachmentController controller = new AttachmentController(null, null);
        
        String result = controller.sanitizeFilename("valid-file_123.pdf");
        assertEquals("valid-file_123.pdf", result);
    }
}
