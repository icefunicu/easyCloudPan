package com.easypan.component;

import com.easypan.entity.constants.CacheTTL;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.DownloadFileDto;
import com.easypan.entity.dto.SysSettingsDto;
import com.easypan.entity.dto.UserSpaceDto;
import com.easypan.entity.po.UserInfo;
import com.easypan.mappers.FileInfoMapper;
import com.easypan.mappers.UserInfoMapper;
import com.google.common.hash.BloomFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

@Component("redisComponent")
public class RedisComponent {

    @Resource
    private RedisUtils<Object> redisUtils;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Resource
    @Qualifier("fileMd5BloomFilter")
    private BloomFilter<String> fileMd5BloomFilter;

    /**
     * 获取系统设置
     *
     * @return
     */
    public SysSettingsDto getSysSettingsDto() {
        SysSettingsDto sysSettingsDto = (SysSettingsDto) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        if (sysSettingsDto == null) {
            sysSettingsDto = new SysSettingsDto();
            redisUtils.setex(Constants.REDIS_KEY_SYS_SETTING, sysSettingsDto, CacheTTL.SYS_CONFIG);
        }
        return sysSettingsDto;
    }

    /**
     * 保存设置
     *
     * @param sysSettingsDto
     */
    public void saveSysSettingsDto(SysSettingsDto sysSettingsDto) {
        redisUtils.setex(Constants.REDIS_KEY_SYS_SETTING, sysSettingsDto, CacheTTL.SYS_CONFIG);
    }

    public void saveDownloadCode(String code, DownloadFileDto downloadFileDto) {
        redisUtils.setex(Constants.REDIS_KEY_DOWNLOAD + code, downloadFileDto, Constants.REDIS_KEY_EXPIRES_FIVE_MIN);
    }

    public DownloadFileDto getDownloadCode(String code) {
        return (DownloadFileDto) redisUtils.get(Constants.REDIS_KEY_DOWNLOAD + code);
    }

    /**
     * 获取用户使用的空间
     *
     * @param userId
     * @return
     */
    public UserSpaceDto getUserSpaceUse(String userId) {
        UserSpaceDto spaceDto = (UserSpaceDto) redisUtils.get(Constants.REDIS_KEY_USER_SPACE_USE + userId);
        if (null == spaceDto) {
            spaceDto = new UserSpaceDto();
            Long useSpace = this.fileInfoMapper.selectUseSpace(userId);
            spaceDto.setUseSpace(useSpace);
            spaceDto.setTotalSpace(getSysSettingsDto().getUserInitUseSpace() * Constants.MB);
            redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE + userId, spaceDto, CacheTTL.WARM_DATA);
        }
        return spaceDto;
    }

    /**
     * 保存已使用的空间
     *
     * @param userId
     */
    public void saveUserSpaceUse(String userId, UserSpaceDto userSpaceDto) {
        redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE + userId, userSpaceDto, CacheTTL.WARM_DATA);
    }

    public UserSpaceDto resetUserSpaceUse(String userId) {
        UserSpaceDto spaceDto = new UserSpaceDto();
        Long useSpace = this.fileInfoMapper.selectUseSpace(userId);
        spaceDto.setUseSpace(useSpace);

        UserInfo userInfo = this.userInfoMapper.selectByUserId(userId);
        spaceDto.setTotalSpace(userInfo.getTotalSpace());
        redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE + userId, spaceDto, CacheTTL.WARM_DATA);
        return spaceDto;
    }

    // 保存文件临时大小
    public void saveFileTempSize(String userId, String fileId, Long fileSize) {
        Long currentSize = getFileTempSize(userId, fileId);
        redisUtils.setex(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + fileId, currentSize + fileSize,
                Constants.REDIS_KEY_EXPIRES_ONE_HOUR);
    }

    public Long getFileTempSize(String userId, String fileId) {
        Long currentSize = getFileSizeFromRedis(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + fileId);
        return currentSize;
    }

    private Long getFileSizeFromRedis(String key) {
        Object sizeObj = redisUtils.get(key);
        if (sizeObj == null) {
            return 0L;
        }
        if (sizeObj instanceof Integer) {
            return ((Integer) sizeObj).longValue();
        } else if (sizeObj instanceof Long) {
            return (Long) sizeObj;
        }

        return 0L;
    }

    public void addBlacklistToken(String token, long expirationTimeInSeconds) {
        redisUtils.setex(Constants.REDIS_KEY_JWT_BLACKLIST + token, "", expirationTimeInSeconds);
    }

    public boolean isTokenBlacklisted(String token) {
        return redisUtils.get(Constants.REDIS_KEY_JWT_BLACKLIST + token) != null;
    }

    public void saveRefreshToken(String userId, String refreshToken, long expirationTimeInSeconds) {
        redisUtils.setex(Constants.REDIS_KEY_REFRESH_TOKEN + userId, refreshToken, expirationTimeInSeconds);
    }

    public String getRefreshToken(String userId) {
        return (String) redisUtils.get(Constants.REDIS_KEY_REFRESH_TOKEN + userId);
    }

    public boolean validateRefreshToken(String userId, String refreshToken) {
        String storedToken = getRefreshToken(userId);
        return refreshToken.equals(storedToken);
    }

    public void deleteRefreshToken(String userId) {
        redisUtils.delete(Constants.REDIS_KEY_REFRESH_TOKEN + userId);
    }

    /**
     * 检查文件 MD5 是否可能存在（布隆过滤器）
     * 用于在秒传场景下降低对数据库的无效访问概率。
     */
    public boolean mightContainFileMd5(String fileMd5) {
        if (fileMd5 == null) {
            return false;
        }
        return fileMd5BloomFilter.mightContain(fileMd5);
    }

    /**
     * 将新的文件 MD5 加入布隆过滤器。
     */
    public void addFileMd5ToBloom(String fileMd5) {
        if (fileMd5 == null) {
            return;
        }
        fileMd5BloomFilter.put(fileMd5);
    }
}
