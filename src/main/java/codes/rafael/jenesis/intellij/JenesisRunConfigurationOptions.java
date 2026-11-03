package codes.rafael.jenesis.intellij;

import com.intellij.execution.configurations.RunConfigurationOptions;
import com.intellij.openapi.components.StoredProperty;

public final class JenesisRunConfigurationOptions extends RunConfigurationOptions {

    private final StoredProperty<String> selectors = string("").provideDelegate(this, "selectors");
    private final StoredProperty<String> properties = string("").provideDelegate(this, "properties");
    private final StoredProperty<String> javaHome = string("").provideDelegate(this, "javaHome");

    public String getSelectors() {
        return selectors.getValue(this);
    }

    public void setSelectors(String value) {
        selectors.setValue(this, value);
    }

    public String getProperties() {
        return properties.getValue(this);
    }

    public void setProperties(String value) {
        properties.setValue(this, value);
    }

    public String getJavaHome() {
        return javaHome.getValue(this);
    }

    public void setJavaHome(String value) {
        javaHome.setValue(this, value);
    }
}
