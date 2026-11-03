package codes.rafael.jenesis.intellij;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotifications;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JenesisLoader {

    public static final String GROUP = "Jenesis";
    private static final List<String> PROPERTIES = List.of("-Djenesis.test.skip=true", "-Djenesis.palette.colors=none");
    private static final int TAIL = 20;

    private JenesisLoader() {
    }

    public static Optional<JenesisBuild> build(Project project) {
        String base = project.getBasePath();
        return base == null ? Optional.empty() : JenesisBuild.of(Path.of(base));
    }

    public static Path javaHome(Project project, String configured) {
        List<Path> candidates = new ArrayList<>();
        if (configured != null && !configured.isBlank()) {
            candidates.add(Path.of(configured.strip()));
            return JenesisBuild.javaHome(candidates);
        }
        Sdk sdk = ProjectRootManager.getInstance(project).getProjectSdk();
        if (sdk != null && sdk.getHomePath() != null) {
            candidates.add(Path.of(sdk.getHomePath()));
        }
        for (Sdk other : ProjectJdkTable.getInstance().getAllJdks()) {
            if (other.getHomePath() != null && !candidates.contains(Path.of(other.getHomePath()))) {
                candidates.add(Path.of(other.getHomePath()));
            }
        }
        String environment = System.getenv("JAVA_HOME");
        if (environment != null && !environment.isBlank() && !candidates.contains(Path.of(environment))) {
            candidates.add(Path.of(environment));
        }
        return JenesisBuild.javaHome(candidates);
    }

    public static void load(Project project) {
        Optional<JenesisBuild> found = build(project);
        if (found.isEmpty()) {
            report(project, NotificationType.ERROR, "No " + JenesisBuild.MAKE + " in " + project.getBasePath()
                    + ": install Jenesis there with curl -fsSL https://get.jenesis.build | bash");
            return;
        }
        JenesisBuild build = found.get();
        Path javaHome;
        try {
            javaHome = javaHome(project, null);
        } catch (IllegalStateException e) {
            report(project, NotificationType.ERROR, e.getMessage());
            return;
        }
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading the Jenesis build", true) {
            @Override
            public void run(ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Running " + JenesisBuild.MAKE + " " + JenesisBuild.LOADER);
                GeneralCommandLine command = new GeneralCommandLine(build.command(javaHome,
                        PROPERTIES,
                        List.of(JenesisBuild.LOADER)))
                        .withWorkDirectory(build.root().toFile())
                        .withCharset(StandardCharsets.UTF_8);
                ProcessOutput output;
                try {
                    output = new CapturingProcessHandler(command).runProcessWithProgressIndicator(indicator);
                } catch (ExecutionException e) {
                    report(project, NotificationType.ERROR, "Could not run Jenesis: " + e.getMessage());
                    return;
                }
                if (output.isCancelled()) {
                    return;
                }
                if (output.isTimeout() || output.getExitCode() != 0) {
                    List<String> lines = new ArrayList<>(output.getStdoutLines());
                    lines.addAll(output.getStderrLines());
                    report(project, NotificationType.ERROR, "Jenesis could not load the build (exit code "
                            + output.getExitCode() + "):<br>"
                            + String.join("<br>", lines.subList(Math.max(0, lines.size() - TAIL), lines.size())
                            .stream()
                            .map(StringUtil::escapeXmlEntities)
                            .toList()));
                    return;
                }
                build.markLoaded();
                VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(build.root());
                if (root != null) {
                    VfsUtil.markDirtyAndRefresh(false, true, true, root);
                }
                EditorNotifications.getInstance(project).updateAllNotifications();
                report(project, NotificationType.INFORMATION, "Loaded the Jenesis build"
                        + build.version().map(version -> " with Jenesis " + version).orElse(""));
            }
        });
    }

    public static void offer(Project project) {
        Notification notification = notification(NotificationType.INFORMATION,
                "This project is a Jenesis build. Load its modules from " + JenesisBuild.LOADER + "?");
        notification.addAction(NotificationAction.createSimpleExpiring("Load Jenesis build", () -> load(project)));
        notification.notify(project);
    }

    private static void report(Project project, NotificationType type, String content) {
        notification(type, content).notify(project);
    }

    private static Notification notification(NotificationType type, String content) {
        return NotificationGroupManager.getInstance().getNotificationGroup(GROUP).createNotification(content, type);
    }
}
