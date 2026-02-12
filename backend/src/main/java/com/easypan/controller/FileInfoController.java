package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.dto.UploadResultDto;
import com.easypan.entity.enums.FileCategoryEnums;
import com.easypan.entity.enums.FileDelFlagEnums;
import com.easypan.entity.enums.FileFolderTypeEnums;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.vo.FileInfoVO;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.entity.vo.FolderVO;
import com.easypan.entity.vo.ResponseVO;
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.annotation.Resource;
import java.util.List;

/**
 * 文件信息 Controller
 */
@Component
@RestController("fileInfoController")
@RequestMapping("/file")
@Tag(name = "File Management", description = "File operations including upload, download, and management")
public class FileInfoController extends CommonFileController {

    /**
     * 根据条件分页查询
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
     * 游标分页查询（性能优于 OFFSET 分页）
     * 适用于大数据量场景，避免深分页性能问题
     */
    @RequestMapping("/loadDataListCursor")
    @GlobalInterceptor(checkParams = true)
    @Operation(summary = "Load Data List with Cursor", description = "Cursor-based pagination for better performance")
    public ResponseVO<com.easypan.entity.query.CursorPage<FileInfoVO>> loadDataListCursor(
            HttpSession session,
            String cursor,
            Integer pageSize) {
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        com.easypan.entity.query.CursorPage<FileInfo> result = 
            fileInfoService.findListByCursor(userDto.getUserId(), cursor, pageSize);
        
        List<FileInfoVO> voList = CopyTools.copyList(result.getList(), FileInfoVO.class);
        com.easypan.entity.query.CursorPage<FileInfoVO> voResult = 
            com.easypan.entity.query.CursorPage.of(voList, result.getNextCursor(), result.getPageSize());
        
        return getSuccessResponseVO(voResult);
    }

    /**
     * 上传文件
     */
    @RequestMapping("/uploadFile")
    @GlobalInterceptor(checkParams = true)
    @Operation(summary = "Upload File", description = "Upload file with chunk support")
    public ResponseVO<UploadResultDto> uploadFile(HttpSession session,
            String fileId,
            MultipartFile file,
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
     * 获取已上传分片信息（用于断点续传）
     */
    @RequestMapping("/uploadedChunks")
    @GlobalInterceptor(checkParams = true)
    @Operation(summary = "Get Uploaded Chunks", description = "Get already uploaded chunk indices for resumable upload")
    public ResponseVO<java.util.List<Integer>> getUploadedChunks(HttpSession session,
            @VerifyParam(required = true) String fileId,
            @VerifyParam(required = true) String filePid) {

        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        String userId = webUserDto.getUserId();

        // 复用已有的临时目录结构：/temp/{userId}{fileId}
        String tempFolder = appConfig.getProjectFolder()
                + com.easypan.entity.constants.Constants.FILE_FOLDER_TEMP
                + userId + fileId;

        java.io.File folder = new java.io.File(tempFolder);
        if (!folder.exists() || !folder.isDirectory()) {
            return getSuccessResponseVO(java.util.Collections.emptyList());
        }

        java.io.File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            return getSuccessResponseVO(java.util.Collections.emptyList());
        }

        java.util.List<Integer> uploadedChunks = java.util.Arrays.stream(files)
                .filter(java.io.File::isFile)
                .map(java.io.File::getName)
                .filter(name -> name.chars().allMatch(Character::isDigit))
                .map(Integer::valueOf)
                .sorted()
                .toList();

        return getSuccessResponseVO(uploadedChunks);
    }

    /**
     * 获取图片
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
     * 根据视频id获取视频分片
     */
    @RequestMapping("/ts/getVideoInfo/{fileId}")
    @Operation(summary = "Get Video Info", description = "Get video m3u8 or ts file")
    public void getVideoInfo(HttpServletResponse response, HttpSession session,
            @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        super.getFile(response, fileId, webUserDto.getUserId());
    }

    /**
     * 根据文件id获取文件
     */
    @RequestMapping("/getFile/{fileId}")
    @Operation(summary = "Get File", description = "Get file content")
    public void getFile(HttpServletResponse response, HttpSession session,
            @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        super.getFile(response, fileId, webUserDto.getUserId());
    }

    /**
     * 新建文件夹
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
     * 获取文件夹信息
     */
    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor(checkParams = true)
    @Operation(summary = "Get Folder Info", description = "Get folder path info")
    public ResponseVO<List<FolderVO>> getFolderInfo(HttpSession session,
            @VerifyParam(required = true) @Parameter(description = "Folder Path") String path) {
        return super.getFolderInfo(path, getUserInfoFromSession(session).getUserId());
    }

    /**
     * 重命名
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
     * 加载所有文件
     */
    @RequestMapping("/loadAllFolder")
    @GlobalInterceptor(checkParams = true)
    @Operation(summary = "Load All Folders", description = "Load all folders for moving files")
    public ResponseVO<List<FileInfoVO>> loadAllFolder(HttpSession session, @VerifyParam(required = true) String filePid,
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
     * 更改文件目录
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
     * 创建下载链接
     */
    @RequestMapping("/createDownloadUrl/{fileId}")
    @GlobalInterceptor(checkParams = true)
    @Operation(summary = "Create Download URL", description = "Create a temporary download URL")
    public ResponseVO<String> createDownloadUrl(HttpSession session,
            @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        return super.createDownloadUrl(fileId, getUserInfoFromSession(session).getUserId());
    }

    /**
     * 下载
     */
    @RequestMapping("/download/{code}")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    @Operation(summary = "Download File", description = "Download file by code")
    public void download(HttpServletRequest request, HttpServletResponse response,
            @PathVariable("code") @VerifyParam(required = true) String code) throws Exception {
        super.download(request, response, code);
    }

    @Resource
    private com.easypan.service.FileOperationService fileOperationService;

    @RequestMapping("/delFile")
    @GlobalInterceptor(checkParams = true)
    @Operation(summary = "Delete File", description = "Delete files to recycle bin")
    public ResponseVO<Void> delFile(HttpSession session, @VerifyParam(required = true) String fileIds) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.removeFile2RecycleBatch(webUserDto.getUserId(), fileIds);
        return getSuccessResponseVO(null);
    }
    
    @RequestMapping("/batchDownload/{fileIds}")
    @GlobalInterceptor(checkParams = true)
    @Operation(summary = "Batch Download", description = "Download multiple files as zip")
    public void batchDownload(HttpServletResponse response, HttpSession session,
            @PathVariable("fileIds") @VerifyParam(required = true) String fileIds) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        try {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=\"download.zip\"");
            fileOperationService.downloadMultipleFiles(java.util.Arrays.asList(fileIds.split(",")), response.getOutputStream());
        } catch (java.io.IOException e) {
            throw new com.easypan.exception.BusinessException("Download failed");
        }
    }
}
