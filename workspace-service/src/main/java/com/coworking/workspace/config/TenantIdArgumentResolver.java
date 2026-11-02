package com.coworking.workspace.config;

import com.coworking.workspace.annotation.TenantId;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class TenantIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(TenantId.class)
                && parameter.getParameterType().equals(String.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer container,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        TenantId tenantId = parameter.getParameterAnnotation(TenantId.class);
        String value = webRequest.getHeader("X-Tenant-ID");
        if ((value == null || value.isBlank()) && tenantId.required()) {
            throw new IllegalArgumentException("Required header X-Tenant-ID is missing");
        }
        return value;
    }
}
