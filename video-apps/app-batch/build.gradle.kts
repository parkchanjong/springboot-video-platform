dependencies {
    implementation(project(":video-core:core-port"))

    implementation("org.springframework.boot:spring-boot-starter-batch")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.batch:spring-batch-test")
}

val appMainClassName = "com.videoservice.manager.VideoInformationManagerApplication"
tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    mainClass.set(appMainClassName)
    archiveClassifier.set("boot")
}