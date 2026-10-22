package org.jackhuang.hmcl.auth;

import com.sun.net.httpserver.HttpServer;
import org.jackhuang.hmcl.auth.yggdrasil.RemoteAuthenticationException;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/// Exercises the actual HTTP device flow against a loopback OAuth provider without real credentials.
@NotNullByDefault
final class OAuthDeviceFlowTest {
    /// Confirms that the standardized device_code field is sent and success is reported once tokens arrive.
    @Test
    void successfulDeviceFlow() throws Exception {
        exercise("{\"access_token\":\"test-access\",\"refresh_token\":\"test-refresh\"}", true);
    }

    /// A rejected grant must not be announced as a successful Microsoft login.
    @Test
    void rejectedDeviceFlow() throws Exception {
        exercise("{\"error\":\"authorization_declined\",\"error_description\":\"User declined\"}", false);
    }

    /// OAuth errors remain failures when providers omit the optional description.
    @Test
    void errorWithoutDescription() throws Exception {
        exercise("{\"error\":\"invalid_client\"}", false);
    }

    /// Runs a complete device flow and checks request format and completion callbacks.
    private void exercise(String tokenResponse, boolean success) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> request = new AtomicReference<>("");
        AtomicBoolean completed = new AtomicBoolean();
        server.createContext("/device", exchange -> {
            byte[] bytes = ("{\"device_code\":\"test-device\",\"user_code\":\"ABCD\","
                    + "\"verification_uri\":\"https://example.com\",\"expires_in\":900,\"interval\":0}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.createContext("/token", exchange -> {
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = tokenResponse.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(success ? 200 : 400, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();
        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            OAuth oauth = new OAuth(base, base, base + "/device", base + "/token");
            OAuth.Callback callback = new OAuth.Callback() {
                /// Device flow must not open a callback server.
                @Override public OAuth.Session startServer() { throw new AssertionError("Unexpected browser flow"); }
                /// Checks the code passed to the login UI.
                @Override public void grantDeviceCode(String code, String uri) { assertEquals("ABCD", code); }
                /// Records the successful completion signal.
                @Override public void loginCompletedDeviceCode() { completed.set(true); }
                /// No browser is opened during this local test.
                @Override public void openBrowser(OAuth.GrantFlow flow, String url) { }
                /// Supplies a non-secret test identifier.
                @Override public String getClientId() { return "test-client"; }
            };
            OAuth.Options options = new OAuth.Options("test-scope", callback);
            if (success) {
                assertEquals("test-access", oauth.authenticate(OAuth.GrantFlow.DEVICE, options).accessToken());
            } else {
                assertThrows(RemoteAuthenticationException.class, () -> oauth.authenticate(OAuth.GrantFlow.DEVICE, options));
            }
            assertTrue(request.get().contains("device_code=test-device"), request.get());
            assertFalse(request.get().contains("&code="), request.get());
            assertEquals(success, completed.get());
        } finally {
            server.stop(0);
        }
    }
}
