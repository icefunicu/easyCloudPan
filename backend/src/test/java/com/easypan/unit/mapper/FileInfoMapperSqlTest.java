package com.easypan.unit.mapper;

import com.easypan.entity.po.FileInfo;
import com.easypan.mappers.FileInfoMapper;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class FileInfoMapperSqlTest {

    @Test
    void updateFileStatusWithOldStatus_shouldUpdateFileSizeAndCover() throws Exception {
        Method method = FileInfoMapper.class.getMethod(
                "updateFileStatusWithOldStatus",
                String.class,
                String.class,
                FileInfo.class,
                Integer.class);
        Update update = method.getAnnotation(Update.class);
        assertNotNull(update, "Missing @Update on updateFileStatusWithOldStatus");

        String sql = String.join(" ", update.value()).toLowerCase();
        assertTrue(sql.contains("file_size"), "SQL should update file_size");
        assertTrue(sql.contains("file_cover"), "SQL should update file_cover");
        assertTrue(sql.contains("last_update_time"), "SQL should update last_update_time");
    }
}

