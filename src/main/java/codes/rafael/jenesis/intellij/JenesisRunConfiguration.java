package codes.rafael.jenesis.intellij;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public final class JenesisRunConfiguration extends RunConfigurationBase<JenesisRunConfigurationOptions> {

    JenesisRunConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    @Override
    protected JenesisRunConfigurationOptions getOptions() {
        return (JenesisRunConfigurationOptions) super.getOptions();
    }

    public String getSelectors() {
        return getOptions().getSelectors();
    }

    public void setSelectors(String selectors) {
        getOptions().setSelectors(selectors);
    }

    public String getProperties() {
        return getOptions().getProperties();
    }

    public void setProperties(String properties) {
        getOptions().setProperties(properties);
    }

    public String getJavaHome() {
        return getOptions().getJavaHome();
    }

    public void setJavaHome(String javaHome) {
        getOptions().setJavaHome(javaHome);
    }

    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new JenesisSettingsEditor();
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        try {
            command();
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new RuntimeConfigurationError(e.getMessage());
        }
    }

    @Override
    public RunProfileState getState(Executor executor, ExecutionEnvironment environment) {
        return new CommandLineState(environment) {
            @Override
            protected ProcessHandler startProcess() throws ExecutionException {
                GeneralCommandLine command;
                try {
                    command = command();
                } catch (IllegalArgumentException | IllegalStateException e) {
                    throw new ExecutionException(e.getMessage(), e);
                }
                ProcessHandler handler = new KillableColoredProcessHandler(command);
                ProcessTerminatedListener.attach(handler);
                return handler;
            }
        };
    }

    private GeneralCommandLine command() {
        JenesisBuild build = JenesisLoader.build(getProject()).orElseThrow(() -> new IllegalStateException("No "
                + JenesisBuild.MAKE + " in " + getProject().getBasePath()
                + ": install Jenesis there with curl -fsSL https://get.jenesis.build | bash"));
        Path javaHome = JenesisLoader.javaHome(getProject(), getJavaHome());
        List<String> command = build.command(javaHome,
                JenesisBuild.words(getProperties()),
                JenesisBuild.words(getSelectors()));
        return new GeneralCommandLine(command)
                .withWorkDirectory(build.root().toFile())
                .withCharset(StandardCharsets.UTF_8);
    }
}
