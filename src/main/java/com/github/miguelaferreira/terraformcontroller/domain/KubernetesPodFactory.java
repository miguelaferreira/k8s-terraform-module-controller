package com.github.miguelaferreira.terraformcontroller.domain;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import io.micronaut.kubernetes.client.v1.LocalObjectReference;
import io.micronaut.kubernetes.client.v1.Metadata;
import io.micronaut.kubernetes.client.v1.pods.ConfigMapVolumeSource;
import io.micronaut.kubernetes.client.v1.pods.EmptyDirVolumeSource;
import io.micronaut.kubernetes.client.v1.pods.EnvFromSource;
import io.micronaut.kubernetes.client.v1.pods.Pod;
import io.micronaut.kubernetes.client.v1.pods.PodSpec;
import io.micronaut.kubernetes.client.v1.pods.SecretEnvSource;
import io.micronaut.kubernetes.client.v1.pods.Volume;
import io.micronaut.kubernetes.client.v1.pods.VolumeMount;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KubernetesPodFactory {

    public static final String LABEL_APP = "app";
    public static final String MODULE_SHARE_VOLUME_NAME = "module-share-volume";
    public static final String TERRAFORM_BACKEND_CONFIG_VOLUME_NAME = "backend-config-volume";

    public static Pod build(final Execution model) {
        if (model instanceof ExecutionPodModel) {
            return build((ExecutionPodModel) model);
        } else {
            throw new RuntimeException("Only supported execution model is ExecutionPodModel");
        }
    }

    private static Pod build(final ExecutionPodModel model) {
        final Pod pod = new Pod();
        pod.setMetadata(buildMeta(model));
        pod.setSpec(buildSpec(model));
        return pod;
    }

    private static Metadata buildMeta(final ExecutionPodModel model) {
        final Metadata meta = new Metadata();
        meta.setName(model.getName() + "-" + UUID.randomUUID());
        if (model.hasAppLabel()) {
            meta.setLabels(Map.of(LABEL_APP, model.getAppLabel()));
        }
        return meta;
    }

    private static PodSpec buildSpec(final ExecutionPodModel model) {
        final PodSpec spec = new PodSpec();
        spec.setVolumes(List.of(buildModuleShareVolume(), buildTerraformBackendVolume(model.getTerraformBackendConfigmapName())));
        spec.setInitContainers(List.of(buildInitContainer("module-" + model.getName(), model.getModule())));
        spec.setContainers(List.of(buildContainer("runner-" + model.getName(), model.getRunner())));
        spec.setRestartPolicy("OnFailure");
        spec.setImagePullSecrets(model.getImagePullSecrets()
                                      .map(KubernetesPodFactory::buildObjectReference)
                                      .collect(Collectors.toList()));
        return spec;
    }

    private static LocalObjectReference buildObjectReference(final String secretName) {
        final LocalObjectReference objectReference = new LocalObjectReference();
        objectReference.setName(secretName);
        return objectReference;
    }

    private static Volume buildModuleShareVolume() {
        final Volume volume = new Volume();
        volume.setName(MODULE_SHARE_VOLUME_NAME);
        volume.setEmptyDir(new EmptyDirVolumeSource());
        return volume;
    }

    private static Volume buildTerraformBackendVolume(final String terraformBackendConfigMapName) {
        final Volume volume = new Volume();
        volume.setName(TERRAFORM_BACKEND_CONFIG_VOLUME_NAME);
        final ConfigMapVolumeSource configMap = new ConfigMapVolumeSource();
        configMap.setName(terraformBackendConfigMapName);
        volume.setConfigMap(configMap);
        return volume;
    }

    private static io.micronaut.kubernetes.client.v1.pods.Container buildInitContainer(final String name, final Container model) {
        final io.micronaut.kubernetes.client.v1.pods.Container container = new io.micronaut.kubernetes.client.v1.pods.Container();
        container.setName(name);
        container.setImage(model.getImage());
        container.setCommand(model.getCommand());
        container.setVolumeMounts(List.of(buildModuleShareVolumeMount()));
        container.setImagePullPolicy("Always");
        return container;
    }

    private static io.micronaut.kubernetes.client.v1.pods.Container buildContainer(final String name, final Container model) {
        final io.micronaut.kubernetes.client.v1.pods.Container container = new io.micronaut.kubernetes.client.v1.pods.Container();
        container.setName(name);
        container.setImage(model.getImage());
        container.setCommand(model.getCommand());
        container.setVolumeMounts(List.of(buildModuleShareVolumeMount(), buildTerraformBackendVolumeMount()));
        container.setEnvFrom(buildContainerEnv(model));
        container.setImagePullPolicy("Always");
        return container;
    }

    private static List<EnvFromSource> buildContainerEnv(final Container model) {
        return model.getEnvFrom().stream()
                    .map(KubernetesPodFactory::buildEnvFrom)
                    .collect(Collectors.toList());
    }

    private static EnvFromSource buildEnvFrom(final EnvSourcesModel envSourcesModel) {
        final EnvSourcesModel.SrcType type = envSourcesModel.getType();
        if (type == EnvSourcesModel.SrcType.SECRET_REF) {
            final EnvFromSource envFromSource = new EnvFromSource();
            final SecretEnvSource secretEnvSource = new SecretEnvSource();
            secretEnvSource.setName(envSourcesModel.getName());
            envFromSource.setSecretRef(secretEnvSource);
            return envFromSource;
        }
        log.error("Bug! Unexpected value for {}. Got: {}", type.getClass(), type);
        throw new RuntimeException("Bug!");
    }

    private static VolumeMount buildModuleShareVolumeMount() {
        final VolumeMount volumeMnt = new VolumeMount();
        volumeMnt.setName(MODULE_SHARE_VOLUME_NAME);
        volumeMnt.setMountPath(ModelConstants.MODULE_SHARE_CONTAINER_VOLUME_PATH);
        return volumeMnt;
    }

    private static VolumeMount buildTerraformBackendVolumeMount() {
        final VolumeMount volumeMnt = new VolumeMount();
        volumeMnt.setName(TERRAFORM_BACKEND_CONFIG_VOLUME_NAME);
        volumeMnt.setMountPath(ModelConstants.TERRAFORM_CONFIG_VOLUME_NAME);
        return volumeMnt;
    }
}
