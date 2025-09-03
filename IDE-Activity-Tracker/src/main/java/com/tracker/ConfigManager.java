package com.tracker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class ConfigManager {
    private static final String CONFIG_FILE = "config.json";

    public static void initializeConfig() throws IOException {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter User ID: ");
            String userId = scanner.nextLine();
            System.out.print("Enter Email: ");
            String email = scanner.nextLine();
            System.out.print("Enter Project Root Path: ");
            String rootPath = scanner.nextLine();

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode config = mapper.createObjectNode();
            config.put("userId", userId);
            config.put("email", email);
            config.put("projectRootPath", rootPath);

            mapper.writerWithDefaultPrettyPrinter().writeValue(file, config);
            System.out.println(" Configuration saved to " + CONFIG_FILE);
        }
    }

    public static JsonNode getConfig() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(new File(CONFIG_FILE));
    }
}
