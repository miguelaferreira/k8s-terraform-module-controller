package com.github.miguelaferreira.terraformcontroller;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties("terraform_backend")
public class TerraformBackendConfig {

    private String configMapName = "terraform-backend-config";
    private S3 s3 = new S3();

    @Data
    @ConfigurationProperties("s3")
    public static class S3 {
        private String region = "some-region";
        private boolean encrypt = true;
        private String bucket = "some-bucket";
        private String dynamoDbTable = "some-table";
        private String credentialsSecretName = "some-credentials-secret-names";
    }
}
