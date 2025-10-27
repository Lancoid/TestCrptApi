package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * CrptApi provides functionality to create documents via the CRPT API with request rate limiting.
 */
@SuppressWarnings({
        "SpellCheckingInspection",
        "UnstableApiUsage"
})
public class CrptApi {
    private final RateLimiter rateLimiter;

    /**
     * Constructs a CrptApi instance with rate limiting.
     *
     * @param timeUnit     the time unit for rate limiting
     * @param requestLimit the maximum number of requests per time unit
     */
    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        double permitsPerSecond = requestLimit / (double) timeUnit.toSeconds(1);

        this.rateLimiter = RateLimiter.create(permitsPerSecond);
    }

    /**
     * Creates a document in the CRPT system.
     *
     * @param document  the document to create
     * @param signature the signature for the request
     * @throws Exception if an error occurs during the request
     */
    public void createDocument(Document document, String signature) throws Exception {
        rateLimiter.acquire();

        String json = new ObjectMapper().writeValueAsString(document);

        URL url = new URL("https://markirovka.demo.crpt.tech/lk/documents/create");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Signature", signature);
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();

        if (responseCode != 200 && responseCode != 201) {
            throwError(connection);
        }

        connection.disconnect();
    }

    private static void throwError(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        InputStream errorStream = connection.getErrorStream();
        StringBuilder errorMsg = new StringBuilder();

        errorMsg.append("HTTP ").append(responseCode).append(": ");

        if (errorStream == null) {
            errorMsg.append("No error details provided by server.");

            throw new RuntimeException("API error: " + errorMsg);
        }

        try (
                InputStreamReader stream = new InputStreamReader(errorStream, StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(stream)
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                errorMsg.append(line);
            }
        }


        throw new RuntimeException("API error: " + errorMsg);
    }

    /**
     * Document model for API requests.
     */
    public static class Document {
        private String pg;

        public Document() {
        }

        public Document(String pg) {
            this.pg = pg;
        }

        public String getProductGroup() {
            return pg;
        }

        public void setProductGroup(String productGroup) {
            this.pg = productGroup;
        }
    }
}