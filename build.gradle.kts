/*
 * This file was generated by the Gradle 'init' task.
 */

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
    api(libs.org.slf4j.slf4j.api)
    api(libs.org.apache.commons.commons.text)
    api(libs.fr.com.hp.hpl.jena.rdf.arp.arp)
    api(libs.commons.lang.commons.lang)
    api(libs.org.json.json)
    api(libs.fr.inria.lille.shexjava.shexjava.core)
    api(libs.org.glassfish.jersey.core.jersey.client)
    api(libs.org.glassfish.jersey.inject.jersey.hk2)
    api(libs.javax.xml.bind.jaxb.api)
    api(libs.fr.inria.corese.org.semarglproject.semargl.rdfa)
    api(libs.fr.inria.corese.org.semarglproject.semargl.core)
    api(libs.com.github.jsonld.java.jsonld.java)
    runtimeOnly(libs.org.apache.logging.log4j.log4j.slf4j18.impl)
    testImplementation(libs.junit.junit)
}

group = "fr.inria.corese"
version = "5.0.0-SNAPSHOT"
description = "corese-core"
java.sourceCompatibility = JavaVersion.VERSION_11

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
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