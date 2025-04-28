package com.crushedlemon.chess;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.Map;

import static com.crushedlemon.chess.commons.Utils.extractConnectionId;
import static com.crushedlemon.chess.commons.Utils.extractUserName;

public class ChessConnectFunction implements RequestHandler<Map<String, Object>, Object> {

    private final DynamoDB dynamoDB;
    private final Table table;

    public ChessConnectFunction() {
        this.dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());
        this.table = dynamoDB.getTable("chess-connections");
    }

    @Override
    public Object handleRequest(Map<String, Object> event, Context context) {
        // no change
        String connectionId = extractConnectionId(event);
        String userName = extractUserName(event);
        saveConnection(userName, connectionId);
        return Map.of("statusCode", 200);
    }

    private void saveConnection(String userName, String connectionId) {
        this.table.putItem(Item.fromMap(Map.of("userId", userName, "connectionId", connectionId, "status", "CONNECTED")));
    }

}
