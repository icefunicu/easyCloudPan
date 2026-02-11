package com.easypan;

import com.easypan.component.RedisComponent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

@SpringBootTest
@ActiveProfiles("test")
class EasyPanApplicationTests {

    @MockitoBean(name = "initRun")
    private InitRun initRun;

    @MockitoBean
    private RedisComponent redisComponent;

    @MockitoBean
    private DataSource dataSource;

    @Test
    void contextLoads() {
    }

}
