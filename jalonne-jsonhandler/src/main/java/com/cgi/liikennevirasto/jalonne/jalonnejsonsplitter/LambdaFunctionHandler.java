package com.cgi.liikennevirasto.jalonne.jalonnejsonsplitter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.cgi.liikennevirasto.jalonne.jalonnejsonsplitter.JSONtoObject.PictureMetadata;
import com.google.gson.Gson;

/**
 * In this lambda we read in from given environmental source URL JSON. Split it so that each image has it's own
 * JSON file. We upload each JSON to s3 jobbucket using threads so we minimize time consumed on sending tasks to jobbucket. 
 * Threads are limited by using blocking queue with newFixedThreadPool method.
 * @author alapeijario
 *
 */
public class LambdaFunctionHandler implements RequestHandler<Object, String> {
	String urlToFetch=System.getenv("datasourceurl");
	private Boolean failedThread=false;
	@Override
	public String handleRequest(Object input, Context context) {
		context.getLogger().log("Url to read: " + urlToFetch);
		Gson gson = new Gson();
		readJsonFromURL urlReader= new readJsonFromURL();
		try {
			String jsonString = urlReader.urlReader(urlToFetch,context);
			context.getLogger().log("Alkuperainen JSON luettu pituus" + jsonString.length());
			JSONtoObject listOfMeta= new JSONtoObject();
			PictureMetadata[] metadata=listOfMeta.json2Object(jsonString);
			context.getLogger().log("## Size of picture meta array: " + metadata.length);
			ExecutorService pool = Executors.newFixedThreadPool(512); // limit for maximum write threads
			for (PictureMetadata picMetadata: metadata) {
				picMetadata.maintainer=System.getenv("Maintainer");
				picMetadata.url=urlToFetch+"/"+picMetadata.id; // url to fetch image 
				String generatedString = UUID.randomUUID().toString().replace("-", "");
				// save location format: date/maintainer/<random_string>id<id>
				String savelocation= new SimpleDateFormat("dd.MM.yyyy").format(new Date()) +"/" + System.getenv("Maintainer").toLowerCase()+ "/"+ generatedString+"id"+picMetadata.id+".json";
				String region=System.getenv("Cregion");
				Runnable s3writer= new S3Writer(gson.toJson(picMetadata), region, System.getenv("s3Bucket"),savelocation,context,failedThread);
				pool.execute(s3writer);
			}
			pool.shutdown();			
			while (!pool.isTerminated()) {
				try {
					pool.awaitTermination(4, TimeUnit.MINUTES);
				} catch (InterruptedException e) {
					String errorMessage="Error: Threading failure";
					context.getLogger().log(errorMessage);
					System.err.println(errorMessage);
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			String errorMessage="Error: Failed to read json from URL";
			context.getLogger().log(errorMessage);
			System.err.println(errorMessage);
			e.printStackTrace();
		}
		if (failedThread)
		System.exit(-1);
		return "Success";
	}
}