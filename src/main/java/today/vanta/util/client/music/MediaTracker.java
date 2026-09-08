package today.vanta.util.client.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// from yuri client all credit to kawase for making the core thing or sum and unleg1t for making it work
public class MediaTracker {
    public static final String SOURCE_WINDOWS = "WINDOWS MEDIA";
    public static final String SOURCE_SPOTIFY = "SPOTIFY";
    public static final String SOURCE_GENERIC = "MEDIA PLAYER";

    private static final String OS_NAME = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    private static final boolean WINDOWS = OS_NAME.contains("win");
    private static final boolean LINUX = OS_NAME.contains("linux") || OS_NAME.contains("nix");

    private static final long POLL_INTERVAL_MILLIS = 1000L;
    private static final long COVER_RETRY_MILLIS = 15000L;
    private static final long SESSION_INIT_TIMEOUT_MILLIS = 10000L;
    private static final long QUERY_TIMEOUT_MILLIS = 2000L;
    private static final int COVER_MAX_SIZE = 160;

    private static final Pattern ARTIST_ITEM_PATTERN = Pattern.compile("string \"([^\"]*)\"");
    private static final Pattern MPRIS_SERVICE_PATTERN = Pattern.compile("string \"(org\\.mpris\\.MediaPlayer2\\.[^\"]+)\"");

    private static final String INIT_SCRIPT =
            "$ErrorActionPreference = 'SilentlyContinue'; " +
                    "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; " +
                    "$global:yuriHasWinRT = $false; " +
                    "try { " +
                    "  Add-Type -AssemblyName System.Runtime.WindowsRuntime -ErrorAction Stop; " +
                    "  $null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime]; " +
                    "  $global:yuriAsTask = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]; " +
                    "  function YuriAwait($op, $type) { $t = $global:yuriAsTask.MakeGenericMethod($type).Invoke($null, @($op)); $t.Wait(-1) | Out-Null; $t.Result }; " +
                    "  $global:yuriManager = YuriAwait ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]); " +
                    "  if ($global:yuriManager) { $global:yuriHasWinRT = $true } " +
                    "} catch {}; " +
                    "Write-Output 'YURI_READY'";

    private static final String QUERY_SCRIPT =
            "if ($global:yuriHasWinRT -and $global:yuriManager) { " +
                    "  $yuriSession = $global:yuriManager.GetCurrentSession(); " +
                    "  if ($yuriSession) { " +
                    "    $yuriProps = YuriAwait ($yuriSession.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties]); " +
                    "    $yuriTimeline = $yuriSession.GetTimelineProperties(); " +
                    "    $yuriPlayback = $yuriSession.GetPlaybackInfo(); " +
                    "    Write-Output ('TITLE=' + $yuriProps.Title); " +
                    "    Write-Output ('ARTIST=' + $yuriProps.Artist); " +
                    "    Write-Output ('ALBUM=' + $yuriProps.AlbumTitle); " +
                    "    Write-Output ('POSITION=' + [long]$yuriTimeline.Position.TotalMilliseconds); " +
                    "    Write-Output ('LENGTH=' + [long]($yuriTimeline.EndTime.TotalMilliseconds - $yuriTimeline.StartTime.TotalMilliseconds)); " +
                    "    Write-Output ('STATUS=' + $yuriPlayback.PlaybackStatus); " +
                    "    Write-Output ('APP=' + $yuriSession.SourceAppUserModelId) " +
                    "  } " +
                    "} else { " +
                    "  $p = Get-Process | Where-Object { $_.MainWindowTitle -ne '' -and ($_.ProcessName -match 'spotify|chrome|msedge|firefox|vlc|foobar2000|aimp|wmplayer|mpc-hc') } | Select-Object -First 1; " +
                    "  if ($p) { " +
                    "    $title = $p.MainWindowTitle; " +
                    "    if ($title -and $title -ne 'Spotify') { " +
                    "      if ($title -match '^(.*?) - (.*)$') { " +
                    "        Write-Output ('ARTIST=' + $matches[1]); " +
                    "        Write-Output ('TITLE=' + $matches[2]) " +
                    "      } else { " +
                    "        Write-Output ('TITLE=' + $title) " +
                    "      }; " +
                    "      Write-Output 'STATUS=Playing'; " +
                    "      Write-Output ('APP=' + $p.ProcessName) " +
                    "    } " +
                    "  } " +
                    "}; Write-Output '%s'";

    private static final Pattern TITLE_WIN = Pattern.compile("TITLE=(.*)");
    private static final Pattern ARTIST_WIN = Pattern.compile("ARTIST=(.*)");
    private static final Pattern ALBUM_WIN = Pattern.compile("ALBUM=(.*)");
    private static final Pattern POSITION_WIN = Pattern.compile("POSITION=(\\d+)");
    private static final Pattern LENGTH_WIN = Pattern.compile("LENGTH=(\\d+)");
    private static final Pattern STATUS_WIN = Pattern.compile("STATUS=(.*)");
    private static final Pattern APP_WIN = Pattern.compile("APP=(.*)");

    private static final Pattern TITLE_DBUS = Pattern.compile("string \"xesam:title\"\\s+variant\\s+string \"([^\"]*)\"");
    private static final Pattern ALBUM_DBUS = Pattern.compile("string \"xesam:album\"\\s+variant\\s+string \"([^\"]*)\"");
    private static final Pattern ARTISTS_DBUS = Pattern.compile("string \"xesam:artist\"\\s+variant\\s+array \\[([^\\]]*)]");
    private static final Pattern ART_DBUS = Pattern.compile("string \"mpris:artUrl\"\\s+variant\\s+string \"([^\"]*)\"");
    private static final Pattern LENGTH_DBUS = Pattern.compile("string \"mpris:length\"\\s+variant\\s+(?:uint64|int64) (\\d+)");
    private static final Pattern POSITION_DBUS = Pattern.compile("string \"Position\"\\s+variant\\s+int64 (\\d+)");
    private static final Pattern STATUS_DBUS = Pattern.compile("string \"PlaybackStatus\"\\s+variant\\s+string \"(\\w+)\"");

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Yuri Media Tracker");
        thread.setDaemon(true);
        return thread;
    });

    private final ExecutorService coverExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Yuri Media Cover");
        thread.setDaemon(true);
        return thread;
    });

    private final Object sessionLock = new Object();
    private final AtomicLong sentinelCounter = new AtomicLong();

    private volatile ScheduledFuture<?> pollTask;
    private volatile boolean running;

    private Process psProcess;
    private BufferedWriter psWriter;
    private BlockingQueue<String> psOutput;
    private Thread psReaderThread;

    private volatile MediaTrack track;
    private volatile ResourceLocation coverLocation;

    private volatile long anchorPositionMillis;
    private volatile long anchorTimeMillis;
    private volatile boolean anchorPlaying;
    private volatile long anchorLengthMillis;

    private volatile String coverKey = "";
    private volatile String coverFailedKey;
    private volatile long coverFailedAt;

    public boolean isSupported() {
        return WINDOWS || LINUX;
    }

    public MediaTrack getTrack() {
        return track;
    }

    public ResourceLocation getCoverLocation() {
        return coverLocation;
    }

    public synchronized void start() {
        if (running || !isSupported()) return;
        running = true;
        pollTask = scheduler.scheduleWithFixedDelay(this::pollSafely, 0, POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        running = false;
        if (pollTask != null) {
            pollTask.cancel(true);
            pollTask = null;
        }
        closeSession();
        track = null;
        coverLocation = null;
    }

    public long getPositionMillis() {
        if (track == null) return 0L;
        if (!anchorPlaying) return anchorPositionMillis;
        long estimated = anchorPositionMillis + (System.currentTimeMillis() - anchorTimeMillis);
        if (anchorLengthMillis > 0L) return clamp(estimated, 0L, anchorLengthMillis);
        return Math.max(0L, estimated);
    }

    private void pollSafely() {
        if (!running) return;
        try {
            if (WINDOWS) refreshWindows();
            else if (LINUX) refreshLinuxGeneric();
        } catch (Exception exception) {
            track = null;
        }
    }

    private void refreshLinuxGeneric() {
        List<String> players = findLinuxMprisPlayers();
        if (players.isEmpty()) {
            track = null;
            return;
        }

        String bestPlayer = null;
        String bestReply = null;

        for (String player : players) {
            String reply = runCommand("dbus-send", "--print-reply", "--session",
                    "--dest=" + player, "/org/mpris/MediaPlayer2",
                    "org.freedesktop.DBus.Properties.GetAll", "string:org.mpris.MediaPlayer2.Player");

            if (reply == null) continue;

            String status = match(STATUS_DBUS, reply);
            if ("Playing".equalsIgnoreCase(status)) {
                bestPlayer = player;
                bestReply = reply;
                break;
            } else if (bestReply == null) {
                bestPlayer = player;
                bestReply = reply;
            }
        }

        if (bestReply == null) {
            track = null;
            return;
        }

        String title = match(TITLE_DBUS, bestReply);
        if (title == null || title.trim().isEmpty()) {
            track = null;
            return;
        }

        String album = match(ALBUM_DBUS, bestReply);
        String artistsBlock = match(ARTISTS_DBUS, bestReply);
        String artUrl = match(ART_DBUS, bestReply);
        String length = match(LENGTH_DBUS, bestReply);
        String position = match(POSITION_DBUS, bestReply);
        String status = match(STATUS_DBUS, bestReply);

        StringBuilder artists = new StringBuilder();
        if (artistsBlock != null) {
            Matcher artistMatcher = ARTIST_ITEM_PATTERN.matcher(artistsBlock);
            while (artistMatcher.find()) {
                if (artists.length() > 0) artists.append(", ");
                artists.append(artistMatcher.group(1));
            }
        }

        String source = bestPlayer.contains("spotify") ? SOURCE_SPOTIFY : SOURCE_GENERIC;

        apply(title, artists.toString(), album == null ? "" : album,
                length == null ? 0L : Long.parseLong(length) / 1000L,
                position == null ? 0L : Long.parseLong(position) / 1000L,
                "Playing".equalsIgnoreCase(status),
                artUrl == null ? "" : artUrl.replace("open.spotify.com", "i.scdn.co"),
                source);
    }

    private List<String> findLinuxMprisPlayers() {
        List<String> players = new ArrayList<>();
        String output = runCommand("dbus-send", "--session", "--dest=org.freedesktop.DBus",
                "--type=method_call", "--print-reply", "/org/freedesktop/DBus",
                "org.freedesktop.DBus.ListNames");

        if (output != null) {
            Matcher matcher = MPRIS_SERVICE_PATTERN.matcher(output);
            while (matcher.find()) {
                players.add(matcher.group(1));
            }
        }
        return players;
    }

    private void refreshWindows() {
        synchronized (sessionLock) {
            if (!sessionAlive() && !startSession()) {
                closeSession();
                track = null;
                return;
            }

            String sentinel = "YURI_END_" + sentinelCounter.incrementAndGet();
            try {
                writeLine(String.format(QUERY_SCRIPT, sentinel));
            } catch (IOException exception) {
                closeSession();
                track = null;
                return;
            }

            List<String> lines = readUntil(sentinel, QUERY_TIMEOUT_MILLIS);
            if (lines == null) {
                closeSession();
                track = null;
                return;
            }

            parseWindowsReply(lines);
        }
    }

    private void parseWindowsReply(List<String> lines) {
        String reply = String.join("\n", lines);

        String title = match(TITLE_WIN, reply);
        if (title == null || title.trim().isEmpty()) {
            track = null;
            return;
        }

        String artist = match(ARTIST_WIN, reply);
        String album = match(ALBUM_WIN, reply);
        String position = match(POSITION_WIN, reply);
        String length = match(LENGTH_WIN, reply);
        String status = match(STATUS_WIN, reply);
        String app = match(APP_WIN, reply);

        String source = SOURCE_WINDOWS;
        if (app != null) {
            String appLower = app.toLowerCase(Locale.ROOT);
            if (appLower.contains("spotify")) source = SOURCE_SPOTIFY;
        }

        apply(title.trim(), artist == null ? "" : artist.trim(), album == null ? "" : album.trim(),
                length == null ? 0L : Long.parseLong(length),
                position == null ? 0L : Long.parseLong(position),
                status != null && (status.trim().equalsIgnoreCase("Playing") || status.trim().equals("4")),
                "", source);
    }

    private boolean sessionAlive() {
        return psProcess != null && psProcess.isAlive() && psWriter != null;
    }

    private boolean startSession() {
        closeSession();
        try {
            ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-NoLogo", "-NoProfile", "-ExecutionPolicy", "Bypass");
            builder.redirectErrorStream(true);
            psProcess = builder.start();
            psWriter = new BufferedWriter(new OutputStreamWriter(psProcess.getOutputStream(), StandardCharsets.UTF_8));
            psOutput = new LinkedBlockingQueue<>();

            BufferedReader reader = new BufferedReader(new InputStreamReader(psProcess.getInputStream(), StandardCharsets.UTF_8));
            psReaderThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) psOutput.offer(line);
                } catch (IOException ignored) {
                }
            }, "Yuri Media PS Reader");
            psReaderThread.setDaemon(true);
            psReaderThread.start();

            writeLine(INIT_SCRIPT);
            return readUntil("YURI_READY", SESSION_INIT_TIMEOUT_MILLIS) != null;
        } catch (IOException exception) {
            closeSession();
            return false;
        }
    }

    private void writeLine(String line) throws IOException {
        psWriter.write(line);
        psWriter.newLine();
        psWriter.flush();
    }

    private List<String> readUntil(String sentinel, long timeoutMillis) {
        List<String> lines = new ArrayList<>();
        long deadline = System.currentTimeMillis() + timeoutMillis;

        while (true) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) return null;

            String line;
            try {
                line = psOutput.poll(remaining, TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return null;
            }

            if (line == null) return null;
            if (line.contains(sentinel)) return lines;
            lines.add(line);
        }
    }

    private void closeSession() {
        synchronized (sessionLock) {
            if (psProcess != null) {
                try {
                    if (psWriter != null) psWriter.close();
                } catch (IOException ignored) {
                }
                psProcess.destroyForcibly();
            }
            psProcess = null;
            psWriter = null;
            psOutput = null;
            psReaderThread = null;
        }
    }

    private void apply(String title, String artists, String album, long lengthMillis, long measuredPositionMillis, boolean playing, String artUrl, String source) {
        long now = System.currentTimeMillis();
        MediaTrack previous = track;
        boolean sameTrack = previous != null && previous.getTitle().equals(title) && previous.getArtist().equals(artists);

        boolean playStateChanged = previous == null || previous.isPlaying() != playing;

        if (!sameTrack || playStateChanged) {
            anchorPositionMillis = measuredPositionMillis;
            anchorTimeMillis = now;
        }

        anchorPlaying = playing;
        anchorLengthMillis = lengthMillis;

        track = new MediaTrack(title, artists, album, lengthMillis, measuredPositionMillis, playing, source);

        updateCover(artUrl, title, artists);
    }

    private void updateCover(String artUrl, String title, String artists) {
        String key = artUrl == null || artUrl.trim().isEmpty() ? title + "\u0000" + artists : artUrl;
        if (key.equals(coverKey)) return;
        if (key.equals(coverFailedKey) && System.currentTimeMillis() - coverFailedAt < COVER_RETRY_MILLIS) return;
        coverExecutor.execute(() -> fetchCover(key, artUrl, title, artists));
    }

    private void fetchCover(String key, String artUrl, String title, String artists) {
        try {
            String resolvedUrl = artUrl;
            if (resolvedUrl == null || resolvedUrl.trim().isEmpty()) {
                String body = httpGet("https://itunes.apple.com/search?media=music&entity=song&limit=1&term="
                        + URLEncoder.encode(artists + " " + title, "UTF-8"), null);
                if (body == null) {
                    markCoverFailed(key);
                    return;
                }

                JsonArray results = new JsonParser().parse(body).getAsJsonObject().getAsJsonArray("results");
                if (results == null || results.size() == 0) {
                    markCoverFailed(key);
                    return;
                }

                resolvedUrl = results.get(0).getAsJsonObject().get("artworkUrl100").getAsString().replace("100x100", "600x600");
            }

            byte[] imageBytes = resolvedUrl.startsWith("file://")
                    ? readFileBytes(resolvedUrl.substring(7))
                    : httpGetBytes(resolvedUrl);

            if (imageBytes == null) {
                markCoverFailed(key);
                return;
            }

            BufferedImage sourceImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (sourceImage == null) {
                markCoverFailed(key);
                return;
            }

            BufferedImage scaledImage = scaleDown(sourceImage, COVER_MAX_SIZE);
            ResourceLocation previous = coverLocation;
            ResourceLocation location = new ResourceLocation("yuri", "media/cover_" + System.nanoTime());

            Minecraft.getMinecraft().addScheduledTask(() -> {
                try {
                    DynamicTexture dynamicTexture = new DynamicTexture(scaledImage);
                    Minecraft.getMinecraft().getTextureManager().loadTexture(location, dynamicTexture);
                    coverLocation = location;
                    coverKey = key;
                    coverFailedKey = null;
                    if (previous != null) Minecraft.getMinecraft().getTextureManager().deleteTexture(previous);
                } catch (Exception exception) {
                    markCoverFailed(key);
                }
            });
        } catch (Exception exception) {
            markCoverFailed(key);
        }
    }

    private byte[] readFileBytes(String path) {
        try (InputStream is = new FileInputStream(path);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[4096];
            int read;
            while ((read = is.read(chunk)) != -1) buffer.write(chunk, 0, read);
            return buffer.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    private BufferedImage scaleDown(BufferedImage source, int maxSize) {
        int width = source.getWidth();
        int height = source.getHeight();
        if (width <= maxSize && height <= maxSize) return source;

        double scale = Math.min((double) maxSize / width, (double) maxSize / height);
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();
        return scaled;
    }

    private void markCoverFailed(String key) {
        coverFailedKey = key;
        coverFailedAt = System.currentTimeMillis();
    }

    private String httpGet(String urlString, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "Yuri-Client/1.0");
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet())
                connection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        if (stream == null) return null;

        String body = readAll(stream);
        connection.disconnect();
        return status >= 200 && status < 300 ? body : null;
    }

    private byte[] httpGetBytes(String urlString) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "Yuri-Client/1.0");

        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) return null;

        InputStream stream = connection.getInputStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = stream.read(chunk)) != -1) buffer.write(chunk, 0, read);

        stream.close();
        connection.disconnect();
        return buffer.toByteArray();
    }

    private String readAll(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = stream.read(chunk)) != -1) buffer.write(chunk, 0, read);
        stream.close();
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private long clamp(long value, long min, long max) {
        return value < min ? min : (value > max ? max : value);
    }

    private String match(Pattern pattern, String reply) {
        Matcher matcher = pattern.matcher(reply);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String runCommand(String... command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            InputStream stream = process.getInputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = stream.read(chunk)) != -1) buffer.write(chunk, 0, read);
            String output = new String(buffer.toByteArray(), StandardCharsets.UTF_8);

            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            return process.exitValue() == 0 ? output : null;
        } catch (IOException exception) {
            return null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
