plugins {
    java
    application
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.guava)
    testImplementation(libs.junit.jupiter)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)
    implementation(libs.netty.transport)
    implementation(libs.netty.handler)
    implementation(libs.netty.codec)
    implementation(libs.netty.buffer)
    implementation(libs.netty.common)
    implementation(libs.netty.resolver)
    implementation(libs.netty.codec.http)
}

tasks.register<Jar>("fatJar") {
    archiveBaseName.set("rps-server-all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "com.github.xomarnd.rps.server.ServerApp"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("rps-server-all")
    archiveClassifier.set("")
    archiveVersion.set("")
}

application {
    mainClass.set("com.github.xomarnd.rps.server.ServerApp")
}
