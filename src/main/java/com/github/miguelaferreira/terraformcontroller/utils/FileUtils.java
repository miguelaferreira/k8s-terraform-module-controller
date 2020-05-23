package com.github.miguelaferreira.terraformcontroller.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class FileUtils {

    public static String readResourceContent(final String path) throws IOException {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream, "inputStream is null")));
            return bufferedReader.lines().collect(Collectors.joining("\n"));
        }
    }

    public static String readResourceContent(final String path, final Map<String, String> replacements) throws IOException {
        final String content = readResourceContent(path);
        return replacements.entrySet()
                           .stream()
                           .reduce(content, (String acc, Map.Entry<String, String> entry) -> acc.replaceAll(entry.getKey(), entry.getValue()), (acc1, acc2) -> acc2);
    }
}
