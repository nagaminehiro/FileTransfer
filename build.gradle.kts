plugins {
    id("java")
    id("com.google.protobuf") version "0.9.2"
    id("application")
}

group = "io.grpc.examples"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.grpc:grpc-netty-shaded:1.44.1")
    implementation("io.grpc:grpc-protobuf:1.44.1")
    implementation("io.grpc:grpc-stub:1.44.1")
    implementation("com.google.protobuf:protobuf-java:3.19.1")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    testImplementation("junit:junit:4.13.2")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.19.1"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.44.1"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
            }
        }
    }
}

application {
    mainClass.set("io.grpc.examples.filetransfer.FileTransferServer")
}