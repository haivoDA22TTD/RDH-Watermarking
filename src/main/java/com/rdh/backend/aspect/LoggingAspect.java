package com.rdh.backend.aspect;

import com.rdh.backend.util.LoggingUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AOP Aspect for automatic method logging
 * Logs method entry, exit, execution time, and parameters
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {
    
    /**
     * Pointcut for all service methods
     */
    @Pointcut("within(com.rdh.backend.service..*)")
    public void serviceMethods() {}
    
    /**
     * Pointcut for all controller methods
     */
    @Pointcut("within(com.rdh.backend.controller..*)")
    public void controllerMethods() {}
    
    /**
     * Pointcut for all validator methods
     */
    @Pointcut("within(com.rdh.backend.validation..*)")
    public void validatorMethods() {}
    
    /**
     * Pointcut for all utility methods
     */
    @Pointcut("within(com.rdh.backend.util..*)")
    public void utilityMethods() {}
    
    /**
     * Around advice for service methods
     * Logs method entry, exit, execution time, and handles exceptions
     */
    @Around("serviceMethods()")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "SERVICE");
    }
    
    /**
     * Around advice for controller methods (more verbose)
     */
    @Around("controllerMethods()")
    public Object logControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "CONTROLLER");
    }
    
    /**
     * Around advice for validator methods
     */
    @Around("validatorMethods()")
    public Object logValidatorMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        // Only log at DEBUG level for validators
        if (log.isDebugEnabled()) {
            return logMethodExecution(joinPoint, "VALIDATOR");
        } else {
            return joinPoint.proceed();
        }
    }
    
    /**
     * Generic method execution logging
     */
    private Object logMethodExecution(ProceedingJoinPoint joinPoint, String layerName) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        Object[] args = joinPoint.getArgs();
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Log method entry
            if (log.isDebugEnabled()) {
                logMethodEntry(layerName, className, methodName, args);
            }
            
            // Proceed with method execution
            Object result = joinPoint.proceed();
            
            // Calculate execution time
            long duration = System.currentTimeMillis() - startTime;
            
            // Log method exit
            logMethodExit(layerName, className, methodName, duration, result);
            
            // Log slow methods
            if (duration > 1000) {
                log.warn("⚠️  SLOW METHOD | {}.{} | duration={} | Consider optimization",
                    className, methodName, LoggingUtils.formatDuration(duration));
            }
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            // Log method exception
            log.error("❌ {} EXCEPTION | {}.{} | duration={} | error={}",
                layerName, className, methodName, 
                LoggingUtils.formatDuration(duration), 
                e.getMessage());
            
            throw e;
        }
    }
    
    /**
     * Log method entry with parameters
     */
    private void logMethodEntry(String layerName, String className, String methodName, Object[] args) {
        if (args == null || args.length == 0) {
            log.debug("→ {} ENTRY | {}.{} | args=[]", layerName, className, methodName);
            return;
        }
        
        String formattedArgs = formatMethodArguments(args);
        log.debug("→ {} ENTRY | {}.{} | args=[{}]", layerName, className, methodName, formattedArgs);
    }
    
    /**
     * Log method exit with result
     */
    private void logMethodExit(String layerName, String className, String methodName, long duration, Object result) {
        String resultStr = formatReturnValue(result);
        
        log.info("← {} EXIT | {}.{} | duration={} | result={}",
            layerName, className, methodName, 
            LoggingUtils.formatDuration(duration), 
            resultStr);
    }
    
    /**
     * Format method arguments for logging (with sensitive data masking)
     */
    private String formatMethodArguments(Object[] args) {
        return Arrays.stream(args)
            .map(this::formatArgument)
            .collect(Collectors.joining(", "));
    }
    
    /**
     * Format single argument
     */
    private String formatArgument(Object arg) {
        if (arg == null) {
            return "null";
        }
        
        // Handle MultipartFile
        if (arg instanceof MultipartFile) {
            return LoggingUtils.formatFileInfo((MultipartFile) arg);
        }
        
        // Handle List of MultipartFiles
        if (arg instanceof List) {
            List<?> list = (List<?>) arg;
            if (!list.isEmpty() && list.get(0) instanceof MultipartFile) {
                @SuppressWarnings("unchecked")
                List<MultipartFile> files = (List<MultipartFile>) list;
                return LoggingUtils.formatFilesInfo(files);
            }
        }
        
        // Handle String (check for sensitive data)
        if (arg instanceof String) {
            String str = (String) arg;
            
            // Mask potential passwords
            if (str.toLowerCase().contains("password") || 
                str.toLowerCase().contains("secret") ||
                str.toLowerCase().contains("token")) {
                return "***MASKED***";
            }
            
            // Truncate long strings
            return LoggingUtils.truncate(str, 100);
        }
        
        // Handle primitive types and simple objects
        if (arg.getClass().isPrimitive() || 
            arg instanceof Number || 
            arg instanceof Boolean ||
            arg instanceof Character) {
            return arg.toString();
        }
        
        // Handle enums
        if (arg instanceof Enum) {
            return arg.toString();
        }
        
        // For complex objects, just log class name
        return arg.getClass().getSimpleName() + "@" + Integer.toHexString(arg.hashCode());
    }
    
    /**
     * Format return value for logging
     */
    private String formatReturnValue(Object result) {
        if (result == null) {
            return "null";
        }
        
        // Handle byte arrays (images)
        if (result instanceof byte[]) {
            byte[] bytes = (byte[]) result;
            return "byte[" + LoggingUtils.formatFileSize(bytes.length) + "]";
        }
        
        // Handle ResponseEntity
        if (result.getClass().getSimpleName().equals("ResponseEntity")) {
            return "ResponseEntity";
        }
        
        // Handle simple types
        if (result instanceof String) {
            return LoggingUtils.truncate((String) result, 50);
        }
        
        if (result.getClass().isPrimitive() || 
            result instanceof Number || 
            result instanceof Boolean) {
            return result.toString();
        }
        
        // For complex objects
        return result.getClass().getSimpleName();
    }
}
