plugins {
    `java-library`
    `maven-publish`
    `jacoco`
    id("org.gradlex.extra-java-module-info") version "1.8"
    id("com.gradleup.shadow") version "8.3.1"
    signing
}

java {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_11
}
tasks.withType<Javadoc> { isFailOnError = false }

repositories {
    mavenLocal()
    maven {
        // change URLs to point to your repos, e.g. http://my.org/repo
        val releasesRepoUrl = uri("https://repo.maven.apache.org/maven2/")
        val snapshotsRepoUrl = uri(layout.buildDirectory.dir("repos/snapshots"))
        url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
    }
}

dependencies {
    val jersey_version = "3.0.4"
    val semargl_version = "0.7.1"
//    implementation("org.slf4j:slf4j-api:1.8.0-beta2")


    implementation("org.apache.commons:commons-text:1.10.0")
    api("fr.com.hp.hpl.jena.rdf.arp:arp:2.2.b")
    implementation("commons-lang:commons-lang:2.4")
//    constraints {
//        api("org.slf4j:slf4j-api:1.8.0-beta2")
//    }
    implementation("org.json:json:20240303")
    implementation("fr.inria.lille.shexjava:shexjava-core:1.0")
    implementation("org.glassfish.jersey.core:jersey-client:${jersey_version}")
    implementation("org.glassfish.jersey.inject:jersey-hk2:${jersey_version}")
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("fr.inria.corese.org.semarglproject:semargl-rdfa:${semargl_version}")
    implementation("fr.inria.corese.org.semarglproject:semargl-core:${semargl_version}")
    implementation("com.github.jsonld-java:jsonld-java:0.13.4")
//    constraints {
//        api("org.apache.logging.log4j:log4j-slf4j18-impl:2.18.0")
//    }
    implementation("junit:junit:4.13.2")
}

group = "fr.inria.corese"
version = "4.6.0-SNAPSHOT"
description = "corese-core"

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
        versionMapping {
            usage("java-api") {
                fromResolutionOf("runtimeClasspath")
            }
            usage("java-runtime") {
                fromResolutionResult()
            }
        }
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}

tasks {
    shadowJar {
        this.archiveClassifier = "jar-with-dependencies"
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

extraJavaModuleInfo {
    failOnMissingModuleInfo.set(false)
    automaticModule("fr.com.hp.hpl.jena.rdf.arp:arp", "arp")
    automaticModule("com.github.jsonld-java:jsonld-java", "jsonld.java")
    automaticModule("commons-lang:commons-lang", "commons.lang")
    automaticModule("fr.inria.lille.shexjava:shexjava-core", "shexjava.core")
    automaticModule("org.eclipse.rdf4j:rdf4j-model", "rdf4j.model")
}
