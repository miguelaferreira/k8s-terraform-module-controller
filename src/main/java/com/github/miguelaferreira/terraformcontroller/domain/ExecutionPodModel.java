package com.github.miguelaferreira.terraformcontroller.domain;

import static com.github.miguelaferreira.terraformcontroller.domain.ModelConstants.MODULE_SHARE_CONTAINER_VOLUME_PATH;
import static com.github.miguelaferreira.terraformcontroller.domain.ModelConstants.RUNNER_IMAGE;
import static com.github.miguelaferreira.terraformcontroller.domain.ModelConstants.TERRAFORM_CONFIG_VOLUME_NAME;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
class ExecutionPodModel implements Execution {

    private static final boolean DEBUG = false;

    // Used to read back outputs into a k8s secret
    private static final String TERRAFORM_OUTPUT_FILE = "outputs.json";

    String name;
    String appLabel;
    Container runner;
    Container module;
    String terraformBackendConfigmapName;
    List<String> imagePullSecrets;
    TF_COMMAND tfCommand;

    public boolean hasAppLabel() {
        return Objects.nonNull(appLabel);
    }

    public Stream<String> getImagePullSecrets() {
        return Objects.requireNonNullElse(imagePullSecrets, List.<String>of()).stream();
    }

    static ExecutionPodModel of(final TF_COMMAND tfCommand, final String name, final Input inputModel,
                                final String backendCredentialsSecretName, final String backendConfigMapName,
                                final String variablesSecretName, final List<String> imagePullSecretNames) {
        return ExecutionPodModel.builder()
                                .tfCommand(tfCommand)
                                .name(name)
                                .appLabel(name)
                                .module(buildModuleContainer(name, inputModel.getModule()))
                                .runner(buildRunnerContainer(name, backendCredentialsSecretName, variablesSecretName, tfCommand))
                                .terraformBackendConfigmapName(backendConfigMapName)
                                .imagePullSecrets(imagePullSecretNames)
                                .build();
    }

    private static ContainerModel buildModuleContainer(final String name, final Module module) {
        final List<String> moduleCommand =
                Arrays.asList("sh", "-c", String.format("ls -al / && cp --force --recursive %s %s/module", module.getPath(), MODULE_SHARE_CONTAINER_VOLUME_PATH));
        return ContainerModel.builder()
                             .name(name)
                             .image(module.getImage() + ":" + module.getTag())
                             .command(moduleCommand)
                             .volumePath(MODULE_SHARE_CONTAINER_VOLUME_PATH)
                             .build();
    }

    private static ContainerModel buildRunnerContainer(final String name, final String backendCredentialsSecretName, final String variablesSecretName, final TF_COMMAND tfCommand) {
        final Stream<String> runnerCommands = Stream.concat(getTerraformInitCommands(name), getTerraformCommands(name, tfCommand));
        final List<String> runnerCommand = Arrays.asList("bash", "-vc", runnerCommands.collect(Collectors.joining(" && ")));

        return ContainerModel.builder()
                             .name(name)
                             .image(RUNNER_IMAGE)
                             .command(runnerCommand)
                             .envFrom(buildRunnerContainerEnvironmentSources(backendCredentialsSecretName, variablesSecretName))
                             .build();
    }

    private static Stream<String> getTerraformCommands(final String name, final TF_COMMAND tfCommand) {
        switch (tfCommand) {
            case APPLY:
                return getTerraformApplyCommands(name);
            case DESTROY:
                return getTerraformDestroyCommands();
            default:
                return Stream.empty();
        }
    }

    private static Stream<String> getTerraformInitCommands(final String name) {
        return Stream.of(
                String.format("cp %s/* %s/module/", TERRAFORM_CONFIG_VOLUME_NAME, MODULE_SHARE_CONTAINER_VOLUME_PATH),
                String.format("cd %s/module", MODULE_SHARE_CONTAINER_VOLUME_PATH),
                "ls -al",
                getTerraformInit(name)
        );
    }

    private static Stream<String> getTerraformApplyCommands(final String name) {
        return Stream.of(
                getTerraformApply(),
                getTerraformOutput(),
                getKubectlCreateConfigMap(name)
        );
    }

    private static Stream<String> getTerraformDestroyCommands() {
        return Stream.of(
                getTerraformDestroy()
        );
    }

    private static List<EnvSourcesModel> buildRunnerContainerEnvironmentSources(final String backendCredentialsSecretName, final String variablesSecretName) {
        return List.of(
                EnvSourcesModel.builder().type(EnvSourcesModel.SrcType.SECRET_REF).name(backendCredentialsSecretName).build(),
                EnvSourcesModel.builder().type(EnvSourcesModel.SrcType.SECRET_REF).name(variablesSecretName).build()
        );
    }

    private static String getKubectlCreateConfigMap(final String name) {
        final String fromLiteralFlags =
                String.format("$( jq -r 'to_entries[] | \"--from-literal=\\(.key)=\\(.value .value)\"' %s | xargs )", ExecutionPodModel.TERRAFORM_OUTPUT_FILE);
        return debugWrapper(String.format("kubectl create secret generic outputs-%s %s --dry-run=true -o yaml | kubectl apply -f -", name, fromLiteralFlags));
    }

    private static String getTerraformOutput() {
        return debugWrapper("terraform output -json > " + ExecutionPodModel.TERRAFORM_OUTPUT_FILE);
    }

    private static String getTerraformApply() {
        return debugWrapper("terraform apply -auto-approve -input=false");
    }

    private static String getTerraformDestroy() {
        return debugWrapper("terraform destroy -auto-approve -input=false");
    }

    private static String getTerraformInit(final String name) {
        return debugWrapper(String.format("terraform init -backend-config=key=%s/terraform.tfstate -backend-config=access_key=%s -backend-config=secret_key=%s",
                name, printShellEnvironmentVar(ENV_VAR_TF_BACKEND_AWS_ACCESS_KEY_ID), printShellEnvironmentVar(ENV_VAR_TF_BACKEND_AWS_SECRET_ACCESS_KEY)));
    }

    private static String printShellEnvironmentVar(final String envVar) {
        return "${" + envVar + "}";
    }

    private static String debugWrapper(final String command) {
        return (DEBUG ? "echo \\\"" : "") + command + (DEBUG ? "\\\"" : "");
    }
}
