import java.security.MessageDigest

plugins {
    alias(mihonx.plugins.android.library)

    alias(mihonx.plugins.spotless)
}

android {
    namespace = "eu.kanade.tsuzuki.animeplayer"
}

// The .so files come from the anime-native CI artifact (run 41645408336, see docs/licenses/anime-native/).
// Fail the build if any library differs from the recorded checksums.
val verifyNativeChecksums = tasks.register("verifyNativeChecksums") {
    val checksums = file("native-checksums.txt")
    val libDir = file("src/main/jniLibs/arm64-v8a")
    inputs.file(checksums)
    inputs.dir(libDir)
    doLast {
        val expected = checksums.readLines()
            .filter { it.isNotBlank() }
            .associate { line ->
                val hash = line.substringBefore(' ').trim()
                val name = line.substringAfter(' ').trim().removePrefix("*")
                name to hash
            }
        val actualFiles = libDir.listFiles { f -> f.extension == "so" }.orEmpty().associateBy { it.name }
        check(expected.keys == actualFiles.keys) {
            "Native library set changed: expected ${expected.keys}, found ${actualFiles.keys}"
        }
        expected.forEach { (name, hash) ->
            val digest = MessageDigest.getInstance("SHA-256").digest(actualFiles.getValue(name).readBytes())
            val actual = digest.joinToString("") { "%02x".format(it) }
            check(actual == hash) { "Checksum mismatch for $name: expected $hash, got $actual" }
        }
    }
}

tasks.named("preBuild") { dependsOn(verifyNativeChecksums) }

dependencies {
    implementation(libs.androidx.annotation)
}
