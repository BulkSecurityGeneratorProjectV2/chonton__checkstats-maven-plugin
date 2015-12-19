package org.honton.chas.checkstats;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class StatTest {

    private Gson getGson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson;
    }
    
    private Stat createExpected() {
        Stat inner = new Stat();
        inner.put("n", 3.25);
        inner.put("m", 3);
        Stat expected = new Stat();
        expected.put("name", 1);
        expected.put("name2", 2);
        expected.put("name3", inner);
        return expected;
    }

    @Test
    public void testDeSerialize() {
        String input = "{\"name\": 1, \"name2\": 2, \"name3\": {\"n\": 3.25, \"m\": 3}}";
        
        Gson gson = getGson();
        Stat actual = gson.fromJson(input, Stat.class);
        
        Stat expected = createExpected();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testRoundTrip() {
        Stat expected = createExpected();
        Gson gson = getGson();
        Stat actual = gson.fromJson(gson.toJson(expected), Stat.class);
        Assert.assertEquals(expected, actual);        
        Assert.assertNotSame(expected, actual);        
    }
    
    @Test
    public void testCheckBetterNoCurrent() {
        Stat current = new Stat();
        
        Stat prior = new Stat();
        prior.put("single", 1);
        
        List<Failure> failures = prior.checkIsBetter(current);
        Assert.assertTrue(failures.isEmpty());
    }
    
    @Test
    public void testCheckBetterNoPrior() {
        Stat current = new Stat();
        current.put("single", 1);
        
        Stat prior = new Stat();
        List<Failure> failures = prior.checkIsBetter(current);
        Assert.assertTrue(failures.isEmpty());
    }
    
    @Test
    public void testCheckBetterIsSame() {
        Stat current = new Stat();
        current.put("single", 1);
        
        List<Failure> failures = current.checkIsBetter(current);
        Assert.assertTrue(failures.isEmpty());
    }
    
    @Test
    public void testCheckBetterIsBetter() {
        Stat current = new Stat();
        current.put("single", 1);
        
        Stat prior = new Stat();
        prior.put("single", 2);
        List<Failure> failures = prior.checkIsBetter(current);
        Assert.assertTrue(failures.isEmpty());
    }
    
    @Test
    public void testCheckBetterHasDegraded() {
        Stat current = new Stat();
        current.put("single", 20);
        
        Stat prior = new Stat();
        prior.put("single", 10);
        List<Failure> failures = prior.checkIsBetter(current);
        Assert.assertEquals(1, failures.size());
        
        Failure failure = failures.get(0);
        Assert.assertEquals("single", failure.fieldName);
        Assert.assertEquals(10, failure.priorValue);
        Assert.assertEquals(20, failure.currentValue);
    }
    
    @Test
    public void testCheckBetterHasDegradedSubvalue() {
        Stat current = new Stat();
        Stat inner = new Stat();
        inner.put("single", 200);
        current.put("inner", inner);
        
        Stat prior = new Stat();
        inner = new Stat();
        inner.put("single", 100);
        prior.put("inner", inner);

        List<Failure> failures = prior.checkIsBetter(current);
        Assert.assertEquals(1, failures.size());
        
        Failure failure = failures.get(0);
        Assert.assertEquals("inner.single", failure.fieldName);
        Assert.assertEquals(100, failure.priorValue);
        Assert.assertEquals(200, failure.currentValue);
    }
}
