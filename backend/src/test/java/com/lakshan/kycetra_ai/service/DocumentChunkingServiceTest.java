package com.lakshan.kycetra_ai.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentChunkingServiceTest {

    @Test
    void splitsTextWithExpectedOverlap() {
        String text = "0123456789";

        List<int[]> ranges = chunkRanges(text.length(), 6, 2);

        assertEquals(2, ranges.size());

        assertArrayEquals(new int[]{0, 6}, ranges.get(0));
        assertArrayEquals(new int[]{4, 10}, ranges.get(1));

        assertEquals(
            text.substring(4, 6),
            text.substring(ranges.get(0)[1] - 2, ranges.get(0)[1])
        );
    }

    @Test
    void shortTextProducesOneChunk() {
        List<int[]> ranges = chunkRanges(4, 6, 2);

        assertEquals(1, ranges.size());
        assertArrayEquals(new int[]{0, 4}, ranges.get(0));
    }

    @Test
    void exactChunkSizeProducesOneChunk() {
        List<int[]> ranges = chunkRanges(6, 6, 2);

        assertEquals(1, ranges.size());
        assertArrayEquals(new int[]{0, 6}, ranges.get(0));
    }

    private List<int[]> chunkRanges(
            int textLength,
            int chunkSize,
            int overlap) {

        java.util.ArrayList<int[]> ranges = new java.util.ArrayList<>();

        int start = 0;

        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);
            ranges.add(new int[]{start, end});

            if (end == textLength) {
                break;
            }

            start = end - overlap;
        }

        return ranges;
    }
}