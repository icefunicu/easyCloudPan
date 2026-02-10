package com.easypan.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class JsonUtils {
    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String convertObj2Json(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("convertObj2Json error", e);
            throw new RuntimeException(e);
        }
    }

    public static <T> T convertJson2Obj(String json, Class<T> classz) {
        try {
            return objectMapper.readValue(json, classz);
        } catch (JsonProcessingException e) {
            logger.error("convertJson2Obj error", e);
            throw new RuntimeException(e);
        }
    }

    public static <T> List<T> convertJsonArray2List(String json, Class<T> classz) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, classz));
        } catch (JsonProcessingException e) {
            logger.error("convertJsonArray2List error", e);
            throw new RuntimeException(e);
        }
    }

}
