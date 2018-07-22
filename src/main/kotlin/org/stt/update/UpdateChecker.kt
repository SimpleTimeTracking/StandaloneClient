package org.stt.update

import java.net.URL
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Named

class UpdateChecker @Inject
constructor(@Named("version") val appVersion: String, @Named("release url") val projectURL: URL) {
    fun queryNewerVersion(): CompletionStage<Optional<String>> {
        return CompletableFuture.supplyAsync<String> {
            projectURL.openStream().use { stream -> stream.bufferedReader().readText() }
        }.thenApply { receivedReply ->
            val matcher = TAG_PATTERN.matcher(receivedReply)
            var version: String? = null
            while (matcher.find()) {
                val nextVersion = matcher.group(1)
                if (VERSION_COMPARATOR.compare(nextVersion, appVersion) > 0 && (version == null || VERSION_COMPARATOR.compare(nextVersion, version) > 0)) {
                    version = nextVersion
                }
            }
            Optional.ofNullable(version)
        }
    }

    companion object {
        private val TAG_PATTERN = Pattern.compile("tag_name\":\\s*\"v?([^\"]+)\"", Pattern.MULTILINE)
        private val VERSION_COMPARATOR = VersionComparator()
    }
}
