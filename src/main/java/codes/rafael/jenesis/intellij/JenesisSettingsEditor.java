package codes.rafael.jenesis.intellij;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;

import javax.swing.JComponent;
import javax.swing.JPanel;

final class JenesisSettingsEditor extends SettingsEditor<JenesisRunConfiguration> {

    private final JBTextField selectors = new JBTextField(), properties = new JBTextField();
    private final TextFieldWithBrowseButton javaHome = new TextFieldWithBrowseButton();
    private final JPanel panel;

    JenesisSettingsEditor() {
        selectors.getEmptyText().setText("build");
        properties.getEmptyText().setText("-Djenesis.test.skip=true");
        javaHome.addBrowseFolderListener(null, FileChooserDescriptorFactory.createSingleFolderDescriptor()
                .withTitle("Java Home"));
        panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Selectors:", selectors)
                .addTooltip("Selectors as Make takes them, as stage or +module/test; none builds every module")
                .addLabeledComponent("Properties:", properties)
                .addTooltip("Settings as -Djenesis.<group>.<key>=<value>, passed before Make.java")
                .addLabeledComponent("Java home:", javaHome)
                .addTooltip("A JDK " + JenesisBuild.RELEASE + " or newer; none takes the project SDK, another JDK "
                        + JenesisBuild.RELEASE + " or JAVA_HOME")
                .getPanel();
    }

    @Override
    protected void resetEditorFrom(JenesisRunConfiguration configuration) {
        selectors.setText(configuration.getSelectors());
        properties.setText(configuration.getProperties());
        javaHome.setText(configuration.getJavaHome());
    }

    @Override
    protected void applyEditorTo(JenesisRunConfiguration configuration) {
        configuration.setSelectors(selectors.getText());
        configuration.setProperties(properties.getText());
        configuration.setJavaHome(javaHome.getText());
    }

    @Override
    protected JComponent createEditor() {
        return panel;
    }
}
