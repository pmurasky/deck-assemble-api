import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("com.diffplug.spotless") version "7.2.1"
    id("com.github.spotbugs") version "6.4.2"
    id("net.ltgt.errorprone") version "4.3.0"
    id("info.solidsoft.pitest") version "1.19.0"
    jacoco
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

    errorprone("com.google.errorprone:error_prone_core:2.41.0")
    errorprone("com.uber.nullaway:nullaway:0.12.10")

    compileOnly("org.jspecify:jspecify:1.0.0")

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

    pitest("org.pitest:pitest-junit5-plugin:1.2.3")
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

// ErrorProne needs jdk.compiler internals on JDK 16+.
val errorProneJvmArgs =
    listOf(
        "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
    )

tasks.withType<JavaCompile>().configureEach {
    options.forkOptions.jvmArgs?.addAll(errorProneJvmArgs)
    options.errorprone {
        option("NullAway:AnnotatedPackages", "com.deckassemble")
        // JPA populates entity fields reflectively; NullAway's documented exclusion.
        option(
                "NullAway:ExcludedClassAnnotations",
                "jakarta.persistence.Entity,jakarta.persistence.MappedSuperclass")
        check("NullAway", CheckSeverity.ERROR)
    }
}

// NullAway guards production code; tests build fixtures with nulls and reflection on purpose.
tasks.named<JavaCompile>("compileTestJava") {
    options.errorprone.isEnabled.set(false)
}

tasks.named("check") {
    dependsOn("spotlessCheck", "cpdCheck", "jacocoTestCoverageVerification")}

// 0.8.14+ required for Java 25 bytecode.
jacoco { toolVersion = "0.8.14" }

// Dev-only import runner and the Spring entry point carry no testable logic.
val jacocoExcludedClasses = listOf("**/DeckAssembleApplication.*", "**/DevCardImportRunner.*")

val mainClassTrees =
        sourceSets.named("main").map { main ->
            main.output.classesDirs.files.map { dir -> fileTree(dir) { exclude(jacocoExcludedClasses) } }
        }

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    classDirectories.setFrom(files(mainClassTrees))
}

// Standards: >=80% line coverage (docs/TESTING_STANDARDS.md).
// ponytail: gate measures unit+integration together; split test tasks if unit-only number needed.
tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
    classDirectories.setFrom(files(mainClassTrees))
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

// 1.25.8: first PIT line with Java 25 fixes; gradle plugin ships an older default.
// Not wired into `check` — mutation runs are slow; run on demand via `./gradlew pitest`.
pitest {
    pitestVersion.set("1.25.8")
    targetClasses.set(setOf("com.deckassemble.*"))
    // ponytail: same exclusions as the JaCoCo gate — no logic worth mutating.
    excludedClasses.set(
            setOf("com.deckassemble.DeckAssembleApplication",
                    "com.deckassemble.imports.application.DevCardImportRunner"))
    // Integration tests boot Testcontainers per mutation — unit tests only.
    excludedTestClasses.set(setOf("*IntegrationTest", "*AbstractIntegrationTest", "*ArchitectureTest"))
    threads.set(Runtime.getRuntime().availableProcessors())
    outputFormats.set(setOf("XML", "HTML"))
}
