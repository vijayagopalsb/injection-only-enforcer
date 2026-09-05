package io.github.injectiononly.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface InjectionOnly {

    /**
     * Types that this class is still allowed to construct with {@code new}, even though it's
     * marked @InjectionOnly. Use this for plain value objects, collections, exceptions, or any
     * other type that isn't itself a managed dependency (a service, repository, client, etc.).
     * <p>
     * Anything under java.* / javax.* is exempt automatically and does not need to be listed
     * here - this is for everything else (your own DTOs, third-party library types, and so on).
     */

    Class<?>[] allow() default {};

}
