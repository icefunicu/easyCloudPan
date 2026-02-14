package com.easypan.unit.controller;

import com.easypan.controller.AccountController;
import com.easypan.entity.constants.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AccountController /checkCode tests")
class AccountControllerCheckCodeTest {

    @Test
    @DisplayName("checkCode: should return PNG and store code in session")
    void checkCode_shouldReturnPngAndStoreSessionCode() throws Exception {
        AccountController controller = new AccountController();
        setCaptchaDebugHeader(controller, false);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = new MockHttpSession();

        controller.checkCode(response, session, 0);

        assertNotNull(session.getAttribute(Constants.CHECK_CODE_KEY));
        assertNull(response.getHeader("X-EasyPan-CheckCode"));
        assertTrue(response.getContentType().startsWith("image/png"));

        byte[] bytes = response.getContentAsByteArray();
        assertTrue(bytes.length >= 8);
        byte[] sig = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        for (int i = 0; i < sig.length; i++) {
            assertEquals(sig[i], bytes[i]);
        }
    }

    @Test
    @DisplayName("checkCode: should expose code in header when enabled")
    void checkCode_shouldExposeCodeHeader_whenEnabled() throws Exception {
        AccountController controller = new AccountController();
        setCaptchaDebugHeader(controller, true);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = new MockHttpSession();

        controller.checkCode(response, session, 0);

        String sessionCode = (String) session.getAttribute(Constants.CHECK_CODE_KEY);
        String headerCode = response.getHeader("X-EasyPan-CheckCode");

        assertNotNull(sessionCode);
        assertNotNull(headerCode);
        assertFalse(headerCode.isBlank());
        assertEquals(sessionCode, headerCode);
    }

    private static void setCaptchaDebugHeader(AccountController controller, boolean enabled) throws Exception {
        Field field = AccountController.class.getDeclaredField("captchaDebugHeader");
        field.setAccessible(true);
        field.setBoolean(controller, enabled);
    }
}

