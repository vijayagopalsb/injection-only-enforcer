# InjectionOnly Enforcer

**Compile-time guardrails for dependency injection discipline in Java.**

InjectionOnly Enforcer is a Java annotation processor that helps teams keep
classes IoC-first by discouraging manual object creation inside classes marked
with `@InjectionOnly`. It is especially useful in Spring applications, where
services, repositories, clients, and other managed components should receive
their collaborators through dependency injection rather than construct them
manually.

## Why this project exists

In large Java applications, it is easy for a service to drift into patterns
like these:

```java
@Service
public class UserService {
    private final UserRepository repository = new UserRepository();
}
```

```java
public class OrderService {
    public void create() {
        PaymentClient client = new PaymentClient();
        client.pay();
    }
}
```

These patterns can hide dependency lifecycle problems, make code harder to test,
and reduce the value of inversion of control.

`@InjectionOnly` provides a compile-time rule:

- Classes marked with `@InjectionOnly` cannot manually instantiate non-exempt
  types with `new`.
- Types listed in the annotation's `allow` option are exempt.
- Types in `java.*` and `javax.*` are exempt automatically.

This helps keep dependency construction centralized and predictable.

## Features

- Rejects manual construction of non-exempt types inside annotated classes.
- Allows JDK and `javax.*` types automatically.
- Supports an explicit allowlist through `@InjectionOnly(allow = {...})`.
- Runs as a Java annotation processor.
- Integrates with Maven and Gradle annotation-processing builds.

## Annotation

```java
package io.github.injectiononly.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface InjectionOnly {
    Class<?>[] allow() default {};
}
```

## Usage

Dependencies should be supplied to an `@InjectionOnly` class, typically through
constructor injection:

```java
import io.github.injectiononly.annotation.InjectionOnly;

@InjectionOnly
public class OrderService {
    private final PaymentClient paymentClient;

    public OrderService(PaymentClient paymentClient) {
        this.paymentClient = paymentClient;
    }

    public void createOrder() {
        paymentClient.pay();

        // These constructions are rejected unless their types are exempt.
        // new PaymentClient();
        // new OrderValidator();
    }
}
```

For a complete Maven walkthrough, see the [demo guide](./DEMO.md).

## Allowing specific types

Use `allow` for application or third-party types that are safe to construct
directly, such as value objects:

```java
import io.github.injectiononly.annotation.InjectionOnly;

@InjectionOnly(allow = {OrderRequest.class})
public class OrderService {
    public void create() {
        OrderRequest request = new OrderRequest();
    }
}
```

Types under `java.*` and `javax.*` do not need to be listed. For example,
standard collection classes and exceptions are exempt automatically.

## Installation

### Requirements

- JDK 17 or later.
- Maven or Gradle configured to run Java annotation processors.

The examples use version `1.0.0`. The library is not yet published to Maven
Central. Until it is published, build and install it in your local Maven
repository from a checkout:

```shell
mvn clean install
```

After that, the Maven and Gradle examples below can resolve version `1.0.0`
from your local Maven repository. Once a release is published to a package
repository, users can resolve it from that repository instead.

### Maven

Add the library as a dependency and configure it as an annotation processor:

```xml
<dependencies>
    <dependency>
        <groupId>io.github.injectiononly</groupId>
        <artifactId>injection-only-enforcer</artifactId>
        <version>1.0.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>io.github.injectiononly</groupId>
                        <artifactId>injection-only-enforcer</artifactId>
                        <version>1.0.0</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Gradle (Groovy DSL)

Add Maven Local while the artifact is not published:

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly "io.github.injectiononly:injection-only-enforcer:1.0.0"
    annotationProcessor "io.github.injectiononly:injection-only-enforcer:1.0.0"
}
```

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly("io.github.injectiononly:injection-only-enforcer:1.0.0")
    annotationProcessor("io.github.injectiononly:injection-only-enforcer:1.0.0")
}
```

## How it works

The annotation processor checks `new` expressions in each annotated class. If
the constructed type is not in `java.*` or `javax.*` and is not listed in
`allow`, compilation fails with an error.

## Typical use cases

- Spring `@Service`, `@Component`, and `@Repository` classes.
- Application-layer orchestration logic.
- Teams enforcing dependency boundaries and IoC conventions.
- Java projects that want a lightweight compile-time architecture guardrail.

## Limitations

This project focuses on explicit `new` expressions inside the annotated class.
It is a lightweight compile-time rule, not a complete architecture enforcement
framework; it does not detect indirect construction performed by another class
or factory. The rule applies to the annotated type's own constructors, methods,
and initializers; nested and sibling type bodies are not checked unless those
types are independently annotated.

## Contributing

Contributions are welcome:

1. Fork the repository.
2. Create a feature branch.
3. Make your changes and add or update relevant tests.
4. Open a pull request.

## License

This project is licensed under the Apache License 2.0. See the
[LICENSE](./LICENSE) file for details.

## Project status

InjectionOnly Enforcer is a lightweight Java utility for teams that value
explicit dependency injection and want to make that convention enforceable at
compile time.

## Contact

For questions, ideas, or collaboration, open an issue in the GitHub repository.
