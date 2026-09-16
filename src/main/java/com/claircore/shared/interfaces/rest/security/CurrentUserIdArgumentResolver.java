package com.claircore.shared.interfaces.rest.security;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

/** Resolves {@link CurrentUserId} from the request attribute the authentication filter sets. */
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    /** Request attribute the authentication filter writes the resolved user id to. */
    public static final String USER_ID_ATTRIBUTE = "X-User-Id";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && UUID.class.isAssignableFrom(parameter.getParameterType());
    }

    /** Null for an unauthenticated request, exactly as reading the attribute by hand was. */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return webRequest.getAttribute(USER_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
    }
}
