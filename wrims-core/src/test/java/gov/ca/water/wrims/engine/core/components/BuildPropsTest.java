package gov.ca.water.wrims.engine.core.components;

import org.junit.jupiter.api.Test;
import org.testng.Assert;

class BuildPropsTest {
    @Test
    void testVersionNotNull() {
        BuildProps buildProps = new BuildProps();
        Assert.assertNotNull(buildProps.getVN(), "the version number should not be null");
    }

    @Test
    void testUnknownWhenNotFound() {
        BuildProps buildProps = new BuildProps("not-a-real-file.properties");
        Assert.assertEquals(
                buildProps.getVN(),
                BuildProps.UNKNOWN_VERSION_TAG,
                "the version number should default to an unknown"
        );
    }
}
