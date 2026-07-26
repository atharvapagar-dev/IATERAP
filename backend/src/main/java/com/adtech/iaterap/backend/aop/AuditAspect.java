package com.adtech.iaterap.backend.aop;

import com.adtech.iaterap.backend.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * Write-through audit interceptor.
 * <p>
 * Wraps every create/update/delete operation exposed by the service layer
 * and records the actor, action, entity name, before/after state and
 * request metadata into the audit_log table via {@link AuditService}.
 * <p>
 * This is intentionally implemented as a generic pointcut over the
 * {@code save}/{@code update}/{@code delete} method names already used
 * consistently across the existing service layer, so no existing service
 * class needs to be modified to participate in auditing.
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private static final String ACTION_CREATE = "CREATE";
    private static final String ACTION_UPDATE = "UPDATE";
    private static final String ACTION_DELETE = "DELETE";

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public AuditAspect(AuditService auditService, ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Around(
            "execution(public * com.adtech.iaterap.backend.service.*Service.save(..)) || "
                    + "execution(public * com.adtech.iaterap.backend.service.*Service.update(..)) || "
                    + "execution(public * com.adtech.iaterap.backend.service.*Service.delete(..))"
    )
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        String targetClassName = joinPoint.getTarget().getClass().getSimpleName();

        // AuditService and AuthService are not domain write operations
        // subject to auditing themselves.
        if ("AuditService".equals(targetClassName) || "AuthService".equals(targetClassName)) {
            return joinPoint.proceed();
        }

        String action = resolveAction(joinPoint.getSignature().getName());
        if (action == null) {
            return joinPoint.proceed();
        }

        String entityName = resolveEntityName(targetClassName);
        Object[] args = joinPoint.getArgs();

        String entityId = resolveEntityIdFromArgs(action, args);
        String beforeState = captureBeforeState(joinPoint.getTarget(), action, args);

        Object result = joinPoint.proceed();

        if (entityId == null) {
            entityId = extractId(result);
        }

        String afterState = ACTION_DELETE.equals(action)
                ? null
                : safeSerialize(result);

        writeAuditEntry(action, entityName, entityId, beforeState, afterState);

        return result;
    }

    private void writeAuditEntry(
            String action,
            String entityName,
            String entityId,
            String beforeState,
            String afterState
    ) {
        try {
            auditService.record(
                    resolveActor(),
                    action,
                    entityName,
                    entityId,
                    beforeState,
                    afterState,
                    resolveMetadata()
            );
        } catch (Exception auditException) {
            // Audit logging must never break the underlying business operation.
            log.warn(
                    "Failed to persist audit log for entity {} ({}): {}",
                    entityName,
                    action,
                    auditException.getMessage()
            );
        }
    }

    private String resolveAction(String methodName) {
        if (methodName.startsWith("save")) {
            return ACTION_CREATE;
        }
        if (methodName.startsWith("update")) {
            return ACTION_UPDATE;
        }
        if (methodName.startsWith("delete")) {
            return ACTION_DELETE;
        }
        return null;
    }

    private String resolveEntityName(String targetClassName) {
        return targetClassName.endsWith("Service")
                ? targetClassName.substring(0, targetClassName.length() - "Service".length())
                : targetClassName;
    }

    private String resolveEntityIdFromArgs(String action, Object[] args) {
        if (!ACTION_CREATE.equals(action) && args.length > 0 && args[0] instanceof Long id) {
            return String.valueOf(id);
        }
        return null;
    }

    private String captureBeforeState(Object target, String action, Object[] args) {
        if (ACTION_CREATE.equals(action) || args.length == 0 || !(args[0] instanceof Long id)) {
            return null;
        }

        try {
            Method getById = target.getClass().getMethod("getById", Long.class);
            Object existing = getById.invoke(target, id);
            return safeSerialize(existing);
        } catch (Exception exception) {
            return null;
        }
    }

    private String extractId(Object result) {
        if (result == null) {
            return null;
        }

        try {
            Method getId = result.getClass().getMethod("getId");
            Object id = getId.invoke(result);
            return id != null ? String.valueOf(id) : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private String safeSerialize(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception exception) {
            return String.valueOf(obj);
        }
    }

    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getName() != null) {
            return authentication.getName();
        }

        return "SYSTEM";
    }

    private String resolveMetadata() {
        Object requestAttributes = RequestContextHolder.getRequestAttributes();

        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }

        HttpServletRequest request = servletRequestAttributes.getRequest();
        return request.getMethod() + " " + request.getRequestURI();
    }
}
