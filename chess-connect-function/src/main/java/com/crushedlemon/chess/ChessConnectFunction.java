package com.crushedlemon.chess;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.Map;

public class ChessConnectFunction implements RequestHandler<Map<String, Object>, Object> {

    private final DynamoDB dynamoDB;
    private final Table table;

    public ChessConnectFunction() {
        this.dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());
        this.table = dynamoDB.getTable("chess-connections");
    }

    @Override
    public Object handleRequest(Map<String, Object> event, Context context) {
        String connectionId = extractConnectionId(event);
        String userName = extractUserName(event);
        saveConnection(userName, connectionId);
        return Map.of("statusCode", 200);
    }

    private void saveConnection(String userName, String connectionId) {
        this.table.putItem(Item.fromMap(Map.of("userId", userName, "connectionId", connectionId, "status", "CONNECTED")));
    }

    // TODO : move this to a common class
    private static String extractConnectionId(Map<String, Object> event) {
        Map<String, Object> requestContext = (Map<String, Object>) event.get("requestContext");
        return (String) requestContext.get("connectionId");
    }

    private static String extractUserName(Map<String, Object> event) {
        // For testing purposes, passing the username in query params. Change it to JWT-based identity.
        Map<String, Object> queryStringParameters = (Map<String, Object>) event.get("queryStringParameters");
        return (String) queryStringParameters.get("userName");
    }
}
