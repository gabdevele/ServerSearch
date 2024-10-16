package com.gabdevele.gabmod;

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
import net.minecraft.client.multiplayer.ServerData;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ServerApi {

    private static final ConcurrentHashMap<String, List<ServerData>> cache = new ConcurrentHashMap<>();

    public static CompletableFuture<List<ServerData>> fetchServerData(String query) {
        return CompletableFuture.supplyAsync(() -> {
            if (cache.containsKey(query)) {
                return cache.get(query);
            }

            List<ServerData> serverDataList = new ArrayList<>();
            String apiUrl = "https://minecraft-italia.net/lista/api/server/list";

            try {
                URL url = new URI(apiUrl).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    throw new RuntimeException("HttpResponseCode: " + responseCode);
                } else {
                    StringBuilder inline = new StringBuilder();
                    Scanner scanner = new Scanner(url.openStream());
                    while (scanner.hasNext()) {
                        inline.append(scanner.nextLine());
                    }
                    scanner.close();

                    JsonElement jsonElement = JsonParser.parseString(inline.toString());
                    JsonArray jsonArray = jsonElement.getAsJsonArray();

                    for (int i = 0; i < jsonArray.size() && serverDataList.size() < 5; i++) {
                        JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
                        String name = jsonObject.get("name").getAsString();
                        if (name.toLowerCase().contains(query.toLowerCase())) {
                            String ip = jsonObject.get("java_address").getAsString();
                            serverDataList.add(new ServerData(name, ip, ServerData.Type.OTHER));
                        }
                    }
                }
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }

            cache.put(query, serverDataList);
            return serverDataList;
        });
    }
}