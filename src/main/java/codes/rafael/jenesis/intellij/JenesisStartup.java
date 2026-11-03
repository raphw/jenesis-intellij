package codes.rafael.jenesis.intellij;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

public final class JenesisStartup implements ProjectActivity {

    @Override
    public Object execute(Project project, Continuation<? super Unit> continuation) {
        JenesisLoader.build(project)
                .filter(build -> !build.loaded())
                .ifPresent(_build -> JenesisLoader.offer(project));
        return Unit.INSTANCE;
    }
}
