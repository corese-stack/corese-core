import java.time.LocalDate
import java.time.format.DateTimeFormatter

plugins {
    // Core Gradle plugins
    `java-library`                                              // For creating reusable Java libraries

    // Publishing plugins 
    signing                                                     // Signs artifacts for Maven Central
    `maven-publish`                                             // Enables publishing to Maven repositories
    id("com.vanniktech.maven.publish") version "0.34.0"         // Automates Maven publishing tasks
    
    // Tooling plugins
    `jacoco`                                                    // For code coverage reports
    id("com.gradleup.shadow") version "8.3.7"
    id("org.sonarqube") version "7.3.0.8198"                    // SonarQube integration
    id("com.intershop.gradle.javacc") version "5.0.1"           // JavaCC plugin for parsing JavaCC files
    id("antlr")                                                 // Antlr plugin for generating parsers from grammar files
    `eclipse`                                                   // Eclipse plugin for IDE integration
}

// SonarQube configuration

val currentDate: String = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

sonar {
    properties {
        property("sonar.projectKey", "crs-core-new")
        property("sonar.host.url", "https://sonarqube.inria.fr/sonarqube")
        property("sonar.login", System.getenv("SONAR_TOKEN"))
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.projectDate", currentDate)
    }
}

// === Generated sources directories ===
val javaccGeneratedDir = layout.buildDirectory.dir("generated-src/javacc").get().asFile
val antlrGeneratedDir = layout.buildDirectory.dir("generated-src/antlr").get().asFile
val antlrPackageDir = layout.buildDirectory.dir("generated-src/antlr/fr/inria/corese/core/next/generated/antlr").get().asFile

// JavaCC configuration
javacc {
    configs {
        register("sparqlCorese") {
            inputFile = file("src/main/java/fr/inria/corese/core/sparql/triple/javacc1/sparql_corese.jj")
            packageName = "fr.inria.corese.core.sparql.triple.javacc1"
            outputDir = javaccGeneratedDir
        }
    }
}

// Configure source sets to include generated sources
sourceSets {
    main {
        java {
            setSrcDirs(listOf("src/main/java", javaccGeneratedDir, antlrGeneratedDir))
        }
    }
}

// Compile and test the native implementation without historical Corese classes
// on either classpath. No main output or JavaCC sources are included here.
val nextSources = sourceSets.create("next") {
    java.setSrcDirs(listOf("src/main/java", antlrGeneratedDir))
    java.include("fr/inria/corese/core/next/**/*.java")
    resources.setSrcDirs(listOf("src/main/resources"))
    resources.include("META-INF/services/fr.inria.corese.core.next.storage.api.plugin.StoragePlugin")
}

val nextTestSources = sourceSets.create("nextTest") {
    java.setSrcDirs(listOf("src/test/java"))
    java.include("fr/inria/corese/core/next/**/*.java")
    resources.setSrcDirs(listOf("src/test/resources"))
    compileClasspath = nextSources.output + configurations[compileClasspathConfigurationName]
    runtimeClasspath = output + nextSources.output + configurations[runtimeClasspathConfigurationName]
}

// Native sources and tests define their own dependency graph. The historical
// build inherits those dependencies, while the native build never inherits its output or libraries.
configurations.implementation.get().extendsFrom(configurations[nextSources.implementationConfigurationName])
configurations[nextTestSources.implementationConfigurationName]
    .extendsFrom(configurations[nextSources.implementationConfigurationName])
configurations.testImplementation.get().extendsFrom(configurations[nextTestSources.implementationConfigurationName])
configurations.testRuntimeOnly.get().extendsFrom(configurations[nextTestSources.runtimeOnlyConfigurationName])

tasks.named<JavaCompile>(nextSources.compileJavaTaskName) {
    dependsOn("generateGrammarSource")
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-proc:none", "-Xlint:all", "-Werror"))
}

val nextTest by tasks.registering(Test::class) {
    description = "Tests the native implementation with no historical Corese classes available."
    group = "verification"
    testClassesDirs = nextTestSources.output.classesDirs
    classpath = nextTestSources.runtimeClasspath
    useJUnitPlatform()
    systemProperty("corese.next.isolated", "true")
    systemProperty("java.util.logging.config.file", "src/test/resources/logging.properties")
}

val nextJar by tasks.registering(Jar::class) {
    description = "Builds the isolated native classes and their storage plugin descriptor."
    group = "build"
    archiveClassifier.set("next")
    from(nextSources.output)
}

val nextJavadoc by tasks.registering(Javadoc::class) {
    description = "Checks the native API documentation without historical Corese classes."
    group = "documentation"
    source = nextSources.allJava.matching { exclude("**/generated/**") }
    classpath = nextSources.output + nextSources.compileClasspath
    destinationDir = layout.buildDirectory.dir("docs/next-javadoc").get().asFile
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all,-missing", "-quiet")
}

tasks.register("checkNext") {
    description = "Compiles, packages, and tests next independently of historical Corese classes."
    group = "verification"
    dependsOn(nextTest, nextJar, nextJavadoc)
}

tasks.named("check") {
    dependsOn(nextSources.compileJavaTaskName)
}

// Ensure JavaCC generation happens before compilation
tasks.named("compileJava") {
    dependsOn("javaccSparqlCorese")
}


/////////////////////////
// Project metadata    //
/////////////////////////

object Meta {
    // Project coordinates
    const val groupId = "fr.inria.corese"
    const val artifactId = "corese-core"
    const val version = "4.6.4"

    // Project description
    const val desc = "Corese is a Semantic Web Factory (triple store and SPARQL endpoint) implementing RDF, RDFS, SPARQL 1.1 Query and Update, Shacl. STTL. LDScript."
    const val githubRepo = "corese-stack/corese-core"

    // License information
    const val license = "CeCILL-C License"
    const val licenseUrl = "https://opensource.org/licenses/CeCILL-C"
}

////////////////////////
// Project settings  //
///////////////////////

// Java compilation settings
java {
    withJavadocJar()                             // Include Javadoc JAR in publications
    withSourcesJar()                             // Include sources JAR in publications
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

jacoco {
    toolVersion = "0.8.15"
}

/////////////////////////
// Dependency settings //
/////////////////////////

// Define repositories to resolve dependencies from
repositories {
    mavenLocal()    // First, check the local Maven repository
    mavenCentral()  // Then, check Maven Central
}

dependencies {
    // === Standalone native runtime ===
    add(nextSources.implementationConfigurationName, "org.slf4j:slf4j-api:2.0.17")
    add(nextSources.implementationConfigurationName, "org.antlr:antlr4-runtime:4.13.2")
    add(nextSources.implementationConfigurationName, "com.apicatalog:titanium-json-ld:1.6.0")
    add(nextSources.implementationConfigurationName, "com.apicatalog:titanium-rdf-api:1.0.0")
    add(nextSources.implementationConfigurationName, "org.eclipse.parsson:parsson:1.1.7")
    add(nextSources.implementationConfigurationName, "jakarta.json:jakarta.json-api:2.1.3")
    add(nextSources.implementationConfigurationName, "com.typesafe:config:1.4.3")
    // === Logging ===
    // As a library, corese-core should only depend on the SLF4J API.
    // The actual logging implementation (Logback, Log4j2, etc.) should be chosen
    // by the consuming application (e.g., corese-command).
    api("org.slf4j:slf4j-api:2.0.17")                                                  // Logging API only (SLF4J)

    // === Core dependencies ===
    api("fr.com.hp.hpl.jena.rdf.arp:arp:2.2.b")                                        // RDF/XML parser (Jena ARP)
    implementation("fr.inria.corese.org.semarglproject:semargl-rdfa:0.7.2")            // RDFa parser (Semargl)
    implementation("com.github.jsonld-java:jsonld-java:0.13.4")                        // JSON-LD processing

    // === Antlr dependencies ===
    antlr("org.antlr:antlr4:4.13.2")                                                   // Antlr for parsing (ANTLR 4)

    // === HTTP and XML ===
    implementation("org.glassfish.jersey.core:jersey-client:3.1.10")                   // HTTP client (Jersey)
    implementation("org.glassfish.jersey.inject:jersey-hk2:3.1.10")                    // Dependency injection for Jersey
    implementation("com.sun.activation:jakarta.activation:2.0.1")                      // MIME type handling (Jakarta Activation)

    // === XML parsing ===
    implementation("com.typesafe.akka:akka-stream_2.13:2.6.20")                        // Akka Streams for reactive streams processing
    implementation("com.lightbend.akka:akka-stream-alpakka-xml_2.13:3.0.4")            // Alpakka XML for XML processing with Akka Streams

    // HTML parsing for RDFa
    implementation("org.jsoup:jsoup:1.21.2")

    // === Utilities ===
    implementation("org.apache.commons:commons-text:1.13.1")                           // Text manipulation utilities (Commons Text)
    implementation("org.json:json:20250517")                                           // JSON processing

    // === Test dependencies ===
    add(nextTestSources.implementationConfigurationName, platform("org.junit:junit-bom:5.13.2"))
    add(nextTestSources.implementationConfigurationName, "org.junit.jupiter:junit-jupiter:5.13.2")
    add(nextTestSources.runtimeOnlyConfigurationName, "org.junit.platform:junit-platform-launcher:1.13.2")
    add(nextTestSources.implementationConfigurationName, "org.mockito:mockito-core:5.18.0")
    add(nextTestSources.implementationConfigurationName, "org.mockito:mockito-junit-jupiter:5.18.0")
    add(nextTestSources.runtimeOnlyConfigurationName, "org.apache.logging.log4j:log4j-core:2.25.0")
    add(nextTestSources.runtimeOnlyConfigurationName, "org.apache.logging.log4j:log4j-slf4j2-impl:2.25.0")

}

/////////////////////////
// Publishing settings //
/////////////////////////

mavenPublishing {
    coordinates(Meta.groupId, Meta.artifactId, Meta.version)

    pom {
        name.set(Meta.artifactId)
        description.set(Meta.desc)
        url.set("https://github.com/${Meta.githubRepo}")
        licenses {
            license {
                name.set(Meta.license)
                url.set(Meta.licenseUrl)
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("OlivierCorby")
                name.set("Olivier Corby")
                email.set("olivier.corby@inria.fr")
                url.set("http://www-sop.inria.fr/members/Olivier.Corby")
                organization.set("Inria")
                organizationUrl.set("http://www.inria.fr/")
            }
            developer {
                id.set("remiceres")
                name.set("Rémi Cérès")
                email.set("remi.ceres@inria.fr")
                url.set("http://www-sop.inria.fr/members/Remi.Ceres")
                organization.set("Inria")
                organizationUrl.set("http://www.inria.fr/")
            }
            developer {
                id.set("pierremaillot")
                name.set("Pierre Maillot")
                email.set("pierre.maillot@inria.fr")
                url.set("https://maillpierre.github.io/personal-page/")
                organization.set("Inria")
                organizationUrl.set("http://www.inria.fr/")
            }
        }
        scm {
            url.set("https://github.com/${Meta.githubRepo}/")
            connection.set("scm:git:git://github.com/${Meta.githubRepo}.git")
            developerConnection.set("scm:git:ssh://git@github.com/${Meta.githubRepo}.git")
        }
        issueManagement {
            url.set("https://github.com/${Meta.githubRepo}/issues")
        }
    }

    publishToMavenCentral()
    
    // Only sign publications when GPG keys are available (CI environment)
    if (project.hasProperty("signingInMemoryKey") || project.hasProperty("signing.keyId")) {
        signAllPublications()
    }
}

/////////////////////////
// Task configuration  //
/////////////////////////

// Set UTF-8 encoding for Java compilation tasks
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.add("-Xlint:none")
}

// Configure Javadoc tasks with UTF-8 encoding and disable failure on error.
// This ensures that Javadoc generation won't fail due to minor issues.
tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    isFailOnError = false
    // Configure Javadoc tasks to disable doclint warnings.
    (options as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
}


// Configure the shadow JAR task to include dependencies in the output JAR.
// This creates a single JAR file with all dependencies bundled.
// The JAR file is named with the classifier "jar-with-dependencies".
tasks {
    shadowJar {
        this.archiveClassifier = "jar-with-dependencies"
    }
}

// Configure the build task to depend on the shadow JAR task.
// This ensures that the shadow JAR is built when the project is built.
tasks.build {
    dependsOn(tasks.shadowJar)
}

// Configure Jacoco test report task to depend on the test task,
// so reports are generated after tests run successfully.
tasks.jacocoTestReport {
    dependsOn(tasks.test)

    // Enable XML reporting for Jacoco to allow further processing or CI integration.
    reports {
        xml.required.set(true)
    }
}

// Set the test task to be followed by Jacoco report generation.
// This ensures that test coverage reports are always generated after tests.
tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    systemProperty("java.util.logging.config.file", "src/test/resources/logging.properties")
    finalizedBy(tasks.jacocoTestReport)
}

// Ensure that all local Maven publication tasks depend on signing tasks.
// This guarantees that artifacts are signed before they are published locally.
tasks.withType<PublishToMavenLocal>().configureEach {
    dependsOn(tasks.withType<Sign>())
}

// Ensure that all remote Maven publication tasks depend on signing tasks.
// This guarantees that artifacts are signed before they are published to Maven repositories.
tasks.withType<PublishToMavenRepository>().configureEach {
    dependsOn(tasks.withType<Sign>())
}

// === Antlr generated sources configuration ===

// Configure the Antlr task to generate parser code with specific arguments
tasks.named<AntlrTask>("generateGrammarSource") {
    arguments.addAll(listOf("-visitor", "-long-messages", "-package", "fr.inria.corese.core.next.generated.antlr"))
    outputDirectory = antlrPackageDir
    setSource(fileTree("src/main/antlr") {
        include("**/*.g4")
    })
    outputs.dir(antlrPackageDir)
}

// Ensure Java compilation depends on both JavaCC and Antlr code generation
tasks.named("compileJava") {
    dependsOn("generateGrammarSource", "javaccSparqlCorese")
}

// Ensure sources JAR includes generated sources and depends on code generation
tasks.named<Jar>("sourcesJar") {
    dependsOn("generateGrammarSource", "generateTestGrammarSource", "javaccSparqlCorese")
    from(javaccGeneratedDir)
    from(antlrGeneratedDir)
    includeEmptyDirs = false
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Clean up generated sources on clean
tasks.clean {
    doLast {
        delete(javaccGeneratedDir)
        delete(antlrGeneratedDir)
    }
}

// Ensure generated directories exist before generation
tasks.register("createGeneratedDirs") {
    doLast {
        javaccGeneratedDir.mkdirs()
        antlrGeneratedDir.mkdirs()
        antlrPackageDir.mkdirs()
    }
}

// Make generation tasks depend on directory creation
tasks.named("generateGrammarSource") {
    dependsOn("createGeneratedDirs")
}

tasks.named("javaccSparqlCorese") {
    dependsOn("createGeneratedDirs")
}

// Standalone storage measurements use the existing test runtime, without timing JUnit.
tasks.register("storageBenchmarkClasspath") {
    group = "verification"
    description = "Writes the standalone storage benchmark runtime classpath."
    dependsOn(tasks.testClasses)
    doLast {
        layout.buildDirectory.file("benchmarks/classpath.txt").get().asFile.apply {
            parentFile.mkdirs()
            writeText(sourceSets.test.get().runtimeClasspath.asPath)
        }
    }
}
