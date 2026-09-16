package com.claircore.shared.interfaces.rest.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds the authenticated user's id to a controller parameter.
 *
 * <p>It replaces reading the request attribute by hand, which forced every controller in every
 * context to import the JWT filter from {@code iam.infrastructure} for a single constant — an
 * interfaces layer reaching into another context's infrastructure.
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {
}
