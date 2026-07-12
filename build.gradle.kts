plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "6.25.0"
    id("checkstyle")
    id("com.github.spotbugs") version "6.0.26"
}

group = "tn.inovexahub"
version = "0.0.1-SNAPSHOT"
description = "hardware_store"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    compileOnly("org.projectlombok:lombok")
    runtimeOnly("org.postgresql:postgresql")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testCompileOnly("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

spotless {
    java {
        googleJavaFormat("1.35.0")
        importOrder()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

checkstyle {
    toolVersion = "10.18.2"
    configFile = file("${rootProject.projectDir}/config/checkstyle/checkstyle.xml")
}

spotbugs {
    ignoreFailures = true
    showStackTraces = true
    showProgress = true
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.LOW)
}

dependencies {
    spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.13.0")
}

tasks.withType<Checkstyle>().configureEach {
    reports.xml.required.set(true)
    reports.html.required.set(true)
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    reports.create("xml").required.set(true)
    reports.create("html").required.set(true)
}

tasks.register("lint") {
    group = "verification"
    description = "Run linting checks (checkstyle + spotbugs)"
    dependsOn("checkstyleMain", "checkstyleTest", "spotbugsMain", "spotbugsTest")
}

tasks.register("format") {
    group = "formatting"
    description = "Format code using Spotless"
    dependsOn("spotlessApply")
}
