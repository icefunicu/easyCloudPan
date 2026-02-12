package com.easypan.unit.service;

import com.easypan.component.RedisComponent;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SysSettingsDto;
import com.easypan.entity.po.EmailCode;
import com.easypan.entity.po.UserInfo;
import com.easypan.exception.BusinessException;
import com.easypan.mappers.EmailCodeMapper;
import com.easypan.mappers.UserInfoMapper;
import com.easypan.service.impl.EmailCodeServiceImpl;
import com.easypan.utils.StringTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.internet.MimeMessage;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * EmailCodeService 单元测试
 * 测试邮箱验证码生成、验证、过期、限流等功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailCodeService 单元测试")
class EmailCodeServiceTest {

    @Mock
    private EmailCodeMapper emailCodeMapper;

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private AppConfig appConfig;

    @Mock
    private UserInfoMapper userInfoMapper;

    @Mock
    private RedisComponent redisComponent;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailCodeServiceImpl emailCodeService;

    private String testEmail;
    private String testCode;
    private EmailCode testEmailCode;
    private SysSettingsDto sysSettings;

    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
        testCode = "12345";

        testEmailCode = new EmailCode();
        testEmailCode.setEmail(testEmail);
        testEmailCode.setCode(testCode);
        testEmailCode.setStatus(Constants.ZERO);
        testEmailCode.setCreateTime(new Date());

        sysSettings = new SysSettingsDto();
        sysSettings.setRegisterEmailTitle("EasyCloudPan 注册验证码");
        sysSettings.setRegisterEmailContent("您的验证码是：%s，15分钟内有效");
    }

    // ==================== 发送验证码测试 ====================

    @Test
    @DisplayName("发送注册验证码 - 成功")
    void testSendEmailCode_RegisterSuccess() {
        try (MockedStatic<StringTools> stringToolsMock = mockStatic(StringTools.class)) {
            // Given
            stringToolsMock.when(() -> StringTools.getRandomNumber(Constants.LENGTH_5))
                    .thenReturn(testCode);

            when(userInfoMapper.selectByEmail(testEmail)).thenReturn(null); // 邮箱未注册
            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(appConfig.getSendUserName()).thenReturn("noreply@easypan.com");
            when(redisComponent.getSysSettingsDto()).thenReturn(sysSettings);
            when(emailCodeMapper.insert(any(EmailCode.class))).thenReturn(1);

            // When
            emailCodeService.sendEmailCode(testEmail, Constants.ZERO);

            // Then
            verify(emailCodeMapper, times(1)).disableEmailCode(testEmail);
            verify(emailCodeMapper, times(1)).insert(any(EmailCode.class));
            verify(javaMailSender, times(1)).send(any(MimeMessage.class));
        }
    }

    @Test
    @DisplayName("发送注册验证码 - 邮箱已存在")
    void testSendEmailCode_EmailAlreadyExists() {
        // Given
        UserInfo existingUser = new UserInfo();
        existingUser.setEmail(testEmail);
        when(userInfoMapper.selectByEmail(testEmail)).thenReturn(existingUser);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            emailCodeService.sendEmailCode(testEmail, Constants.ZERO);
        });

        assertEquals("邮箱已经存在", exception.getMessage());
        verify(emailCodeMapper, never()).insert(any(EmailCode.class));
        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("发送找回密码验证码 - 成功")
    void testSendEmailCode_ResetPasswordSuccess() {
        try (MockedStatic<StringTools> stringToolsMock = mockStatic(StringTools.class)) {
            // Given
            stringToolsMock.when(() -> StringTools.getRandomNumber(Constants.LENGTH_5))
                    .thenReturn(testCode);

            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(appConfig.getSendUserName()).thenReturn("noreply@easypan.com");
            when(redisComponent.getSysSettingsDto()).thenReturn(sysSettings);
            when(emailCodeMapper.insert(any(EmailCode.class))).thenReturn(1);

            // When
            emailCodeService.sendEmailCode(testEmail, 1); // type=1 表示找回密码

            // Then
            verify(userInfoMapper, never()).selectByEmail(anyString()); // 找回密码不检查邮箱是否存在
            verify(emailCodeMapper, times(1)).disableEmailCode(testEmail);
            verify(emailCodeMapper, times(1)).insert(any(EmailCode.class));
            verify(javaMailSender, times(1)).send(any(MimeMessage.class));
        }
    }

    @Test
    @DisplayName("发送验证码 - 邮件发送失败")
    void testSendEmailCode_EmailSendFailure() {
        try (MockedStatic<StringTools> stringToolsMock = mockStatic(StringTools.class)) {
            // Given
            stringToolsMock.when(() -> StringTools.getRandomNumber(Constants.LENGTH_5))
                    .thenReturn(testCode);

            when(userInfoMapper.selectByEmail(testEmail)).thenReturn(null);
            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(appConfig.getSendUserName()).thenReturn("noreply@easypan.com");
            when(redisComponent.getSysSettingsDto()).thenReturn(sysSettings);
            doThrow(new RuntimeException("SMTP connection failed"))
                    .when(javaMailSender).send(any(MimeMessage.class));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                emailCodeService.sendEmailCode(testEmail, Constants.ZERO);
            });

            assertEquals("邮件发送失败", exception.getMessage());
            verify(emailCodeMapper, never()).insert(any(EmailCode.class));
        }
    }

    // ==================== 验证码校验测试 ====================

    @Test
    @DisplayName("校验验证码 - 成功")
    void testCheckCode_Success() {
        // Given
        when(emailCodeMapper.selectByEmailAndCode(testEmail, testCode)).thenReturn(testEmailCode);

        // When
        emailCodeService.checkCode(testEmail, testCode);

        // Then
        verify(emailCodeMapper, times(1)).disableEmailCode(testEmail);
    }

    @Test
    @DisplayName("校验验证码 - 验证码不存在")
    void testCheckCode_CodeNotFound() {
        // Given
        when(emailCodeMapper.selectByEmailAndCode(testEmail, testCode)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            emailCodeService.checkCode(testEmail, testCode);
        });

        assertEquals("邮箱验证码不正确", exception.getMessage());
        verify(emailCodeMapper, never()).disableEmailCode(anyString());
    }

    @Test
    @DisplayName("校验验证码 - 验证码已使用")
    void testCheckCode_CodeAlreadyUsed() {
        // Given
        testEmailCode.setStatus(1); // 已使用
        when(emailCodeMapper.selectByEmailAndCode(testEmail, testCode)).thenReturn(testEmailCode);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            emailCodeService.checkCode(testEmail, testCode);
        });

        assertEquals("邮箱验证码已失效", exception.getMessage());
        verify(emailCodeMapper, never()).disableEmailCode(anyString());
    }

    @Test
    @DisplayName("校验验证码 - 验证码已过期（超过15分钟）")
    void testCheckCode_CodeExpired() {
        // Given
        Date expiredTime = new Date(System.currentTimeMillis() - 16 * 60 * 1000); // 16分钟前
        testEmailCode.setCreateTime(expiredTime);
        when(emailCodeMapper.selectByEmailAndCode(testEmail, testCode)).thenReturn(testEmailCode);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            emailCodeService.checkCode(testEmail, testCode);
        });

        assertEquals("邮箱验证码已失效", exception.getMessage());
        verify(emailCodeMapper, never()).disableEmailCode(anyString());
    }

    @Test
    @DisplayName("校验验证码 - 验证码在有效期内（14分钟）")
    void testCheckCode_WithinValidPeriod() {
        // Given
        Date recentTime = new Date(System.currentTimeMillis() - 14 * 60 * 1000); // 14分钟前
        testEmailCode.setCreateTime(recentTime);
        when(emailCodeMapper.selectByEmailAndCode(testEmail, testCode)).thenReturn(testEmailCode);

        // When
        emailCodeService.checkCode(testEmail, testCode);

        // Then
        verify(emailCodeMapper, times(1)).disableEmailCode(testEmail);
    }

    // ==================== 基础CRUD测试 ====================

    @Test
    @DisplayName("根据邮箱和验证码查询")
    void testGetEmailCodeByEmailAndCode() {
        // Given
        when(emailCodeMapper.selectByEmailAndCode(testEmail, testCode)).thenReturn(testEmailCode);

        // When
        EmailCode result = emailCodeService.getEmailCodeByEmailAndCode(testEmail, testCode);

        // Then
        assertNotNull(result);
        assertEquals(testEmail, result.getEmail());
        assertEquals(testCode, result.getCode());
    }

    @Test
    @DisplayName("根据邮箱和验证码删除")
    void testDeleteEmailCodeByEmailAndCode() {
        // Given
        when(emailCodeMapper.deleteByEmailAndCode(testEmail, testCode)).thenReturn(1);

        // When
        Integer result = emailCodeService.deleteEmailCodeByEmailAndCode(testEmail, testCode);

        // Then
        assertEquals(1, result);
        verify(emailCodeMapper, times(1)).deleteByEmailAndCode(testEmail, testCode);
    }

    @Test
    @DisplayName("新增验证码")
    void testAdd() {
        // Given
        when(emailCodeMapper.insert(testEmailCode)).thenReturn(1);

        // When
        Integer result = emailCodeService.add(testEmailCode);

        // Then
        assertEquals(1, result);
        verify(emailCodeMapper, times(1)).insert(testEmailCode);
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("校验验证码 - 刚好15分钟边界")
    void testCheckCode_ExactlyFifteenMinutes() {
        // Given
        Date exactTime = new Date(System.currentTimeMillis() - 15 * 60 * 1000 + 1000); // 15分钟少1秒
        testEmailCode.setCreateTime(exactTime);
        when(emailCodeMapper.selectByEmailAndCode(testEmail, testCode)).thenReturn(testEmailCode);

        // When
        emailCodeService.checkCode(testEmail, testCode);

        // Then
        verify(emailCodeMapper, times(1)).disableEmailCode(testEmail);
    }

    @Test
    @DisplayName("发送验证码 - 空邮箱")
    void testSendEmailCode_EmptyEmail() {
        // Given
        String emptyEmail = "";
        
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(appConfig.getSendUserName()).thenReturn("noreply@easypan.com");

        // When & Then - 应该抛出异常或被邮件发送器拒绝
        assertThrows(Exception.class, () -> {
            emailCodeService.sendEmailCode(emptyEmail, Constants.ZERO);
        });
    }
}
