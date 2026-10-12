package com.crustcrunch.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class StorageServiceTest {

    private static byte[] bytes(int... values) {
        byte[] out = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (byte) values[i];
        }
        return out;
    }

    @Test
    void acceptsJpegHeader() {
        assertThat(StorageService.looksLikeImage(bytes(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0, 1)))
                .isTrue();
    }

    @Test
    void acceptsPngHeader() {
        assertThat(StorageService.looksLikeImage(bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D)))
                .isTrue();
    }

    @Test
    void acceptsWebpHeader() {
        assertThat(StorageService.looksLikeImage(bytes('R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P')))
                .isTrue();
    }

    @Test
    void rejectsScriptsAndOtherFormats() {
        assertThat(StorageService.looksLikeImage("<script>alert(1)".getBytes(StandardCharsets.US_ASCII))).isFalse();
        assertThat(StorageService.looksLikeImage("<?php echo 1; ?>".getBytes(StandardCharsets.US_ASCII))).isFalse();
        assertThat(StorageService.looksLikeImage("GIF89a......".getBytes(StandardCharsets.US_ASCII))).isFalse();
        assertThat(StorageService.looksLikeImage("%PDF-1.7....".getBytes(StandardCharsets.US_ASCII))).isFalse();
        // RIFF container that isn't WebP (e.g. WAV audio)
        assertThat(StorageService.looksLikeImage(bytes('R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'A', 'V', 'E')))
                .isFalse();
    }

    @Test
    void rejectsEmptyAndTruncatedHeaders() {
        assertThat(StorageService.looksLikeImage(new byte[0])).isFalse();
        assertThat(StorageService.looksLikeImage(bytes(0xFF, 0xD8))).isFalse();
        assertThat(StorageService.looksLikeImage(bytes(0x89, 'P', 'N', 'G'))).isFalse();
        assertThat(StorageService.looksLikeImage(bytes('R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B'))).isFalse();
    }
}
