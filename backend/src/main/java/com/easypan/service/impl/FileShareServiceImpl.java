package com.easypan.service.impl;

import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SessionShareDto;
import com.easypan.entity.enums.PageSize;
import com.easypan.entity.enums.ResponseCodeEnum;
import com.easypan.entity.enums.ShareValidTypeEnums;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.po.FileShare;
import com.easypan.entity.query.FileShareQuery;
import com.easypan.entity.query.SimplePage;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.exception.BusinessException;
import com.easypan.mappers.FileShareMapper;
import com.easypan.service.FileShareService;
import com.easypan.utils.DateUtil;
import com.easypan.utils.QueryWrapperBuilder;
import com.easypan.utils.StringTools;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;

import static com.easypan.entity.po.table.FileShareTableDef.FILE_SHARE;

/**
 * 文件分享服务实现类.
 */
@Service("fileShareService")
public class FileShareServiceImpl implements FileShareService {

    @Resource
    private FileShareMapper fileShareMapper;

    @Override
    public List<FileShare> findListByParam(FileShareQuery param) {
        QueryWrapper qw = QueryWrapperBuilder.build(param);
        return this.fileShareMapper.selectListByQuery(qw);
    }

    @Override
    public Integer findCountByParam(FileShareQuery param) {
        QueryWrapper qw = QueryWrapperBuilder.build(param, false);
        return Math.toIntExact(this.fileShareMapper.selectCountByQuery(qw));
    }

    @Override
    public PaginationResultVO<FileShare> findListByPage(FileShareQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<FileShare> list = this.findListByParam(param);
        PaginationResultVO<FileShare> result = new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(),
                page.getPageTotal(), list);
        return result;
    }

    @Override
    public Integer add(FileShare bean) {
        return this.fileShareMapper.insert(bean);
    }

    @Override
    public Integer addBatch(List<FileShare> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.fileShareMapper.insertBatch(listBean);
    }

    @Override
    public Integer addOrUpdateBatch(List<FileShare> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.fileShareMapper.insertOrUpdateBatch(listBean);
    }

    @Override
    public FileShare getFileShareByShareId(String shareId) {
        return this.fileShareMapper.selectOneByQuery(
                QueryWrapper.create().where(FILE_SHARE.SHARE_ID.eq(shareId)));
    }

    @Override
    public Integer updateFileShareByShareId(FileShare bean, String shareId) {
        return this.fileShareMapper.updateByQuery(bean, 
                QueryWrapper.create().where(FILE_SHARE.SHARE_ID.eq(shareId)));
    }

    @Override
    public Integer deleteFileShareByShareId(String shareId) {
        return this.fileShareMapper.deleteByQuery(
                QueryWrapper.create().where(FILE_SHARE.SHARE_ID.eq(shareId)));
    }

    @Override
    public void saveShare(FileShare share) {
        ShareValidTypeEnums typeEnum = ShareValidTypeEnums.getByType(share.getValidType());
        if (null == typeEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "分享有效期类型无效");
        }
        if (typeEnum != ShareValidTypeEnums.FOREVER) {
            share.setExpireTime(DateUtil.getAfterDate(typeEnum.getDays()));
        }
        Date curDate = new Date();
        share.setShareTime(curDate);
        if (StringTools.isEmpty(share.getCode())) {
            share.setCode(StringTools.getRandomString(Constants.LENGTH_5));
        }
        share.setShareId(StringTools.getRandomString(Constants.LENGTH_20));
        
        FileInfo fileInfo = this.fileShareMapper.selectFileInfoByFileId(share.getFileId());
        if (fileInfo != null) {
            share.setFileName(fileInfo.getFileName());
            share.setFolderType(fileInfo.getFolderType());
            share.setFileCategory(fileInfo.getFileCategory());
            share.setFileType(fileInfo.getFileType());
            share.setFileCover(fileInfo.getFileCover());
        }
        
        this.fileShareMapper.insert(share);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFileShareBatch(String[] shareIdArray, String userId) {
        Integer count = this.fileShareMapper.deleteFileShareBatch(shareIdArray, userId);
        if (count != shareIdArray.length) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "部分分享记录不存在或无权删除");
        }
    }

    @Override
    public SessionShareDto checkShareCode(String shareId, String code) {
        FileShare share = this.fileShareMapper.selectOneByQuery(
                QueryWrapper.create().where(FILE_SHARE.SHARE_ID.eq(shareId)));
        if (null == share || (share.getExpireTime() != null && new Date().after(share.getExpireTime()))) {
            throw new BusinessException(ResponseCodeEnum.CODE_902);
        }
        if (!share.getCode().equals(code)) {
            throw new BusinessException("提取码错误");
        }

        this.fileShareMapper.updateShareShowCount(shareId);
        SessionShareDto shareSessionDto = new SessionShareDto();
        shareSessionDto.setShareId(shareId);
        shareSessionDto.setShareUserId(share.getUserId());
        shareSessionDto.setFileId(share.getFileId());
        shareSessionDto.setExpireTime(share.getExpireTime());
        return shareSessionDto;
    }
}
