package com.tracker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;

public class ActivityLogger {

    private static final String LOG_DIR = "logs";

    public static void log(JsonNode jsonNode) {
        try {
            // Ensure log directory exists
            Path logDirPath = Paths.get(LOG_DIR);
            if (!Files.exists(logDirPath)) {
                Files.createDirectories(logDirPath);
            }

            // Create file name with current date
            String date = LocalDate.now().toString();
            String fileName = "activity_log_" + date + ".txt";
            Path logFilePath = logDirPath.resolve(fileName);

            // Convert JSON to string
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);

            // Write to file in append mode
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath.toFile(), true))) {
                writer.write(jsonString);
                writer.newLine();
            }

            System.out.println("Session logged to file: " + logFilePath);
        } catch (IOException e) {
            System.err.println("Failed to log session: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
