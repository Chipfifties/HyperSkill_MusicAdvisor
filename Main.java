package advisor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

public class Main {
    private static final Scanner sc = new Scanner(System.in);
    private static String accessPointURL = "https://accounts.spotify.com";
    private static String resourceURL = "https://api.spotify.com";
    private static String category;
    private static int page = 5;
    private static boolean isAuthorized = false;

    public static void main(String[] args) {

        getCommandLineArguments(args);

        while (true) {
            String input = sc.nextLine();
            if (input.equals("exit")) {
                System.out.println("---GOODBYE!---");
                return;
            } else if (input.equals("auth") || isAuthorized) {
                if (input.contains("playlists")) {
                    category = input.substring(10);
                    input = input.substring(0, 9);
                }
                setAction(input);
            } else {
                System.out.println("Please, provide access for application.");
            }
        }
    }

    private static void getCommandLineArguments(String[] args) {
        if (Arrays.asList(args).contains("-access")) {
            accessPointURL = args[Arrays.asList(args).indexOf("-access") + 1];
            resourceURL = args[Arrays.asList(args).indexOf("-resource") + 1];
        }
        if (Arrays.asList(args).contains("-page")) {
            page = Integer.parseInt(args[Arrays.asList(args).indexOf("-page") + 1]);
        }
    }

    private static void setAction(String input) {
        try {
            switch (input) {
                case "new":
                    new JsonHandler(page).printNew(ProcessAuth.getNewReleases(resourceURL));
                    break;
                case "featured":
                    new JsonHandler(page).printFeatured(ProcessAuth.getFeatured(resourceURL));
                    break;
                case "categories":
                    new JsonHandler(page).printCategories(ProcessAuth.getCategories(resourceURL));
                    break;
                case "playlists":
                    getPlaylists(category);
                    break;
                case "auth":
                    getToken();
                    break;
                default:
                    System.out.println("Wrong command.");
                    break;
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Error");
        }
    }

    private static void getToken() throws IOException, InterruptedException {
        isAuthorized = ProcessAuth.authorize(accessPointURL);
        if (!isAuthorized) {
            System.out.println("---FAILED TO AUTHORIZE---");
        }
    }

    private static void getPlaylists(String name) throws IOException, InterruptedException {
        String id = "";
        JsonObject jsonObject = ProcessAuth.getCategories(resourceURL);
        for (JsonElement category : jsonObject.get("categories").getAsJsonObject().get("items").getAsJsonArray()) {
            if (name.equals(category.getAsJsonObject().get("name").getAsString())) {
                id = category.getAsJsonObject().get("id").getAsString();
            }
        }
        new JsonHandler(page).printPlaylists(ProcessAuth.getPlaylists(resourceURL, id));
    }
}
