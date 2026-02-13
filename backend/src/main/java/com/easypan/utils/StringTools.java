package com.easypan.utils;

import com.easypan.entity.constants.Constants;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * 字符串工具类.
 */
public class StringTools {

    /**
     * MD5 加密.
     *
     * @param originString 原始字符串
     * @return 加密后的字符串
     */
    public static String encodeByMD5(String originString) {
        return StringTools.isEmpty(originString) ? null : DigestUtils.md5Hex(originString);
    }

    /**
     * 判断字符串是否为空.
     *
     * @param str 字符串
     * @return 是否为空
     */
    public static boolean isEmpty(String str) {

        if (null == str || "".equals(str) || "null".equals(str) || "\u0000".equals(str)) {
            return true;
        } else if ("".equals(str.trim())) {
            return true;
        }
        return false;
    }

    /**
     * 获取文件后缀.
     *
     * @param fileName 文件名
     * @return 后缀
     */
    public static String getFileSuffix(String fileName) {
        Integer index = fileName.lastIndexOf(".");
        if (index == -1) {
            return "";
        }
        String suffix = fileName.substring(index);
        return suffix;
    }

    /**
     * 获取无后缀的文件名.
     *
     * @param fileName 文件名
     * @return 无后缀的文件名
     */
    public static String getFileNameNoSuffix(String fileName) {
        Integer index = fileName.lastIndexOf(".");
        if (index == -1) {
            return fileName;
        }
        fileName = fileName.substring(0, index);
        return fileName;
    }

    /**
     * 重命名文件.
     *
     * @param fileName 文件名
     * @return 新文件名
     */
    public static String rename(String fileName) {
        String fileNameReal = getFileNameNoSuffix(fileName);
        String suffix = getFileSuffix(fileName);
        return fileNameReal + "_" + getRandomString(Constants.LENGTH_5) + suffix;
    }

    /**
     * 获取随机字符串.
     *
     * @param count 长度
     * @return 随机字符串
     */
    public static final String getRandomString(Integer count) {
        return RandomStringUtils.random(count, true, true);
    }

    /**
     * 获取随机数字.
     *
     * @param count 长度
     * @return 随机数字
     */
    public static final String getRandomNumber(Integer count) {
        return RandomStringUtils.random(count, false, true);
    }

    /**
     * 转义标题中的 HTML 标签.
     *
     * @param content 内容
     * @return 转义后的内容
     */
    public static String escapeTitle(String content) {
        if (isEmpty(content)) {
            return content;
        }
        content = content.replace("<", "&lt;");
        return content;
    }

    /**
     * 转义 HTML 内容.
     *
     * @param content 内容
     * @return 转义后的内容
     */
    public static String escapeHtml(String content) {
        if (isEmpty(content)) {
            return content;
        }
        content = content.replace("<", "&lt;");
        content = content.replace(" ", "&nbsp;");
        content = content.replace("\n", "<br>");
        return content;
    }

    /**
     * 检查路径是否安全.
     *
     * @param path 路径
     * @return 是否安全
     */
    public static boolean pathIsOk(String path) {
        if (StringTools.isEmpty(path)) {
            return true;
        }
        if (path.contains("../") || path.contains("..\\")) {
            return false;
        }
        return true;
    }
}
