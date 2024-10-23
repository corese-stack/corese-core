plugins {
    `java-library`
    `maven-publish`
    `jacoco`
    id("org.gradlex.extra-java-module-info") version "1.8"
    id("com.gradleup.shadow") version "8.3.1"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    signing
}

group = "fr.inria.corese"
version = "4.6.0-SNAPSHOT"
description = "corese-core"

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

signing {
    val signingKey = providers
        .environmentVariable("GPG_SIGNING_KEY")
        .forUseAtConfigurationTime()

    val signingPassphrase = providers
        .environmentVariable("GPG_SIGNING_PASSPHRASE")
        .forUseAtConfigurationTime()
    
    if (signingKey.isPresent && signingPassphrase.isPresent) {
        useInMemoryPgpKeys(signingKey.get(), signingPassphrase.get())
        val extension = extensions.getByName("publishing") as PublishingExtension
        sign(extension.publications)
    }
}

object Meta {
  const val desc = "Corese is a Semantic Web Factory (triple store and SPARQL endpoint) implementing RDF, RDFS, SPARQL 1.1 Query and Update, Shacl. STTL. LDScript."
  const val license = "CeCILL-C License"
  const val licenseUrl = "https://opensource.org/licenses/CeCILL-C"
  const val githubRepo = "corese-stack/corese-core"
  const val release = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
  const val snapshot = "https://oss.sonatype.org/content/repositories/snapshots/"
}



publishing {

  publications {
    create<MavenPublication>("maven") {

      groupId = project.group.toString()
      artifactId = project.name
      version = project.version.toString()

      from(components["java"])
      versionMapping {
          usage("java-api") {
              fromResolutionOf("runtimeClasspath")
          }
          usage("java-runtime") {
              fromResolutionResult()
          }
      }
      artifact(tasks["sourcesJar"])
      artifact(tasks["javadocJar"])
      pom {
        name.set(project.name)
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
            url.set("http://www-sop.inria.fr/members/Pierre.Maillot")
            organization.set("Inria")
            organizationUrl.set("http://www.inria.fr/")
          }
        }
        scm {
          url.set(
            "https://github.com/${Meta.githubRepo}.git"
          )
          connection.set(
            "scm:git:git://github.com/${Meta.githubRepo}.git"
          )
          developerConnection.set(
            "scm:git:git://github.com/${Meta.githubRepo}.git"
          )
        }
        issueManagement {
          url.set("https://github.com/${Meta.githubRepo}/issues")
        }
      }
    }
  }
}

nexusPublishing {
  repositories {
    sonatype {
      nexusUrl.set(uri(Meta.release))
      snapshotRepositoryUrl.set(uri(Meta.snapshot))
      val ossrhUsername = providers
        .environmentVariable("OSSRH_USERNAME")
        .forUseAtConfigurationTime()
      val ossrhPassword = providers
        .environmentVariable("OSSRH_PASSWORD")
        .forUseAtConfigurationTime()
      if (ossrhUsername.isPresent && ossrhPassword.isPresent) {
        username.set(ossrhUsername.get())
        password.set(ossrhPassword.get())
      }
    }
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



// publishing {
//     publications.create<MavenPublication>("maven") {
//         from(components["java"])
//         versionMapping {
//             usage("java-api") {
//                 fromResolutionOf("runtimeClasspath")
//             }
//             usage("java-runtime") {
//                 fromResolutionResult()
//             }
//         }
//     }
// }

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
