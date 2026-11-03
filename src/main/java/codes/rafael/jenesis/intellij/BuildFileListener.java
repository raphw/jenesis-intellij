package codes.rafael.jenesis.intellij;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.ui.EditorNotifications;

import java.util.List;
import java.util.Optional;

public final class BuildFileListener implements BulkFileListener {

    private final Project project;

    public BuildFileListener(Project project) {
        this.project = project;
    }

    @Override
    public void after(List<? extends VFileEvent> events) {
        Optional<JenesisBuild> build = JenesisLoader.build(project);
        if (build.isEmpty()) {
            return;
        }
        for (VFileEvent event : events) {
            VirtualFile file = event.getFile();
            if (file != null && file.isInLocalFileSystem() && build.get().defines(file.toNioPath())) {
                EditorNotifications.getInstance(project).updateNotifications(file);
            }
        }
    }
}
