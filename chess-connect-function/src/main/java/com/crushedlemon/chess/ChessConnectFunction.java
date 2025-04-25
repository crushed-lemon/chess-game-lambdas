package com.crushedlemon.chess;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.Map;

public class ChessConnectFunction implements RequestHandler<Map<String, Object>, Object> {

    @Override
    public Object handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("User is now connected!");
        for (Map.Entry e : event.entrySet()) {
            context.getLogger().log("========PARAMS ARE=========");
            context.getLogger().log(e.getKey().toString());
            context.getLogger().log(e.getValue().toString());
            context.getLogger().log("****************");
        }
        context.getLogger().log("========CONTEXT=========");
        context.getLogger().log(context.toString());
        context.getLogger().log("****************");
        return Map.of("statusCode", 200);
    }
}
