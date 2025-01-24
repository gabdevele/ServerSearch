package com.gabdevele.serversearch;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.multiplayer.ServerData;
import org.slf4j.Logger;
import java.util.concurrent.CompletableFuture;

public class ServerApi {
    //cache brings a lot of problems, so I temporarily removed it
    //private static final ConcurrentHashMap<String, List<ServerData>> cache = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String API_URL = "https://gabdevele.ddns.net/mod";

    public static CompletableFuture<List<ServerData>> fetchServerData(String query, boolean offlineMode) {
        return CompletableFuture.supplyAsync(() -> {
//            if (cache.containsKey(query)) {
//                return cache.get(query);
//            }

            List<ServerData> serverDataList;
            String apiUrl = buildApiUrl(query, offlineMode);

            try {
                JsonArray jsonArray = fetchJsonArrayFromApi(apiUrl);
                serverDataList = parseServerDataList(jsonArray);
            } catch (IOException | URISyntaxException e) {
                LOGGER.error("Failed to fetch server data", e);
                return null;
            }

//            cache.put(query, serverDataList);
            return serverDataList;
        });
    }

    private static String buildApiUrl(String query,boolean offlineMode) {
        return API_URL + "/search?name=" + query + "&cracked=" + offlineMode;
    }

    private static JsonArray fetchJsonArrayFromApi(String apiUrl) throws IOException, URISyntaxException {
        URL url = new URI(apiUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();

        StringBuilder inline = new StringBuilder();
        try (Scanner scanner = new Scanner(url.openStream())) {
            while (scanner.hasNext()) {
                inline.append(scanner.nextLine());
            }
        }

        JsonElement jsonElement = JsonParser.parseString(inline.toString());
        return jsonElement.getAsJsonArray();
    }

    private static List<ServerData> parseServerDataList(JsonArray jsonArray) {
        List<ServerData> serverDataList = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
            String name = jsonObject.get("name").getAsString();
            String ip = jsonObject.get("address").getAsString();
            ServerData serverData = new ServerData(name, ip, ServerData.Type.OTHER);
            serverDataList.add(serverData);
        }
        return serverDataList;
    }
}