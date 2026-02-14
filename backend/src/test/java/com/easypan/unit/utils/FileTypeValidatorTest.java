package com.easypan.unit.utils;

import com.easypan.utils.FileTypeValidator;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FileTypeValidator unit tests")
class FileTypeValidatorTest {

    @Test
    @DisplayName("validateFileType: PNG header should pass")
    void validateFileType_pngHeader_shouldPass() {
        byte[] pngHeader = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00
        };
        boolean ok = FileTypeValidator.validateFileType(new ByteArrayInputStream(pngHeader), ".png");
        assertTrue(ok);
    }

    @Test
    @DisplayName("validateFileType: non-mark-supported InputStream should pass")
    void validateFileType_nonMarkSupportedStream_shouldPass() throws IOException {
        byte[] pngHeader = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00
        };
        InputStream raw = new ByteArrayInputStream(pngHeader);
        InputStream nonMark = new FilterInputStream(raw) {
            @Override
            public boolean markSupported() {
                return false;
            }

            @Override
            public synchronized void mark(int readlimit) {
                // no-op
            }

            @Override
            public synchronized void reset() throws IOException {
                throw new IOException("mark/reset not supported");
            }
        };

        boolean ok = FileTypeValidator.validateFileType(nonMark, "png");
        assertTrue(ok);
    }

    @Test
    @DisplayName("validateFileType: TXT without BOM should pass")
    void validateFileType_txtWithoutBom_shouldPass() {
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);
        boolean ok = FileTypeValidator.validateFileType(new ByteArrayInputStream(bytes), ".txt");
        assertTrue(ok);
    }

    @Test
    @DisplayName("validateFileType: MP4 with variable box size should pass")
    void validateFileType_mp4VariableBoxSize_shouldPass() {
        byte[] mp4Header = new byte[] {
                0x00, 0x00, 0x00, 0x1C,
                0x66, 0x74, 0x79, 0x70,
                0x69, 0x73, 0x6F, 0x6D
        };
        boolean ok = FileTypeValidator.validateFileType(new ByteArrayInputStream(mp4Header), ".mp4");
        assertTrue(ok);
    }

    @Test
    @DisplayName("validateFileType: MOV with variable box size should pass")
    void validateFileType_movVariableBoxSize_shouldPass() {
        byte[] movHeader = new byte[] {
                0x00, 0x00, 0x00, 0x1C,
                0x66, 0x74, 0x79, 0x70,
                0x71, 0x74, 0x20, 0x20
        };
        boolean ok = FileTypeValidator.validateFileType(new ByteArrayInputStream(movHeader), ".mov");
        assertTrue(ok);
    }

    @Test
    @DisplayName("validateFileType: TAR should pass (avoid false negatives)")
    void validateFileType_tar_shouldPass() {
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);
        boolean ok = FileTypeValidator.validateFileType(new ByteArrayInputStream(bytes), ".tar");
        assertTrue(ok);
    }
}
