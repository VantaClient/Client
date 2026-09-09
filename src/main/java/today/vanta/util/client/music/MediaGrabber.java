package today.vanta.util.client.music;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

// made by our generous AI friend, Claude!
public final class MediaGrabber {
    private static final long POLL_INTERVAL_MS = 1000L;

    private static volatile String artist = "";
    private static volatile String title = "";
    private static volatile long millisLength = 0L;
    private static volatile long millisPosition = 0L;
    private static volatile byte[] coverBytes = null;

    private static String lastArtKey = "";
    private static volatile boolean running = false;
    private static boolean loggedFailure = false;

    private static final Backend BACKEND = detectBackend();


    public static synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        Thread pollThread = new Thread(MediaGrabber::pollLoop, "MediaGrabber-Poller");
        pollThread.setDaemon(true);
        pollThread.start();
    }

    public static synchronized void stop() {
        running = false;
        BACKEND.close();
    }

    private static Backend detectBackend() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return new WindowsBackend();
        } else if (os.contains("mac") || os.contains("darwin")) {
            return new MacBackend();
        } else {
            return new LinuxBackend();
        }
    }

    private static void pollLoop() {
        while (running) {
            try {
                Snapshot snapshot = BACKEND.poll();
                applySnapshot(snapshot);
            } catch (Exception e) {
                if (!loggedFailure) {
                    System.err.println("[MediaPlayerInfo] Couldn't read media info via "
                            + BACKEND.getClass().getSimpleName() + " (is the required tool installed and on PATH? "
                            + "see class docs for what's needed on " + System.getProperty("os.name") + "): "
                            + e.getMessage());
                    loggedFailure = true;
                }
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static void applySnapshot(Snapshot rawSnapshot) {
        if (rawSnapshot == null || !rawSnapshot.hasMedia) {
            artist = "";
            title = "";
            millisLength = 0L;
            millisPosition = 0L;
            coverBytes = null;
            lastArtKey = "";
            return;
        }

        Snapshot snapshot = rawSnapshot.withItunesFallbackIfMissingArt();

        artist = snapshot.artist;
        title = snapshot.title;
        millisLength = snapshot.millisLength;
        millisPosition = snapshot.millisPosition;

        String artKey = snapshot.artKey();
        if (!artKey.equals(lastArtKey)) {
            lastArtKey = artKey;
            coverBytes = snapshot.resolveCoverBytes();
        }
    }

    public static String getArtist() {
        return artist;
    }

    public static String getTitle() {
        return title;
    }

    public static long getMillisLength() {
        return millisLength;
    }

    public static long getMillisPosition() {
        return millisPosition;
    }

    public static byte[] getCoverBytes() {
        return coverBytes;
    }

    public static BufferedImage getCoverImage() {
        byte[] bytes = coverBytes;
        if (bytes == null) {
            return null;
        }
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            return null;
        }
    }

    private static final class Snapshot {
        final boolean hasMedia;
        final String artist;
        final String title;
        final long millisLength;
        final long millisPosition;

        final String coverRef;
        final CoverKind coverKind;

        static final Snapshot EMPTY = new Snapshot(false, "", "", 0, 0, null, CoverKind.NONE);

        Snapshot(boolean hasMedia, String artist, String title, long millisLength, long millisPosition,
                 String coverRef, CoverKind coverKind) {
            this.hasMedia = hasMedia;
            this.artist = artist;
            this.title = title;
            this.millisLength = millisLength;
            this.millisPosition = millisPosition;
            this.coverRef = coverRef;
            this.coverKind = coverKind;
        }

        Snapshot withItunesFallbackIfMissingArt() {
            if (coverKind != CoverKind.NONE) {
                return this;
            }
            if ((artist == null || artist.isEmpty()) && (title == null || title.isEmpty())) {
                return this;
            }
            String query = (artist == null ? "" : artist) + "\u0000" + (title == null ? "" : title);
            return new Snapshot(hasMedia, artist, title, millisLength, millisPosition, query, CoverKind.ITUNES_SEARCH);
        }

        String artKey() {
            if (coverKind == CoverKind.BASE64) {
                String ref = coverRef == null ? "" : coverRef;
                return "b64:" + ref.length() + ":" + ref.substring(0, Math.min(32, ref.length()));
            }
            return coverKind + ":" + coverRef;
        }

        byte[] resolveCoverBytes() {
            if (coverRef == null || coverRef.isEmpty()) {
                return null;
            }
            try {
                switch (coverKind) {
                    case URL:
                        return fetchUrlBytes(coverRef);
                    case FILE:
                        return fetchFileBytes(coverRef);
                    case BASE64:
                        return Base64.getDecoder().decode(coverRef);
                    case ITUNES_SEARCH:
                        String[] parts = coverRef.split("\u0000", 2);
                        String queryArtist = parts.length > 0 ? parts[0] : "";
                        String queryTitle = parts.length > 1 ? parts[1] : "";
                        return fetchItunesCoverArt(queryArtist, queryTitle);
                    default:
                        return null;
                }
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private enum CoverKind { NONE, URL, FILE, BASE64, ITUNES_SEARCH }

    private interface Backend {
        Snapshot poll() throws Exception;

        default void close() {
        }
    }

    private static byte[] fetchFileBytes(String pathOrFileUrl) throws IOException, java.net.URISyntaxException {
        File file = pathOrFileUrl.startsWith("file://")
                ? new File(new URI(pathOrFileUrl))
                : new File(pathOrFileUrl);
        try (FileInputStream in = new FileInputStream(file)) {
            return readAllBytes(in);
        }
    }

    private static byte[] fetchUrlBytes(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        connection.setRequestProperty("User-Agent", "MediaPlayerInfo/1.0");
        try (InputStream in = connection.getInputStream()) {
            return readAllBytes(in);
        }
    }

    private static String fetchUrlText(String url) throws IOException {
        return new String(fetchUrlBytes(url), StandardCharsets.UTF_8);
    }

    private static byte[] fetchItunesCoverArt(String artist, String title) {
        try {
            String term = (artist + " " + title).trim();
            if (term.isEmpty()) {
                return null;
            }
            String url = "https://itunes.apple.com/search?media=music&entity=song&limit=1&term="
                    + URLEncoder.encode(term, "UTF-8");
            String json = fetchUrlText(url);
            if (json == null) {
                return null;
            }
            String artworkUrl = extractJsonStringField(json, "artworkUrl100");
            if (artworkUrl == null || artworkUrl.isEmpty()) {
                return null;
            }
            artworkUrl = artworkUrl.replace("100x100", "600x600");
            return fetchUrlBytes(artworkUrl);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractJsonStringField(String json, String field) {
        String needle = "\"" + field + "\":\"";
        int idx = json.indexOf(needle);
        if (idx == -1) {
            return null;
        }
        int start = idx + needle.length();
        int end = json.indexOf('"', start);
        if (end == -1) {
            return null;
        }
        return json.substring(start, end).replace("\\/", "/");
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static String readAllText(InputStream in) throws IOException {
        return new String(readAllBytes(in), StandardCharsets.UTF_8).trim();
    }

    private static String runCommand(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(false).start();
        String output;
        try (InputStream in = process.getInputStream()) {
            output = readAllText(in);
        }
        process.waitFor();
        return output;
    }

    private static long parseLongSafe(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static double parseDoubleSafe(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static String[] splitFixed(String input, char separator, int count) {
        String[] result = new String[count];
        int start = 0;
        int partIndex = 0;
        for (int i = 0; i < input.length() && partIndex < count - 1; i++) {
            if (input.charAt(i) == separator) {
                result[partIndex++] = input.substring(start, i);
                start = i + 1;
            }
        }
        result[partIndex] = input.substring(start);
        for (int i = partIndex + 1; i < count; i++) {
            result[i] = "";
        }
        return result;
    }

    private static final class LinuxBackend implements Backend {
        private static final char SEP = '\u001F';

        @Override
        public Snapshot poll() throws Exception {
            String format = "{{artist}}" + SEP + "{{title}}" + SEP + "{{mpris:artUrl}}" + SEP + "{{mpris:length}}";
            String metadata = runCommand("playerctl", "metadata", "--format", format);

            if (metadata == null || metadata.isEmpty()) {
                return Snapshot.EMPTY;
            }

            String[] parts = splitFixed(metadata, SEP, 4);
            String artist = parts[0];
            String title = parts[1];
            String artUrl = parts[2];
            long millisLength = parseLongSafe(parts[3]) / 1000L;

            long millisPosition = 0L;
            String positionOutput = runCommand("playerctl", "position");
            if (positionOutput != null && !positionOutput.isEmpty()) {
                millisPosition = (long) (parseDoubleSafe(positionOutput) * 1000.0);
            }

            if (artUrl == null || artUrl.isEmpty()) {
                return new Snapshot(true, artist, title, millisLength, millisPosition, null, CoverKind.NONE);
            }
            CoverKind kind = artUrl.startsWith("http://") || artUrl.startsWith("https://")
                    ? CoverKind.URL
                    : CoverKind.FILE;
            return new Snapshot(true, artist, title, millisLength, millisPosition, artUrl, kind);
        }
    }

    private static final class WindowsBackend implements Backend {
        private static final long SESSION_INIT_TIMEOUT_MS = 10000L;
        private static final long QUERY_TIMEOUT_MS = 2000L;

        private static final String INIT_SCRIPT =
                "$ErrorActionPreference = 'SilentlyContinue'\n" +
                        "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8\n" +
                        "$global:mpiHasWinRT = $false\n" +
                        "try {\n" +
                        "  Add-Type -AssemblyName System.Runtime.WindowsRuntime -ErrorAction Stop\n" +
                        "  $null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime]\n" +
                        "  $global:mpiAsTask = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {\n" +
                        "    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and\n" +
                        "    $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'\n" +
                        "  })[0]\n" +
                        "  function MpiAwait($op, $type) {\n" +
                        "    $t = $global:mpiAsTask.MakeGenericMethod($type).Invoke($null, @($op))\n" +
                        "    $t.Wait(-1) | Out-Null\n" +
                        "    $t.Result\n" +
                        "  }\n" +
                        "  $global:mpiManager = MpiAwait ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])\n" +
                        "  if ($global:mpiManager) { $global:mpiHasWinRT = $true }\n" +
                        "} catch {}\n" +
                        "Write-Output 'MPI_READY'\n";

        private static final String QUERY_SCRIPT_TEMPLATE =
                "if ($global:mpiHasWinRT -and $global:mpiManager) {\n" +
                        "  $mpiSession = $global:mpiManager.GetCurrentSession()\n" +
                        "  if ($mpiSession) {\n" +
                        "    $mpiProps = MpiAwait ($mpiSession.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])\n" +
                        "    $mpiTimeline = $mpiSession.GetTimelineProperties()\n" +
                        "    $mpiArtist = ($mpiProps.Artist -replace \"`r|`n\", ' ')\n" +
                        "    $mpiTitle = ($mpiProps.Title -replace \"`r|`n\", ' ')\n" +
                        "    Write-Output ('TITLE=' + $mpiTitle)\n" +
                        "    Write-Output ('ARTIST=' + $mpiArtist)\n" +
                        "    Write-Output ('POSITION=' + [long]$mpiTimeline.Position.TotalMilliseconds)\n" +
                        "    Write-Output ('LENGTH=' + [long]($mpiTimeline.EndTime.TotalMilliseconds - $mpiTimeline.StartTime.TotalMilliseconds))\n" +
                        "  }\n" +
                        "}\n" +
                        "Write-Output '%s'\n";

        private final Object sessionLock = new Object();
        private final AtomicLong sentinelCounter = new AtomicLong();

        private Process psProcess;
        private BufferedWriter psWriter;
        private BlockingQueue<String> psOutput;
        private Thread psReaderThread;

        @Override
        public Snapshot poll() throws Exception {
            synchronized (sessionLock) {
                if (!sessionAlive() && !startSession()) {
                    closeSession();
                    throw new IOException("Couldn't start or initialize a PowerShell session for WinRT media queries");
                }

                String sentinel = "MPI_END_" + sentinelCounter.incrementAndGet();
                writeLine(String.format(QUERY_SCRIPT_TEMPLATE, sentinel));

                List<String> lines = readUntil(sentinel, QUERY_TIMEOUT_MS);
                if (lines == null) {
                    closeSession();
                    throw new IOException("PowerShell session for WinRT media queries stopped responding");
                }

                return parseReply(lines);
            }
        }

        @Override
        public void close() {
            synchronized (sessionLock) {
                closeSession();
            }
        }

        private Snapshot parseReply(List<String> lines) {
            String reply = String.join("\n", lines);
            String title = extractField(reply, "TITLE=");
            if (title == null || title.trim().isEmpty()) {
                return Snapshot.EMPTY;
            }
            String artist = extractField(reply, "ARTIST=");
            String position = extractField(reply, "POSITION=");
            String length = extractField(reply, "LENGTH=");

            return new Snapshot(true, artist == null ? "" : artist.trim(), title.trim(),
                    length == null ? 0L : parseLongSafe(length),
                    position == null ? 0L : parseLongSafe(position),
                    null, CoverKind.NONE);
        }

        private static String extractField(String reply, String prefix) {
            for (String line : reply.split("\n")) {
                if (line.startsWith(prefix)) {
                    return line.substring(prefix.length());
                }
            }
            return null;
        }

        private boolean sessionAlive() {
            return psProcess != null && psProcess.isAlive() && psWriter != null;
        }

        private boolean startSession() {
            closeSession();
            try {
                ProcessBuilder builder = new ProcessBuilder(
                        "powershell.exe", "-NoLogo", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", "-");
                builder.redirectErrorStream(true);
                psProcess = builder.start();
                psWriter = new BufferedWriter(new OutputStreamWriter(psProcess.getOutputStream(), StandardCharsets.UTF_8));

                psOutput = new LinkedBlockingQueue<>();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(psProcess.getInputStream(), StandardCharsets.UTF_8));
                psReaderThread = new Thread(() -> {
                    try {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            psOutput.offer(line);
                        }
                    } catch (IOException ignored) {
                    }
                }, "MediaPlayerInfo-PS-Reader");
                psReaderThread.setDaemon(true);
                psReaderThread.start();

                writeLine(INIT_SCRIPT);
                return readUntil("MPI_READY", SESSION_INIT_TIMEOUT_MS) != null;
            } catch (IOException e) {
                closeSession();
                return false;
            }
        }

        private void writeLine(String line) throws IOException {
            psWriter.write(line);
            psWriter.newLine();
            psWriter.flush();
        }

        private List<String> readUntil(String sentinel, long timeoutMs) {
            List<String> lines = new ArrayList<>();
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (true) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    return null;
                }
                String line;
                try {
                    line = psOutput.poll(remaining, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                if (line == null) {
                    return null;
                }
                if (line.contains(sentinel)) {
                    return lines;
                }
                lines.add(line);
            }
        }

        private void closeSession() {
            if (psProcess != null) {
                try {
                    if (psWriter != null) {
                        psWriter.close();
                    }
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

    private static final class MacBackend implements Backend {
        @Override
        public Snapshot poll() throws Exception {
            String textOut = runCommand("nowplaying-cli", "get", "artist", "title", "duration", "elapsedTime");
            if (textOut == null || textOut.trim().isEmpty()) {
                return Snapshot.EMPTY;
            }

            String[] lines = textOut.split("\\R", -1);
            String artist = lines.length > 0 ? cleanNil(lines[0]) : "";
            String title = lines.length > 1 ? cleanNil(lines[1]) : "";
            long millisLength = lines.length > 2 ? (long) (parseDoubleSafe(cleanNil(lines[2])) * 1000.0) : 0L;
            long millisPosition = lines.length > 3 ? (long) (parseDoubleSafe(cleanNil(lines[3])) * 1000.0) : 0L;

            if (title.isEmpty() && artist.isEmpty()) {
                return Snapshot.EMPTY;
            }

            String artworkBase64 = null;
            try {
                String art = runCommand("nowplaying-cli", "get", "artworkData");
                if (art != null && !art.trim().isEmpty() && !"(null)".equals(art.trim())) {
                    artworkBase64 = art.trim();
                }
            } catch (Exception ignored) {
            }

            CoverKind kind = artworkBase64 != null ? CoverKind.BASE64 : CoverKind.NONE;
            return new Snapshot(true, artist, title, millisLength, millisPosition, artworkBase64, kind);
        }

        private static String cleanNil(String s) {
            if (s == null) {
                return "";
            }
            String trimmed = s.trim();
            return "(null)".equals(trimmed) ? "" : trimmed;
        }
    }
}