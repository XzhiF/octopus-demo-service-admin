package com.octopus.demo.admin.aspect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.octopus.demo.admin.annotation.OperLog;
import com.octopus.demo.admin.entity.SysOperLog;
import com.octopus.demo.admin.security.AuthContext;
import com.octopus.demo.admin.service.OperLogService;
import com.octopus.demo.admin.web.ClientIpResolver;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Writes one {@code sys_oper_log} row per {@link OperLog}-annotated invocation. */
@Aspect
@Component
public class OperLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperLogAspect.class);
    private static final int PARAMS_MAX_LENGTH = 2000;
    private static final int ERROR_MAX_LENGTH = 500;
    private static final String MASK = "******";
    private static final String ANONYMOUS = "anonymous";
    private static final Set<String> MASKED_FIELDS = Set.of("password", "passwordhash");

    private final ObjectMapper objectMapper;
    private final OperLogService operLogService;

    public OperLogAspect(ObjectMapper objectMapper, OperLogService operLogService) {
        this.objectMapper = objectMapper;
        this.operLogService = operLogService;
    }

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        long startedAt = System.nanoTime();
        HttpServletRequest request = currentRequest();
        try {
            Object result = joinPoint.proceed();
            write(operLog, request, joinPoint.getArgs(), 1, null, elapsedMs(startedAt));
            return result;
        } catch (Throwable ex) {
            write(operLog, request, joinPoint.getArgs(), 0, errorMessage(ex), elapsedMs(startedAt));
            throw ex;
        }
    }

    private void write(OperLog operLog, HttpServletRequest request, Object[] args,
                       int status, String errorMsg, long durationMs) {
        try {
            SysOperLog entry = new SysOperLog();
            entry.setTitle(operLog.title());
            entry.setModule(operLog.module());
            entry.setHttpMethod(request == null ? null : request.getMethod());
            entry.setRequestUri(request == null ? null : request.getRequestURI());
            entry.setParams(serializeArgs(args));
            entry.setOperator(currentUsername());
            entry.setIp(request == null ? null : ClientIpResolver.resolve(request));
            entry.setStatus(status);
            entry.setErrorMsg(errorMsg);
            entry.setDurationMs(durationMs);
            entry.setCreatedAt(LocalDateTime.now());
            operLogService.save(entry);
        } catch (Exception ex) {
            log.warn("Failed to persist operation log for '{}'", operLog.title(), ex);
        }
    }

    private String serializeArgs(Object[] args) {
        List<Object> serializable = new ArrayList<>();
        for (Object arg : args) {
            if (arg instanceof ServletRequest || arg instanceof ServletResponse
                    || arg instanceof Authentication) {
                continue;
            }
            serializable.add(arg);
        }
        try {
            JsonNode node = objectMapper.valueToTree(serializable);
            maskSecrets(node);
            return truncate(objectMapper.writeValueAsString(node), PARAMS_MAX_LENGTH);
        } catch (Exception ex) {
            log.warn("Failed to serialize operation log params", ex);
            return null;
        }
    }

    private void maskSecrets(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            List<String> fieldNames = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining(fieldNames::add);
            for (String fieldName : fieldNames) {
                if (MASKED_FIELDS.contains(fieldName.toLowerCase(Locale.ROOT))) {
                    objectNode.put(fieldName, MASK);
                } else {
                    maskSecrets(objectNode.get(fieldName));
                }
            }
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(this::maskSecrets);
        }
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthContext context
                && context.username() != null && !context.username().isBlank()) {
            return context.username();
        }
        return ANONYMOUS;
    }

    private static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private static long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private static String errorMessage(Throwable ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }
        return truncate(message, ERROR_MAX_LENGTH);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
