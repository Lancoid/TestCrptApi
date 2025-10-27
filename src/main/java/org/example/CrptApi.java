package org.example;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

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
    public void createDocument(Document document, String signature, String bearerToken) throws Exception {
        if (document == null || document.getProductGroup() == null || bearerToken == null) {
            throw new IllegalArgumentException("Document, productGroup и bearerToken не должны быть null");
        }

        rateLimiter.acquire();

        String json = objectMapper.writeValueAsString(document);

        URL url = new URL("https://ismp.crpt.ru/api/v3/lk/documents/create?pg=" + document.getProductGroup());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
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
        @JsonProperty("document_format")
        private String documentFormat;
        @JsonProperty("product_document")
        private String productDocument;
        @JsonIgnore
        private String productGroup;
        private String signature;
        private String type;

        public Document() {
        }

        public Document(String document_format, String product_document, String product_group, String signature, String type) {
            this.documentFormat = document_format;
            this.productDocument = product_document;
            this.productGroup = product_group;
            this.signature = signature;
            this.type = type;
        }

        public String getDocumentFormat() {
            return documentFormat;
        }

        public void setDocumentFormat(String documentFormat) {
            this.documentFormat = documentFormat;
        }

        public String getProductDocument() {
            return productDocument;
        }

        public void setProductDocument(String productDocument) {
            this.productDocument = productDocument;
        }

        public String getProductGroup() {
            return productGroup;
        }

        public void setProductGroup(String productGroup) {
            this.productGroup = productGroup;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}