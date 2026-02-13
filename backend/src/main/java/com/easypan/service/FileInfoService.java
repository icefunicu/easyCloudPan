package com.easypan.service;

import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.dto.UploadResultDto;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.query.CursorPage;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.vo.PaginationResultVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件信息业务接口.
 */
public interface FileInfoService {

    /**
     * 根据条件查询列表.
     *
     * @param param 查询参数
     * @return 文件列表
     */
    List<FileInfo> findListByParam(FileInfoQuery param);

    /**
     * 根据条件查询数量.
     *
     * @param param 查询参数
     * @return 数量
     */
    Integer findCountByParam(FileInfoQuery param);

    /**
     * 分页查询.
     *
     * @param param 查询参数
     * @return 分页结果
     */
    PaginationResultVO<FileInfo> findListByPage(FileInfoQuery param);

    /**
     * 游标分页查询（性能优于 OFFSET 分页）.
     *
     * @param userId 用户ID
     * @param cursor 游标（格式：timestamp_id）
     * @param pageSize 每页数量
     * @return 游标分页结果
     */
    CursorPage<FileInfo> findListByCursor(String userId, String cursor, Integer pageSize);

    /**
     * 新增.
     *
     * @param bean 文件信息
     * @return 影响行数
     */
    Integer add(FileInfo bean);

    /**
     * 批量新增.
     *
     * @param listBean 文件列表
     * @return 影响行数
     */
    Integer addBatch(List<FileInfo> listBean);

    /**
     * 批量新增/修改.
     *
     * @param listBean 文件列表
     * @return 影响行数
     */
    Integer addOrUpdateBatch(List<FileInfo> listBean);

    /**
     * 根据FileIdAndUserId查询对象.
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 文件信息
     */
    FileInfo getFileInfoByFileIdAndUserId(String fileId, String userId);

    /**
     * 根据FileIdAndUserId修改.
     *
     * @param bean 文件信息
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 影响行数
     */
    Integer updateFileInfoByFileIdAndUserId(FileInfo bean, String fileId, String userId);

    /**
     * 根据FileIdAndUserId删除.
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 影响行数
     */
    Integer deleteFileInfoByFileIdAndUserId(String fileId, String userId);

    /**
     * 上传文件.
     *
     * @param webUserDto 用户信息
     * @param fileId 文件ID
     * @param file 文件
     * @param fileName 文件名
     * @param filePid 父目录ID
     * @param fileMd5 文件MD5
     * @param chunkIndex 分片索引
     * @param chunks 总分片数
     * @return 上传结果
     */
    UploadResultDto uploadFile(SessionWebUserDto webUserDto, String fileId, MultipartFile file,
            String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks);

    /**
     * 重命名.
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @param fileName 新文件名
     * @return 文件信息
     */
    FileInfo rename(String fileId, String userId, String fileName);

    /**
     * 新建文件夹.
     *
     * @param filePid 父目录ID
     * @param userId 用户ID
     * @param folderName 文件夹名
     * @return 文件信息
     */
    FileInfo newFolder(String filePid, String userId, String folderName);

    /**
     * 更改文件目录.
     *
     * @param fileIds 文件ID列表
     * @param filePid 目标目录ID
     * @param userId 用户ID
     */
    void changeFileFolder(String fileIds, String filePid, String userId);

    /**
     * 移动文件到回收站.
     *
     * @param userId 用户ID
     * @param fileIds 文件ID列表
     */
    void removeFile2RecycleBatch(String userId, String fileIds);

    /**
     * 从回收站恢复文件.
     *
     * @param userId 用户ID
     * @param fileIds 文件ID列表
     */
    void recoverFileBatch(String userId, String fileIds);

    /**
     * 删除文件.
     *
     * @param userId 用户ID
     * @param fileIds 文件ID列表
     * @param adminOp 是否管理员操作
     */
    void delFileBatch(String userId, String fileIds, Boolean adminOp);

    /**
     * 检查根目录.
     *
     * @param rootFilePid 根目录ID
     * @param userId 用户ID
     * @param fileId 文件ID
     */
    void checkRootFilePid(String rootFilePid, String userId, String fileId);

    /**
     * 保存分享.
     *
     * @param shareRootFilePid 分享根目录ID
     * @param shareFileIds 分享文件ID列表
     * @param myFolderId 我的文件夹ID
     * @param shareUserId 分享用户ID
     * @param cureentUserId 当前用户ID
     */
    void saveShare(String shareRootFilePid, String shareFileIds, String myFolderId,
            String shareUserId, String cureentUserId);

    /**
     * 获取用户已使用空间.
     *
     * @param userId 用户ID
     * @return 已使用空间
     */
    Long getUserUseSpace(@Param("userId") String userId);

    /**
     * 删除用户所有文件.
     *
     * @param userId 用户ID
     */
    void deleteFileByUserId(@Param("userId") String userId);
}
