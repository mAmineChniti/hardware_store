plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "6.25.0"
    id("checkstyle")
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
    implementation(platform("me.paulschwarz:spring-dotenv-bom:5.1.0"))
    implementation("me.paulschwarz:springboot4-dotenv")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
    implementation("org.apache.pdfbox:pdfbox:3.0.8")
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    compileOnly("org.projectlombok:lombok")
    runtimeOnly("org.postgresql:postgresql")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.h2database:h2")
    testCompileOnly("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:all")
    options.compilerArgs.add("-Xlint:-processing")
    options.encoding = "UTF-8"
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

tasks.withType<Checkstyle>().configureEach {
    reports.xml.required.set(true)
    reports.html.required.set(true)
}

tasks.register("lint") {
    group = "verification"
    description = "Run linting checks (checkstyle)"
    dependsOn("checkstyleMain", "checkstyleTest")
}

tasks.register("format") {
    group = "formatting"
    description = "Format code using Spotless"
    dependsOn("spotlessApply")
}
