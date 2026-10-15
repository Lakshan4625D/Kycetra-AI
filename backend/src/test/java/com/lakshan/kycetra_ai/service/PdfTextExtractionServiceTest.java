
package com.lakshan.kycetra_ai.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PdfTextExtractionServiceTest {

    @TempDir
    Path tempDirectory;

    private final PdfTextExtractionService extractionService =
            new PdfTextExtractionService();

    @Test
    void extractsTextFromValidPdf() throws IOException {
        Path pdfPath = tempDirectory.resolve("identity.pdf");

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream stream =
                         new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                        12
                );
                stream.newLineAtOffset(50, 700);
                stream.showText("Kycetra AI identity verification");
                stream.endText();
            }

            document.save(pdfPath.toFile());
        }

        String text = extractionService.extractText(pdfPath);

        assertTrue(text.contains("Kycetra AI identity verification"));
    }

    @Test
    void rejectsMissingPdf() {
        Path missingPath = tempDirectory.resolve("missing.pdf");

        assertThrows(
                IllegalArgumentException.class,
                () -> extractionService.extractText(missingPath)
        );
    }

    @Test
    void rejectsPdfWithoutExtractableText() throws IOException {
        Path pdfPath = tempDirectory.resolve("blank.pdf");

        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(pdfPath.toFile());
        }

        assertThrows(
                IllegalArgumentException.class,
                () -> extractionService.extractText(pdfPath)
        );
    }
}
