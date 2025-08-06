package com.odyssey.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Crash reporting system for The Odyssey.
 * Captures and logs detailed crash information for debugging and analysis.
 */
public class CrashReporter {
    private static final Logger logger = LoggerFactory.getLogger(CrashReporter.class);
    private static final String CRASH_REPORTS_DIR = "crash-reports";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    private final Gson gson;
    private final GameConfig config;
    
    public CrashReporter(GameConfig config) {
        this.config = config;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        
        // Ensure crash reports directory exists
        try {
            Files.createDirectories(Paths.get(CRASH_REPORTS_DIR));
        } catch (IOException e) {
            logger.error("Failed to create crash reports directory", e);
        }
    }
    
    /**
     * Report a crash with detailed system information
     */
    public void reportCrash(String context, Throwable throwable) {
        logger.error("CRASH DETECTED in context: {}", context, throwable);
        
        try {
            CrashReport report = createCrashReport(context, throwable);
            saveCrashReport(report);
            
            // Also log to console for immediate visibility
            logger.error("Crash report saved to: {}", report.getFilename());
            logger.error("Stack trace: {}", report.getStackTrace());
            
        } catch (Exception e) {
            logger.error("Failed to generate crash report", e);
        }
    }
    
    /**
     * Create a detailed crash report
     */
    private CrashReport createCrashReport(String context, Throwable throwable) {
        CrashReport report = new CrashReport();
        
        // Basic information
        report.timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        report.context = context;
        report.exceptionType = throwable.getClass().getSimpleName();
        report.message = throwable.getMessage();
        report.stackTrace = getStackTrace(throwable);
        
        // System information
        report.systemInfo = gatherSystemInfo();
        
        // Game configuration
        report.gameConfig = gatherGameConfig();
        
        // Runtime information
        report.runtimeInfo = gatherRuntimeInfo();
        
        // Generate filename
        report.filename = String.format("crash-%s-%s.json", 
                report.timestamp, 
                report.exceptionType.toLowerCase());
        
        return report;
    }
    
    /**
     * Save crash report to file
     */
    private void saveCrashReport(CrashReport report) throws IOException {
        Path reportPath = Paths.get(CRASH_REPORTS_DIR, report.filename);
        String json = gson.toJson(report);
        Files.writeString(reportPath, json);
    }
    
    /**
     * Get stack trace as string
     */
    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * Gather system information
     */
    private Map<String, Object> gatherSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // Operating system
        info.put("os.name", System.getProperty("os.name"));
        info.put("os.version", System.getProperty("os.version"));
        info.put("os.arch", System.getProperty("os.arch"));
        
        // Java runtime
        info.put("java.version", System.getProperty("java.version"));
        info.put("java.vendor", System.getProperty("java.vendor"));
        info.put("java.vm.name", System.getProperty("java.vm.name"));
        info.put("java.vm.version", System.getProperty("java.vm.version"));
        
        // Hardware
        info.put("processors", Runtime.getRuntime().availableProcessors());
        
        return info;
    }
    
    /**
     * Gather game configuration information
     */
    private Map<String, Object> gatherGameConfig() {
        Map<String, Object> info = new HashMap<>();
        
        if (config != null) {
            info.put("windowWidth", config.getWindowWidth());
            info.put("windowHeight", config.getWindowHeight());
            info.put("fullscreen", config.isFullscreen());
            info.put("debugMode", config.isDebugMode());
            info.put("renderDistance", config.getRenderDistance());
            info.put("enableTidalSystem", config.isEnableTidalSystem());
            info.put("enableWeatherSystem", config.isEnableWeatherSystem());
            info.put("enableMultithreading", config.isEnableMultithreading());
            info.put("workerThreads", config.getWorkerThreads());
        }
        
        return info;
    }
    
    /**
     * Gather runtime information
     */
    private Map<String, Object> gatherRuntimeInfo() {
        Map<String, Object> info = new HashMap<>();
        
        Runtime runtime = Runtime.getRuntime();
        
        // Memory information
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        info.put("memory.max", formatBytes(maxMemory));
        info.put("memory.total", formatBytes(totalMemory));
        info.put("memory.used", formatBytes(usedMemory));
        info.put("memory.free", formatBytes(freeMemory));
        info.put("memory.usage.percent", Math.round((double) usedMemory / totalMemory * 100));
        
        // Thread information
        info.put("threads.active", Thread.activeCount());
        
        return info;
    }
    
    /**
     * Format bytes in human-readable format
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * Crash report data structure
     */
    public static class CrashReport {
        public String timestamp;
        public String context;
        public String exceptionType;
        public String message;
        public String stackTrace;
        public Map<String, Object> systemInfo;
        public Map<String, Object> gameConfig;
        public Map<String, Object> runtimeInfo;
        public String filename;
        
        public String getFilename() {
            return filename;
        }
        
        public String getStackTrace() {
            return stackTrace;
        }
    }
}