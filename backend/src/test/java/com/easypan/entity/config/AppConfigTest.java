package com.easypan.entity.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppConfigTest {

    @Test
    void getFileRootPathShouldKeepExistingFileSuffix() {
        AppConfig config = new AppConfig();
        config.setProjectFolder("E:/Project/easyCloudPan/backend/file/");

        assertEquals("E:/Project/easyCloudPan/backend/file", config.getFileRootPath());
    }

    @Test
    void getFileRootPathShouldAppendFileSuffixForBasePath() {
        AppConfig config = new AppConfig();
        config.setProjectFolder("/data/easypan/");

        assertEquals("/data/easypan/file", config.getFileRootPath());
    }

    @Test
    void getFileRootPathShouldFallbackToDefaultWhenProjectFolderMissing() {
        AppConfig config = new AppConfig();
        config.setProjectFolder("");

        assertEquals("/file", config.getFileRootPath());
    }
}
