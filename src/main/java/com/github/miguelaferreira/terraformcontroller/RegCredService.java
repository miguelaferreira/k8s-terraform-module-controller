package com.github.miguelaferreira.terraformcontroller;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class RegCredService {

    private final List<String> secrets;

    @Inject
    public RegCredService(final ControllerConfig controllerConfig) {
        this.secrets = controllerConfig.getContainerRegistryCredSecrets();
    }

    public List<String> getSecrets() {
        return secrets;
    }
}
