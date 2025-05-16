plugins {
    java
    application
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

application {
    mainClass.set("com.github.xomarnd.rps.server.ServerApp")
}
