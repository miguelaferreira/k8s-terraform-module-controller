package com.github.miguelaferreira.terraformcontroller;

import io.micronaut.context.ApplicationContext;
import io.micronaut.kubernetes.client.v1.KubernetesConfiguration;
import io.micronaut.runtime.Micronaut;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Application {

    public static void main(final String[] args) {
        log.info("K8s Terraform Controller Started cmd");

        final ApplicationContext ctx = Micronaut.run(Application.class);
        final KubernetesConfiguration kubernetesConfiguration = ctx.getBean(KubernetesConfiguration.class);
        ctx.registerSingleton(kubernetesConfiguration.getNamespace());
        final Controller bean = ctx.getBean(Controller.class);
        bean.start();
    }
}
