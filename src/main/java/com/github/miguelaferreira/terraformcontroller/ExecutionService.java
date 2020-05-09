package com.github.miguelaferreira.terraformcontroller;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.github.miguelaferreira.terraformcontroller.domain.Execution;
import com.github.miguelaferreira.terraformcontroller.domain.KubernetesPodFactory;
import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.kubernetes.client.v1.pods.Pod;
import io.reactivex.Flowable;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/*
 * This class is responsible for turning inputs into pods and
 * starting their execution.
 */
@Slf4j
@Singleton
public class ExecutionService {

    private final ConcurrentHashMap<String, ExecutionInfo> executionStatus = new ConcurrentHashMap<>();
    private final String namespace;
    private final KubernetesClient k8sClient;

    @Inject
    public ExecutionService(final String namespace, final KubernetesClient k8sClient) {
        this.namespace = namespace;
        this.k8sClient = k8sClient;
    }

    public Pod executeInput(final Execution executionPodModel) {
        final Pod pod = KubernetesPodFactory.build(executionPodModel);

        final String name = executionPodModel.getName();
        final Pod createdPod = Flowable.fromPublisher(this.k8sClient.createPod(namespace, pod)).blockingFirst();
        final String phase = createdPod.getStatus().getPhase();
        log.info("Pod for input {} created, pod status phase is {}", name, phase);

        if (!executionStatus.containsKey(name)) {
            executionStatus.put(name, ExecutionInfo.builder().name(name).build());
        }

        final ExecutionInfo executionInfo = executionStatus.get(name);
        executionInfo.getGeneratedPodNames().add(pod.getMetadata().getName());
        executionInfo.getPodStatusPhases().add(phase);

        return pod;
    }

    @Value
    @Builder
    static class ExecutionInfo {
        String name;
        List<String> generatedPodNames = new ArrayList<>();
        List<String> podStatusPhases = new ArrayList<>();
    }
}
