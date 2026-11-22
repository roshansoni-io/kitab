package io.github.roshansoni_io.kitab.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

public abstract class ValidateReleaseSigningTask extends DefaultTask {
    @Input
    public abstract Property<Boolean> getSigningConfigured();

    @TaskAction
    public void validateSigning() {
        if (!getSigningConfigured().get()) {
            throw new GradleException(
                "Configure release signing in keystore.properties before building a release APK or bundle."
            );
        }
    }
}
