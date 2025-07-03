package graalpy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.graalvm.polyglot.Context;
import org.graalvm.python.embedding.GraalPyResources;
import org.junit.jupiter.api.Test;

final class PythonTest {

    @Test
    void testRunHelloWorld() {
        String path = "src/test/resources/python";
        try (var context = createPythonContext(path)) {
            // Configure the Python context to include the path to the Python resources, otherwise a ModuleNotFoundError will occur
            context.getBindings("python").putMember("sys_path", path);
            context.eval("python", "import sys");
            context.eval("python", "sys.path.append(sys_path)");

            HelloWorld world = context.eval("python", "import HelloWorld; HelloWorld").as(HelloWorld.class);
            world.main();

            String result = world.getText();
            assertEquals("Hello, World!", result);
        }
    }

    static Context createPythonContext(String pythonResourcesDirectory) {
        return GraalPyResources.contextBuilder(Path.of(pythonResourcesDirectory)).build();
    }
}
