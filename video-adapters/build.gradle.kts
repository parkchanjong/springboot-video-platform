dependencies {
    implementation(project(":video-core:core-port"))
    implementation(project(":video-core:core-domain"))
    implementation(project(":video-commons"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-starter-web")

    runtimeOnly("com.mysql:mysql-connector-j")

    integrationImplementation("org.springframework.boot:spring-boot-starter-test")

    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("de.flapdoodle.embed:de.flapdoodle.embed.mongo.spring3x:4.16.1")
    testImplementation("com.github.codemonstur:embedded-redis:1.4.3")
}
