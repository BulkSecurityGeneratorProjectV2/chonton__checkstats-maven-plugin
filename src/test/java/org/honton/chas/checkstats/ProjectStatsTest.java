package org.honton.chas.checkstats;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

public class ProjectStatsTest {
    
    private ProjectStats createExpected() {
        Stat inner = new Stat();
        inner.put("n", 3.25);
        inner.put("m", 3);
        ProjectStats expected = new ProjectStats();
        expected.addFile("org/example/File.java");
        expected.addStat("stat", inner);
        return expected;
    }

    @Test
    public void testFileRoundTrip() throws IOException {
        ProjectStats expected = createExpected();
        File tmp = File.createTempFile("test", "reportRootStat");
        expected.write(tmp);
        
        ProjectStats actual = ProjectStats.read(tmp);
        Assert.assertEquals(expected, actual);        
        Assert.assertNotSame(expected, actual);        
    }
    
    ProjectStats readResource(String resourceName) throws URISyntaxException {
        URL url = getClass().getResource(resourceName);
        return ProjectStats.read(new File(url.toURI()));
    }
    
    @Test
    public void testCheckComplexWorsened() throws URISyntaxException {
        ProjectStats prior = readResource("PriorComplex.json");
        Assert.assertEquals(2, prior.getFiles().size());
        Assert.assertEquals(2, prior.getStats().size());

        ProjectStats current = readResource("CurrentWorse.json");
        Assert.assertEquals(1, prior.checkIsBetter(current).size());
        Assert.assertEquals(0, current.checkIsBetter(prior).size());
    }
    
    @Test
    public void testCheckPriorWithoutError() throws URISyntaxException {
        ProjectStats prior = readResource("PriorWithoutError.json");
        Assert.assertEquals(2, prior.getFiles().size());
        Assert.assertEquals(2, prior.getStats().size());

        ProjectStats current = readResource("CurrentWorse.json");
        Assert.assertEquals(0, current.checkIsBetter(prior).size());
        Assert.assertEquals(1, prior.checkIsBetter(current).size());
    }

}
