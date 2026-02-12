package com.easypan.unit.service;

import com.easypan.component.RedisComponent;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.dto.SysSettingsDto;
import com.easypan.entity.dto.UserSpaceDto;
import com.easypan.entity.enums.UserStatusEnum;
import com.easypan.entity.po.UserInfo;
import com.easypan.exception.BusinessException;
import com.easypan.mappers.UserInfoMapper;
import com.easypan.service.EmailCodeService;
import com.easypan.service.FileInfoService;
import com.easypan.service.TenantQuotaService;
import com.easypan.service.impl.UserInfoServiceImpl;
import com.easypan.utils.StringTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserInfoService 单元测试
 * 
 * 测试用户服务的核心功能：
 * 1. 用户注册
 * 2. 用户登录
 * 3. 空间使用更新
 * 
 * 需求：1.2.1
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserInfoService 单元测试")
class UserInfoServiceTest {

    @Mock
    private UserInfoMapper userInfoMapper;

    @Mock
    private EmailCodeService emailCodeService;

    @Mock
    private FileInfoService fileInfoService;

    @Mock
    private RedisComponent redisComponent;

    @Mock
    private AppConfig appConfig;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TenantQuotaService tenantQuotaService;

    @InjectMocks
    private UserInfoServiceImpl userInfoService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_NICKNAME = "TestUser";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_EMAIL_CODE = "123456";
    private static final String TEST_USER_ID = "1234567890";

    @BeforeEach
    void setUp() {
        // Mock StringTools.getRandomNumber behavior
        // Note: Since StringTools is a utility class, we'll work with the actual implementation
    }

    @Test
    @DisplayName("用户注册成功")
    void testRegister_Success() {
        // Given: 邮箱和昵称都不存在
        when(userInfoMapper.selectByEmail(TEST_EMAIL)).thenReturn(null);
        when(userInfoMapper.selectByNickName(TEST_NICKNAME)).thenReturn(null);
        doNothing().when(emailCodeService).checkCode(TEST_EMAIL, TEST_EMAIL_CODE);
        doNothing().when(tenantQuotaService).checkUserQuota();
        
        SysSettingsDto sysSettings = new SysSettingsDto();
        sysSettings.setUserInitUseSpace(5); // 5MB
        when(redisComponent.getSysSettingsDto()).thenReturn(sysSettings);
        
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn("encoded_password");
        when(userInfoMapper.insert(any(UserInfo.class))).thenReturn(1);

        // When: 注册用户
        userInfoService.register(TEST_EMAIL, TEST_NICKNAME, TEST_PASSWORD, TEST_EMAIL_CODE);

        // Then: 验证邮箱验证码被检查
        verify(emailCodeService).checkCode(TEST_EMAIL, TEST_EMAIL_CODE);

        // 验证插入了新用户
        ArgumentCaptor<UserInfo> userCaptor = ArgumentCaptor.forClass(UserInfo.class);
        verify(userInfoMapper).insert(userCaptor.capture());
        
        UserInfo insertedUser = userCaptor.getValue();
        assertNotNull(insertedUser.getUserId());
        assertEquals(TEST_NICKNAME, insertedUser.getNickName());
        assertEquals(TEST_EMAIL, insertedUser.getEmail());
        assertEquals("encoded_password", insertedUser.getPassword());
        assertEquals(UserStatusEnum.ENABLE.getStatus(), insertedUser.getStatus());
        assertEquals(5 * 1024 * 1024L, insertedUser.getTotalSpace()); // 5MB in bytes
        assertEquals(0L, insertedUser.getUseSpace());
        assertNotNull(insertedUser.getJoinTime());
    }

    @Test
    @DisplayName("用户注册失败 - 邮箱已存在")
    void testRegister_EmailExists() {
        // Given: 邮箱已存在
        doNothing().when(tenantQuotaService).checkUserQuota();
        UserInfo existingUser = new UserInfo();
        existingUser.setEmail(TEST_EMAIL);
        when(userInfoMapper.selectByEmail(TEST_EMAIL)).thenReturn(existingUser);

        // When & Then: 应该抛出异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userInfoService.register(TEST_EMAIL, TEST_NICKNAME, TEST_PASSWORD, TEST_EMAIL_CODE);
        });
        
        assertEquals("邮箱账号已经存在", exception.getMessage());
        
        // 验证没有插入新用户
        verify(userInfoMapper, never()).insert(any(UserInfo.class));
    }

    @Test
    @DisplayName("用户注册失败 - 昵称已存在")
    void testRegister_NicknameExists() {
        // Given: 昵称已存在
        doNothing().when(tenantQuotaService).checkUserQuota();
        when(userInfoMapper.selectByEmail(TEST_EMAIL)).thenReturn(null);
        
        UserInfo existingUser = new UserInfo();
        existingUser.setNickName(TEST_NICKNAME);
        when(userInfoMapper.selectByNickName(TEST_NICKNAME)).thenReturn(existingUser);

        // When & Then: 应该抛出异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userInfoService.register(TEST_EMAIL, TEST_NICKNAME, TEST_PASSWORD, TEST_EMAIL_CODE);
        });
        
        assertEquals("昵称已经存在", exception.getMessage());
        
        // 验证没有插入新用户
        verify(userInfoMapper, never()).insert(any(UserInfo.class));
    }

    @Test
    @DisplayName("用户登录成功 - BCrypt 密码")
    void testLogin_Success_BCrypt() {
        // Given: 用户存在且密码正确（BCrypt）
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(TEST_USER_ID);
        userInfo.setEmail(TEST_EMAIL);
        userInfo.setNickName(TEST_NICKNAME);
        userInfo.setPassword("$2a$10$encoded_bcrypt_password");
        userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
        userInfo.setTotalSpace(5 * 1024 * 1024L);
        
        when(userInfoMapper.selectByEmail(TEST_EMAIL)).thenReturn(userInfo);
        when(passwordEncoder.matches(TEST_PASSWORD, userInfo.getPassword())).thenReturn(true);
        when(userInfoMapper.updateByUserId(any(UserInfo.class), eq(TEST_USER_ID))).thenReturn(1);
        when(appConfig.getAdminEmails()).thenReturn("admin@example.com");
        when(fileInfoService.getUserUseSpace(TEST_USER_ID)).thenReturn(1024L);
        doNothing().when(redisComponent).saveUserSpaceUse(eq(TEST_USER_ID), any(UserSpaceDto.class));

        // When: 登录
        SessionWebUserDto session = userInfoService.login(TEST_EMAIL, TEST_PASSWORD);

        // Then: 返回会话信息
        assertNotNull(session);
        assertEquals(TEST_NICKNAME, session.getNickName());
        assertEquals(TEST_USER_ID, session.getUserId());
        assertFalse(session.getAdmin()); // 不是管理员

        // 验证更新了最后登录时间
        ArgumentCaptor<UserInfo> updateCaptor = ArgumentCaptor.forClass(UserInfo.class);
        verify(userInfoMapper).updateByUserId(updateCaptor.capture(), eq(TEST_USER_ID));
        assertNotNull(updateCaptor.getValue().getLastLoginTime());
        
        // 验证保存了用户空间信息到 Redis
        ArgumentCaptor<UserSpaceDto> spaceCaptor = ArgumentCaptor.forClass(UserSpaceDto.class);
        verify(redisComponent).saveUserSpaceUse(eq(TEST_USER_ID), spaceCaptor.capture());
        assertEquals(1024L, spaceCaptor.getValue().getUseSpace());
        assertEquals(5 * 1024 * 1024L, spaceCaptor.getValue().getTotalSpace());
    }

    @Test
    @DisplayName("用户登录成功 - MD5 密码自动升级")
    void testLogin_Success_MD5_Upgrade() {
        // Given: 用户存在且使用 MD5 密码
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(TEST_USER_ID);
        userInfo.setEmail(TEST_EMAIL);
        userInfo.setNickName(TEST_NICKNAME);
        userInfo.setPassword(StringTools.encodeByMD5(TEST_PASSWORD)); // 32位 MD5
        userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
        userInfo.setTotalSpace(5 * 1024 * 1024L);
        
        when(userInfoMapper.selectByEmail(TEST_EMAIL)).thenReturn(userInfo);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn("$2a$10$new_bcrypt_password");
        when(userInfoMapper.updateByUserId(any(UserInfo.class), eq(TEST_USER_ID))).thenReturn(1);
        when(appConfig.getAdminEmails()).thenReturn("admin@example.com");
        when(fileInfoService.getUserUseSpace(TEST_USER_ID)).thenReturn(0L);
        doNothing().when(redisComponent).saveUserSpaceUse(eq(TEST_USER_ID), any(UserSpaceDto.class));

        // When: 登录
        SessionWebUserDto session = userInfoService.login(TEST_EMAIL, TEST_PASSWORD);

        // Then: 登录成功
        assertNotNull(session);
        assertEquals(TEST_USER_ID, session.getUserId());

        // 验证密码被升级为 BCrypt
        ArgumentCaptor<UserInfo> updateCaptor = ArgumentCaptor.forClass(UserInfo.class);
        verify(userInfoMapper).updateByUserId(updateCaptor.capture(), eq(TEST_USER_ID));
        assertEquals("$2a$10$new_bcrypt_password", updateCaptor.getValue().getPassword());
    }

    @Test
    @DisplayName("用户登录成功 - 管理员用户")
    void testLogin_Success_Admin() {
        // Given: 用户是管理员
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(TEST_USER_ID);
        userInfo.setEmail(TEST_EMAIL);
        userInfo.setNickName(TEST_NICKNAME);
        userInfo.setPassword("$2a$10$encoded_bcrypt_password");
        userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
        userInfo.setTotalSpace(5 * 1024 * 1024L);
        
        when(userInfoMapper.selectByEmail(TEST_EMAIL)).thenReturn(userInfo);
        when(passwordEncoder.matches(TEST_PASSWORD, userInfo.getPassword())).thenReturn(true);
        when(userInfoMapper.updateByUserId(any(UserInfo.class), eq(TEST_USER_ID))).thenReturn(1);
        when(appConfig.getAdminEmails()).thenReturn(TEST_EMAIL + ",other@example.com");
        when(fileInfoService.getUserUseSpace(TEST_USER_ID)).thenReturn(0L);
        doNothing().when(redisComponent).saveUserSpaceUse(eq(TEST_USER_ID), any(UserSpaceDto.class));

        // When: 登录
        SessionWebUserDto session = userInfoService.login(TEST_EMAIL, TEST_PASSWORD);

        // Then: 应该标记为管理员
        assertNotNull(session);
        assertTrue(session.getAdmin());
    }

    @Test
    @DisplayName("用户登录失败 - 账号不存在")
    void testLogin_Fail_UserNotExists() {
        // Given: 用户不存在
        when(userInfoMapper.selectByEmail(TEST_EMAIL)).thenReturn(null);

        // When & Then: 应该抛出异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userInfoService.login(TEST_EMAIL, TEST_PASSWORD);
        });
        
        assertEquals("账号或者密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("用户登录失败 - 密码错误")
    void testLogin_Fail_WrongPassword() {
        // Given: 用户存在但密码错误
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(TEST_USER_ID);
        userInfo.setEmail(TEST_EMAIL);
        userInfo.setPassword("$2a$10$encoded_bcrypt_password");
        
        when(userInfoMapper.selectByEmail(TEST_EMAIL)).thenReturn(userInfo);
        when(passwordEncoder.matches(TEST_PASSWORD, userInfo.getPassword())).thenReturn(false);

        // When & Then: 应该抛出异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userInfoService.login(TEST_EMAIL, TEST_PASSWORD);
        });
        
        assertEquals("账号或者密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("用户登录失败 - 账号已禁用")
    void testLogin_Fail_AccountDisabled() {
        // Given: 用户存在但账号已禁用
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(TEST_USER_ID);
        userInfo.setEmail(TEST_EMAIL);
        userInfo.setPassword("$2a$10$encoded_bcrypt_password");
        userInfo.setStatus(UserStatusEnum.DISABLE.getStatus());
        
        when(userInfoMapper.selectByEmail(TEST_EMAIL)).thenReturn(userInfo);
        when(passwordEncoder.matches(TEST_PASSWORD, userInfo.getPassword())).thenReturn(true);

        // When & Then: 应该抛出异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userInfoService.login(TEST_EMAIL, TEST_PASSWORD);
        });
        
        assertEquals("账号已禁用", exception.getMessage());
    }

    @Test
    @DisplayName("修改用户空间成功")
    void testChangeUserSpace_Success() {
        // Given: 修改用户空间为 10MB
        Integer changeSpace = 10; // MB
        when(userInfoMapper.updateUserSpaceAdmin(eq(TEST_USER_ID), isNull(), eq(10 * 1024 * 1024L)))
            .thenReturn(1);
        
        UserSpaceDto spaceDto = new UserSpaceDto();
        spaceDto.setUseSpace(0L);
        spaceDto.setTotalSpace(10 * 1024 * 1024L);
        when(redisComponent.resetUserSpaceUse(TEST_USER_ID)).thenReturn(spaceDto);

        // When: 修改用户空间
        userInfoService.changeUserSpace(TEST_USER_ID, changeSpace);

        // Then: 验证更新了数据库
        verify(userInfoMapper).updateUserSpaceAdmin(TEST_USER_ID, null, 10 * 1024 * 1024L);
        
        // 验证重置了 Redis 缓存
        verify(redisComponent).resetUserSpaceUse(TEST_USER_ID);
    }

    @Test
    @DisplayName("修改用户空间 - 增加空间")
    void testChangeUserSpace_Increase() {
        // Given: 增加 5MB 空间
        Integer changeSpace = 5;
        when(userInfoMapper.updateUserSpaceAdmin(eq(TEST_USER_ID), isNull(), eq(5 * 1024 * 1024L)))
            .thenReturn(1);
        
        UserSpaceDto spaceDto = new UserSpaceDto();
        when(redisComponent.resetUserSpaceUse(TEST_USER_ID)).thenReturn(spaceDto);

        // When: 修改用户空间
        userInfoService.changeUserSpace(TEST_USER_ID, changeSpace);

        // Then: 验证调用了正确的参数
        verify(userInfoMapper).updateUserSpaceAdmin(TEST_USER_ID, null, 5 * 1024 * 1024L);
        verify(redisComponent).resetUserSpaceUse(TEST_USER_ID);
    }

    @Test
    @DisplayName("修改用户空间 - 减少空间（负数）")
    void testChangeUserSpace_Decrease() {
        // Given: 减少 3MB 空间
        Integer changeSpace = -3;
        when(userInfoMapper.updateUserSpaceAdmin(eq(TEST_USER_ID), isNull(), eq(-3 * 1024 * 1024L)))
            .thenReturn(1);
        
        UserSpaceDto spaceDto = new UserSpaceDto();
        when(redisComponent.resetUserSpaceUse(TEST_USER_ID)).thenReturn(spaceDto);

        // When: 修改用户空间
        userInfoService.changeUserSpace(TEST_USER_ID, changeSpace);

        // Then: 验证调用了正确的参数
        verify(userInfoMapper).updateUserSpaceAdmin(TEST_USER_ID, null, -3 * 1024 * 1024L);
        verify(redisComponent).resetUserSpaceUse(TEST_USER_ID);
    }
}
