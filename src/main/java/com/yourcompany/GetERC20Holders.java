package com.yourcompany;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.util.concurrent.CompletableFuture;

public class GetERC20Holders {
    private static String contractAddress = "0xb24cd494fae4c180a89975f1328eab2a7d5d8f11";
    private static final String URL = "https://rpc.ankr.com/multichain/79258ce7f7ee046decc3b5292a24eb4bf7c910d7e39b691384c7ce0cfb839a01";

    private static int count = 0;
    private static int activeRequests = 0;


    static AsyncHttpClient client = new DefaultAsyncHttpClient();

    public static void main(String[] args) {
        sendRequestAndGetNextToken(null);
    }

    private static void sendRequestAndGetNextToken(String pageToken) {
        incrementActiveRequests();
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }

            if( !client.isClosed() ) {
            count++;
            String requestBody = "{\"jsonrpc\":\"2.0\",\"method\":\"ankr_getTokenHolders\",\"params\":{\"blockchain\":\"eth\",\"contractAddress\":\""
                    + contractAddress + "\",\"pageSize\":100,\"syncCheck\":false},\"id\": " + count + "}";
            if (pageToken != null) {
                requestBody = requestBody.replaceFirst("}", ",\"pageToken\":\"" + pageToken + "\"}");
            }
            System.out.println("-------------------------------\n" + "Request:" + requestBody);

            client.preparePost(URL)
                    .setHeader("accept", "application/json")
                    .setHeader("content-type", "application/json")
                    .setBody(requestBody)
                    .execute()
                    .toCompletableFuture()
                    .thenAccept(response -> processResponse(response, pageToken))
                    .exceptionally(throwable -> {
                        throwable.printStackTrace();
                        return null;
                    })
                    .whenComplete((resp, throwable) -> {
                        try {
                            decrementActiveRequests();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        
                }


    });
}

    private static void processResponse(Response response, String pageToken) {
        try {
            String responseBody = response.getResponseBody();
            JSONObject json = new JSONObject(responseBody);
            JSONArray tokenHolders = json.getJSONObject("result").getJSONArray("holders");
            saveJsonArraytoFile(tokenHolders, "ERC20 Holders.txt");
            String nextPageToken = json.getJSONObject("result").optString("nextPageToken", null);
            System.out.println("NextPageToken:" + nextPageToken + "\n");

            if (nextPageToken != null && !nextPageToken.isEmpty()) {
                sendRequestAndGetNextToken(nextPageToken);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void saveJsonArraytoFile(JSONArray array, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            for (int i = 0; i < array.length(); i++) {
                // System.out.println(array.get(i));
                writer.write(array.get(i).toString());
                writer.newLine();
            }
        }
    }

    private static synchronized void incrementActiveRequests() {
        activeRequests++;
    }

    private static synchronized void decrementActiveRequests() throws IOException {
        activeRequests--;
        if (activeRequests == 0) {
            client.close(); // 当没有活跃请求时关闭客户端
        }
    }

}
