package com.github.miguelaferreira.terraformcontroller.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
class NameValuePair implements Variable {
    String name;
    String value;
}
