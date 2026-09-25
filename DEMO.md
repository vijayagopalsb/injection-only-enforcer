# Create and run the demo project

This guide walks through creating a small Maven application that uses
InjectionOnly Enforcer. The example deliberately fails compilation when it
constructs an application type with `new` inside an `@InjectionOnly` class.

## Requirements

- JDK 17 or later
- Maven 3.6 or later
- A local checkout of this repository

## 1. Build and install the enforcer locally

From the root of the `injection-only-enforcer` checkout, run:

```shell
mvn clean install
```

This builds and tests the library, then installs its `1.0.0` artifact into
your local Maven repository. The demo below uses that local artifact. If the
library version changes, use the new version in both the dependency and
annotation processor configuration in the demo's `pom.xml`.

This demo requires no separate demo repository: create the files locally as
described below, then run the commands from the demo project directory.

## 2. Create a Maven demo project

Create this directory and file structure:

```text
injection-only-demo/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── io/
                └── github/
                    └── injectiononly/
                        └── demo/
                            ├── DemoService.java
                            └── Helper.java
```

Create `pom.xml` in the `injection-only-demo` directory:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.github.injectiononly.demo</groupId>
    <artifactId>injection-only-demo</artifactId>
    <version>1.0.0</version>

    <properties>
        <maven.compiler.release>17</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <injection-only-enforcer.version>1.0.0</injection-only-enforcer.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>io.github.injectiononly</groupId>
            <artifactId>injection-only-enforcer</artifactId>
            <version>${injection-only-enforcer.version}</version>
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
                            <version>${injection-only-enforcer.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

The `provided` dependency makes the annotation available when compiling the
demo without adding the enforcer to the application's runtime dependencies.
`annotationProcessorPaths` explicitly tells Maven's compiler plugin to run the
processor.

## 3. Add the demo classes

Create `src/main/java/io/github/injectiononly/demo/Helper.java`:

```java
package io.github.injectiononly.demo;

public class Helper {
}
```

Create `src/main/java/io/github/injectiononly/demo/DemoService.java`:

```java
package io.github.injectiononly.demo;

import io.github.injectiononly.annotation.InjectionOnly;

import java.util.ArrayList;
import java.util.List;

@InjectionOnly
public class DemoService {

    public void test() {
        Helper helper = new Helper(); // Compile-time violation

        StringBuffer buffer = new StringBuffer(); // Allowed: java.* type
        String name = new String("Vijay");        // Allowed: java.* type
        List<String> names = new ArrayList<>();   // Allowed: java.* types
    }
}
```

## 4. Run the demonstration

From the `injection-only-demo` directory, run:

```shell
mvn clean compile
```

Compilation is expected to fail on `new Helper()` with a diagnostic similar to:

```text
Manual instantiation using 'new' is not allowed in @InjectionOnly classes.
```

The other constructions compile because their types are in `java.*`. This is
the intended demonstration: the annotation processor reports a compile-time
error for a disallowed manual construction.

## 5. Try the allowed alternatives

### Pass the collaborator into the class

Replace `DemoService.java` with the following to make the dependency an input
instead of constructing it inside the class:

```java
package io.github.injectiononly.demo;

import io.github.injectiononly.annotation.InjectionOnly;

@InjectionOnly
public class DemoService {
    private final Helper helper;

    public DemoService(Helper helper) {
        this.helper = helper;
    }
}
```

Run `mvn clean compile` again. It should succeed. A dependency injection
framework or other composition root can provide the `Helper` instance; this
minimal demo does not include or configure Spring.

### Explicitly allow a type

If direct construction is intentional, add the type to the annotation's
allowlist:

```java
package io.github.injectiononly.demo;

import io.github.injectiononly.annotation.InjectionOnly;

@InjectionOnly(allow = {Helper.class})
public class DemoService {
    public void test() {
        Helper helper = new Helper(); // Allowed by the annotation allowlist
    }
}
```

Run `mvn clean compile` again; this version should also compile.

## What this demo does and does not show

The processor checks explicit `new` expressions inside classes annotated with
`@InjectionOnly`. It does not create an IoC container, inject dependencies, or
detect indirect construction performed by another class or factory. The
annotation is a source-retention compile-time rule.
