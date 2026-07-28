package today.vanta.util.client.network;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.util.Session;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import today.vanta.util.client.Strings;
import today.vanta.util.system.OperatingSystem;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class MicrosoftUtil {
    public static final RequestConfig REQUEST_CONFIG = RequestConfig
            .custom()
            .setConnectionRequestTimeout(30_000)
            .setConnectTimeout(30_000)
            .setSocketTimeout(30_000)
            .build();

    public static final String CLIENT_ID = "54fd49e4-2103-4044-9603-2b028c814ec3";
    public static final String USER_AGENT = Strings.CLIENT_NAME + "/" + Strings.CLIENT_VERSION;
    private static final int[] TRY_BIND_PORTS = {
            59125, 59126, 59127, 59128, 59129, 59130, 59131,
            59132, 59133, 59134, 59135, 1234, 1235, 1236, 1237,
            80, 8080, 19364, 19365, 19366, 27930, 27931, 27932,
            27933, 27934, 42069
    };
    private static final String REDIRECT_PATH = "/in_game_account_switcher_long_enough_uri_to_prevent_accidental_leaks_on_screensharing_even_if_you_have_like_extremely_big_screen_though_it_might_not_mork_but_we_will_try_it_anyway_to_prevent_funny_things_from_happening_or_something";

    private static String redirectUri(int port) {
        return String.format("http://localhost:%d%s", port, REDIRECT_PATH);
    }

    private static JsonObject parseJsonResponse(HttpResponse res, String context) throws Exception {
        final int statusCode = res.getStatusLine().getStatusCode();
        final String rawResponse = EntityUtils.toString(res.getEntity());

        if (statusCode < 200 || statusCode >= 300) {
            throw new Exception(String.format(
                    "%s returned HTTP %d: %s",
                    context,
                    statusCode,
                    StringUtils.isBlank(rawResponse) ? "<empty body>" : rawResponse
            ));
        }

        try {
            return new JsonParser().parse(rawResponse).getAsJsonObject();
        } catch (Exception e) {
            throw new Exception(String.format(
                    "%s returned non-JSON response (HTTP %d): %s",
                    context,
                    statusCode,
                    rawResponse
            ));
        }
    }

    private static int bindToSupportedPort(HttpServer server) {
        for (int port : TRY_BIND_PORTS) {
            try {
                server.bind(new InetSocketAddress(port), 0);
                return port;
            } catch (Exception ignored) {
            }
        }

        throw new IllegalStateException("Unable to bind to any supported port.");
    }

    public static final class AuthCodeResult {
        public final String code;
        public final String redirectUri;

        public AuthCodeResult(String code, String redirectUri) {
            this.code = code;
            this.redirectUri = redirectUri;
        }
    }

    public static final class MicrosoftTokens {
        public final String accessToken;
        public final String refreshToken;

        public MicrosoftTokens(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }

    public static final class LoginResult {
        public final Session session;
        public final String refreshToken;

        public LoginResult(Session session, String refreshToken) {
            this.session = session;
            this.refreshToken = refreshToken;
        }
    }

    public static CompletableFuture<AuthCodeResult> acquireMSAuthCode(
            final Executor executor
    ) {
        return acquireMSAuthCode(OperatingSystem.getOperatingSystem()::open, executor);
    }

    public static CompletableFuture<AuthCodeResult> acquireMSAuthCode(
            final Consumer<URI> browserAction,
            final Executor executor
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final String state = RandomStringUtils.randomAlphanumeric(8);

                final HttpServer server = HttpServer.create();

                final CountDownLatch latch = new CountDownLatch(1);
                final AtomicReference<String> authCode = new AtomicReference<>(null),
                        errorMsg = new AtomicReference<>(null);

                final int port = bindToSupportedPort(server);
                final String redirectUri = redirectUri(port);

                server.createContext(REDIRECT_PATH, exchange -> {
                    final Map<String, String> query = URLEncodedUtils
                            .parse(
                                    exchange.getRequestURI().toString().replaceAll(REDIRECT_PATH + "\\?", ""),
                                    StandardCharsets.UTF_8
                            )
                            .stream()
                            .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));

                    if (!state.equals(query.get("state"))) {
                        errorMsg.set(
                                String.format("State mismatch! Expected '%s' but got '%s'.", state, query.get("state"))
                        );
                    } else if (query.containsKey("code")) {
                        authCode.set(query.get("code"));
                    } else if (query.containsKey("error")) {
                        errorMsg.set(String.format("%s: %s", query.get("error"), query.get("error_description")));
                    }

                    NetworkUtil.sendClasspathResource(exchange, "/assets/vanta/callback.html", MicrosoftUtil.class);

                    latch.countDown();
                });

                server.createContext("/assets/", exchange -> {
                    String path = exchange.getRequestURI().getPath();
                    if (!NetworkUtil.isSafePath(path, "/assets/")) {
                        NetworkUtil.sendNotFound(exchange);
                        return;
                    }

                    NetworkUtil.sendClasspathResource(exchange, path, MicrosoftUtil.class);
                });

                server.start();

                final URIBuilder uriBuilder = new URIBuilder("https://login.live.com/oauth20_authorize.srf")
                        .addParameter("client_id", CLIENT_ID)
                        .addParameter("response_type", "code")
                        .addParameter("redirect_uri", redirectUri)
                        .addParameter("scope", "XboxLive.signin XboxLive.offline_access")
                        .addParameter("state", state)
                        .addParameter("prompt", "select_account");
                final URI uri = uriBuilder.build();

                browserAction.accept(uri);

                try {
                    latch.await();

                    return new AuthCodeResult(
                            Optional.ofNullable(authCode.get())
                            .filter(code -> !StringUtils.isBlank(code))
                            .orElseThrow(() -> new Exception(
                                    Optional.ofNullable(errorMsg.get())
                                            .orElse("There was no auth code or error description present.")
                            )),
                            redirectUri
                    );
                } finally {
                    new Thread(() -> {
                        try {
                            Thread.sleep(1500);
                        } catch (InterruptedException ignored) {
                        }
                        server.stop(2);
                    }, "MsAuthCallbackServerStopper").start();
                }
            } catch (InterruptedException e) {
                throw new CancellationException("Microsoft auth code acquisition was cancelled!");
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Microsoft auth code!", e);
            }
        }, executor);
    }

    public static CompletableFuture<MicrosoftTokens> acquireMSTokensFromAuthCode(
            final String authCode,
            final String redirectUri,
            final Executor executor
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                final HttpPost request = new HttpPost(URI.create("https://login.live.com/oauth20_token.srf"));
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Content-Type", "application/x-www-form-urlencoded");
                request.setHeader("Accept", "application/json");
                request.setHeader("User-Agent", USER_AGENT);
                request.setEntity(new UrlEncodedFormEntity(
                        Arrays.asList(
                                new BasicNameValuePair("client_id", CLIENT_ID),
                                new BasicNameValuePair("grant_type", "authorization_code"),
                                new BasicNameValuePair("code", authCode),
                                new BasicNameValuePair("redirect_uri", redirectUri)
                        ),
                        StandardCharsets.UTF_8.name()
                ));

                final HttpResponse res = client.execute(request);
                final JsonObject json = parseJsonResponse(res, "Microsoft OAuth token endpoint");

                String accessToken = Optional.ofNullable(json.get("access_token"))
                        .map(JsonElement::getAsString)
                        .filter(token -> !StringUtils.isBlank(token))
                        .orElseThrow(() -> new Exception(
                                json.has("error") ? String.format(
                                        "%s: %s",
                                        json.get("error").getAsString(),
                                        json.get("error_description").getAsString()
                                ) : "There was no access token or error description present."
                        ));
                String refreshToken = Optional.ofNullable(json.get("refresh_token"))
                        .map(JsonElement::getAsString)
                        .filter(token -> !StringUtils.isBlank(token))
                        .orElseThrow(() -> new Exception("There was no refresh token present."));
                return new MicrosoftTokens(accessToken, refreshToken);
            } catch (InterruptedException e) {
                throw new CancellationException("Microsoft access token acquisition was cancelled!");
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Microsoft access token!", e);
            }
        }, executor);
    }

    public static CompletableFuture<MicrosoftTokens> acquireMSTokensFromRefreshToken(
            final String refreshToken,
            final Executor executor
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                final HttpPost request = new HttpPost(URI.create("https://login.live.com/oauth20_token.srf"));
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Content-Type", "application/x-www-form-urlencoded");
                request.setHeader("Accept", "application/json");
                request.setHeader("User-Agent", USER_AGENT);
                request.setEntity(new UrlEncodedFormEntity(
                        Arrays.asList(
                                new BasicNameValuePair("client_id", CLIENT_ID),
                                new BasicNameValuePair("grant_type", "refresh_token"),
                                new BasicNameValuePair("refresh_token", refreshToken),
                                new BasicNameValuePair("scope", "XboxLive.signin XboxLive.offline_access")
                        ),
                        StandardCharsets.UTF_8.name()
                ));

                final HttpResponse res = client.execute(request);
                final JsonObject json = parseJsonResponse(res, "Microsoft OAuth token endpoint");

                String accessToken = Optional.ofNullable(json.get("access_token"))
                        .map(JsonElement::getAsString)
                        .filter(token -> !StringUtils.isBlank(token))
                        .orElseThrow(() -> new Exception(
                                json.has("error") ? String.format(
                                        "%s: %s",
                                        json.get("error").getAsString(),
                                        json.get("error_description").getAsString()
                                ) : "There was no access token or error description present."
                        ));
                String nextRefreshToken = Optional.ofNullable(json.get("refresh_token"))
                        .map(JsonElement::getAsString)
                        .filter(token -> !StringUtils.isBlank(token))
                        .orElse(refreshToken);
                return new MicrosoftTokens(accessToken, nextRefreshToken);
            } catch (InterruptedException e) {
                throw new CancellationException("Microsoft refresh token acquisition was cancelled!");
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Microsoft refresh token!", e);
            }
        }, executor);
    }

    public static CompletableFuture<String> acquireMSAccessToken(
            final String authCode,
            final String redirectUri,
            final Executor executor
    ) {
        return acquireMSTokensFromAuthCode(authCode, redirectUri, executor).thenApply(tokens -> tokens.accessToken);
    }

    public static CompletableFuture<String> acquireMSAccessToken(
            final String refreshToken,
            final Executor executor
    ) {
        return acquireMSTokensFromRefreshToken(refreshToken, executor).thenApply(tokens -> tokens.accessToken);
    }

    public static CompletableFuture<String> acquireXboxAccessToken(
            final String accessToken,
            final Executor executor
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                final HttpPost request = new HttpPost(URI.create("https://user.auth.xboxlive.com/user/authenticate"));
                final JsonObject entity = new JsonObject();
                final JsonObject properties = new JsonObject();
                properties.addProperty("AuthMethod", "RPS");
                properties.addProperty("SiteName", "user.auth.xboxlive.com");
                properties.addProperty("RpsTicket", String.format("d=%s", accessToken));
                entity.add("Properties", properties);
                entity.addProperty("RelyingParty", "http://auth.xboxlive.com");
                entity.addProperty("TokenType", "JWT");
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Content-Type", "application/json");
                request.setHeader("Accept", "application/json");
                request.setHeader("User-Agent", USER_AGENT);
                request.setEntity(new StringEntity(entity.toString()));

                final HttpResponse res = client.execute(request);
                final JsonObject json = parseJsonResponse(res, "Xbox Live authentication endpoint");

                return Optional.ofNullable(json.get("Token"))
                        .map(JsonElement::getAsString)
                        .filter(token -> !StringUtils.isBlank(token))
                        .orElseThrow(() -> new Exception(
                                json.has("XErr") ? String.format(
                                        "%s: %s", json.get("XErr").getAsString(), json.get("Message").getAsString()
                                ) : "There was no access token or error description present."
                        ));
            } catch (InterruptedException e) {
                throw new CancellationException("Xbox Live access token acquisition was cancelled!");
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Xbox Live access token!", e);
            }
        }, executor);
    }

    public static CompletableFuture<Map<String, String>> acquireXboxXstsToken(
            final String accessToken,
            final Executor executor
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                // Build a new HTTP request
                final HttpPost request = new HttpPost("https://xsts.auth.xboxlive.com/xsts/authorize");
                final JsonObject entity = new JsonObject();
                final JsonObject properties = new JsonObject();
                final JsonArray userTokens = new JsonArray();
                userTokens.add(new JsonPrimitive(accessToken));
                properties.addProperty("SandboxId", "RETAIL");
                properties.add("UserTokens", userTokens);
                entity.add("Properties", properties);
                entity.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
                entity.addProperty("TokenType", "JWT");
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Content-Type", "application/json");
                request.setHeader("Accept", "application/json");
                request.setHeader("User-Agent", USER_AGENT);
                request.setEntity(new StringEntity(entity.toString()));

                final HttpResponse res = client.execute(request);
                final JsonObject json = parseJsonResponse(res, "Xbox Live XSTS endpoint");

                return Optional.ofNullable(json.get("Token"))
                        .map(JsonElement::getAsString)
                        .filter(token -> !StringUtils.isBlank(token))
                        .map(token -> {
                            final String uhs = json.get("DisplayClaims").getAsJsonObject()
                                    .get("xui").getAsJsonArray()
                                    .get(0).getAsJsonObject()
                                    .get("uhs").getAsString();

                            Map<String, String> result = new HashMap<>();
                            result.put("Token", token);
                            result.put("uhs", uhs);
                            return result;
                        })
                        .orElseThrow(() -> new Exception(
                                json.has("XErr") ? String.format(
                                        "%s: %s", json.get("XErr").getAsString(), json.get("Message").getAsString()
                                ) : "There was no access token or error description present."
                        ));
            } catch (InterruptedException e) {
                throw new CancellationException("Xbox Live XSTS token acquisition was cancelled!");
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Xbox Live XSTS token!", e);
            }
        }, executor);
    }

    public static CompletableFuture<String> acquireMCAccessToken(
            final String xstsToken,
            final String userHash,
            final Executor executor
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                final HttpPost request = new HttpPost(URI.create("https://api.minecraftservices.com/authentication/login_with_xbox"));
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Content-Type", "application/json");
                request.setHeader("Accept", "application/json");
                request.setHeader("User-Agent", USER_AGENT);
                request.setEntity(new StringEntity(
                        String.format("{\"identityToken\": \"XBL3.0 x=%s;%s\"}", userHash, xstsToken)
                ));

                final HttpResponse res = client.execute(request);
                final JsonObject json = parseJsonResponse(res, "Minecraft services login_with_xbox endpoint");

                return Optional.ofNullable(json.get("access_token"))
                        .map(JsonElement::getAsString)
                        .filter(token -> !StringUtils.isBlank(token))
                        .orElseThrow(() -> new Exception(
                                json.has("error") ? String.format(
                                        "%s: %s", json.get("error").getAsString(), json.get("errorMessage").getAsString()
                                ) : "There was no access token or error description present."
                        ));
            } catch (InterruptedException e) {
                throw new CancellationException("Minecraft access token acquisition was cancelled!");
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Minecraft access token!", e);
            }
        }, executor);
    }

    public static CompletableFuture<LoginResult> login(
            final String mcToken,
            final String refreshToken,
            final Executor executor
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                final HttpGet request = new HttpGet(URI.create("https://api.minecraftservices.com/minecraft/profile"));
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Authorization", "Bearer " + mcToken);
                request.setHeader("Accept", "application/json");
                request.setHeader("User-Agent", USER_AGENT);

                final HttpResponse res = client.execute(request);
                final JsonObject json = parseJsonResponse(res, "Minecraft profile endpoint");

                Session session = Optional.ofNullable(json.get("id"))
                        .map(JsonElement::getAsString)
                        .filter(uuid -> !StringUtils.isBlank(uuid))
                        .map(uuid -> new Session(
                                json.get("name").getAsString(),
                                uuid,
                                mcToken,
                                Session.Type.MOJANG.toString()
                        ))
                        .orElseThrow(() -> new Exception(
                                json.has("error") ? String.format(
                                        "%s: %s", json.get("error").getAsString(), json.get("errorMessage").getAsString()
                                ) : "There was no profile or error description present."
                        ));
                return new LoginResult(session, refreshToken);
            } catch (InterruptedException e) {
                throw new CancellationException("Minecraft profile fetching was cancelled!");
            } catch (Exception e) {
                throw new CompletionException("Unable to fetch Minecraft profile!", e);
            }
        }, executor);
    }

    public static CompletableFuture<Session> login(
            final String mcToken,
            final Executor executor
    ) {
        return login(mcToken, null, executor).thenApply(result -> result.session);
    }
}
