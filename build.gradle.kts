plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("com.diffplug.spotless") version "7.2.1"
    id("com.github.spotbugs") version "6.4.2"
    pmd
    checkstyle
}

group = "com.deckassemble"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

val testcontainersVersion = "1.21.3"

val pmdCpd by configurations.creating

dependencies {
    pmdCpd("net.sourceforge.pmd:pmd-cli:7.14.0")
    pmdCpd("net.sourceforge.pmd:pmd-java:7.14.0")

    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jackson")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

spotless {
    java {
        googleJavaFormat("1.28.0").aosp()
        importOrder()
        removeUnusedImports()
        formatAnnotations()
    }
}

pmd {
    toolVersion = "7.14.0"
    ruleSetConfig = resources.text.fromFile("config/pmd/java-ruleset.xml")
    ruleSets = emptyList()
    isConsoleOutput = true
    // Zero-tolerance: violations fail the build (default, stated explicitly).
    isIgnoreFailures = false
}

tasks.withType<org.gradle.api.plugins.quality.Pmd> {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

checkstyle {
    toolVersion = "10.21.1"
    configFile = file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    maxWarnings = 0
}

tasks.withType<Checkstyle> {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// Standards apply Checkstyle to main sources only (tests excluded).
tasks.named("checkstyleTest") {
    enabled = false
}

// Gradle 9 removed the built-in Cpd task; run PMD's CPD CLI directly.
// Token threshold per docs/STATIC_ANALYSIS_STANDARDS.md.
tasks.register<JavaExec>("cpdCheck") {
    group = "verification"
    description = "Copy-paste detection via PMD CPD."
    classpath = pmdCpd
    mainClass.set("net.sourceforge.pmd.cli.PmdCli")
    args("cpd", "--dir", "src/main/java", "--language", "java", "--minimum-tokens", "100")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named("check") {
    dependsOn("spotlessCheck", "cpdCheck")
}

spotbugs {
    toolVersion.set("4.9.8")
    ignoreFailures.set(false)
    excludeFilter.set(file("config/spotbugs/exclude.xml"))
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
    reports {
        create("xml") { required.set(true) }
        create("html") { required.set(true) }
    }
}

// Standards run SpotBugs on main sources only.
tasks.named("spotbugsTest") {
    enabled = false
}
