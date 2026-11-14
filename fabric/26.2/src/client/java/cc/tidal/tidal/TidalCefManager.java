/*
 * tidal — proprietary source-viewing license
 * maintained by graphine studios
 * see LICENSE and CREDITS.md for licensing and attribution.
 *
 * provided here for inspection only.
 * this notice does not claim ownership of other contributors' material.
 * no general permission is granted by graphine studios to reuse, copy,
 * modify, redistribute or create derivative works from this file.
 */

package cc.tidal.tidal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.CefInitializationException;
import me.friwi.jcefmaven.EnumProgress;
import me.friwi.jcefmaven.UnsupportedPlatformException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefPaintEvent;
import org.cef.browser.CefMessageRouter;
import org.cef.browser.TidalOsrBrowser;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.cef.handler.CefRequestHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class TidalCefManager {
    private enum State { DORMANT, STARTING, READY, FAILED, STOPPING, TERMINATED }

    private record DirtyRegion(int x, int y, int width, int height) {}
    private static final Logger LOGGER = LoggerFactory.getLogger("Tidal CEF");
    private static final int MAX_FRAME_DIMENSION = 8192;
    private static final long MAX_FRAME_PIXELS = 16_777_216L;
    private static final int MAX_QUERY_LENGTH = 32_768;
    private static final int MAX_DIRTY_REGIONS = 64;
    private static final int TARGET_FRAME_RATE = 60;
    private static final long STATE_PUSH_INTERVAL_NANOS = 250_000_000L;
    private static final Path CEF_ROOT =
            FabricLoader.getInstance().getConfigDir().resolve("tidal").resolve("cef");
    private static final Path FRONTEND_DIR = CEF_ROOT.resolve("frontend");
    private static final Object FRAME_LOCK = new Object();
    private static final List<DirtyRegion> DIRTY_REGIONS = new ArrayList<>();
    private static final AtomicBoolean FIRST_PAINT = new AtomicBoolean();
    private static final AtomicBoolean FIRST_PAINT_CALLBACK = new AtomicBoolean();
    private static final AtomicBoolean FIRST_PAINT_SIZE_MISMATCH = new AtomicBoolean();
    private static final AtomicBoolean FRAME_DISPLAYED = new AtomicBoolean();
    private static volatile State state = State.DORMANT;
    private static volatile String detail = "CEF is idle; it starts only when Tidal is opened.";
    private static volatile float progress;
    private static volatile boolean terminal;
    private static volatile boolean disableRequested;
    private static volatile boolean gameStopping;
    private static volatile boolean visible;
    private static volatile CefApp cefApp;
    private static volatile CefClient cefClient;
    private static volatile TidalOsrBrowser browser;
    private static volatile CefMessageRouter messageRouter;
    private static volatile int frameWidth;
    private static volatile int frameHeight;
    private static volatile int requestedViewWidth;
    private static volatile int requestedViewHeight;
    private static volatile int requestedFrameWidth;
    private static volatile int requestedFrameHeight;
    private static byte[] framePixels = new byte[0];
    private static ByteBuffer uploadBuffer = ByteBuffer.allocateDirect(4);
    private static boolean fullFrameDirty;
    private static long dirtyPixelCount;
    private static boolean pendingNonEmptyFrame;
    private static ExecutorService cefWorker;
    private static String page = "HUD";
    private static long lastStatePushNanos;
    private static String lastPublishedSnapshot;
    private static volatile long startupNanos;
    private static volatile long shownNanos;
    private static volatile double requestedDeviceScale = 1.0;
    private static volatile long lastDisplayRefreshAttemptNanos;
    private static volatile boolean frameDisplayTimedOut;
    private static volatile CountDownLatch frontendLoadLatch = new CountDownLatch(1);
    private static volatile boolean frontendLoadFailed;

    private TidalCefManager() {}

    static synchronized boolean activate(int logicalWidth, int logicalHeight,
                                         double scaleX, double scaleY) {
        if (!TidalConfig.useCefUi) {
            detail = "CEF is disabled; the native renderer is active.";
            return false;
        }
        try {
            rememberRequestedSize(logicalWidth, logicalHeight, scaleX, scaleY);
        } catch (IOException exception) {
            failBeforeStart(exception.getMessage());
            return false;
        }
        if (state == State.READY) {
            resize(logicalWidth, logicalHeight, scaleX, scaleY);
            if (state != State.READY) return false;
            setVisible(true);
            return state == State.READY;
        }
        if (state == State.STARTING) {
            resize(logicalWidth, logicalHeight, scaleX, scaleY);
            setVisible(true);
            return state == State.STARTING;
        }
        if (state == State.STOPPING) return false;
        if (terminal || state == State.TERMINATED || state == State.STOPPING) {
            state = State.TERMINATED;
            detail = "CEF was shut down and cannot be restarted until Minecraft restarts.";
            return false;
        }
        if (ModuleLayer.boot().findModule("java.desktop").isEmpty()) {
            failBeforeStart("The Java runtime does not include java.desktop; CEF OSR is unavailable.");
            return false;
        }
        String osName = System.getProperty("os.name", "");
        String os = osName.toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        LOGGER.info("CEF OSR preflight: java.desktop present; java.awt.headless property={}, "
                        + "platform={}/{}; windowless rendering enabled.",
                System.getProperty("java.awt.headless", "<unset>"), osName, arch);
        if (os.contains("win") && (arch.equals("aarch64") || arch.equals("arm64"))) {
            failBeforeStart("JCEF windowless rendering is unsupported on Windows ARM64.");
            return false;
        }
        List<String> missingJvmOptions = missingMacJvmOptions(os);
        if (!missingJvmOptions.isEmpty()) {
            failBeforeStart("CEF OSR requires Minecraft JVM options: "
                    + String.join(", ", missingJvmOptions));
            return false;
        }
        disableRequested = false;
        gameStopping = false;
        beginDisplaySession();
        state = State.STARTING;
        detail = "Preparing the local CEF runtime.";
        progress = 0;
        startupNanos = System.nanoTime();
        ensureWorker().execute(TidalCefManager::initialize);
        return true;
    }

    static synchronized void prewarm(int logicalWidth, int logicalHeight,
                                     double scaleX, double scaleY) {
        if (!TidalConfig.useCefUi || state != State.DORMANT) return;
        if (activate(logicalWidth, logicalHeight, scaleX, scaleY)) setVisible(false);
    }

    private static synchronized ExecutorService ensureWorker() {
        if (cefWorker == null || cefWorker.isShutdown()) {
            cefWorker = Executors.newSingleThreadExecutor(task -> {
                Thread thread = new Thread(task, "Tidal CEF Runtime");
                thread.setDaemon(true);
                return thread;
            });
        }
        return cefWorker;
    }

    private static void failBeforeStart(String reason) {
        state = State.FAILED;
        detail = reason;
        progress = 0;
        LOGGER.warn(reason);
    }

    private static void beginDisplaySession() {
        visible = true;
        shownNanos = System.nanoTime();
        FRAME_DISPLAYED.set(false);
        frameDisplayTimedOut = false;
        lastDisplayRefreshAttemptNanos = 0;
        synchronized (FRAME_LOCK) {
            DIRTY_REGIONS.clear();
            dirtyPixelCount = 0;
            if (frameWidth > 0 && frameHeight > 0 && FIRST_PAINT.get()) {
                fullFrameDirty = true;
            }
        }
    }

    static void failForNoDisplayedFrame() {
        if (FRAME_DISPLAYED.get() || frameDisplayTimedOut) return;
        frameDisplayTimedOut = true;
        visible = false;
        frontendLoadLatch.countDown();
        TidalOsrBrowser currentBrowser = browser;
        if (currentBrowser != null) {
            requestBrowserThrottle(currentBrowser, "native-fallback");
        }
        state = State.FAILED;
        detail = "No non-empty CEF frame within 5s after refresh.";
        progress = 0;
        LOGGER.error("{}; resize, visibility, and repaint were attempted; "
                + "switched to the native interface.", detail);
    }

    static void failForRenderException(Throwable failure) {
        frameDisplayTimedOut = true;
        visible = false;
        frontendLoadLatch.countDown();
        TidalOsrBrowser currentBrowser = browser;
        if (currentBrowser != null) {
            requestBrowserThrottle(currentBrowser, "render-exception-fallback");
        }
        state = State.FAILED;
        detail = "CEF rendering failed; switched to the native interface: "
                + conciseMessage(failure);
        progress = 0;
    }

    private static List<String> missingMacJvmOptions(String os) {
        List<String> required = new ArrayList<>();
        if (os.contains("mac")) {
            required.add("--add-opens=java.desktop/sun.awt=ALL-UNNAMED");
            required.add("--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED");
            required.add("--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED");
        }
        List<String> supplied = ManagementFactory.getRuntimeMXBean().getInputArguments();
        return required.stream().filter(option -> !containsJvmOption(supplied, option)).toList();
    }

    private static boolean containsJvmOption(List<String> supplied, String option) {
        if (supplied.contains(option)) return true;
        int separator = option.indexOf('=');
        if (separator < 0) return false;
        String switchName = option.substring(0, separator);
        String switchValue = option.substring(separator + 1);
        for (int index = 0; index + 1 < supplied.size(); index++) {
            if (supplied.get(index).equals(switchName)
                    && supplied.get(index + 1).equals(switchValue)) return true;
        }
        return false;
    }

    private static void initialize() {
        armCefInitializationWatchdog();
        boolean appCreated = false;
        try {
            Path frontend = extractFrontend();
            if (disableRequested || gameStopping) {
                state = TidalConfig.useCefUi && !gameStopping ? State.FAILED : State.DORMANT;
                if (state == State.DORMANT) {
                    detail = "CEF is disabled; the native renderer is active.";
                }
                progress = 0;
                return;
            }
            Files.createDirectories(CEF_ROOT.resolve("runtime"));
            Files.createDirectories(CEF_ROOT.resolve("cache"));

            CefAppBuilder builder = new CefAppBuilder();
            builder.setInstallDir(CEF_ROOT.resolve("runtime").toFile());
            builder.setProgressHandler((progressState, value) -> updateProgress(progressState, value));
            CefSettings cefSettings = builder.getCefSettings();
            cefSettings.windowless_rendering_enabled = true;
            cefSettings.background_color = cefSettings.new ColorType(0, 0, 0, 0);
            cefSettings.cache_path = CEF_ROOT.resolve("cache").toString();
            cefSettings.root_cache_path = CEF_ROOT.resolve("cache").toString();
            CefApp createdApp = builder.build();
            cefApp = createdApp;
            appCreated = true;
            if (disableRequested || gameStopping) {
                shutdownRuntime(true);
                return;
            }

            CefClient createdClient = createdApp.createClient();
            cefClient = createdClient;
            CefMessageRouter router = CefMessageRouter.create(new BridgeHandler());
            messageRouter = router;
            createdClient.addMessageRouter(router);
            createdClient.addDisplayHandler(new CefDisplayHandlerAdapter() {
                @Override
                public boolean onConsoleMessage(CefBrowser source, CefSettings.LogSeverity severity,
                                                String message, String sourceUrl, int line) {
                    LOGGER.info("CEF console {} at {}:{}: {}",
                            severity, sourceUrl, line, message);
                    return true;
                }
            });
            Path safeFrontend = frontend.toAbsolutePath().normalize();
            String frontendUrl = safeFrontend.resolve("index.html").toUri().toString();
            createdClient.addRequestHandler(new CefRequestHandlerAdapter() {
                @Override
                public boolean onBeforeBrowse(CefBrowser ignored, org.cef.browser.CefFrame frame,
                                              org.cef.network.CefRequest request,
                                              boolean userGesture, boolean isRedirect) {
                    try {
                        URI uri = URI.create(request.getURL());
                        if (!"file".equalsIgnoreCase(uri.getScheme())) return true;
                        Path requested = Path.of(uri).toAbsolutePath().normalize();
                        boolean blocked = !requested.startsWith(safeFrontend);
                        if (frame.isMain()) {
                            LOGGER.info("CEF main-frame navigation {}: {}",
                                    blocked ? "blocked" : "allowed", request.getURL());
                        }
                        return blocked;
                    } catch (IllegalArgumentException exception) {
                        LOGGER.warn("CEF rejected an invalid navigation URL.", exception);
                        return true;
                    }
                }
            });
            createdClient.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
                @Override
                public boolean onBeforePopup(CefBrowser browser, org.cef.browser.CefFrame frame,
                                              String targetUrl, String targetFrameName) {
                    LOGGER.info("CEF onBeforePopup at +{} ms: targetUrl={}, targetFrameName={}",
                            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startupNanos),
                            targetUrl, targetFrameName);
                    return true;
                }

                @Override
                public void onAfterCreated(CefBrowser createdBrowser) {
                    LOGGER.info("CEF onAfterCreated at +{} ms; browser URL={}.",
                            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startupNanos),
                            createdBrowser.getURL());
                    if (createdBrowser instanceof TidalOsrBrowser osrBrowser) {
                        requestBrowserRefresh(osrBrowser, requestedViewWidth,
                                requestedViewHeight, requestedDeviceScale, "onAfterCreated");
                    } else {
                        LOGGER.error("CEF created an unexpected browser type {}; "
                                + "could not request an off-screen refresh.",
                                createdBrowser.getClass().getName());
                    }
                }

                @Override
                public void onBeforeClose(CefBrowser closingBrowser) {
                    LOGGER.info("CEF onBeforeClose at +{} ms.",
                            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startupNanos));
                }
            });
            createdClient.addLoadHandler(new CefLoadHandlerAdapter() {
                @Override
                public void onLoadStart(CefBrowser loadingBrowser,
                                        org.cef.browser.CefFrame frame,
                                        org.cef.network.CefRequest.TransitionType transitionType) {
                    if (frame.isMain()) {
                        LOGGER.info("CEF onLoadStart at +{} ms; URL={}",
                                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startupNanos),
                                loadingBrowser.getURL());
                    }
                }

                @Override
                public void onLoadEnd(CefBrowser loadedBrowser, org.cef.browser.CefFrame frame,
                                      int httpStatusCode) {
                    if (frame.isMain()) {
                        frontendLoadFailed = httpStatusCode >= 400;
                        LOGGER.info("CEF onLoadEnd at +{} ms; status={}; URL={}",
                                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startupNanos),
                                httpStatusCode,
                                loadedBrowser.getURL());
                        frontendLoadLatch.countDown();
                        if (!frontendLoadFailed) {
                            TidalOsrBrowser currentBrowser = browser;
                            if (currentBrowser != null) {
                                requestBrowserRefresh(currentBrowser, requestedViewWidth,
                                        requestedViewHeight, requestedDeviceScale,
                                        "main-frame-load-end");
                            }
                        }
                    }
                }

                @Override
                public void onLoadError(CefBrowser failedBrowser, org.cef.browser.CefFrame frame,
                                        org.cef.handler.CefLoadHandler.ErrorCode errorCode,
                                        String errorText, String failedUrl) {
                    LOGGER.error("CEF onLoadError at +{} ms; errorCode={}; errorText={}; URL={}",
                            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startupNanos),
                            errorCode, errorText, failedUrl);
                    if (frame.isMain()) {
                        frontendLoadFailed = true;
                        frontendLoadLatch.countDown();
                    }
                }
            });
            frontendLoadFailed = false;
            frontendLoadLatch = new CountDownLatch(1);
            FIRST_PAINT_CALLBACK.set(false);
            FIRST_PAINT_SIZE_MISMATCH.set(false);
            int pixelWidth = requestedFrameWidth;
            int pixelHeight = requestedFrameHeight;
            ensureSupportedFrameSize(pixelWidth, pixelHeight);
            synchronized (FRAME_LOCK) {
                resetFrameBuffer(pixelWidth, pixelHeight);
                FIRST_PAINT.set(false);
            }
            TidalOsrBrowser createdBrowser = new TidalOsrBrowser(createdClient, frontendUrl,
                    null, requestedViewWidth, requestedViewHeight, requestedDeviceScale,
                    TidalCefManager::acceptOsrPaint);
            browser = createdBrowser;
            LOGGER.info("CEF custom OSR createBrowser ENTRY at +{} ms; thread={}; URL={}",
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startupNanos),
                    Thread.currentThread().getName(), frontendUrl);
            createdBrowser.createImmediately();
            LOGGER.info("CEF custom OSR createBrowser RETURN at +{} ms; thread={}; URL={}",
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startupNanos),
                    Thread.currentThread().getName(), createdBrowser.getURL());
            createdBrowser.setWindowlessFrameRate(TARGET_FRAME_RATE);
            LOGGER.info("CEF custom OSR browser created at +{} ms; thread={}; URL={}",
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startupNanos),
                    Thread.currentThread().getName(), createdBrowser.getURL());
            applyRequestedSize();

            detail = "Waiting for the frontend to load.";
            progress = 0.95f;
            if (!frontendLoadLatch.await(30, TimeUnit.SECONDS)) {
                throw new IOException("CEF frontend did not finish loading within 30 seconds (browser URL: "
                        + createdBrowser.getURL() + ").");
            }
            if (frontendLoadFailed) {
                throw new IOException("CEF failed to load its local frontend (browser URL: "
                        + createdBrowser.getURL() + ").");
            }
            if (frameDisplayTimedOut) return;
            if (disableRequested || gameStopping) {
                shutdownRuntime(true);
                return;
            }
            state = State.READY;
            createdBrowser.setWindowlessFrameRate(visible ? TARGET_FRAME_RATE : 1);
            if (!visible) requestBrowserThrottle(createdBrowser, "prewarm-complete");
            detail = visible ? "CEF frontend loaded; waiting for a visible frame."
                    : "CEF frontend loaded; the native renderer is shown until opened.";
            progress = 1;
            lastPublishedSnapshot = null;
            LOGGER.info("CEF frontend initialized from {}; first frame will be required "
                    + "when the browser is shown.", frontend);
        } catch (IOException | UnsupportedPlatformException | InterruptedException
                 | CefInitializationException | RuntimeException | LinkageError exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            if (frameDisplayTimedOut) return;
            if (disableRequested || gameStopping) {
                if (cefApp != null) {
                    terminal = true;
                    shutdownRuntime(true);
                } else {
                    state = State.DORMANT;
                    detail = "CEF is disabled; the native renderer is active.";
                    progress = 0;
                }
                return;
            }
            String message = conciseMessage(exception);
            LOGGER.error("CEF initialization failed; retaining the native renderer: {}", message,
                    exception);
            detail = "CEF unavailable: " + message;
            progress = 0;
            if (appCreated || cefApp != null) {
                terminal = true;
                shutdownRuntime(false);
                detail = "CEF startup failed: " + message;
            } else {
                state = State.FAILED;
            }
        }
    }

    private static String conciseMessage(Throwable exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) message = exception.getClass().getSimpleName();
        return message.length() > 200 ? message.substring(0, 197) + "..." : message;
    }

    private static void armCefInitializationWatchdog() {
        Thread watchdog = new Thread(() -> {
            long previous = 0;
            for (long seconds : new long[]{5, 15, 40}) {
                try {
                    TimeUnit.SECONDS.sleep(seconds - previous);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
                previous = seconds;
                dumpCefRelevantThreads("CEF startup watchdog +" + seconds + "s");
            }
        }, "Tidal CEF Startup Watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        LOGGER.info("CEF startup watchdog armed at +{} ms; thread={}",
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startupNanos), watchdog.getName());
    }

    private static void dumpCefRelevantThreads(String label) {
        LOGGER.warn("{}: ThreadMXBean full-stack snapshot follows.", label);
        for (var thread : ManagementFactory.getThreadMXBean().dumpAllThreads(true, true)) {
            String name = thread.getThreadName();
            boolean selected = name.equals("Tidal CEF Runtime") || name.equals("AWT-EventQueue-0")
                    || name.equals("Render thread");
            for (StackTraceElement frame : thread.getStackTrace()) {
                String frameText = frame.toString().toLowerCase(Locale.ROOT);
                if (frameText.contains("cefbrowser")
                        || frameText.contains("createbrowser")) selected = true;
            }
            if (!selected) continue;
            LOGGER.warn("{} THREAD name={} id={} state={} lock={} owner={} ownerId={} blocked={} waited={} native={}",
                    label, name, thread.getThreadId(), thread.getThreadState(), thread.getLockName(),
                    thread.getLockOwnerName(), thread.getLockOwnerId(), thread.getBlockedTime(),
                    thread.getWaitedTime(), thread.isInNative());
            for (StackTraceElement frame : thread.getStackTrace()) {
                LOGGER.warn("{}   at {}", label, frame);
            }
        }
    }

    private static int pixelSize(int logicalSize, double scale) {
        return Math.max(1, (int) Math.round(logicalSize * Math.max(1, scale)));
    }

    private static void updateProgress(EnumProgress progressState, float value) {
        String phase = switch (progressState) {
            case LOCATING -> "Checking the local CEF runtime.";
            case DOWNLOADING -> "Downloading the platform CEF runtime.";
            case EXTRACTING -> "Extracting the CEF runtime.";
            case INSTALL -> "Installing the CEF runtime.";
            case INITIALIZING -> "Starting the embedded browser.";
            case INITIALIZED -> "Browser initialized; preparing the UI.";
        };
        detail = phase;
        progress = Math.max(0, Math.min(1, value > 1 ? value / 100 : value));
    }

    private static Path extractFrontend() throws IOException {
        List<String> files = new ArrayList<>();
        files.add("index.html");
        files.add(".vite/manifest.json");
        try (InputStream input = TidalCefManager.class.getResourceAsStream(
                "/assets/tidal/cef/.vite/manifest.json")) {
            if (input == null) throw new IOException("Packaged CEF frontend manifest is missing.");
            JsonObject manifest = JsonParser.parseReader(
                    new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject();
            collectFrontendFiles(manifest, files);
        } catch (RuntimeException exception) {
            throw new IOException("Packaged CEF frontend manifest is invalid.", exception);
        }
        Path root = FRONTEND_DIR.toAbsolutePath().normalize();
        Files.createDirectories(CEF_ROOT);
        if (Files.isSymbolicLink(CEF_ROOT) || Files.isSymbolicLink(root)) {
            throw new IOException("CEF frontend cache cannot be a symbolic link.");
        }
        Files.createDirectories(root);
        for (String file : files.stream().distinct().toList()) {
            if (!isSafeRelativePath(file)) {
                throw new IOException("Unsafe path in packaged CEF manifest.");
            }
            Path target = root.resolve(file).normalize();
            if (!target.startsWith(root)) {
                throw new IOException("CEF frontend path escaped its cache directory.");
            }
            verifyNoSymlinkTraversal(root, target);
            String resource = "/assets/tidal/cef/" + file.replace('\\', '/');
            Path parent = target.getParent();
            if (parent != null) Files.createDirectories(parent);
            try (InputStream source = TidalCefManager.class.getResourceAsStream(resource)) {
                if (source == null) throw new IOException("Packaged CEF asset is missing: " + file);
                Path temporary = Files.createTempFile(parent, "tidal-cef-", ".tmp");
                try {
                    Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
                    try {
                        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException exception) {
                        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException exception) {
                    Files.deleteIfExists(temporary);
                    throw exception;
                }
            }
        }
        return root;
    }

    private static void verifyNoSymlinkTraversal(Path root, Path target) throws IOException {
        Path current = root;
        if (Files.isSymbolicLink(current)) {
            throw new IOException("CEF frontend cache contains an unsafe symbolic link.");
        }
        for (Path segment : root.relativize(target)) {
            current = current.resolve(segment);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)
                    && Files.isSymbolicLink(current)) {
                throw new IOException("CEF frontend cache contains an unsafe symbolic link.");
            }
        }
    }

    private static void collectFrontendFiles(JsonElement element, List<String> files) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (var entry : object.entrySet()) {
                if (entry.getKey().equals("file") && entry.getValue().isJsonPrimitive()) {
                    files.add(entry.getValue().getAsString());
                } else if (entry.getKey().equals("css") || entry.getKey().equals("assets")) {
                    collectFrontendFiles(entry.getValue(), files);
                } else if (entry.getValue().isJsonObject()) {
                    collectFrontendFiles(entry.getValue(), files);
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) collectFrontendFiles(child, files);
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            files.add(element.getAsString());
        }
    }

    private static boolean isSafeRelativePath(String value) {
        if (value == null || value.isBlank() || value.indexOf('\\') >= 0) return false;
        try {
            Path path = Path.of(value).normalize();
            return !path.isAbsolute() && !path.startsWith("..")
                    && !path.toString().isBlank() && !path.toString().equals(".");
        } catch (RuntimeException exception) {
            return false;
        }
    }

    static void acceptOsrPaint(CefPaintEvent event) {
        boolean popup = event.getPopup();
        ByteBuffer source = event.getRenderedFrame();
        int width = event.getWidth();
        int height = event.getHeight();
        if (FIRST_PAINT_CALLBACK.compareAndSet(false, true)) {
            LOGGER.info("CEF first native onPaint callback: popup={}, frame={}x{}, expected={}x{}, "
                            + "bufferBytes={}, thread={}.",
                    popup, width, height, requestedFrameWidth, requestedFrameHeight,
                    source == null ? -1 : source.remaining(), Thread.currentThread().getName());
        }
        long byteCount = (long) width * height * 4;
        if (source == null || width <= 0 || height <= 0
                || width > MAX_FRAME_DIMENSION || height > MAX_FRAME_DIMENSION
                || (long) width * height > MAX_FRAME_PIXELS
                || byteCount > source.remaining()) {
            LOGGER.warn("Ignoring invalid CEF frame dimensions {}x{}.", width, height);
            return;
        }
        if (popup) return;
        if (width != requestedFrameWidth || height != requestedFrameHeight) {
            if (FIRST_PAINT_SIZE_MISMATCH.compareAndSet(false, true)) {
                LOGGER.warn("Ignoring CEF frame {}x{}; requested size is {}x{}.",
                        width, height, requestedFrameWidth, requestedFrameHeight);
            }
            return;
        }
        synchronized (FRAME_LOCK) {
            if (width != frameWidth || height != frameHeight) {
                resetFrameBuffer(width, height);
            }
            int sourceOffset = source.position();
            Rectangle[] dirtyRects = event.getDirtyRects();
            if (dirtyRects.length == 0 && !FIRST_PAINT.get()) {
                dirtyRects = new Rectangle[]{new Rectangle(0, 0, width, height)};
            }
            for (Rectangle rectangle : dirtyRects) {
                int left = Math.max(0, rectangle.x);
                int top = Math.max(0, rectangle.y);
                int right = Math.min(width, rectangle.x + rectangle.width);
                int bottom = Math.min(height, rectangle.y + rectangle.height);
                if (right <= left || bottom <= top) continue;
                for (int y = top; y < bottom; y++) {
                    int rowOffset = (y * width + left) * 4;
                    int inputOffset = sourceOffset + rowOffset;
                    for (int x = left; x < right; x++) {
                        int target = rowOffset + (x - left) * 4;
                        int blue = source.get(inputOffset) & 0xFF;
                        int green = source.get(inputOffset + 1) & 0xFF;
                        int red = source.get(inputOffset + 2) & 0xFF;
                        int alpha = source.get(inputOffset + 3) & 0xFF;
                        if (alpha > 0 && alpha < 255) {
                            red = Math.min(255, red * 255 / alpha);
                            green = Math.min(255, green * 255 / alpha);
                            blue = Math.min(255, blue * 255 / alpha);
                        }
                        framePixels[target] = (byte) red;
                        framePixels[target + 1] = (byte) green;
                        framePixels[target + 2] = (byte) blue;
                        framePixels[target + 3] = (byte) alpha;
                        pendingNonEmptyFrame |= alpha > 0;
                        inputOffset += 4;
                    }
                }
                long regionPixels = (long) (right - left) * (bottom - top);
                if (DIRTY_REGIONS.size() >= MAX_DIRTY_REGIONS
                        || dirtyPixelCount + regionPixels > (long) width * height) {
                    DIRTY_REGIONS.clear();
                    dirtyPixelCount = 0;
                    fullFrameDirty = true;
                    break;
                }
                DIRTY_REGIONS.add(new DirtyRegion(left, top, right - left, bottom - top));
                dirtyPixelCount += regionPixels;
            }
            if (DIRTY_REGIONS.isEmpty() && !fullFrameDirty) {
                DIRTY_REGIONS.add(new DirtyRegion(0, 0, width, height));
                fullFrameDirty = true;
            }
            if (FIRST_PAINT.compareAndSet(false, true)) {
                LOGGER.info("CEF first onPaint at +{} ms; frame={}x{}.",
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startupNanos),
                        width, height);
            }
        }
    }

    private static void resetFrameBuffer(int width, int height) {
        frameWidth = width;
        frameHeight = height;
        framePixels = new byte[width * height * 4];
        uploadBuffer = ByteBuffer.allocateDirect(Math.max(4, width * height * 4));
        DIRTY_REGIONS.clear();
        fullFrameDirty = true;
        dirtyPixelCount = 0;
        pendingNonEmptyFrame = false;
        FIRST_PAINT.set(false);
        FRAME_DISPLAYED.set(false);
    }

    private static void rememberRequestedSize(int logicalWidth, int logicalHeight,
                                               double scaleX, double scaleY) throws IOException {
        int pixelWidth = pixelSize(logicalWidth, scaleX);
        int pixelHeight = pixelSize(logicalHeight, scaleY);
        ensureSupportedFrameSize(pixelWidth, pixelHeight);
        requestedViewWidth = Math.max(1, logicalWidth);
        requestedViewHeight = Math.max(1, logicalHeight);
        requestedFrameWidth = pixelWidth;
        requestedFrameHeight = pixelHeight;
        requestedDeviceScale = Math.max(1.0, Math.max(scaleX, scaleY));
    }

    static synchronized void resize(int logicalWidth, int logicalHeight,
                                    double scaleX, double scaleY) {
        try {
            rememberRequestedSize(logicalWidth, logicalHeight, scaleX, scaleY);
        } catch (IOException exception) {
            boolean wasStarting = state == State.STARTING;
            detail = exception.getMessage();
            state = State.FAILED;
            if (wasStarting) disableRequested = true;
            if (cefApp != null) {
                terminal = true;
                ensureWorker().execute(() -> shutdownRuntime(false));
            }
            return;
        }
        applyRequestedSize();
    }

    private static synchronized void applyRequestedSize() {
        int viewWidth = requestedViewWidth;
        int viewHeight = requestedViewHeight;
        int pixelWidth = requestedFrameWidth;
        int pixelHeight = requestedFrameHeight;
        double deviceScale = requestedDeviceScale;
        TidalOsrBrowser currentBrowser = browser;
        if (currentBrowser == null) return;
        EventQueue.invokeLater(() -> {
            try {
                currentBrowser.resize(viewWidth, viewHeight, deviceScale);
                currentBrowser.requestRepaint();
                currentBrowser.setWindowlessFrameRate(TARGET_FRAME_RATE);
                LOGGER.info("CEF resize at +{} ms: view={}x{}, scale={}, frame={}x{}; "
                                + "repaint=invalidate; retaining last uploaded frame until resized paint.",
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startupNanos),
                        viewWidth, viewHeight, deviceScale, pixelWidth, pixelHeight);
            } catch (RuntimeException | LinkageError failure) {
                LOGGER.error("CEF resize/repaint failed for {}x{}.", pixelWidth, pixelHeight,
                        failure);
            }
        });
    }

    static void refreshForScreenOpen(int logicalWidth, int logicalHeight,
                                     double scaleX, double scaleY) {
        try {
            rememberRequestedSize(logicalWidth, logicalHeight, scaleX, scaleY);
        } catch (IOException exception) {
            failBeforeStart(exception.getMessage());
            return;
        }
        if (!visible) beginDisplaySession();
        visible = true;
        if (state == State.READY) detail = "CEF is rendering inside Minecraft.";
        executeScript("window.dispatchEvent(new Event('tidal:shown'));");
        TidalOsrBrowser currentBrowser = browser;
        if (currentBrowser == null) {
            LOGGER.info("CEF screen-open refresh deferred at +{} ms: browser not created; "
                            + "onAfterCreated will retry at view={}x{}, scale={}, frame={}x{}.",
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startupNanos),
                    requestedViewWidth, requestedViewHeight, requestedDeviceScale,
                    requestedFrameWidth, requestedFrameHeight);
            return;
        }
        requestBrowserRefresh(currentBrowser, requestedViewWidth, requestedViewHeight,
                requestedDeviceScale, "screen-open");
    }

    private static void requestBrowserRefresh(TidalOsrBrowser currentBrowser, int width,
                                             int height, double deviceScale, String reason) {
        EventQueue.invokeLater(() -> {
            long attemptNanos = System.nanoTime();
            lastDisplayRefreshAttemptNanos = attemptNanos;
            try {
                currentBrowser.setWindowVisibility(true);
                currentBrowser.setFocus(true);
                currentBrowser.resize(width, height, deviceScale);
                currentBrowser.requestRepaint();
                currentBrowser.setWindowlessFrameRate(TARGET_FRAME_RATE);
                LOGGER.info("CEF browser refresh at +{} ms: reason={}, visible=true, focus=true, "
                                + "view={}x{}, scale={}, repaint=invalidate.",
                        TimeUnit.NANOSECONDS.toMillis(attemptNanos - startupNanos),
                        reason, width, height, deviceScale);
            } catch (RuntimeException | LinkageError failure) {
                LOGGER.error("CEF browser refresh failed: reason={}, size={}x{}.",
                        reason, width, height, failure);
            }
        });
    }

    private static void requestBrowserThrottle(CefBrowser currentBrowser, String reason) {
        EventQueue.invokeLater(() -> {
            try {
                currentBrowser.setFocus(false);
                currentBrowser.setWindowlessFrameRate(1);
                LOGGER.info("CEF browser throttled at +{} ms: reason={}; visibility remains enabled.",
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startupNanos),
                        reason);
            } catch (RuntimeException | LinkageError failure) {
                LOGGER.error("CEF browser throttling failed: reason={}.", reason, failure);
            }
        });
    }

    static boolean hasDisplayRefreshAttemptSince(long openedAtNanos) {
        return lastDisplayRefreshAttemptNanos >= openedAtNanos;
    }

    static void setVisible(boolean visible) {
        boolean wasVisible = TidalCefManager.visible;
        if (visible && !wasVisible) beginDisplaySession();
        TidalCefManager.visible = visible;
        if (visible) {
            executeScript("window.dispatchEvent(new Event('tidal:shown'));");
            if (!FIRST_PAINT.get()) {
                CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS).execute(() -> {
                    if (TidalCefManager.visible && state == State.READY && !FIRST_PAINT.get()) {
                        state = State.FAILED;
                        detail = "CEF loaded the frontend but produced no off-screen frame within 30 seconds.";
                        progress = 0;
                        LOGGER.error(detail);
                    }
                });
            }
        }
        TidalOsrBrowser currentBrowser = browser;
        if (state == State.READY) {
            detail = visible ? "CEF is rendering inside Minecraft."
                    : "CEF is initialized but hidden; the native renderer is shown.";
        }
        if (!visible && wasVisible && currentBrowser != null) {
            requestBrowserThrottle(currentBrowser, "screen-close");
        }
    }

    static boolean hasFrame() {
        return FRAME_DISPLAYED.get();
    }

    static boolean isReady() {
        return state == State.READY;
    }

    static boolean hasPaint() {
        return FIRST_PAINT.get();
    }

    private static void ensureSupportedFrameSize(int width, int height) throws IOException {
        if (width <= 0 || height <= 0 || width > MAX_FRAME_DIMENSION
                || height > MAX_FRAME_DIMENSION || (long) width * height > MAX_FRAME_PIXELS) {
            throw new IOException("CEF window size exceeds the safe off-screen rendering limit.");
        }
    }

    static int frameWidth() {
        synchronized (FRAME_LOCK) {
            return frameWidth;
        }
    }

    static int frameHeight() {
        synchronized (FRAME_LOCK) {
            return frameHeight;
        }
    }

    static boolean uploadDirtyFrame(DynamicTexture texture) {
        if (texture == null || texture.getTexture() == null) return false;
        synchronized (FRAME_LOCK) {
            if (!fullFrameDirty && DIRTY_REGIONS.isEmpty()) return false;
            if (texture.getPixels().getWidth() != frameWidth
                    || texture.getPixels().getHeight() != frameHeight) return false;
            if (fullFrameDirty) {
                DIRTY_REGIONS.clear();
                DIRTY_REGIONS.add(new DirtyRegion(0, 0, frameWidth, frameHeight));
            }
            var encoder = com.mojang.blaze3d.systems.RenderSystem.getDevice()
                    .createCommandEncoder();
            for (DirtyRegion dirty : DIRTY_REGIONS) {
                uploadBuffer.clear();
                int rowLength = dirty.width * 4;
                for (int y = dirty.y; y < dirty.y + dirty.height; y++) {
                    uploadBuffer.put(framePixels, (y * frameWidth + dirty.x) * 4, rowLength);
                }
                uploadBuffer.flip();
                encoder.writeToTexture(texture.getTexture(), uploadBuffer, 0, 0,
                        dirty.x, dirty.y, dirty.width, dirty.height);
            }
            DIRTY_REGIONS.clear();
            dirtyPixelCount = 0;
            fullFrameDirty = false;
            if (visible && pendingNonEmptyFrame && FRAME_DISPLAYED.compareAndSet(false, true)) {
                LOGGER.info("First non-empty CEF frame uploaded for display {} ms after open; "
                                + "frame={}x{}; BGRA swizzled to straight-alpha RGBA off render thread.",
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - shownNanos),
                        frameWidth, frameHeight);
            }
            return true;
        }
    }

    static String statusSummary() {
        return switch (state) {
            case DORMANT -> TidalConfig.useCefUi
                    ? "CEF idle; it starts when opened."
                    : "CEF disabled; native renderer active.";
            case STARTING -> "CEF starting.";
            case READY -> visible ? "CEF active." : "CEF initialized but hidden.";
            case FAILED -> "CEF not ready: " + detail;
            case STOPPING -> "CEF stopping.";
            case TERMINATED -> "Restart required; CEF cannot restart this session.";
        };
    }

    static JsonObject statusJson() {
        JsonObject status = new JsonObject();
        status.addProperty("state", switch (state) {
            case DORMANT -> TidalConfig.useCefUi ? "Not started" : "Disabled";
            case STARTING -> "Starting";
            case READY -> "Ready";
            case FAILED -> "Fallback";
            case STOPPING -> "Stopping";
            case TERMINATED -> "Restart required";
        });
        status.addProperty("detail", detail);
        status.addProperty("progress", progress);
        status.addProperty("restartRequired", terminal || state == State.TERMINATED);
        status.addProperty("renderer", state == State.READY && visible ? "CEF" : "Native");
        return status;
    }

    static boolean isStarted() {
        return state == State.STARTING || state == State.READY || cefApp != null;
    }

    static boolean isFailed() {
        return state == State.FAILED;
    }

    static boolean isRestartRequired() {
        return terminal || state == State.TERMINATED;
    }

    static String statusDetail() {
        return detail;
    }

    static float progress() {
        return progress;
    }

    static void tick() {
        if (state != State.READY && state != State.STARTING) return;
        long now = System.nanoTime();
        if (now - lastStatePushNanos < STATE_PUSH_INTERVAL_NANOS) return;
        lastStatePushNanos = now;
        publishSnapshotIfChanged();
    }

    static void publishSnapshotIfChanged() {
        if (state != State.READY && state != State.STARTING) return;
        JsonObject snapshot = snapshot();
        String json = snapshot.toString();
        if (json.equals(lastPublishedSnapshot)) return;
        lastPublishedSnapshot = json;
        executeScript("window.tidalUpdate&&window.tidalUpdate(" + json + ");");
    }

    private static void executeScript(String script) {
        CefBrowser currentBrowser = browser;
        if (currentBrowser == null) return;
        EventQueue.invokeLater(() -> {
            if (browser == currentBrowser
                    && (state == State.READY || state == State.STARTING)) {
                currentBrowser.executeJavaScript(script, currentBrowser.getURL(), 0);
            }
        });
    }

    private static JsonObject snapshot() {
        JsonObject result = new JsonObject();
        result.addProperty("page", page);
        result.addProperty("query", "");
        JsonObject settings = new JsonObject();
        settings.addProperty("useCefUi", TidalConfig.useCefUi);
        settings.addProperty("hudEnabled", TidalConfig.hudEnabled);
        settings.addProperty("uiAnimations", TidalConfig.uiAnimations);
        settings.addProperty("lightTheme", TidalConfig.lightTheme);
        settings.addProperty("advancedDeveloperMode", TidalConfig.advancedDeveloperMode);
        settings.addProperty("uiAntialiasing", TidalConfig.uiAntialiasing);
        result.add("settings", settings);
        result.add("cef", statusJson());

        JsonArray modules = new JsonArray();
        for (TidalModule module : TidalModules.all()) {
            if (!module.implemented) continue;
            JsonObject item = new JsonObject();
            item.addProperty("id", module.id);
            item.addProperty("name", module.name);
            item.addProperty("description", module.description);
            item.addProperty("group", module.group.name());
            item.addProperty("enabled", module.enabled);
            item.addProperty("supported", true);
            item.addProperty("keybind", "");
            modules.add(item);
        }
        result.add("modules", modules);

        JsonArray mods = new JsonArray();
        for (TidalModManager.ModEntry mod : TidalModManager.all()) {
            JsonObject item = new JsonObject();
            item.addProperty("id", mod.id);
            item.addProperty("name", mod.name);
            item.addProperty("version", mod.version);
            item.addProperty("description", mod.description);
            item.addProperty("status", mod.status());
            item.addProperty("statusDetail", mod.status());
            item.addProperty("supportsToggle", mod.canToggle());
            item.addProperty("enabled", mod.enabled);
            item.addProperty("hasSettings", mod.canOpenSettings());
            mods.add(item);
        }
        result.add("mods", mods);
        return result;
    }

    private static final class BridgeHandler extends CefMessageRouterHandlerAdapter {
        @Override
        public boolean onQuery(CefBrowser source, org.cef.browser.CefFrame frame, long queryId,
                               String request, boolean persistent, CefQueryCallback callback) {
            if (request == null || request.length() > MAX_QUERY_LENGTH) {
                callback.failure(400, "The request is invalid or too large.");
                return true;
            }
            try {
                JsonObject message = JsonParser.parseString(request).getAsJsonObject();
                Minecraft.getInstance().execute(() -> handleQuery(message, callback));
            } catch (RuntimeException exception) {
                callback.failure(400, "The request could not be parsed.");
            }
            return true;
        }
    }

    private static void handleQuery(JsonObject request, CefQueryCallback callback) {
        try {
            String type = request.has("type") ? request.get("type").getAsString() : "";
            switch (type) {
                case "snapshot" -> callback.success(snapshot().toString());
                case "modIcon" -> {
                    String id = stringField(request, "id");
                    JsonObject response = new JsonObject();
                    String icon = TidalModManager.iconData(id);
                    if (icon == null) response.add("data", null);
                    else response.addProperty("data", icon);
                    callback.success(response.toString());
                }
                case "navigate" -> {
                    String destination = stringField(request, "page");
                    if (!List.of("HUD", "Modules", "Mods", "Settings").contains(destination)) {
                        throw new IllegalArgumentException("Unknown destination.");
                    }
                    page = destination;
                    callback.success(snapshot().toString());
                }
                case "setModule" -> {
                    String id = stringField(request, "id");
                    TidalModule module = TidalModules.byId(id);
                    if (module == null || !module.implemented) {
                        throw new IllegalArgumentException("This feature is unavailable.");
                    }
                    boolean enabled = booleanField(request, "enabled");
                    module.enabled = enabled;
                    if (enabled && module.group == TidalModule.Group.HUD) {
                        TidalHud.placeInAvailableSpace(module);
                    }
                    TidalConfig.save();
                    callback.success(snapshot().toString());
                }
                case "setMod" -> {
                    String id = stringField(request, "id");
                    TidalModManager.ModEntry mod = findMod(id);
                    if (mod == null || !mod.canToggle()) {
                        throw new IllegalArgumentException(
                                "This mod does not provide a cooperative runtime toggle.");
                    }
                    boolean enabled = booleanField(request, "enabled");
                    if (!mod.pending && mod.enabled != enabled) TidalModManager.requestToggle(mod);
                    callback.success(snapshot().toString());
                }
                case "openModSettings" -> {
                    TidalModManager.ModEntry mod = findMod(stringField(request, "id"));
                    if (mod == null || !mod.canOpenSettings()) {
                        throw new IllegalArgumentException("Settings are unavailable for this mod.");
                    }
                    if (!TidalModManager.openSettings(mod, Minecraft.getInstance().gui.screen())) {
                        throw new IllegalStateException(mod.settingsFailure == null
                                ? "Could not open mod settings." : mod.settingsFailure);
                    }
                    callback.success(snapshot().toString());
                }
                case "openControls" -> {
                    callback.success(snapshot().toString());
                    Minecraft client = Minecraft.getInstance();
                    client.gui.setScreen(new net.minecraft.client.gui.screens.options.controls.ControlsScreen(
                            client.gui.screen(), client.options));
                }
                case "openHudEditor" -> {
                    callback.success(snapshot().toString());
                    Minecraft client = Minecraft.getInstance();
                    client.gui.setScreen(new TidalHudEditorScreen(client.gui.screen()));
                }
                case "setSetting" -> updateSetting(request, callback);
                case "close" -> {
                    callback.success(snapshot().toString());
                    Minecraft.getInstance().execute(() ->
                            Minecraft.getInstance().gui.setScreen(null));
                }
                case "escape" -> {
                    callback.success("{}");
                    TidalCefScreen.closeFromPage();
                }
                case "minimize" -> {
                    callback.success("{}");
                    TidalCefScreen.closeFromPage();
                }
                default -> throw new IllegalArgumentException("Unsupported Tidal action.");
            }
        } catch (RuntimeException exception) {
            callback.failure(400, conciseMessage(exception));
        }
    }

    private static void updateSetting(JsonObject request, CefQueryCallback callback) {
        String setting = stringField(request, "setting");
        JsonElement value = request.get("value");
        switch (setting) {
            case "useCefUi" -> TidalConfig.useCefUi = value.getAsBoolean();
            case "hudEnabled" -> TidalConfig.hudEnabled = value.getAsBoolean();
            case "uiAnimations" -> TidalConfig.uiAnimations = value.getAsBoolean();
            case "lightTheme" -> {
                TidalConfig.lightTheme = value.getAsBoolean();
                TidalTheme.apply(TidalConfig.lightTheme, TidalConfig.themeAccent);
            }
            case "advancedDeveloperMode" ->
                    TidalConfig.advancedDeveloperMode = value.getAsBoolean();
            case "uiAntialiasing" -> {
                int quality = value.getAsInt();
                TidalConfig.uiAntialiasing = quality == 4 ? 4 : quality == 2 ? 2 : 1;
            }
            default -> throw new IllegalArgumentException("This setting is not configurable here.");
        }
        TidalConfig.save();
        JsonObject current = snapshot();
        callback.success(current.toString());
        if (setting.equals("useCefUi") && !TidalConfig.useCefUi) {
            Minecraft.getInstance().execute(() -> {
                disable();
                Minecraft.getInstance().gui.setScreen(TidalScreen.settingsScreen());
            });
        }
    }

    private static String stringField(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
            throw new IllegalArgumentException("Missing request field.");
        }
        return object.get(key).getAsString();
    }

    private static boolean booleanField(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()
                || !object.get(key).getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("Missing request field.");
        }
        return object.get(key).getAsBoolean();
    }

    private static TidalModManager.ModEntry findMod(String id) {
        for (TidalModManager.ModEntry mod : TidalModManager.all()) {
            if (mod.id.equals(id)) return mod;
        }
        return null;
    }

    static boolean dispatchMouse(int eventId, double x, double y, int button,
                                 int clickCount, int modifiers,
                                 int logicalWidth, int logicalHeight) {
        TidalOsrBrowser currentBrowser = browser;
        if (currentBrowser == null || logicalWidth <= 0 || logicalHeight <= 0) {
            return false;
        }
        Rectangle viewRect = currentBrowser.getViewRect(currentBrowser);
        if (viewRect.width <= 0 || viewRect.height <= 0) return false;
        double scaleX = viewRect.width / (double) logicalWidth;
        double scaleY = viewRect.height / (double) logicalHeight;
        int pixelX = (int) Math.round(x * scaleX);
        int pixelY = (int) Math.round(y * scaleY);
        int awtModifiers = toAwtModifiers(modifiers, eventId, button);
        EventQueue.invokeLater(() -> {
            currentBrowser.mouseEvent(eventId, pixelX, pixelY, awtModifiers, button, clickCount);
        });
        return true;
    }

    static boolean dispatchScroll(double x, double y, double amount, int modifiers,
                                  int logicalWidth, int logicalHeight) {
        TidalOsrBrowser currentBrowser = browser;
        if (currentBrowser == null || logicalWidth <= 0 || logicalHeight <= 0) {
            return false;
        }
        Rectangle viewRect = currentBrowser.getViewRect(currentBrowser);
        if (viewRect.width <= 0 || viewRect.height <= 0) return false;
        double scaleX = viewRect.width / (double) logicalWidth;
        double scaleY = viewRect.height / (double) logicalHeight;
        int pixelX = (int) Math.round(x * scaleX);
        int pixelY = (int) Math.round(y * scaleY);
        int rotation = (int) Math.signum(-amount);
        if (rotation == 0) return false;
        int awtModifiers = toAwtModifiers(modifiers, MouseWheelEvent.MOUSE_WHEEL, -1);
        EventQueue.invokeLater(() -> {
            currentBrowser.wheelEvent(pixelX, pixelY, awtModifiers, rotation);
        });
        return true;
    }

    private static int toAwtModifiers(int modifiers, int eventId, int button) {
        int awtModifiers = 0;
        if ((modifiers & 1) != 0) awtModifiers |= InputEvent.SHIFT_DOWN_MASK;
        if ((modifiers & 2) != 0) awtModifiers |= InputEvent.CTRL_DOWN_MASK;
        if ((modifiers & 4) != 0) awtModifiers |= InputEvent.ALT_DOWN_MASK;
        if ((modifiers & 8) != 0) awtModifiers |= InputEvent.META_DOWN_MASK;
        if ((eventId == MouseEvent.MOUSE_PRESSED || eventId == MouseEvent.MOUSE_DRAGGED)
                && button == 0) {
            awtModifiers |= InputEvent.BUTTON1_DOWN_MASK;
        }
        if ((eventId == MouseEvent.MOUSE_PRESSED || eventId == MouseEvent.MOUSE_DRAGGED)
                && button == 1) {
            awtModifiers |= InputEvent.BUTTON3_DOWN_MASK;
        }
        if ((eventId == MouseEvent.MOUSE_PRESSED || eventId == MouseEvent.MOUSE_DRAGGED)
                && button == 2) {
            awtModifiers |= InputEvent.BUTTON2_DOWN_MASK;
        }
        return awtModifiers;
    }

    static boolean dispatchKey(int keyCode, int scanCode, int modifiers, boolean pressed) {
        TidalOsrBrowser currentBrowser = browser;
        if (currentBrowser == null) return false;
        int awtKey = toAwtKeyCode(keyCode);
        if (awtKey == 0) return false;
        int awtModifiers = toAwtModifiers(modifiers, 0, -1);
        int eventId = pressed ? java.awt.event.KeyEvent.KEY_PRESSED
                : java.awt.event.KeyEvent.KEY_RELEASED;
        EventQueue.invokeLater(() -> {
            currentBrowser.keyEvent(eventId, awtModifiers, awtKey,
                    java.awt.event.KeyEvent.CHAR_UNDEFINED);
        });
        return true;
    }

    static boolean dispatchCharacter(int codepoint, int modifiers) {
        TidalOsrBrowser currentBrowser = browser;
        if (currentBrowser == null || !Character.isValidCodePoint(codepoint)) return false;
        int awtModifiers = toAwtModifiers(modifiers, 0, -1);
        EventQueue.invokeLater(() -> {
            for (char codeUnit : Character.toChars(codepoint)) {
                currentBrowser.keyEvent(java.awt.event.KeyEvent.KEY_TYPED, awtModifiers,
                        java.awt.event.KeyEvent.VK_UNDEFINED, codeUnit);
            }
        });
        return true;
    }

    private static int toAwtKeyCode(int key) {
        if (key >= 32 && key <= 96) return key;
        return switch (key) {
            case 256 -> java.awt.event.KeyEvent.VK_ESCAPE;
            case 257 -> java.awt.event.KeyEvent.VK_ENTER;
            case 258 -> java.awt.event.KeyEvent.VK_TAB;
            case 259 -> java.awt.event.KeyEvent.VK_BACK_SPACE;
            case 260 -> java.awt.event.KeyEvent.VK_INSERT;
            case 261 -> java.awt.event.KeyEvent.VK_DELETE;
            case 262 -> java.awt.event.KeyEvent.VK_RIGHT;
            case 263 -> java.awt.event.KeyEvent.VK_LEFT;
            case 264 -> java.awt.event.KeyEvent.VK_DOWN;
            case 265 -> java.awt.event.KeyEvent.VK_UP;
            case 266 -> java.awt.event.KeyEvent.VK_PAGE_UP;
            case 267 -> java.awt.event.KeyEvent.VK_PAGE_DOWN;
            case 268 -> java.awt.event.KeyEvent.VK_HOME;
            case 269 -> java.awt.event.KeyEvent.VK_END;
            case 280 -> java.awt.event.KeyEvent.VK_CAPS_LOCK;
            case 340, 344 -> java.awt.event.KeyEvent.VK_SHIFT;
            case 341, 345 -> java.awt.event.KeyEvent.VK_CONTROL;
            case 342, 346 -> java.awt.event.KeyEvent.VK_ALT;
            case 343, 347 -> java.awt.event.KeyEvent.VK_META;
            default -> key >= 290 && key <= 314
                    ? java.awt.event.KeyEvent.VK_F1 + key - 290 : 0;
        };
    }

    private static void shutdownRuntime(boolean expected) {
        TidalOsrBrowser oldBrowser = browser;
        CefClient oldClient = cefClient;
        CefMessageRouter oldRouter = messageRouter;
        CefApp oldApp = cefApp;
        if (oldBrowser != null) {
            try {
                oldBrowser.setCloseAllowed();
                oldBrowser.close(true);
            } catch (RuntimeException | LinkageError exception) {
                LOGGER.warn("Could not close the CEF browser cleanly.", exception);
            }
        }
        if (oldClient != null && oldRouter != null) {
            try {
                oldClient.removeMessageRouter(oldRouter);
                oldRouter.dispose();
            } catch (RuntimeException | LinkageError exception) {
                LOGGER.warn("Could not dispose the CEF message bridge cleanly.", exception);
            }
        }
        if (oldClient != null) {
            try {
                oldClient.dispose();
            } catch (RuntimeException | LinkageError exception) {
                LOGGER.warn("Could not dispose the CEF client cleanly.", exception);
            }
        }
        boolean runtimeStopped = oldApp == null;
        if (oldApp != null) {
            try {
                oldApp.dispose();
                runtimeStopped = CefApp.getState() == CefApp.CefAppState.TERMINATED;
            } catch (RuntimeException | LinkageError exception) {
                LOGGER.warn("Could not stop the CEF runtime cleanly.", exception);
            }
        }
        browser = null;
        cefClient = null;
        messageRouter = null;
        cefApp = null;
        if (oldApp != null) terminal = true;
        if (terminal) {
            state = runtimeStopped ? State.TERMINATED : State.STOPPING;
            if (expected) {
                detail = runtimeStopped
                        ? "CEF has stopped. Restart Minecraft before enabling it again."
                        : "CEF is stopping. Restart Minecraft before enabling it again.";
            }
        } else if (expected) {
            state = State.DORMANT;
            detail = "CEF is disabled; the native renderer is active.";
            progress = 0;
        }
        releaseTexture();
        if (terminal && cefWorker != null) cefWorker.shutdown();
    }

    static void disable() {
        TidalCefManager.disableRequested = true;
        State current = state;
        if (current == State.DORMANT && cefApp == null) {
            detail = "CEF is disabled; the native renderer is active.";
            progress = 0;
            return;
        }
        if (current == State.STARTING) {
            state = State.STOPPING;
            detail = "CEF is shutting down.";
            return;
        }
        if (current == State.READY || current == State.FAILED || current == State.STOPPING) {
            state = State.STOPPING;
            if (cefApp != null) terminal = true;
            detail = cefApp == null
                    ? "CEF is disabled; the native renderer is active."
                    : "CEF is shutting down; a restart will be required to enable it again.";
            ExecutorService worker = ensureWorker();
            worker.execute(() -> shutdownRuntime(true));
        }
    }

    private static void releaseTexture() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return;
        minecraft.execute(TidalCefScreen::releaseTexture);
    }

    static void shutdownForGameExit() {
        synchronized (TidalCefManager.class) {
            boolean wasStarting = state == State.STARTING;
            gameStopping = true;
            state = State.STOPPING;
            disableRequested = true;
            detail = "Stopping the CEF runtime.";
            if (wasStarting) {
                if (cefWorker != null) cefWorker.shutdown();
                return;
            }
            if (cefApp == null) {
                if (cefWorker != null) cefWorker.shutdown();
                return;
            }
            ExecutorService worker = ensureWorker();
            if (state != State.STARTING) worker.execute(() -> shutdownRuntime(true));
            worker.shutdown();
        }
    }

}
