package com.datacom.common.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Replaces Spring Boot's default whitelabel error page and BasicErrorController. Never leaks a
 * stack trace, an exception class name or an internal path to the browser (SEC-13 / ELEV-5) — only
 * a status, a French message and, for 5xx, the correlation id already logged server-side
 * (US-17/CA-3).
 */
@Controller
public class GlobalErrorController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorController.class);

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        HttpStatus status = resolveStatus(request);
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);

        if (status.is5xxServerError()) {
            Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
            log.error(
                    "Erreur serveur non gérée [correlationId={}, path={}]",
                    correlationId,
                    request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI),
                    exception instanceof Throwable throwable ? throwable : null);
        }

        model.addAttribute("status", status.value());
        model.addAttribute("correlationId", correlationId);

        return switch (status) {
            case FORBIDDEN -> "error/403";
            case NOT_FOUND -> "error/404";
            default -> status.is5xxServerError() ? "error/500" : "error/error";
        };
    }

    private HttpStatus resolveStatus(HttpServletRequest request) {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        HttpStatus status = HttpStatus.resolve((Integer) code);
        return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
