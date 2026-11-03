package codes.rafael.jenesis.intellij;

import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.ui.EditorNotificationProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginDescriptorTest {

    private static Document descriptor;

    @BeforeAll
    public static void read() throws Exception {
        try (InputStream in = PluginDescriptorTest.class.getResourceAsStream("/META-INF/plugin.xml")) {
            assertThat(in).as("the plugin descriptor is a resource of the plugin").isNotNull();
            descriptor = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
        }
    }

    @Test
    public void declares_the_version_of_the_pom() throws Exception {
        Document pom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(Path.of("pom.xml").toFile());
        String version = null;
        NodeList children = pom.getDocumentElement().getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            if (children.item(index) instanceof Element element && element.getTagName().equals("version")) {
                version = element.getTextContent().strip();
            }
        }

        assertThat(text("version")).as("plugin.xml and pom.xml name one version").isEqualTo(version);
    }

    @Test
    public void implements_every_extension_it_declares() throws Exception {
        Map<String, Class<?>> extensions = Map.of("postStartupActivity", ProjectActivity.class,
                "editorNotificationProvider", EditorNotificationProvider.class,
                "configurationType", ConfigurationType.class);
        for (Map.Entry<String, Class<?>> extension : extensions.entrySet()) {
            List<String> implementations = attributes(extension.getKey(), "implementation");
            assertThat(implementations).as(extension.getKey()).isNotEmpty();
            for (String implementation : implementations) {
                assertThat(extension.getValue()).as(implementation).isAssignableFrom(load(implementation));
            }
        }
    }

    @Test
    public void listens_on_the_topic_each_listener_implements() throws Exception {
        NodeList listeners = descriptor.getElementsByTagName("listener");
        assertThat(listeners.getLength()).isPositive();
        for (int index = 0; index < listeners.getLength(); index++) {
            Element listener = (Element) listeners.item(index);
            assertThat(load(listener.getAttribute("topic"))).isAssignableFrom(load(listener.getAttribute("class")));
            assertThat(load(listener.getAttribute("class")).getConstructor(com.intellij.openapi.project.Project.class))
                    .as("a project listener is created with its project")
                    .isNotNull();
        }
    }

    @Test
    public void declares_actions_that_are_actions_with_their_icons() throws Exception {
        NodeList actions = descriptor.getElementsByTagName("action");
        assertThat(actions.getLength()).isPositive();
        for (int index = 0; index < actions.getLength(); index++) {
            Element action = (Element) actions.item(index);
            assertThat(AnAction.class).isAssignableFrom(load(action.getAttribute("class")));
            assertThat(PluginDescriptorTest.class.getResource(action.getAttribute("icon"))).isNotNull();
        }
    }

    @Test
    public void ships_the_icons_it_names_in_both_themes() {
        for (String icon : List.of("/META-INF/pluginIcon.svg",
                "/META-INF/pluginIcon_dark.svg",
                "/icons/jenesis.svg",
                "/icons/jenesis_dark.svg")) {
            assertThat(PluginDescriptorTest.class.getResource(icon)).as(icon).isNotNull();
        }
    }

    @Test
    public void declares_the_notification_group_it_reports_to() {
        assertThat(attributes("notificationGroup", "id")).containsExactly(JenesisLoader.GROUP);
    }

    private static Class<?> load(String name) throws ClassNotFoundException {
        return Class.forName(name, false, PluginDescriptorTest.class.getClassLoader());
    }

    private static String text(String tag) {
        NodeList nodes = descriptor.getElementsByTagName(tag);
        assertThat(nodes.getLength()).as(tag).isEqualTo(1);
        return nodes.item(0).getTextContent().strip();
    }

    private static List<String> attributes(String tag, String attribute) {
        List<String> values = new ArrayList<>();
        NodeList nodes = descriptor.getElementsByTagName(tag);
        for (int index = 0; index < nodes.getLength(); index++) {
            values.add(((Element) nodes.item(index)).getAttribute(attribute));
        }
        return values;
    }
}
