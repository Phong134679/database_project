package com.library.api;

import com.google.gson.JsonObject;
import com.library.exception.LibraryException;
import com.library.model.DigitalJournal;
import com.library.model.Fine;
import com.library.model.PaymentTransaction;
import com.library.repository.*;
import com.library.service.BorrowService;
import com.library.service.DigitalJournalService;
import com.library.service.FineService;
import com.library.service.MemberService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight HTTP REST API & Static Web Server based on JDK built-in HttpServer.
 * Serves REST endpoints under /api/* and serves pure HTML/CSS/JS frontend on the same port.
 */
public class ApiServer {

    private static final int DEFAULT_PORT = 8080;
    private static final String WEB_DIR = "web";

    private final int port;
    private HttpServer server;

    private final ResourceRepository resourceRepo;
    private final PersonRepository personRepo;
    private final UniversityMemberRepository memberRepo;
    private final BorrowRepository borrowRepo;
    private final FineRepository fineRepo;
    private final PaymentTransactionRepository paymentRepo;
    private final DigitalJournalRepository journalRepo;
    private final LicensePackageRepository licenseRepo;
    private final GrantedLicenseRepository grantedLicenseRepo;
    private final AccessLogRepository accessLogRepo;

    private final MemberService memberService;
    private final BorrowService borrowService;
    private final FineService fineService;
    private final DigitalJournalService journalService;

    public ApiServer(int port) {
        this.port = port;

        // Repositories
        this.resourceRepo = new ResourceRepository();
        this.personRepo = new PersonRepository();
        this.memberRepo = new UniversityMemberRepository();
        this.borrowRepo = new BorrowRepository();
        this.fineRepo = new FineRepository();
        this.paymentRepo = new PaymentTransactionRepository();
        this.journalRepo = new DigitalJournalRepository();
        this.licenseRepo = new LicensePackageRepository();
        this.grantedLicenseRepo = new GrantedLicenseRepository();
        this.accessLogRepo = new AccessLogRepository();

        // Services
        this.memberService = new MemberService(personRepo, memberRepo, borrowRepo, fineRepo);
        this.borrowService = new BorrowService(borrowRepo, fineRepo, memberService);
        this.fineService = new FineService(fineRepo, paymentRepo);
        this.journalService = new DigitalJournalService(journalRepo, licenseRepo, grantedLicenseRepo, accessLogRepo, personRepo);
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // API Endpoints
        server.createContext("/api/health", new HealthHandler());
        server.createContext("/api/resources", new ResourceHandler());
        server.createContext("/api/copies", new CopyHandler());
        server.createContext("/api/members", new MemberHandler());
        server.createContext("/api/borrows", new BorrowHandler());
        server.createContext("/api/fines", new FineHandler());
        server.createContext("/api/digital-journals", new DigitalJournalHandler());
        server.createContext("/api/access-logs", new AccessLogHandler());
        server.createContext("/api/licenses", new LicensePackageHandler());
        server.createContext("/api/granted-licenses", new GrantedLicenseHandler());

        // Static Web UI Handler (fallback root context)
        server.createContext("/", new StaticWebHandler());

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("===============================================================================");
        System.out.println("  LIBRARY MANAGEMENT SYSTEM - HTTP REST API & WEB SERVER                       ");
        System.out.println("===============================================================================");
        System.out.println("  Web UI & API running at: http://localhost:" + port + "/");
        System.out.println("  Health check:            http://localhost:" + port + "/api/health");
        System.out.println("  Resources catalog:       http://localhost:" + port + "/api/resources");
        System.out.println("  Members list:            http://localhost:" + port + "/api/members");
        System.out.println("  Fines list:              http://localhost:" + port + "/api/fines");
        System.out.println("  Access logs:             http://localhost:" + port + "/api/access-logs");
        System.out.println("===============================================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("[ApiServer] Server stopped.");
        }
    }

    // =========================================================================
    // 1. Health Handler
    // =========================================================================
    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.enableCors(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("status", "UP");
            res.put("message", "Library Management System API is running");
            res.put("timestamp", LocalDateTime.now().toString());
            HttpUtils.sendJson(exchange, 200, res);
        }
    }

    // =========================================================================
    // 2. Resources Handler: GET /api/resources
    // =========================================================================
    private class ResourceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.enableCors(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Method " + exchange.getRequestMethod() + " is not allowed.");
                return;
            }

            try {
                List<Map<String, Object>> resources = resourceRepo.findAllResourcesDetailed();
                HttpUtils.sendJson(exchange, 200, resources);
            } catch (Exception e) {
                HttpUtils.handleException(exchange, e);
            }
        }
    }

    // =========================================================================
    // 3. Copies Handler: GET /api/copies
    // =========================================================================
    private class CopyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.enableCors(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Method " + exchange.getRequestMethod() + " is not allowed.");
                return;
            }

            try {
                List<Map<String, Object>> copies = borrowRepo.findAllCopiesDetailed();
                HttpUtils.sendJson(exchange, 200, copies);
            } catch (Exception e) {
                HttpUtils.handleException(exchange, e);
            }
        }
    }

    // =========================================================================
    // 4. Members Handler: GET /api/members
    // =========================================================================
    private class MemberHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.enableCors(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Method " + exchange.getRequestMethod() + " is not allowed.");
                return;
            }

            try {
                List<Map<String, Object>> members = memberService.getAllPersonsDetailed();
                HttpUtils.sendJson(exchange, 200, members);
            } catch (Exception e) {
                HttpUtils.handleException(exchange, e);
            }
        }
    }

    // =========================================================================
    // 5. Borrows Handler: GET /api/borrows, POST /api/borrows, POST /api/borrows/{id}/return
    // =========================================================================
    private class BorrowHandler implements HttpHandler {
        private final Pattern returnPattern = Pattern.compile("^/api/borrows/(\\d+)/return/?$");

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.enableCors(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            Matcher returnMatcher = returnPattern.matcher(path);
            if (returnMatcher.matches()) {
                if (!"POST".equalsIgnoreCase(method)) {
                    HttpUtils.sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Return endpoint requires POST.");
                    return;
                }
                int borrowId = Integer.parseInt(returnMatcher.group(1));
                handleReturn(exchange, borrowId);
                return;
            }

            if ("/api/borrows".equals(path) || "/api/borrows/".equals(path)) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetBorrows(exchange);
                } else if ("POST".equalsIgnoreCase(method)) {
                    handleCreateBorrow(exchange);
                } else {
                    HttpUtils.sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Method not allowed.");
                }
                return;
            }

            HttpUtils.sendError(exchange, 404, "NOT_FOUND", "Endpoint not found: " + path);
        }

        private void handleGetBorrows(HttpExchange exchange) throws IOException {
            try {
                List<Map<String, Object>> list = borrowService.getAllBorrowsDetailed();
                HttpUtils.sendJson(exchange, 200, list);
            } catch (Exception e) {
                HttpUtils.handleException(exchange, e);
            }
        }

        private void handleCreateBorrow(HttpExchange exchange) throws IOException {
            try {
                JsonObject json = HttpUtils.readJsonBody(exchange);
                if (!json.has("memberId") || json.get("memberId").isJsonNull()
                        || !json.has("copyId") || json.get("copyId").isJsonNull()) {
                    HttpUtils.sendError(exchange, 400, "INVALID_INPUT", "Yêu cầu cả 'memberId' và 'copyId'.");
                    return;
                }

                String memberId = json.get("memberId").getAsString();
                String copyId = json.get("copyId").getAsString();

                Map<String, Object> borrowResult = borrowService.borrowBook(memberId, copyId);
                Map<String, Object> res = new LinkedHashMap<>();
                res.put("success", true);
                res.put("message", "Mượn sách thành công.");
                res.put("borrowId", borrowResult.get("borrowId"));
                res.put("memberId", borrowResult.get("memberId"));
                res.put("copyId", borrowResult.get("copyId"));
                res.put("dueDate", borrowResult.get("dueDate"));
                HttpUtils.sendJson(exchange, 201, res);
            } catch (Exception e) {
                HttpUtils.handleException(exchange, e);
            }
        }

        private void handleReturn(HttpExchange exchange, int borrowId) throws IOException {
            try {
                JsonObject json = HttpUtils.readJsonBody(exchange);
                LocalDate returnDate = null;
                if (json != null && json.has("returnDate") && !json.get("returnDate").isJsonNull()) {
                    String dateStr = json.get("returnDate").getAsString();
                    if (!dateStr.trim().isEmpty()) {
                        returnDate = LocalDate.parse(dateStr.trim());
                    }
                }

                Map<String, Object> returnResult = borrowService.returnBookWithResult(borrowId, returnDate);
                int fineId = returnResult.get("fineId") != null ? ((Number) returnResult.get("fineId")).intValue() : 0;
                Fine fine = fineId > 0 ? fineService.getFineDetails(fineId) : null;

                Map<String, Object> res = new LinkedHashMap<>();
                res.put("success", true);
                res.put("message", "Trả sách thành công.");
                res.put("borrowId", borrowId);
                res.put("returnDate", (returnDate != null ? returnDate : LocalDate.now()).toString());
                res.put("fineGenerated", fine != null);
                if (fine != null) {
                    Map<String, Object> fineInfo = new LinkedHashMap<>();
                    fineInfo.put("fineId", fine.getFineId());
                    fineInfo.put("amount", fine.getTotalFineAmount());
                    fineInfo.put("status", fine.getPaymentStatus());
                    res.put("fine", fineInfo);
                }

                HttpUtils.sendJson(exchange, 200, res);
            } catch (Exception e) {
                HttpUtils.handleException(exchange, e);
            }
        }
    }

    // =========================================================================
    // 6. Fines Handler: GET /api/fines, GET /api/fines/{id}, POST /api/fines/{id}/payments
    // =========================================================================
    private class FineHandler implements HttpHandler {
        private final Pattern paymentPattern = Pattern.compile("^/api/fines/(\\d+)/payments/?$");
        private final Pattern detailPattern = Pattern.compile("^/api/fines/(\\d+)/?$");

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.enableCors(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            // POST /api/fines/{id}/payments
            Matcher payMatcher = paymentPattern.matcher(path);
            if (payMatcher.matches()) {
                if (!"POST".equalsIgnoreCase(method)) {
                    HttpUtils.sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Payments endpoint requires POST.");
                    return;
                }
                int fineId = Integer.parseInt(payMatcher.group(1));
                handlePayment(exchange, fineId);
                return;
            }

            // GET /api/fines/{id}
            Matcher detailMatcher = detailPattern.matcher(path);
            if (detailMatcher.matches()) {
                if (!"GET".equalsIgnoreCase(method)) {
                    HttpUtils.sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Fine detail requires GET.");
                    return;
                }
                int fineId = Integer.parseInt(detailMatcher.group(1));
                handleFineDetail(exchange, fineId);
                return;
            }

            // GET /api/fines
            if ("/api/fines".equals(path) || "/api/fines/".equals(path)) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetFines(exchange);
                } else {
                    HttpUtils.sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Method not allowed.");
                }
                return;
            }

            HttpUtils.sendError(exchange, 404, "NOT_FOUND", "Endpoint not found: " + path);
        }

        private void handleGetFines(HttpExchange exchange) throws IOException {
            try {
                List<Map<String, Object>> fines = fineService.getAllFinesDetailed();
                HttpUtils.sendJson(exchange, 200, fines);
            } catch (Exception e) {
                HttpUtils.handleException(exchange, e);
            }
        }

        private void handleFineDetail(HttpExchange exchange, int fineId) throws IOException {
            try {
                Fine fine = fineService.getFineDetails(fineId);

                Map<String, Object> res = new LinkedHashMap<>();
                res.put("fineId", fine.getFineId());
                res.put("borrowId", fine.getBorrowId());
                res.put("totalFineAmount", fine.getTotalFineAmount());
                res.put("paymentStatus", fine.getPaymentStatus());

                HttpUtils.sendJson(exchange, 200, res);
            } catch (Exception e) {
                HttpUtils.handleException(exchange, e);
            }
        }

        private void handlePayment(HttpExchange exchange, int fineId) throws IOException {
            try {
                // Không có bước tính toán nào, thủ thư trực tiếp xác nhận đã thanh toán
                boolean success = fineService.confirmPayment(fineId);
                Fine fine = fineService.getFineDetails(fineId);

                Map<String, Object> res = new LinkedHashMap<>();
                res.put("success", success);
                res.put("message", "Thủ thư đã xác nhận thanh toán thành công.");
                res.put("fineId", fineId);
                res.put("status", fine.getPaymentStatus());

                HttpUtils.sendJson(exchange, 200, res);
            } catch (Exception e) {
                HttpUtils.handleException(exchange, e);
            }
        }
    }

    // =========================================================================
    // 7. Digital Journals Handler: GET /api/digital-journals, POST /api/digital-journals/{id}/access
    // =========================================================================
    private class DigitalJournalHandler implements HttpHandler {
        private final Pattern accessPattern = Pattern.compile("^/api/digital-journals/(\\d+)/access/?$");

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.enableCors(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            Matcher accessMatcher = accessPattern.matcher(path);
            if (accessMatcher.matches()) {
                if (!"POST".equalsIgnoreCase(method)) {
                    HttpUtils.sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Access endpoint requires POST.");
                    return;
                }
                int journalId = Integer.parseInt(accessMatcher.group(1));
                handleAccess(exchange, journalId);
                return;
            }

            if ("/api/digital-journals".equals(path) || "/api/digital-journals/".equals(path)) {
                if ("GET".equalsIgnoreCase(method)) {
                    try {
                        List<DigitalJournal> list = journalService.getAllJournals();
                        HttpUtils.sendJson(exchange, 200, list);
                    } catch (Exception e) {
                        HttpUtils.handleException(exchange, e);
                    }
                } else {
                    HttpUtils.sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Method not allowed.");
                }
                return;
            }

            HttpUtils.sendError(exchange, 404, "NOT_FOUND", "Endpoint not found: " + path);
        }

        private void handleAccess(HttpExchange exchange, int journalId) throws IOException {
            try {
                JsonObject json = HttpUtils.readJsonBody(exchange);
                Integer personId = null;
                if (json.has("personId") && !json.get("personId").isJsonNull()) {
                    String raw = json.get("personId").getAsString().trim();
                    if (!raw.isEmpty()) {
                        try {
                            personId = Integer.parseInt(raw);
                        } catch (NumberFormatException e) {
                            HttpUtils.sendError(exchange, 400, "INVALID_INPUT", "personId phải là số nguyên.");
                            return;
                        }
                    }
                }

                DigitalJournal journal = journalService.getJournalDetails(journalId);
                Map<String, Object> accessResult = journalService.accessDigitalJournalWithResult(personId, journalId);
                boolean allowed = Boolean.TRUE.equals(accessResult.get("allowed"));
                String reason = (String) accessResult.get("reason");
                long logId = ((Number) accessResult.get("logId")).longValue();

                if (allowed) {
                    boolean isAuthor = personId != null && personId > 0 && journalRepo.isAuthorOfResource(personId, journalId);
                    Map<String, Object> res = new LinkedHashMap<>();
                    res.put("accessGranted", true);
                    res.put("logId", logId);
                    res.put("isPublic", journal.isPublic());
                    res.put("isAuthor", isAuthor);
                    res.put("message", reason);
                    res.put("journalId", journalId);
                    res.put("title", journal.getTitle());
                    res.put("url", journal.getUrl());
                    res.put("format", journal.getFormat());
                    res.put("fileSize", journal.getFileSize());
                    HttpUtils.sendJson(exchange, 200, res);
                } else {
                    Map<String, Object> res = new LinkedHashMap<>();
                    res.put("accessGranted", false);
                    res.put("logId", 0);
                    res.put("errorCode", "ACCESS_DENIED");
                    res.put("message", reason);
                    res.put("journalId", journalId);
                    res.put("title", journal.getTitle());
                    HttpUtils.sendJson(exchange, 403, res);
                }
            } catch (Exception e) {
                HttpUtils.handleException(exchange, e);
            }
        }
    }

    // =========================================================================
    // 8. Access Logs Handler: GET /api/access-logs
    // =========================================================================
    private class AccessLogHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.enableCors(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Method not allowed.");
                return;
            }

            try {
                List<Map<String, Object>> logs = journalService.getAllAccessLogsDetailed();
                HttpUtils.sendJson(exchange, 200, logs);
            } catch (Exception e) {
                HttpUtils.handleException(exchange, e);
            }
        }
    }

    // =========================================================================
    // 8b. License Packages Handler: GET /api/licenses
    // =========================================================================
    private class LicensePackageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.enableCors(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Method not allowed.");
                return;
            }
            try {
                HttpUtils.sendJson(exchange, 200, journalService.getAllLicensePackages());
            } catch (Exception e) {
                HttpUtils.handleException(exchange, e);
            }
        }
    }

    // =========================================================================
    // 8c. Granted Licenses Handler (MANUAL Grant & Revoke):
    //     GET    /api/granted-licenses
    //     POST   /api/granted-licenses        { personId, licenseId }
    //     DELETE /api/granted-licenses/{id}
    // =========================================================================
    private class GrantedLicenseHandler implements HttpHandler {
        private final Pattern idPattern = Pattern.compile("^/api/granted-licenses/(\\d+)/?$");

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtils.enableCors(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            Matcher idMatcher = idPattern.matcher(path);
            if (idMatcher.matches()) {
                if ("DELETE".equalsIgnoreCase(method)) {
                    int grantId = Integer.parseInt(idMatcher.group(1));
                    try {
                        journalService.revokeLicenseByGrantId(grantId);
                        Map<String, Object> res = new LinkedHashMap<>();
                        res.put("success", true);
                        res.put("message", "Thu hồi License thành công (Grant ID: " + grantId + ").");
                        res.put("grantId", grantId);
                        HttpUtils.sendJson(exchange, 200, res);
                    } catch (Exception e) {
                        HttpUtils.handleException(exchange, e);
                    }
                    return;
                }
                HttpUtils.sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Method not allowed.");
                return;
            }

            if ("/api/granted-licenses".equals(path) || "/api/granted-licenses/".equals(path)) {
                if ("GET".equalsIgnoreCase(method)) {
                    try {
                        HttpUtils.sendJson(exchange, 200, journalService.getAllGrantedLicensesDetailed());
                    } catch (Exception e) {
                        HttpUtils.handleException(exchange, e);
                    }
                    return;
                } else if ("POST".equalsIgnoreCase(method)) {
                    try {
                        JsonObject json = HttpUtils.readJsonBody(exchange);
                        if (!json.has("personId") || !json.has("licenseId")) {
                            HttpUtils.sendError(exchange, 400, "INVALID_INPUT", "Yêu cầu cả 'personId' và 'licenseId'.");
                            return;
                        }
                        int personId = json.get("personId").getAsInt();
                        int licenseId = json.get("licenseId").getAsInt();
                        var grant = journalService.grantLicenseManual(personId, licenseId);
                        Map<String, Object> res = new LinkedHashMap<>();
                        res.put("success", true);
                        res.put("message", "Cấp License MANUAL thành công.");
                        res.put("grantId", grant.getGrantId());
                        res.put("personId", grant.getPersonId());
                        res.put("licenseId", grant.getLicenseId());
                        HttpUtils.sendJson(exchange, 201, res);
                    } catch (Exception e) {
                        HttpUtils.handleException(exchange, e);
                    }
                    return;
                }
            }

            HttpUtils.sendError(exchange, 404, "NOT_FOUND", "Endpoint not found: " + path);
        }
    }

    // =========================================================================
    // 9. Static Web UI Handler: Serves files from Project/web/
    // =========================================================================
    private class StaticWebHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HttpUtils.enableCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            File baseDir = new File(WEB_DIR);
            if (!baseDir.exists() || !baseDir.isDirectory()) {
                // Try Project/web if running from root
                baseDir = new File("Project/" + WEB_DIR);
            }

            File file = new File(baseDir, path.startsWith("/") ? path.substring(1) : path);
            if (!file.exists() || file.isDirectory()) {
                file = new File(baseDir, "index.html");
            }

            if (!file.exists()) {
                String notFound = "<h1>404 Not Found</h1><p>File not found: " + path + "</p>";
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(404, notFound.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes());
                }
                return;
            }

            String contentType = getContentType(file.getName());
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.getResponseHeaders().set("Pragma", "no-cache");
            exchange.getResponseHeaders().set("Expires", "0");

            byte[] bytes = Files.readAllBytes(file.toPath());
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
            System.out.printf("[ApiServer] %-6s %-30s -> 200 OK (%d bytes)%n", exchange.getRequestMethod(), path, bytes.length);
        }

        private String getContentType(String fileName) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
            if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
            if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".svg")) return "image/svg+xml";
            if (lower.endsWith(".ico")) return "image/x-icon";
            return "text/plain; charset=UTF-8";
        }
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        try {
            ApiServer apiServer = new ApiServer(port);
            apiServer.start();

            Runtime.getRuntime().addShutdownHook(new Thread(apiServer::stop));
        } catch (Exception e) {
            System.err.println("[ApiServer] Lỗi khi khởi chạy server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
