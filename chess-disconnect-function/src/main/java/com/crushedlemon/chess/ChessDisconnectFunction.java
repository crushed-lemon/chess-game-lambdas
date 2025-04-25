package com.crushedlemon.chess;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.Iterator;
import java.util.Map;

public class ChessDisconnectFunction implements RequestHandler<Map<String, Object>, Object> {

    private final DynamoDB dynamoDB;
    private final Table table;

    public ChessDisconnectFunction() {
        this.dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());
        this.table = dynamoDB.getTable("chess-connections");
    }

    @Override
    public Object handleRequest(Map<String, Object> event, Context context) {
        String connectionId = extractConnectionId(event);
        removeConnection(connectionId);
        return Map.of("statusCode", 200);
    }

    private void removeConnection(String connectionId) {
        QuerySpec querySpec = new QuerySpec()
                .withKeyConditionExpression("connectionId = :v_connection_id")
                .withValueMap(new ValueMap().withString(":v_connection_id", connectionId));

        Iterator<Item> iterator = table.getIndex("connectionId-index").query(querySpec).iterator();

        if (iterator.hasNext()) {
            this.table.deleteItem("userId", iterator.next().get("userId"));
        }
    }

    // TODO : move this to a common class
    private static String extractConnectionId(Map<String, Object> event) {
        Map<String, Object> requestContext = (Map<String, Object>) event.get("requestContext");
        return (String) requestContext.get("connectionId");
    }
}
