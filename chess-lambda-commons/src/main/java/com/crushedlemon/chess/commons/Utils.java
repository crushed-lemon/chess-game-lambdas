package com.crushedlemon.chess.commons;

import com.crushedlemon.chess.commons.model.GamePreferences;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class Utils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String extractConnectionId(Map<String, Object> event) {
        Map<String, Object> requestContext = (Map<String, Object>) event.get("requestContext");
        return (String) requestContext.get("connectionId");
    }

    public static String extractUserName(Map<String, Object> event) {
        // For testing purposes, passing the username in request body. Change it to JWT-based identity.
        String bodyJson = (String) event.get("body");
        try {
            Map<String, Object> bodyMap = (Map<String, Object>) objectMapper.readValue(bodyJson, Map.class);
            return  (String) bodyMap.get("user");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static GamePreferences extractGamePreferences(Map<String, Object> event) {
        // TODO : Extract the Game preferences
        return GamePreferences.builder().build();
    }
}
