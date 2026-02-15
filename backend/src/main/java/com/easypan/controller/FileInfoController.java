package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.dto.UploadResultDto;
import com.easypan.entity.enums.FileCategoryEnums;
import com.easypan.entity.enums.FileDelFlagEnums;
import com.easypan.entity.enums.FileFolderTypeEnums;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.query.CursorPage;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.vo.FileInfoVO;
import com.easypan.entity.vo.FolderVO;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.exception.BusinessException;
import com.easypan.service.FileOperationService;
import com.easypan.utils.CopyTools;
import com.easypan.utils.StringTools;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 文件信息控制器类.
 */
@Component
@RestController("fileInfoController")
@RequestMapping("/file")
@Tag(name = "File Management", description = "File operations including upload, download, and management")
public class FileInfoController extends CommonFileController {

    @Resource
    private FileOperationService fileOperationService;

    /**
     * 根据条件分页查询.
     *
     * @param session  HTTP 会话
     * @param query    查询参数
     * @param category 分类
     * @return 分页结果
     */
    @RequestMapping("/loadDataList")
    @GlobalInterceptor(checkParams = true)
    @Operation(summary = "Load Data List", description = "Pagination query for file list")
    public ResponseVO<PaginationResultVO<FileInfoVO>> loadDataList(HttpSession session, FileInfoQuery query,
            String category) {
        FileCategoryEnums categoryEnum = FileCategoryEnums.getByCode(category);
        if (null != categoryEnum) {
            query.setFileCategory(categoryEnum.getCategory());
        }
        query.setUserId(getUserInfoFromSession(session).getUserId());
        query.setOrderBy("last_update_time desc");
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        PaginationResultVO<FileInfo> result = fileInfoService.findListByPage(query);
        return getSuccessResponseVO(convert2PaginationVO(result, FileInfoVO.class));
    }

    /**
     * 游标分页查询（性能优于 OFFSET 分页）.
     * 适用于大数据量场景，避免深分页性能问题.
     *
     * @param session  HTTP 会话
     * @param cursor   游标
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @RequestMapping("/loadDataListCursor")
    @GlobalInterceptor(checkParams = true)
    @Operation(summary = "Load Data List with Cursor", description = "Cursor-based pagination for better performance")
    public ResponseVO<CursorPage<FileInfoVO>> loadDataListCursor(
            HttpSession session,
            String cursor,
            Integer pageSize) {
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        CursorPage<FileInfo> result = fileInfoService.findListByCursor(userDto.getUserId(), cursor, pageSize);

        List<FileInfoVO> voList = CopyTools.copyList(result.getList(), FileInfoVO.class);
        CursorPage<FileInfoVO> voResult = CursorPage.of(voList, result.getNextCursor(), result.getPageSize());

        return getSuccessResponseVO(voResult);
    }

    /**
     * 上传文件.
     *
     * @param session    HTTP 会话
     * @param fileId     文件ID
     * @param file       文件
     * @param fileName   文件名
     * @param filePid    父目录ID
     * @param fileMd5    文件MD5
     * @param chunkIndex 分片索引
     * @param chunks     总分片数
     * @return 上传结果
     */
    @RequestMapping("/uploadFile")
    @GlobalInterceptor(checkParams = true)
    @Operation(summary = "Upload File", description = "Upload file with chunk support")
    public ResponseVO<UploadResultDto> uploadFile(HttpSession session,
            String fileId,
            @org.springframework.web.bind.annotation.RequestParam("file") MultipartFile file,
            @VerifyParam(required = true) String fileName,
            @VerifyParam(required = true) String filePid,
            @VerifyParam(required = true) String fileMd5,
            @VerifyParam(required = true) Integer chunkIndex,
            @VerifyParam(required = true) Integer chunks) {

        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        UploadResultDto resultDto = fileInfoService.uploadFile(webUserDto, fileId, file, fileName, filePid, fileMd5,
                chunkIndex, chunks);
        return getSuccessResponseVO(resultDto);
    }

    /**
     * 获取已上传分片信息（用于断点续传）.
     *
     * @param session HTTP 会话
     * @param fileId  文件ID
     * @param filePid 父目录ID
     * @return 已上传分片列表
     */
    @RequestMapping("/uploadedChunks")
    @GlobalInterceptor(checkParams = true)
    @Operation(summary = "Get Uploaded Chunks", description = "Get already uploaded chunk indices for resumable upload")
    public ResponseVO<List<Integer>> getUploadedChunks(HttpSession session,
            @VerifyParam(required = true) String fileId,
            @VerifyParam(required = true) String filePid) {

        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        String userId = webUserDto.getUserId();

        String tempFolder = appConfig.getProjectFolder()
                + Constants.FILE_FOLDER_TEMP
                + userId + fileId;

        File folder = new File(tempFolder);
        if (!folder.exists() || !folder.isDirectory()) {
            return getSuccessResponseVO(Collections.emptyList());
        }

        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            return getSuccessResponseVO(Collections.emptyList());
        }

        List<Integer> uploadedChunks = Arrays.stream(files)
                .filter(File::isFile)
                .map(File::getName)
                .filter(name -> name.chars().allMatch(Character::isDigit))
                .map(Integer::valueOf)
                .sorted()
                .toList();

        return getSuccessResponseVO(uploadedChunks);
    }

    @Resource
    private com.easypan.service.UploadProgressService uploadProgressService;

    /**
     * 获取上传进度.
     *
     * @param session HTTP 会话
     * @param fileId  文件ID
     * @return 进度信息
     */
    @RequestMapping("/getUploadProgress")
    @GlobalInterceptor(checkParams = true)
    @Operation(summary = "Get Upload Progress", description = "Get current upload progress")
    public ResponseVO<com.easypan.entity.dto.UploadProgressDto> getUploadProgress(HttpSession session,
            @VerifyParam(required = true) String fileId) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        com.easypan.entity.dto.UploadProgressDto progress = uploadProgressService.getProgress(webUserDto.getUserId(),
                fileId);
        return getSuccessResponseVO(progress);
    }

    /**
     * 查询文件转码状态.
     *
     * @param session HTTP 会话
     * @param fileId  文件ID
     * @return 包含 status 字段的响应
     */
    @RequestMapping("/transferStatus")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<com.easypan.entity.dto.FileTransferStatusDto> getTransferStatus(
            HttpSession session,
            @VerifyParam(required = true) String fileId) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileId, webUserDto.getUserId());
        if (fileInfo == null) {
            throw new BusinessException("文件不存在");
        }
        com.easypan.entity.dto.FileTransferStatusDto dto = new com.easypan.entity.dto.FileTransferStatusDto();
        dto.setFileId(fileId);
        dto.setStatus(fileInfo.getStatus());
        return getSuccessResponseVO(dto);
    }

    /**
     * 获取图片.
     *
     * @param session     HTTP 会话
     * @param response    HTTP 响应
     * @param imageFolder 图片文件夹
     * @param imageName   图片名称
     */
    @RequestMapping("/getImage/{imageFolder}/{imageName}")
    @GlobalInterceptor(checkLogin = true)
    @Operation(summary = "Get Image", description = "Get image by folder and name")
    public void getImage(HttpSession session, HttpServletResponse response,
            @PathVariable("imageFolder") String imageFolder,
            @PathVariable("imageName") String imageName) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        super.getImage(response, imageFolder, imageName, webUserDto.getUserId());
    }

    /**
     * 根据视频id获取视频分片.
     *
     * @param response HTTP 响应
     * @param session  HTTP 会话
     * @param fileId   文件ID
     */
    @RequestMapping("/ts/getVideoInfo/{fileId}")
    @Operation(summary = "Get Video Info", description = "Get video m3u8 or ts file")
    public void getVideoInfo(HttpServletResponse response, HttpSession session,
            @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        super.getFile(response, fileId, webUserDto.getUserId());
    }

    /**
     * 根据文件id获取文件.
     *
     * @param response HTTP 响应
     * @param session  HTTP 会话
     * @param fileId   文件ID
     */
    @RequestMapping("/getFile/{fileId}")
    @Operation(summary = "Get File", description = "Get file content")
    public void getFile(HttpServletResponse response, HttpSession session,
            @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        super.getFile(response, fileId, webUserDto.getUserId());
    }

    /**
     * 新建文件夹.
     *
     * @param session  HTTP 会话
     * @param filePid  父目录ID
     * @param fileName 文件名
     * @return 文件信息
     */
    @RequestMapping("/newFoloder")
    @GlobalInterceptor(checkParams = true)
    @Operation(summary = "New Folder", description = "Create a new folder")
    public ResponseVO<FileInfo> newFoloder(HttpSession session,
            @VerifyParam(required = true) String filePid,
            @VerifyParam(required = true) String fileName) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        FileInfo fileInfo = fileInfoService.newFolder(filePid, webUserDto.getUserId(), fileName);
        return getSuccessResponseVO(fileInfo);
    }

    /**
     * 获取文件夹信息.
     *
     * @param session HTTP 会话
     * @param path    路径
     * @return 文件夹列表
     */
    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor(checkParams = true)
    @Operation(summary = "Get Folder Info", description = "Get folder path info")
    public ResponseVO<List<FolderVO>> getFolderInfo(HttpSession session,
            @VerifyParam(required = true) @Parameter(description = "Folder Path") String path) {
        return super.getFolderInfo(path, getUserInfoFromSession(session).getUserId());
    }

    /**
     * 重命名.
     *
     * @param session  HTTP 会话
     * @param fileId   文件ID
     * @param fileName 新文件名
     * @return 文件信息
     */
    @RequestMapping("/rename")
    @GlobalInterceptor(checkParams = true)
    @Operation(summary = "Rename File", description = "Rename a file or folder")
    public ResponseVO<FileInfoVO> rename(HttpSession session,
            @VerifyParam(required = true) String fileId,
            @VerifyParam(required = true) String fileName) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        FileInfo fileInfo = fileInfoService.rename(fileId, webUserDto.getUserId(), fileName);
        return getSuccessResponseVO(CopyTools.copy(fileInfo, FileInfoVO.class));
    }

    /**
     * 加载所有文件.
     *
     * @param session        HTTP 会话
     * @param filePid        父目录ID
     * @param currentFileIds 当前文件ID列表
     * @return 文件列表
     */
    @RequestMapping("/loadAllFolder")
    @GlobalInterceptor(checkParams = true)
    @Operation(summary = "Load All Folders", description = "Load all folders for moving files")
    public ResponseVO<List<FileInfoVO>> loadAllFolder(HttpSession session,
            @VerifyParam(required = true) String filePid,
            String currentFileIds) {
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(getUserInfoFromSession(session).getUserId());
        query.setFilePid(filePid);
        query.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        if (!StringTools.isEmpty(currentFileIds)) {
            query.setExcludeFileIdArray(currentFileIds.split(","));
        }
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        query.setOrderBy("create_time desc");
        List<FileInfo> fileInfoList = fileInfoService.findListByParam(query);
        return getSuccessResponseVO(CopyTools.copyList(fileInfoList, FileInfoVO.class));
    }

    /**
     * 更改文件目录.
     *
     * @param session HTTP 会话
     * @param fileIds 文件ID列表
     * @param filePid 目标目录ID
     * @return 响应对象
     */
    @RequestMapping("/changeFileFolder")
    @GlobalInterceptor(checkParams = true)
    @Operation(summary = "Change File Folder", description = "Move files to another folder")
    public ResponseVO<Void> changeFileFolder(HttpSession session,
            @VerifyParam(required = true) String fileIds,
            @VerifyParam(required = true) String filePid) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.changeFileFolder(fileIds, filePid, webUserDto.getUserId());
        return getSuccessResponseVO(null);
    }

    /**
     * 创建下载链接.
     *
     * @param session HTTP 会话
     * @param fileId  文件ID
     * @return 下载码
     */
    @RequestMapping("/createDownloadUrl/{fileId}")
    @GlobalInterceptor(checkParams = true)
    @Operation(summary = "Create Download URL", description = "Create a temporary download URL")
    public ResponseVO<String> createDownloadUrl(HttpSession session,
            @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        return super.createDownloadUrl(fileId, getUserInfoFromSession(session).getUserId());
    }

    /**
     * 下载.
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param code     下载码
     * @throws Exception 异常
     */
    @RequestMapping("/download/{code}")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    @Operation(summary = "Download File", description = "Download file by code")
    public void download(HttpServletRequest request, HttpServletResponse response,
            @PathVariable("code") @VerifyParam(required = true) String code) throws Exception {
        super.download(request, response, code);
    }

    /**
     * 删除文件.
     *
     * @param session HTTP 会话
     * @param fileIds 文件ID列表
     * @return 响应对象
     */
    @RequestMapping("/delFile")
    @GlobalInterceptor(checkParams = true)
    @Operation(summary = "Delete File", description = "Delete files to recycle bin")
    public ResponseVO<Void> delFile(HttpSession session, @VerifyParam(required = true) String fileIds) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.removeFile2RecycleBatch(webUserDto.getUserId(), fileIds);
        return getSuccessResponseVO(null);
    }

    /**
     * 批量下载.
     *
     * @param response HTTP 响应
     * @param session  HTTP 会话
     * @param fileIds  文件ID列表
     */
    @RequestMapping("/batchDownload/{fileIds}")
    @GlobalInterceptor(checkParams = true)
    @Operation(summary = "Batch Download", description = "Download multiple files as zip")
    public void batchDownload(HttpServletResponse response, HttpSession session,
            @PathVariable("fileIds") @VerifyParam(required = true) String fileIds) {
        try {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=\"download.zip\"");
            fileOperationService.downloadMultipleFiles(
                    Arrays.asList(fileIds.split(",")), response.getOutputStream());
        } catch (IOException e) {
            throw new BusinessException("文件下载失败，请重试");
        }
    }
}
