package org.stt.update;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

public class UpdateChecker {
    private static final Pattern TAG_PATTERN = Pattern.compile("tag_name\":\\s*\"v?([^\"]+)\"", Pattern.MULTILINE);
    private static final VersionComparator VERSION_COMPARATOR = new VersionComparator();
    private final URL projectURL;
    private final String appVersion;

    @Inject
    public UpdateChecker(@Named("version.info") Properties versionInfo) {
        try {
            projectURL = requireNonNull(new URL(versionInfo.getProperty("release.url")));
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
        appVersion = requireNonNull(versionInfo.getProperty("app.version"));
    }

    public CompletionStage<Optional<String>> queryNewerVersion() {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStream in = projectURL.openStream()) {
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                return result.toString(StandardCharsets.UTF_8.name());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).thenApply(receivedReply -> {
            Matcher matcher = TAG_PATTERN.matcher(receivedReply);
            String version = null;
            while (matcher.find()) {
                String nextVersion = matcher.group(1);
                if (VERSION_COMPARATOR.compare(nextVersion, appVersion) > 0
                        && (version == null || VERSION_COMPARATOR.compare(nextVersion, version) > 0)) {
                    version = nextVersion;
                }
            }
            return Optional.ofNullable(version);
        });
    }
}
