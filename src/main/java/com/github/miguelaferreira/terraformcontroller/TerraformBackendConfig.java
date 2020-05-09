package com.github.miguelaferreira.terraformcontroller;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties("terraform_backend")
public class TerraformBackendConfig {

    private String configMapName = "terraform-backend-config";
    private S3 s3;

    @Data
    @ConfigurationProperties("s3")
    public static class S3 {
        private String region = "eu-west-1";
        private boolean encrypt = true;
        private String bucket;
        private String dynamoDbTable;
        private String credentialsSecretName;
        // Todo: remove 👇
        private String awsAccessKeyId;
        private String awsSecretAccessKey;
    }
}
