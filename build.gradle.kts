plugins {
    // Core Gradle plugins
    `java-library`                                              // For creating reusable Java libraries

    // Publishing plugins 
    signing                                                     // Signs artifacts for Maven Central
    `maven-publish`                                             // Enables publishing to Maven repositories
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0" // Automates Nexus publishing
    
    // Tooling plugins
    `jacoco`                                                    // For code coverage reports
    id("org.gradlex.extra-java-module-info") version "1.9"      // Module metadata for JARs without module info
    id("com.gradleup.shadow") version "8.3.5"                   // Bundles dependencies into a single JAR
}

/////////////////////////
// Project metadata    //
/////////////////////////

object Meta {
    // Project coordinates
    const val groupId = "fr.inria.corese"
    const val artifactId = "corese-core"
    const val version = "4.6.2"

    // Project description
    const val desc = "Corese is a Semantic Web Factory (triple store and SPARQL endpoint) implementing RDF, RDFS, SPARQL 1.1 Query and Update, Shacl. STTL. LDScript."
    const val githubRepo = "corese-stack/corese-core"
  
    // License information
    const val license = "CeCILL-C License"
    const val licenseUrl = "https://opensource.org/licenses/CeCILL-C"
  
    // Sonatype OSSRH publishing settings
    const val release = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
    const val snapshot = "https://oss.sonatype.org/content/repositories/snapshots/"
}

////////////////////////
// Project settings  //
///////////////////////

// Java compilation settings
java {
    withJavadocJar()                             // Include Javadoc JAR in publications
    withSourcesJar()                             // Include sources JAR in publications
    sourceCompatibility = JavaVersion.VERSION_11 // Configure minimum Java version
}

/////////////////////////
// Dependency settings //
/////////////////////////

// Define repositories to resolve dependencies from
repositories {
    mavenLocal()    // First, check the local Maven repository
    mavenCentral()  // Then, check Maven Central
}

// Define dependencies
dependencies {
    val jersey_version = "3.0.4"
    val semargl_version = "0.7.1"

    api("fr.com.hp.hpl.jena.rdf.arp:arp:2.2.b")                                          // RDF parser to handle RDF/XML files
    implementation("org.apache.commons:commons-text:1.10.0")                             // Text manipulation utilities (Apache Commons)
    implementation("commons-lang:commons-lang:2.4")                                      // Basic functions for handling strings and objects
    implementation("org.json:json:20240303")                                             // JSON processing for reading, writing, and manipulating JSON objects
    implementation("fr.inria.lille.shexjava:shexjava-core:1.0")                          // ShEx implementation for RDF validation
    implementation("org.glassfish.jersey.core:jersey-client:${jersey_version}")          // REST client for creating HTTP requests
    implementation("org.glassfish.jersey.inject:jersey-hk2:${jersey_version}")           // Dependency injection for Jersey
    implementation("javax.xml.bind:jaxb-api:2.3.1")                                      // JAXB API for converting between Java objects and XML
    implementation("fr.inria.corese.org.semarglproject:semargl-rdfa:${semargl_version}") // RDFa parser to extract RDF metadata from HTML
    implementation("fr.inria.corese.org.semarglproject:semargl-core:${semargl_version}") // Semargl core for RDF parsing and transformation
    implementation("com.github.jsonld-java:jsonld-java:0.13.4")                          // JSON-LD processing for Linked Data in JSON format
    implementation("junit:junit:4.13.2")                                                 // JUnit framework for unit testing in Java
    api("org.slf4j:slf4j-api:2.0.9")                                                     // Simple Logging Facade for Java (SLF4J)
}

// Configure extra Java module information for dependencies without module-info
extraJavaModuleInfo {
    // If a library is missing module info, the build process will not fail.
    failOnMissingModuleInfo.set(false)

    // Map automatic module names for non-modular libraries.
    automaticModule("fr.com.hp.hpl.jena.rdf.arp:arp", "arp") // Module for Jena RDF ARP
    automaticModule("com.github.jsonld-java:jsonld-java", "jsonld.java") // Module for JSON-LD Java
    automaticModule("commons-lang:commons-lang", "commons.lang") // Module for Commons Lang
    automaticModule("fr.inria.lille.shexjava:shexjava-core", "shexjava.core")
    automaticModule("org.eclipse.rdf4j:rdf4j-model", "rdf4j.model")
}


/////////////////////////
// Publishing settings //
/////////////////////////

// Publication configuration for Maven repositories
publishing {
    publications {
        create<MavenPublication>("mavenJava") {

            // Configure the publication to include JAR, sources, and Javadoc
            from(components["java"])

            // Configures version mapping to control how dependency versions are resolved 
            // for different usage contexts (API and runtime).
            versionMapping {
                // Defines version mapping for Java API usage.
                // Sets the version to be resolved from the runtimeClasspath configuration.
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }

                // Defines version mapping for Java runtime usage.
                // Uses the result of dependency resolution to determine the version.
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }

            // Configure the publication metadata
            groupId = Meta.groupId
            artifactId = Meta.artifactId
            version = Meta.version

            pom {
                name.set(Meta.artifactId)
                description.set(Meta.desc)
                url.set("https://github.com/${Meta.githubRepo}")
                licenses {
                    license {
                        name.set(Meta.license)
                        url.set(Meta.licenseUrl)
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
                    url.set("https://github.com/${Meta.githubRepo}.git")
                    connection.set("scm:git:git://github.com/${Meta.githubRepo}.git")
                    developerConnection.set("scm:git:git://github.com/${Meta.githubRepo}.git")
                }
                issueManagement {
                    url.set("https://github.com/${Meta.githubRepo}/issues")
                }
            }
        }
    }
}

// Configure artifact signing
signing {
    // Retrieve the GPG signing key and passphrase from environment variables for secure access.
    val signingKey = providers.environmentVariable("GPG_SIGNING_KEY")
    val signingPassphrase = providers.environmentVariable("GPG_SIGNING_PASSPHRASE")

    // Sign the publications if the GPG signing key and passphrase are available.
    if (signingKey.isPresent && signingPassphrase.isPresent) {
        useInMemoryPgpKeys(signingKey.get(), signingPassphrase.get())
        sign(publishing.publications)
    }
}

// Configure Nexus publishing and credentials
nexusPublishing {
    repositories {
        // Configure Sonatype OSSRH repository for publishing.
        sonatype {
            // Retrieve Sonatype OSSRH credentials from environment variables.
            val ossrhUsername = providers.environmentVariable("OSSRH_USERNAME")
            val ossrhPassword = providers.environmentVariable("OSSRH_PASSWORD")

            // Set the credentials for Sonatype OSSRH if they are available.
            if (ossrhUsername.isPresent && ossrhPassword.isPresent) {
                username.set(ossrhUsername.get())
                password.set(ossrhPassword.get())
            }

            // Define the package group for this publication, typically following the group ID.
            packageGroup.set(Meta.groupId)
        }
    }
}

/////////////////////////
// Task configuration  //
/////////////////////////

// Set UTF-8 encoding for Java compilation tasks
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:none")
}

// Configure Javadoc tasks with UTF-8 encoding and disable failure on error.
// This ensures that Javadoc generation won't fail due to minor issues.
tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
    isFailOnError = false
}

// Configure the shadow JAR task to include dependencies in the output JAR.
// This creates a single JAR file with all dependencies bundled.
// The JAR file is named with the classifier "jar-with-dependencies".
tasks {
    shadowJar {
        this.archiveClassifier = "jar-with-dependencies"
            }
}

// Configure Javadoc tasks to disable doclint warnings.
tasks {
    javadoc {
        options {
            (this as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
        }
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
    // testLogging {
    //     events("passed", "skipped", "failed") // Affiche les résultats des tests
    //     showStandardStreams = true           // Affiche les sorties console des tests
    // }
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
