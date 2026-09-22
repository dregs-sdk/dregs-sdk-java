package com.dregs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/**
 * A real HTTP server on loopback, standing in for the Dregs API.
 *
 * <p>The JDK ships one, so the test suite mocks HTTP without a test-only HTTP dependency, and it
 * exercises the actual {@code HttpClient} rather than a stub in front of it. Responses are queued;
 * a request with nothing queued gets the fallback, and one with neither gets a 500 that will fail
 * the test loudly.
 */
final class StubServer implements AutoCloseable {

    private final HttpServer server;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final ConcurrentLinkedQueue<Reply> queued = new ConcurrentLinkedQueue<>();
    private final List<Recorded> recorded = Collections.synchronizedList(new ArrayList<>());

    private volatile Reply fallback;

    StubServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not start the stub server.", exception);
        }

        server.createContext("/", this::handle);
        server.setExecutor(executor);
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/api";
    }

    /** Queues one reply, consumed by the next request. */
    StubServer enqueue(Reply reply) {
        queued.add(reply);

        return this;
    }

    /** Sets the reply every request gets once the queue is empty. */
    StubServer always(Reply reply) {
        fallback = reply;

        return this;
    }

    List<Recorded> requests() {
        return List.copyOf(recorded);
    }

    Recorded lastRequest() {
        List<Recorded> all = requests();

        return all.get(all.size() - 1);
    }

    int callCount() {
        return recorded.size();
    }

    private void handle(HttpExchange exchange) throws IOException {
        String body;

        try (InputStream in = exchange.getRequestBody()) {
            body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        recorded.add(new Recorded(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getRawPath(),
                exchange.getRequestURI().getRawQuery(),
                new LinkedHashMap<>(exchange.getRequestHeaders()),
                body));

        Reply reply = queued.poll();

        if (reply == null) {
            reply = fallback;
        }

        if (reply == null) {
            reply = Reply.json(500, "{\"message\":\"the stub server had no reply queued\"}");
        }

        if (reply.delayMillis() > 0) {
            try {
                Thread.sleep(reply.delayMillis());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }

        if (reply.drop()) {
            // Closing without sending a status is how a dropped connection looks from the client.
            exchange.close();

            return;
        }

        reply.headers().forEach((name, value) -> exchange.getResponseHeaders().add(name, value));

        byte[] payload = reply.body() == null
                ? new byte[0]
                : reply.body().getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(reply.status(), payload.length == 0 ? -1 : payload.length);

        if (payload.length > 0) {
            exchange.getResponseBody().write(payload);
        }

        exchange.close();
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    /** One request the stub saw. */
    record Recorded(
            String method, String path, String query, Map<String, List<String>> headers, String body) {

        /** Header names are case-insensitive, and the JDK's server rewrites their capitalization. */
        String header(String name) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    List<String> values = entry.getValue();

                    return values == null || values.isEmpty() ? null : values.get(0);
                }
            }

            return null;
        }
    }

    /** One reply the stub will send. */
    record Reply(
            int status,
            String body,
            Map<String, String> headers,
            long delayMillis,
            boolean drop) {

        static Reply json(int status, String body) {
            return new Reply(status, body, Map.of("Content-Type", "application/json"), 0, false);
        }

        static Reply status(int status) {
            return new Reply(status, null, Map.of(), 0, false);
        }

        static Reply text(int status, String body) {
            return new Reply(status, body, Map.of("Content-Type", "text/html"), 0, false);
        }

        /** Closes the connection without answering, the way a dropped connection looks. */
        static Reply dropped() {
            return new Reply(0, null, Map.of(), 0, true);
        }

        /** Sits on the request long enough for the client's timeout to fire. */
        static Reply slow(long millis) {
            return new Reply(200, "{}", Map.of(), millis, false);
        }

        Reply withHeader(String name, String value) {
            Map<String, String> merged = new LinkedHashMap<>(headers);

            merged.put(name, value);

            return new Reply(status, body, merged, delayMillis, drop);
        }
    }
}
