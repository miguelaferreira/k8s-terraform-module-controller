package com.github.miguelaferreira.terraformcontroller.utils;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

class FileUtilsTest {

    @Test
    void readResourceContent() throws Exception {
        final String content = FileUtils.readResourceContent("some/path/resource-file-test.txt");
        final List<String> lines = Arrays.asList(content.split("\n"));

        assertThat(lines, Matchers.hasSize(3));
        assertThat(lines, Matchers.contains("This is a Test!", "", "This is another test!"));
    }

    @Test
    void readResourceContentWithReplacements() throws Exception {
        final String content = FileUtils.readResourceContent("terraform/backend.tf", Map.of(
                "VAR_REMOTE_STATE_REGION", "region",
                "VAR_REMOTE_STATE_BUCKET", "bucket",
                "VAR_REMOTE_STATE_DYNAMODB_TABLE", "table",
                "VAR_REMOTE_STATE_ENCRYPT", "true"
        ));
        final List<String> lines = Arrays.asList(content.split("\n"));

        assertThat(lines, Matchers.hasSize(10));
        assertThat(lines, Matchers.contains(
                "terraform {",
                "  backend \"s3\" {",
                "    region         = \"region\"",
                "    bucket         = \"bucket\"",
                "    dynamodb_table = \"table\"",
                "    encrypt        = \"true\"",
                "",
                "    skip_metadata_api_check = true",
                "  }",
                "}"
        ));
    }
}
