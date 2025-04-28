package com.crushedlemon.chess.commons;

import com.crushedlemon.chess.commons.model.GamePreferences;

import java.util.Map;

public class Utils {

    public static String extractConnectionId(Map<String, Object> event) {
        Map<String, Object> requestContext = (Map<String, Object>) event.get("requestContext");
        return (String) requestContext.get("connectionId");
    }

    public static String extractUserName(Map<String, Object> event) {
        // For testing purposes, passing the username in query params. Change it to JWT-based identity.
        Map<String, Object> queryStringParameters = (Map<String, Object>) event.get("queryStringParameters");
        return (String) queryStringParameters.get("userName");
    }

    public static GamePreferences extractGamePreferences(Map<String, Object> event) {
        // TODO : Extract the Game preferences
        return GamePreferences.builder().build();
    }
}
