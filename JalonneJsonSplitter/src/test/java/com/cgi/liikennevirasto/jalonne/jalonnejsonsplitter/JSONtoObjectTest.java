package com.cgi.liikennevirasto.jalonne.jalonnejsonsplitter;


import com.cgi.liikennevirasto.jalonne.jalonnejsonsplitter.JSONtoObject.PictureMetadata;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import org.junit.Assert;
import org.junit.Test;


public class JSONtoObjectTest {
	

	
	
    @Test
    public void jsonToObject() {

    	JSONtoObject jo= new JSONtoObject();
    	PictureMetadata[] metadata=jo.json2Object(getReourceFile("testjson.json"));
    	Assert.assertEquals(2, metadata.length);
    	Assert.assertEquals(301, metadata[0].id);
    	Assert.assertEquals("2018-05-14T14:05:40+00:00", metadata[0].properties.time);
    	Assert.assertEquals(203, metadata[0].properties.attitude.bearing);
    	Assert.assertEquals(0, metadata[0].properties.attitude.elevation);
    	Assert.assertEquals(0, metadata[0].properties.attitude.bank);
    	Assert.assertEquals("3e494f1222f38a6edfb9919add566bf0d4df03e8f0a98aa7dd5b8c2f13ecfc5f", metadata[0].properties.digest.sha256);
    	Assert.assertEquals("7115601.59", metadata[0].geometry.coordinates.get(0).toString());
    	Assert.assertEquals("383687.05", metadata[0].geometry.coordinates.get(1).toString());
    	Assert.assertEquals("Point", metadata[0].geometry.type);
    	Assert.assertEquals(302, metadata[1].id);

    	
    
    
    
    
    }








    private String getReourceFile(String fileName) {

    	StringBuilder sb = new StringBuilder("");

    	//Get file from resources folder
    	ClassLoader classLoader = getClass().getClassLoader();
    	File file = new File(classLoader.getResource(fileName).getFile());

    	try (Scanner scanner = new Scanner(file)) {

    		while (scanner.hasNextLine()) {
    			String line = scanner.nextLine();
    			sb.append(line).append("\n");
    		}

    		scanner.close();

    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    		
    	return sb.toString();

      }




}




