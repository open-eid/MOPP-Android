package ee.ria.DigiDoc.android;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class LibdigidocppPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getTasks().create("updatelibdigidocpp", UpdateLibdigidocppTask.class);
    }
}
