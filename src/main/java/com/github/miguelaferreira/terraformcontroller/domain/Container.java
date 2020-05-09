package com.github.miguelaferreira.terraformcontroller.domain;

import java.util.List;

public interface Container {
    String getImage();

    List<String> getCommand();

    List<EnvSourcesModel> getEnvFrom();
}
