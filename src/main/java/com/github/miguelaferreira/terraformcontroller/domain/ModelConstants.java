package com.github.miguelaferreira.terraformcontroller.domain;

import java.util.Map;

import com.github.miguelaferreira.terraformcontroller.TerraformBackendConfig;

public class ModelConstants {

    static final String RUNNER_IMAGE = "registry.gitlab.com/open-source-devex/containers/build-terraform:latest";
    public static final String MODULE_SHARE_CONTAINER_VOLUME_PATH = "/module-share";
    public static final String TERRAFORM_CONFIG_VOLUME_NAME = "/terraform-config";
    public static final String TERRAFORM_BACKEND_FILE = "terraform/backend.tf";
    //    public static final String TERRAFORM_BACKEND_CONFIG_MAP_NAME = "terraform-backend-config";

    public static Map<String, String> getConfigReplacement(final TerraformBackendConfig.S3 s3Backend) {
        return Map.of(
                "VAR_REMOTE_STATE_REGION", s3Backend.getRegion(),
                "VAR_REMOTE_STATE_BUCKET", s3Backend.getBucket(),
                "VAR_REMOTE_STATE_DYNAMODB_TABLE", s3Backend.getDynamoDbTable(),
                "VAR_REMOTE_STATE_ENCRYPT", String.valueOf(s3Backend.isEncrypt())
        );
    }
}
