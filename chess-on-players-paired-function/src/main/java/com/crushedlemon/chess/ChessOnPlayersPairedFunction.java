package com.crushedlemon.chess;

import software.amazon.awssdk.services.apigatewaymanagementapi.ApiGatewayManagementApiClient;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.PostToConnectionRequest;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.PostToConnectionResponse;
import software.amazon.awssdk.core.SdkBytes;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static java.util.Map.entry;

public class ChessOnPlayersPairedFunction implements RequestHandler<SQSEvent, Void> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String CONNECTIONS_URI = "https://wec2i3hiw3.execute-api.eu-north-1.amazonaws.com/production";

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        for (SQSEvent.SQSMessage message : event.getRecords()) {
            String body = message.getBody();
            context.getLogger().log("Received message: " + body);
            try {
                Map<String, Object> messageMap = (Map<String, Object>) objectMapper.readValue(body, Map.class);
                String whiteUser = (String) messageMap.get("whiteUser");
                String blackUser = (String) messageMap.get("blackUser");

                Map<String, Object> gameSettingsMap = (Map<String, Object>) messageMap.get("gameSettings");
                Integer gameDuration = (Integer) gameSettingsMap.get("gameDuration");
                Integer incrementPerMove = (Integer) gameSettingsMap.get("incrementPerMove");

                String gameId = UUID.randomUUID().toString();

                DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());
                Table chessGamesTable = dynamoDB.getTable("chess-games");

                Long startTime = Instant.now().toEpochMilli();

                String whiteConnectionId = getConnectionId(dynamoDB, whiteUser);
                String blackConnectionId = getConnectionId(dynamoDB, blackUser);

                Item item = Item.fromMap(
                        Map.ofEntries(
                                entry("gameId", gameId),
                                entry("whiteUser", whiteUser),
                                entry("blackUser", blackUser),
                                entry("whiteConnectionId", whiteConnectionId),
                                entry("blackConnectionId", blackConnectionId),
                                entry("gameDuration", gameDuration),
                                entry("incrementPerMove", incrementPerMove),
                                entry("startTime", startTime),
                                entry("gameState", "ONGOING"),
                                entry("gameType", "GAME_TYPE_CLASSIC"),
                                entry("gameResult", "GAME_RESULT_UNKNOWN"),
                                entry("board", "RNBQKBNRPPPPPPPPXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXpppppppprnbqkbnr"),
                                entry("flags", 15),
                                entry("winnerId", "")
                ));

                chessGamesTable.putItem(item);

                sendStartGameMessage(blackConnectionId, buildStartGameMessage(gameId, "BLACK"));
                sendStartGameMessage(whiteConnectionId, buildStartGameMessage(gameId, "WHITE"));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private void sendStartGameMessage(String connectionId, String startGameMessage) {
        ApiGatewayManagementApiClient client = ApiGatewayManagementApiClient.builder()
                .endpointOverride(URI.create(CONNECTIONS_URI))
                .build();

        PostToConnectionRequest postRequest = PostToConnectionRequest.builder()
                .connectionId(connectionId)
                .data(SdkBytes.fromUtf8String(startGameMessage))
                .build();

        try {
            PostToConnectionResponse response = client.postToConnection(postRequest);
            System.out.println("Message sent! Status Code: " + response.sdkHttpResponse().statusCode());
        } catch (Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    private String buildStartGameMessage(String gameId, String color) {
        try {
            return objectMapper.writeValueAsString(Map.of("action", "gameStarted", "gameId", gameId, "color", color));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String getConnectionId(DynamoDB dynamoDB, String user) {
        Table chessConnectionsTable = dynamoDB.getTable("chess-connections");
        Item item = chessConnectionsTable.getItem("userId", user);
        return (String) item.get("connectionId");
    }
}
