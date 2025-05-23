package com.crushedlemon.chess.commons;

import com.crushedlemon.chess.commons.model.GameDuration;
import com.crushedlemon.chess.commons.model.GamePreferences;
import com.crushedlemon.chess.commons.model.IncrementPerMove;
import com.crushedlemon.chess.commons.model.PlayAs;
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
        // For testing purposes, passing the username in query params. Change it to JWT-based identity.
        Map<String, Object> queryStringParameters = (Map<String, Object>) event.get("queryStringParameters");
        if (queryStringParameters == null) {
            // For testing purposes, passing the username in request body. Change it to JWT-based identity.
            String bodyJson = (String) event.get("body");
            try {
                Map<String, Object> bodyMap = (Map<String, Object>) objectMapper.readValue(bodyJson, Map.class);
                return  (String) bodyMap.get("userName");
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return (String) queryStringParameters.get("userName");
    }

    public static GamePreferences extractGamePreferences(Map<String, Object> event) {
        String bodyJson = (String) event.get("body");
        try {
            Map<String, Object> bodyMap = (Map<String, Object>) objectMapper.readValue(bodyJson, Map.class);
            Map<String, Object> gamePreferences = (Map<String, Object>) bodyMap.get("gamePreferences");

            String gameDurationString = (String) gamePreferences.get("gameDuration");
            GameDuration gameDuration = GameDuration.valueOf(gameDurationString);

            String incrementPerMoveString = (String) gamePreferences.get("incrementPerMove");
            IncrementPerMove incrementPerMove = IncrementPerMove.valueOf(incrementPerMoveString);

            String playAsString = (String) gamePreferences.get("playAs");
            PlayAs playAs = PlayAs.valueOf(playAsString);

            return GamePreferences.builder()
                    .gameDuration(gameDuration)
                    .incrementPerMove(incrementPerMove)
                    .playAs(playAs)
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
