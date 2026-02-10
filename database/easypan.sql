/*
 Navicat Premium Data Transfer

 Source Server         : Mysql
 Source Server Type    : MySQL
 Source Server Version : 80032 (8.0.32)
 Source Host           : localhost:3306
 Source Schema         : easypan

 Target Server Type    : MySQL
 Target Server Version : 80032 (8.0.32)
 File Encoding         : 65001

 Date: 22/04/2024 15:04:54
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for email_code
-- ----------------------------
DROP TABLE IF EXISTS `email_code`;
CREATE TABLE `email_code`  (
  `email` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '邮箱',
  `code` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '编号',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '0:未使用  1:已使用',
  PRIMARY KEY (`email`, `code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '邮箱验证码' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of email_code
-- ----------------------------
INSERT INTO `email_code` VALUES ('2041487752@qq.com', '30459', '2024-03-16 16:21:41', 1);
INSERT INTO `email_code` VALUES ('im@icefun.icu', '79579', '2024-03-10 21:32:30', 1);

-- ----------------------------
-- Table structure for file_info
-- ----------------------------
DROP TABLE IF EXISTS `file_info`;
CREATE TABLE `file_info`  (
  `file_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件ID',
  `user_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `file_md5` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'md5值，第一次上传记录',
  `file_pid` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '父级ID',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小',
  `file_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件名称',
  `file_cover` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '封面',
  `file_path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件路径',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `last_update_time` datetime NULL DEFAULT NULL COMMENT '最后更新时间',
  `folder_type` tinyint(1) NULL DEFAULT NULL COMMENT '0:文件 1:目录',
  `file_category` tinyint(1) NULL DEFAULT NULL COMMENT '1:视频 2:音频  3:图片 4:文档 5:其他',
  `file_type` tinyint(1) NULL DEFAULT NULL COMMENT ' 1:视频 2:音频  3:图片 4:pdf 5:doc 6:excel 7:txt 8:code 9:zip 10:其他',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '0:转码中 1转码失败 2:转码成功',
  `recovery_time` datetime NULL DEFAULT NULL COMMENT '回收站时间',
  `del_flag` tinyint(1) NULL DEFAULT 2 COMMENT '删除标记 0:删除  1:回收站  2:正常',
  PRIMARY KEY (`file_id`, `user_id`) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_md5`(`file_md5` ASC) USING BTREE,
  INDEX `idx_file_pid`(`file_pid` ASC) USING BTREE,
  INDEX `idx_del_flag`(`del_flag` ASC) USING BTREE,
  INDEX `idx_recovery_time`(`recovery_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of file_info
-- ----------------------------
INSERT INTO `file_info` VALUES ('ECRbnErOx6', '8546374074', NULL, '0', NULL, '文件', NULL, NULL, '2024-03-15 15:45:33', '2024-03-15 15:45:33', 1, NULL, NULL, 2, NULL, 2);
INSERT INTO `file_info` VALUES ('iLlL5nAfGP', '8546374074', 'a6fde0a218ab52c264d5706c16f65dbb', '0', 39816485, 'BLACKPINK - ‘뚜두뚜두 (DDU-DU DDU-DU)’ M_V.mp4', '202403/8546374074iLlL5nAfGP.png', '202403/8546374074iLlL5nAfGP.mp4', '2024-03-14 20:49:31', '2024-03-14 20:49:31', 0, 1, 1, 2, NULL, 2);
INSERT INTO `file_info` VALUES ('OSg5iEf7Eu', '1147240164', 'd49b6891b9fa33ddbf2ce4c7e9ebe492', '0', 1867645, 'yuanshen.jpg', '202403/1147240164OSg5iEf7Eu_.jpg', '202403/1147240164OSg5iEf7Eu.jpg', '2024-03-16 19:56:48', '2024-03-16 19:56:48', 0, 3, 3, 2, NULL, 2);
INSERT INTO `file_info` VALUES ('QKBal8x8gy', '8546374074', 'b765263edae0e0c97d09b50e4db9c41f', 'ECRbnErOx6', 1844977, 'wallhaven-1pokr3.jpg', '202403/8546374074QKBal8x8gy_.jpg', '202403/8546374074QKBal8x8gy.jpg', '2024-03-15 15:45:50', '2024-03-15 15:45:50', 0, 3, 3, 2, NULL, 2);

-- ----------------------------
-- Table structure for file_share
-- ----------------------------
DROP TABLE IF EXISTS `file_share`;
CREATE TABLE `file_share`  (
  `share_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分享ID',
  `file_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件ID',
  `user_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `valid_type` tinyint(1) NULL DEFAULT NULL COMMENT '有效期类型 0:1天 1:7天 2:30天 3:永久有效',
  `expire_time` datetime NULL DEFAULT NULL COMMENT '失效时间',
  `share_time` datetime NULL DEFAULT NULL COMMENT '分享时间',
  `code` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '提取码',
  `show_count` int NULL DEFAULT 0 COMMENT '浏览次数',
  PRIMARY KEY (`share_id`) USING BTREE,
  INDEX `idx_file_id`(`file_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_share_time`(`share_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '分享信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of file_share
-- ----------------------------
INSERT INTO `file_share` VALUES ('JUaAODkJyn2oJqhdt4BQ', 'OSg5iEf7Eu', '1147240164', 0, '2024-03-17 19:58:32', '2024-03-16 19:58:32', '7n3Zf', 0);

-- ----------------------------
-- Table structure for user_info
-- ----------------------------
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info`  (
  `user_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `nick_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '昵称',
  `email` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `qq_open_id` varchar(35) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'qqOpenID',
  `qq_avatar` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'qq头像',
  `password` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '密码',
  `join_time` datetime NULL DEFAULT NULL COMMENT '加入时间',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `status` tinyint NULL DEFAULT NULL COMMENT '0:禁用 1:正常',
  `use_space` bigint NULL DEFAULT 0 COMMENT '使用空间单位byte',
  `total_space` bigint NULL DEFAULT NULL COMMENT '总空间',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `key_email`(`email` ASC) USING BTREE,
  UNIQUE INDEX `key_nick_name`(`nick_name` ASC) USING BTREE,
  UNIQUE INDEX `key_qq_open_id`(`qq_open_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_info
-- ----------------------------
INSERT INTO `user_info` VALUES ('1147240164', 'admin', '2041487752@qq.com', NULL, NULL, '2dc5d9bb37f3c0b7b81db599b992e4be', '2024-03-16 16:22:50', '2024-03-16 19:56:34', 1, 1867645, 1073741824);
INSERT INTO `user_info` VALUES ('3178033358', '测试账号', 'test@qq.com', NULL, '', '47ec2dd791e31e2ef2076caf64ed9b3d', NULL, '2024-03-08 16:16:18', 1, 238302835, 1073741824);
INSERT INTO `user_info` VALUES ('8546374074', 'im', 'im@icefun.icu', NULL, '', '2dc5d9bb37f3c0b7b81db599b992e4be', '2024-03-10 21:34:04', '2024-03-16 16:19:12', 1, 41661462, 1073741824);

SET FOREIGN_KEY_CHECKS = 1;
