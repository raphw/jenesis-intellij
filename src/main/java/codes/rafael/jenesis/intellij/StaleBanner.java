package codes.rafael.jenesis.intellij;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotificationProvider;

import javax.swing.JComponent;
import java.util.Optional;
import java.util.function.Function;

public final class StaleBanner implements EditorNotificationProvider, DumbAware {

    @Override
    public Function<? super FileEditor, ? extends JComponent> collectNotificationData(Project project,
                                                                                     VirtualFile file) {
        if (!file.isInLocalFileSystem()) {
            return null;
        }
        Optional<JenesisBuild> build = JenesisLoader.build(project);
        if (build.isEmpty() || !build.get().stale(file.toNioPath())) {
            return null;
        }
        String text = build.get().loaded()
                ? "This file changed after the Jenesis build was loaded"
                : "The modules of this Jenesis build are not loaded";
        return editor -> {
            EditorNotificationPanel panel = new EditorNotificationPanel(editor, EditorNotificationPanel.Status.Info);
            panel.setText(text);
            panel.createActionLabel("Load Jenesis build", () -> JenesisLoader.load(project));
            return panel;
        };
    }
}
