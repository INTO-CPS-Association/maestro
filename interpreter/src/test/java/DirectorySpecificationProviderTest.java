import org.apache.commons.io.FileUtils;
import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.interpreter.DirectorySpecificationProvider;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DirectorySpecificationProviderTest {

    @Test
    public void scanTest() throws IOException {
        Path base = Paths.get("target", "DirectorySpecificationProviderTest", "root");

        Path s1 = base.resolve(Paths.get("stage1"));
        s1.toFile().mkdirs();
        FileUtils.write(s1.resolve("spec.mabl").toFile(), "", StandardCharsets.UTF_8);
        Path s2 = base.resolve(Paths.get("stage2"));
        s2.toFile().mkdirs();
        FileUtils.write(s2.resolve("spec.mabl").toFile(), "", StandardCharsets.UTF_8);

        DirectorySpecificationProvider provider = new DirectorySpecificationProvider(base.toFile(), f -> new ARootDocument(), 0);

        Assert.assertEquals(2, provider.get().size());
    }
}
