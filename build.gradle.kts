plugins {
    id("qupath-conventions")
}

qupathExtension {
    name = "qupath-extension-adaptive-whole-tissue"
    group = "org.sangha.qupath"
    version = "0.1.0-SNAPSHOT"
    description = "Exact and adaptive whole-tissue pixel-classifier measurement for QuPath"
    automaticModule = "org.sangha.qupath.adaptive"
}

dependencies {
    implementation(libs.bundles.qupath)
    implementation(libs.bundles.logging)
    testImplementation(libs.junit)
}
