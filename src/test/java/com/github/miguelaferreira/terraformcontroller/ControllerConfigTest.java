package com.github.miguelaferreira.terraformcontroller;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.util.HashMap;

import io.micronaut.context.ApplicationContext;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

class ControllerConfigTest {

    @Test
    void kubeconfigConfiguration() {
        final HashMap<String, Object> items = new HashMap<>();
        items.put("controller.kubeconfig", new File("some-file"));

        final ApplicationContext ctx = ApplicationContext.run(ApplicationContext.class, items);
        final ControllerConfig controllerConfig = ctx.getBean(ControllerConfig.class);

        assertThat(controllerConfig.getKubeconfig(), Matchers.notNullValue());

        ctx.close();
    }

    @Test
    void inClusterConfiguration() {
        final HashMap<String, Object> items = new HashMap<>();

        final ApplicationContext ctx = ApplicationContext.run(ApplicationContext.class, items);
        final ControllerConfig controllerConfig = ctx.getBean(ControllerConfig.class);

        assertThat(controllerConfig.getKubeconfig(), Matchers.nullValue());

        ctx.close();
    }
}
