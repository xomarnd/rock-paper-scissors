plugins {
    java
    application
}

dependencies {
    implementation(libs.guava)
    testImplementation(libs.junit.jupiter)
}

application {
    mainClass.set("com.github.xomarnd.rps.client.ClientApp")
}