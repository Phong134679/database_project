package com.library.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.library.exception.*;
import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility helpers for handling HTTP requests, responses, CORS headers,
 * JSON payloads, and mapping LibraryException to structured HTTP error responses.
 */
public class HttpUtils {

    public static void enableCors(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
        exchange.getResponseHeaders().set("Pragma", "no-cache");
        exchange.getResponseHeaders().set("Expires", "0");
    }

    public static void sendJson(HttpExchange exchange, int statusCode, Object data) throws IOException {
        enableCors(exchange);
        String json = JsonUtils.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void sendError(HttpExchange exchange, int statusCode, String errorCode, String message) throws IOException {
        enableCors(exchange);
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("success", false);
        err.put("errorCode", errorCode != null ? errorCode : "INTERNAL_ERROR");
        err.put("message", message);
        err.put("timestamp", LocalDateTime.now().toString());

        sendJson(exchange, statusCode, err);
    }

    public static void handleException(HttpExchange exchange, Exception ex) throws IOException {
        if (ex instanceof ResourceNotFoundException) {
            ResourceNotFoundException rnfe = (ResourceNotFoundException) ex;
            sendError(exchange, 404, rnfe.getErrorCode(), rnfe.getMessage());
        } else if (ex instanceof IneligibleMemberException) {
            IneligibleMemberException ime = (IneligibleMemberException) ex;
            sendError(exchange, 403, ime.getErrorCode(), ime.getMessage());
        } else if (ex instanceof LicenseExpiredException) {
            LicenseExpiredException lee = (LicenseExpiredException) ex;
            sendError(exchange, 403, lee.getErrorCode(), lee.getMessage());
        } else if (ex instanceof BusinessException) {
            BusinessException be = (BusinessException) ex;
            sendError(exchange, 400, be.getErrorCode(), be.getMessage());
        } else if (ex instanceof ValidationException) {
            ValidationException ve = (ValidationException) ex;
            sendError(exchange, 400, ve.getErrorCode(), ve.getMessage());
        } else if (ex instanceof DatabaseException) {
            DatabaseException de = (DatabaseException) ex;
            sendError(exchange, 500, de.getErrorCode(), de.getMessage());
        } else if (ex instanceof LibraryException) {
            LibraryException le = (LibraryException) ex;
            sendError(exchange, 400, le.getErrorCode(), le.getMessage());
        } else {
            sendError(exchange, 500, "INTERNAL_SERVER_ERROR", ex.getMessage() != null ? ex.getMessage() : ex.toString());
        }
    }

    public static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            return baos.toString(StandardCharsets.UTF_8.name());
        }
    }

    public static JsonObject readJsonBody(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        if (body == null || body.trim().isEmpty()) {
            return new JsonObject();
        }
        return JsonParser.parseString(body).getAsJsonObject();
    }

    public static Map<String, String> parseQueryParams(URI uri) {
        String query = uri.getQuery();
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> params = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(kv[0], kv[1]);
            } else if (kv.length == 1) {
                params.put(kv[0], "");
            }
        }
        return params;
    }
}
