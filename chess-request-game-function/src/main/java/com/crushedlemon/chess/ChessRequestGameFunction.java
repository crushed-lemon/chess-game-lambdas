package com.crushedlemon.chess;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.crushedlemon.chess.commons.model.GamePreferences;
import com.crushedlemon.chess.commons.model.PlayAs;
import org.json.JSONObject;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static com.crushedlemon.chess.commons.Utils.extractGamePreferences;
import static com.crushedlemon.chess.commons.Utils.extractUserName;

public class ChessRequestGameFunction implements RequestHandler<Map<String, Object>, Object> {

    private static final String SQS_URL = "https://sqs.eu-north-1.amazonaws.com/610596007825/chess-paired-players";
    private final DynamoDB dynamoDb;
    private final Table chessLobbyTable;

    public ChessRequestGameFunction() {
        this.dynamoDb = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());
        this.chessLobbyTable = dynamoDb.getTable("chess-lobby");
    }

    @Override
    public Object handleRequest(Map<String, Object> event, Context context) {
        String userName = extractUserName(event);
        GamePreferences gamePreferences = extractGamePreferences(event);

        Optional<Item> waitingUserOpt = findWaitingUser(gamePreferences);

        if (waitingUserOpt.isPresent()) {
            // Pair these two users for a game, and remove them from lobby
            removeUserFromLobby(waitingUserOpt.get());
            String whitePlayer = getWhitePlayer(userName, gamePreferences, waitingUserOpt.get());
            String blackPlayer = whitePlayer.equals(userName) ? getUserName(waitingUserOpt.get()) : userName;
            String message = getSqsMessage(whitePlayer, blackPlayer, gamePreferences);
            sendToSqs(message);
        } else {
            // Send this user to the lobby to wait for an appropriate partner
            chessLobbyTable.putItem(toItem(userName, gamePreferences));
        }
        return Map.of("statusCode", 200);
    }

    private Optional<Item> findWaitingUser(GamePreferences gamePreferences) {
        QuerySpec querySpec = new QuerySpec()
                .withKeyConditionExpression("gameDuration = :v_game_duration AND incrementPerMove = :v_increment_per_move")
                .withValueMap(new ValueMap()
                        .withInt(":v_game_duration", gamePreferences.getGameDuration().getDurationInSeconds())
                        .withInt(":v_increment_per_move", gamePreferences.getIncrementPerMove().getIncrementInSeconds()));

        Iterator<Item> iterator = chessLobbyTable.getIndex("gameDuration-incrementPerMove-index").query(querySpec).iterator();

        while (iterator.hasNext()) {
            Item otherUser = iterator.next();
            PlayAs selfPlayAs = gamePreferences.getPlayAs();
            PlayAs otherPlayAs = PlayAs.valueOf((String) otherUser.get("playAs"));
            if (areCompatible(selfPlayAs, otherPlayAs)) {
                return Optional.of(otherUser);
            }
        }
        return Optional.empty();
    }

    private String getWhitePlayer(String userName, GamePreferences gamePreferences, Item waitingUser) {
        PlayAs selfPlayAs = gamePreferences.getPlayAs();
        PlayAs otherPlayAs = PlayAs.valueOf((String) waitingUser.get("playAs"));

        if (selfPlayAs.equals(PlayAs.WHITE) || otherPlayAs.equals(PlayAs.BLACK)) {
            return userName;
        }
        if (selfPlayAs.equals(PlayAs.BLACK) || otherPlayAs.equals(PlayAs.WHITE)) {
            return getUserName(waitingUser);
        }

        int number = getRandomNumber();
        if (number == 0) {
            return userName;
        } else {
            return getUserName(waitingUser);
        }
    }

    private String getSqsMessage(String whiteUser, String blackUser, GamePreferences gamePreferences) {
        JSONObject messageJson = new JSONObject();
        messageJson.put("whiteUser", whiteUser);
        messageJson.put("blackUser", blackUser);

        JSONObject gameSettings = new JSONObject();
        gameSettings.put("gameDuration", gamePreferences.getGameDuration().getDurationInSeconds());
        gameSettings.put("incrementPerMove", gamePreferences.getIncrementPerMove().getIncrementInSeconds());

        messageJson.put("gameSettings", gameSettings);

        return messageJson.toString();
    }

    private void sendToSqs(String message) {
        try (SqsClient sqsClient = SqsClient.builder().region(Region.EU_NORTH_1).build()) {
            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(SQS_URL)
                    .messageBody(message)
                    .build();

            sqsClient.sendMessage(sendMsgRequest);
        }
    }

    private void removeUserFromLobby(Item waitingUser) {
        chessLobbyTable.deleteItem("userId", getUserName(waitingUser));
    }

    private String getUserName(Item waitingUser) {
        return waitingUser.getString("userId");
    }

    private boolean areCompatible(PlayAs selfPlayAs, PlayAs otherPlayAs) {
        if (selfPlayAs.equals(PlayAs.ANY) || otherPlayAs.equals(PlayAs.ANY)) {
            return true;
        }
        return !selfPlayAs.equals(otherPlayAs);
    }

    private Item toItem(String userName, GamePreferences gamePreferences) {
        return Item.fromMap(Map.of(
                "userId", userName,
                "gameDuration", gamePreferences.getGameDuration().getDurationInSeconds(),
                "incrementPerMove", gamePreferences.getIncrementPerMove().getIncrementInSeconds(),
                "playAs", gamePreferences.getPlayAs().toString()
        ));
    }

    private int getRandomNumber() {
        // TODO : Don't initialize the random number generator here, inject it
        Random random = new Random();
        return random.nextInt(2);
    }
}
