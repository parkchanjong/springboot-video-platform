dependencies {
    implementation(project(":video-core:core-usecase"))
    implementation(project(":video-core:core-port"))
    implementation(project(":video-core:core-domain"))
    implementation(project(":video-commons"))

    implementation(project(":testFixtures"))

    implementation("org.springframework.data:spring-data-commons")
    implementation("org.springframework:spring-context")

    implementation("io.jsonwebtoken:jjwt-api")
    implementation("io.jsonwebtoken:jjwt-impl")
    implementation("io.jsonwebtoken:jjwt-jackson")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}