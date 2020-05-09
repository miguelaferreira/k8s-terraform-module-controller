package com.github.miguelaferreira.terraformcontroller.domain;

import java.util.List;

public interface Execution {

    // Todo: either make this configurable or document requirement for it!
    String ENV_VAR_TF_BACKEND_AWS_SECRET_ACCESS_KEY = "TF_BACKEND_AWS_SECRET_ACCESS_KEY";
    String ENV_VAR_TF_BACKEND_AWS_ACCESS_KEY_ID = "TF_BACKEND_AWS_ACCESS_KEY_ID";

    String getName();

    enum TF_COMMAND {
        APPLY, DESTROY
    }

    static Execution defaultExecution(final TF_COMMAND tfCommand, final String name, final Input inputModel,
                                      final String backendCredentialsSecretName, final String backendConfigMapName,
                                      final String variablesSecretName, final List<String> imagePullSecretNames) {
        return ExecutionPodModel.of(tfCommand, name, inputModel, backendCredentialsSecretName, backendConfigMapName, variablesSecretName, imagePullSecretNames);
    }
}
