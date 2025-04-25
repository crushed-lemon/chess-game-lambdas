package com.crushedlemon.chess;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.Map;

public class ChessDisconnectFunction implements RequestHandler<Map<String, Object>, Object> {

    @Override
    public Object handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("User is now disconnected!");
        context.getLogger().log("And we know which one also");
        return Map.of("statusCode", 200);
    }
}
