package com.github.miguelaferreira.terraformcontroller;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;

import io.micronaut.context.ApplicationContext;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

class ControllerConfigTest {

    @Test
    void testConfiguration() {
        final HashMap<String, Object> items = new HashMap<>();
        items.put("controller.input_label_selector", "someLabel");

        final ApplicationContext ctx = ApplicationContext.run(ApplicationContext.class, items);
        final ControllerConfig controllerConfig = ctx.getBean(ControllerConfig.class);

        assertThat(controllerConfig.getInputLabelSelector(), Matchers.notNullValue());
        assertThat(controllerConfig.getInputLabelSelector(), Matchers.is("someLabel"));

        ctx.close();
    }
}
