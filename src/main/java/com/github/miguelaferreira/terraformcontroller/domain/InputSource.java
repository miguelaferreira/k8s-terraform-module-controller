package com.github.miguelaferreira.terraformcontroller.domain;

import java.util.Map;

public interface InputSource {
    String getImage();

    String getTag();

    String getPath();

    Map<String, Object> getVariables();
}
