package com.github.miguelaferreira.terraformcontroller.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ModuleModel implements Module {
    String image;
    String tag;
    String path;
}
