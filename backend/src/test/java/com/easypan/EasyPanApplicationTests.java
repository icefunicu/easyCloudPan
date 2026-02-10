package com.easypan;

import com.easypan.component.RedisComponent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

@SpringBootTest
@ActiveProfiles("test")
class EasyPanApplicationTests {

    @MockBean(name = "initRun")
    private InitRun initRun;

    @MockBean
    private RedisComponent redisComponent;

    @MockBean
    private DataSource dataSource;

    @Test
    void contextLoads() {
    }

}
