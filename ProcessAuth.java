package advisor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class ProcessAuth {
    private static final String NEW = "/v1/browse/new-releases?";
    private static final String FEATURED = "/v1/browse/featured-playlists?";
    private static final String CATEGORIES = "/v1/browse/categories";
    private static final String PLAYLISTS = "/v1/browse/categories/%s/playlists";
    private static final String CLIENT_ID = "";
    private static final String CLIENT_SECRET = "";
    private static final String REDIRECT_URI = "http://localhost:8080";
    private static final String GRANT_TYPE = "authorization_code";
    private static final String RESPONSE_TYPE = "code";
    private static String authorizationCode;
    private static HttpResponse<String> response;
    private static final HttpClient client = HttpClient.newBuilder().build();
    private static String accessToken;

    static boolean authorize(String accessPoint) throws IOException, InterruptedException {
        System.out.println("use this link to request the access code:");
        System.out.println(accessPoint
                + "/authorize"
                + "?client_id=" + CLIENT_ID
                + "&redirect_uri=" + REDIRECT_URI
                + "&response_type=" + RESPONSE_TYPE);
        System.out.println("waiting for code...");
        getRequest(accessPoint);
        return response.statusCode() == 200;
    }

    static void getRequest(String accessPoint) throws InterruptedException, IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/",
                exchange -> {
                    String query = exchange.getRequestURI().getQuery();
                    String responseBody;
                    if (query != null && query.contains("code")) {
                        System.out.println(query);
                        authorizationCode = query.substring(5);
                        responseBody = "Got the code. Return back to your program.";
                        System.out.println("code received");
                        System.out.println("making http request for access_token...");
                        try {
                            response = requestAccessToken(accessPoint);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                        accessToken = json.get("access_token").getAsString();
                        System.out.println("Success!");
                    } else {
                        System.out.println(query);
                        responseBody = "Authorization code not found. Try again.";
                    }
                    exchange.sendResponseHeaders(200, responseBody.length());
                    exchange.getResponseBody().write(responseBody.getBytes());
                    exchange.getResponseBody().close();
                }
        );

        server.start();
        while (authorizationCode == null) {
            Thread.sleep(100);
        }
        server.stop(1);
    }

    private static HttpResponse<String> requestAccessToken(String accessPoint) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(
                        "grant_type=" + GRANT_TYPE
                                + "&code=" + authorizationCode
                                + "&redirect_uri=" + REDIRECT_URI
                                + "&client_id=" + CLIENT_ID
                                + "&client_secret=" + CLIENT_SECRET))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .uri(URI.create(accessPoint + "/api/token"))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static JsonObject getNewReleases(String resourceURL) throws IOException, InterruptedException {
        response = sendRequest(resourceURL + NEW);
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    public static JsonObject getCategories(String resourceURL) throws IOException, InterruptedException {
        response = sendRequest(resourceURL + CATEGORIES);
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    public static JsonObject getPlaylists(String resourceURL, String id) throws IOException, InterruptedException {
        response = sendRequest(resourceURL + String.format(PLAYLISTS, id));
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    public static JsonObject getFeatured(String resourceURL) throws IOException, InterruptedException {
        response = sendRequest(resourceURL + FEATURED);
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private static HttpResponse<String> sendRequest(String url) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .uri(URI.create(url))
                .GET()
                .build();
        return client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    }
}
