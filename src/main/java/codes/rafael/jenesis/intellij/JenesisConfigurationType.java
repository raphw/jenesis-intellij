package codes.rafael.jenesis.intellij;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.components.BaseState;
import com.intellij.openapi.project.Project;

public final class JenesisConfigurationType extends ConfigurationTypeBase {

    public static final String ID = "JenesisRunConfiguration";

    public JenesisConfigurationType() {
        super(ID, "Jenesis", "Runs selectors of a Jenesis build", JenesisIcons.JENESIS);
        addFactory(new ConfigurationFactory(this) {
            @Override
            public String getId() {
                return ID;
            }

            @Override
            public RunConfiguration createTemplateConfiguration(Project project) {
                return new JenesisRunConfiguration(project, this, "Jenesis");
            }

            @Override
            public Class<? extends BaseState> getOptionsClass() {
                return JenesisRunConfigurationOptions.class;
            }
        });
    }
}
