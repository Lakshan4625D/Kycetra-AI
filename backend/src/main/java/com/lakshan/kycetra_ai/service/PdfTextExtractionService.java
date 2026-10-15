
package com.lakshan.kycetra_ai.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class PdfTextExtractionService {

    private static final int MAX_PAGES = 500;

    public String extractText(Path pdfPath) throws IOException {
        if (pdfPath == null || !Files.isRegularFile(pdfPath)) {
            throw new IllegalArgumentException(
                    "PDF file does not exist."
            );
        }

        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException(
                        "Encrypted PDFs are not supported."
                );
            }

            if (document.getNumberOfPages() > MAX_PAGES) {
                throw new IllegalArgumentException(
                        "PDF exceeds the maximum of 500 pages."
                );
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document).trim();

            if (text.isBlank()) {
                throw new IllegalArgumentException(
                        "No extractable text found. The PDF may require OCR."
                );
            }

            return text;
        }
    }
}
