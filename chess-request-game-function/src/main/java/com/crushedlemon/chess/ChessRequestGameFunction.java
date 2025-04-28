package com.crushedlemon.chess;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.crushedlemon.chess.commons.model.GamePreferences;

import java.util.Map;
import java.util.Optional;

public class ChessRequestGameFunction implements RequestHandler<Map<String, Object>, Object> {

    private final DynamoDB dynamoDb;

    ChessRequestGameFunction() {
        this.dynamoDb = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());
    }

    @Override
    public Object handleRequest(Map<String, Object> event, Context context) {
        // A common table used by many sections of the code
        Table chessLobbyTable = dynamoDb.getTable("chess-lobby");

        String userName = extractUserName(event);
        GamePreferences gamePreferences = extractGamePreferences(event);

        Optional<String> waitingUser = findWaitingUser(chessLobbyTable, gamePreferences);

        if (waitingUser.isPresent()) {
            // Pair these two users for a game
        } else {
            // Send this user to the lobby to wait for an appropriate partner
            chessLobbyTable.putItem(toItem(userName, gamePreferences));
        }
        return Map.of("statusCode", 200);
    }

    private Optional<String> findWaitingUser(Table chessLobbyTable, GamePreferences gamePreferences) {
        // TODO : Find a waiting user in the lobby
        return Optional.empty();
    }

    private Item toItem(String userName, GamePreferences gamePreferences) {
        return Item.fromMap(Map.of(
                "userId", userName,
                "gameDuration", gamePreferences.getGameDuration().toString(),
                "incrementPerMove", gamePreferences.getIncrementPerMove().toString(),
                "playAs", gamePreferences.getPlayAs().toString()
        ));
    }

    private GamePreferences extractGamePreferences(Map<String, Object> event) {
        // TODO : Extract the Game preferences
        return GamePreferences.builder().build();
    }

    private static String extractUserName(Map<String, Object> event) {
        // For testing purposes, passing the username in query params. Change it to JWT-based identity.
        Map<String, Object> queryStringParameters = (Map<String, Object>) event.get("queryStringParameters");
        return (String) queryStringParameters.get("userName");
    }
}