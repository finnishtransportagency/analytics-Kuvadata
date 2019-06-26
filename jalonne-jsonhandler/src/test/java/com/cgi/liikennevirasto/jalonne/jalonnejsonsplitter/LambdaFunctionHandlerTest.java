package com.cgi.liikennevirasto.jalonne.jalonnejsonsplitter;

import java.io.IOException;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.amazonaws.services.lambda.runtime.Context;

/**
 * A simple test harness for locally invoking your Lambda function handler.
 */
public class LambdaFunctionHandlerTest {

    private static Object input;

    @BeforeClass
    public static void createInput() throws IOException {
        // TODO: set up your sample input object here.
        input = null;
    }

    @Rule
    public final EnvironmentVariables environmentVariables
      = new EnvironmentVariables();
    
    private Context createContext() {
        TestContext ctx = new TestContext();

        // TODO: customize your context here if needed.
        ctx.setFunctionName("Your Function Name");

        return ctx;
    }
    
    @Test
    public void testLambdaFunctionHandler() {
    	environmentVariables.set("datasource", "https://s3.eu-central-1.amazonaws.com/jalonne-server-mock/images");
    	environmentVariables.set("Maintainer", "Test");
    	environmentVariables.set("Cregion", "eu-central-1"); //s3Bucket
    //	environmentVariables.set("s3Bucket", "testbucket");
    	environmentVariables.set("s3Bucket", "jalonnejobbucketdev");
    	
    	LambdaFunctionHandler handler = new LambdaFunctionHandler();
        Context ctx = createContext();

        String output = handler.handleRequest(input, ctx);

        // TODO: validate output here if needed.
        Assert.assertEquals("Success", output);
    }
    
    

    
    
}
