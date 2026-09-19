package com.rdh.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Logging configuration and startup banner
 */
@Configuration
public class LoggingConfig {
    
    private static final Logger log = LoggerFactory.getLogger(LoggingConfig.class);
    
    private final Environment env;
    
    public LoggingConfig(Environment env) {
        this.env = env;
    }
    
    /**
     * Log application startup information
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logApplicationStartup() {
        String protocol = "http";
        if (env.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }
        
        String serverPort = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "/");
        String hostAddress = "localhost";
        
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("The host name could not be determined, using 'localhost' as fallback");
        }
        
        String[] activeProfiles = env.getActiveProfiles();
        if (activeProfiles.length == 0) {
            activeProfiles = env.getDefaultProfiles();
        }
        
        log.info("\n" +
            "═══════════════════════════════════════════════════════════════════════════════\n" +
            "  🚀 RDH Backend Application Started Successfully!\n" +
            "═══════════════════════════════════════════════════════════════════════════════\n" +
            "  Application:     {}\n" +
            "  Version:         {}\n" +
            "  Active Profiles: {}\n" +
            "  Local URL:       {}://localhost:{}{}\n" +
            "  External URL:    {}://{}:{}{}\n" +
            "  Swagger UI:      {}://localhost:{}/swagger-ui.html\n" +
            "  API Docs:        {}://localhost:{}/v3/api-docs\n" +
            "  Log Level:       {}\n" +
            "  Log Path:        {}\n" +
            "═══════════════════════════════════════════════════════════════════════════════",
            env.getProperty("spring.application.name", "rdh-backend"),
            env.getProperty("application.version", "1.0.0"),
            Arrays.toString(activeProfiles),
            protocol, serverPort, contextPath,
            protocol, hostAddress, serverPort, contextPath,
            protocol, serverPort,
            protocol, serverPort,
            env.getProperty("logging.level.com.rdh.backend", "INFO"),
            env.getProperty("logging.file.path", "./logs")
        );
        
        // Log important configuration
        log.info("Max file upload size: {}", env.getProperty("spring.servlet.multipart.max-file-size"));
        log.info("Max request size: {}", env.getProperty("spring.servlet.multipart.max-request-size"));
        
        // Warn if running in production with DEBUG level
        if (Arrays.asList(activeProfiles).contains("prod") && 
            "DEBUG".equals(env.getProperty("logging.level.com.rdh.backend"))) {
            log.warn("⚠️  WARNING: Running in production mode with DEBUG logging level!");
            log.warn("⚠️  This may impact performance and expose sensitive information");
        }
    }
}
