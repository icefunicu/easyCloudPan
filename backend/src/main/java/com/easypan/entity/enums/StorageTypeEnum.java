package com.easypan.entity.enums;

public enum StorageTypeEnum {
    LOCAL("local", "本地存储"),
    MINIO("minio", "MinIO"),
    OSS("oss", "阿里云OSS");

    private final String code;
    private final String desc;

    StorageTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static StorageTypeEnum getByCode(String code) {
        for (StorageTypeEnum item : StorageTypeEnum.values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }
}
