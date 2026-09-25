
# InjectionOnly Enforcer

Compile-time guardrails for dependency injection discipline in Java.

InjectionOnly Enforcer helps teams keep their classes IoC-first by discouraging manual object creation inside classes marked with `@InjectionOnly`. If a class is designed to receive dependencies through dependency injection, this library helps enforce that rule at compile time.

This is especially useful in Spring-based applications, where services, repositories, clients, and other managed components should not construct their own collaborators manually.

## Why this project exists

In large Java applications, especially with Spring, it is easy for a service to drift into this pattern:

```java
@Service
public class UserService {
    private final UserRepository repository = new UserRepository();
}

or:

public class OrderService {
    public void create() {
        PaymentClient client = new PaymentClient();
        client.pay();
    }
}

These patterns often hide dependency lifecycle problems, make code harder to test, and reduce the value of inversion of control.

 @InjectionOnly  gives you a simple compile-time rule:

• If a class is marked  @InjectionOnly 
• then manual construction using  new  is rejected
• except for explicitly allowed types or standard JDK types

This keeps dependency construction centralized and predictable.

Features

• Enforces  @InjectionOnly  on annotated classes
• Rejects manual  new  inside the annotated class
• Allows JDK types automatically
• Supports an explicit allowlist via  @InjectionOnly(allow = {...}) 
• Works as a Java annotation processor
• Lightweight and easy to plug into Maven or Gradle builds

Annotation

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

Example

import io.github.injectiononly.annotation.InjectionOnly;

@InjectionOnly
public class OrderService {

    private final PaymentClient paymentClient;

    public OrderService(PaymentClient paymentClient) {
        this.paymentClient = paymentClient;
    }

    public void createOrder() {
        // allowed: dependency passed in via constructor
        paymentClient.pay();

        // forbidden: manual construction inside an @InjectionOnly class
        // new PaymentClient();

        // also forbidden for many custom types
        // new OrderValidator();
    }
}

This compiles cleanly only if the dependency is received via injection rather than created manually.

Allow list

Sometimes a type is a plain value object or utility that is acceptable to construct directly, even in an injection-only class.

import io.github.injectiononly.annotation.InjectionOnly;

@InjectionOnly(allow = { OrderRequest.class, UUID.class })
public class OrderService {

    public void create() {
        OrderRequest request = new OrderRequest();
        // allowed because declared in allow()
    }
}

The processor also exempts standard JDK types automatically, such as:

•  java.lang.String 
•  java.util.ArrayList 
•  java.lang.StringBuilder 
• standard runtime exceptions like  IllegalArgumentException 

Installation

Maven

Add the dependency and register the processor:

<dependencies>
    <dependency>
        <groupId>io.github.injectiononly</groupId>
        <artifactId>injection-only-enforcer</artifactId>
        <version>1.0.0</version>
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

Gradle

dependencies {
    implementation "io.github.injectiononly:injection-only-enforcer:1.0.0"
    annotationProcessor "io.github.injectiononly:injection-only-enforcer:1.0.0"
}

How it works

This library is a Java annotation processor.

When the compiler sees  @InjectionOnly , it walks the annotated class and checks for manual instantiation via  new . If the constructed type is:

• not a JDK type
• not explicitly whitelisted in  allow() 

then it raises a compiler error.

In other words, it turns architectural intent into a build-time safeguard.

Typical use cases

• Spring  @Service ,  @Component ,  @Repository  classes
• application-layer orchestration logic
• factories or orchestrators that should be IoC-driven
• teams enforcing clean dependency boundaries
• architecture governance in larger Java codebases

Benefits

• Fail fast during compilation
• Avoid runtime dependency surprises
• Make IoC rules visible and enforceable
• Improve testability and clarity
• Encourage a cleaner architecture

Limitations

This project is intentionally focused on the core rule:

• it enforces manual construction inside  @InjectionOnly  classes
• it is designed as a lightweight compile-time guardrail
• it is not a full architectural framework

It is best used as a practical project-level enforcement tool, not as a replacement for a broader design review process.

Contributing

Contributions are welcome.

If you want to improve the project:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add or update relevant tests
5. Open a pull request

License

This project is licensed under the Apache License 2.0.

See the LICENSE file for details.

Project status

This project is currently designed as a lightweight open-source tooling library for Java applications that value explicit dependency injection discipline.

It is especially suitable for teams that want stronger compile-time rules without introducing heavy architectural tooling.

Contact

For questions, ideas, or collaboration, please open an issue or contact the maintainer through the GitHub project.