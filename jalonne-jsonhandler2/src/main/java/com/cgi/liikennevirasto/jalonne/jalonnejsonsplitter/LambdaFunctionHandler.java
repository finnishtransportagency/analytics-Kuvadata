package com.cgi.liikennevirasto.jalonne.jalonnejsonsplitter;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
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
		String region=System.getenv("Cregion");
		AmazonS3 s3Client = AmazonS3Client.builder().withRegion(region).build();
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
				
				//koordinaattimuunnokset
				List<Double> koordinaatit = picMetadata.geometry.coordinates;
																//longX					//latY
				Point2D.Double fromETRS89 = new Point2D.Double(koordinaatit.get(1), koordinaatit.get(0));
				Point2D.Double toWGS84 = Koordinaattimuuntaja.convertFromETRS89ToWGS84(fromETRS89);
				List<Double> muunnetutkoordinaatit = new ArrayList<Double>();
				muunnetutkoordinaatit.add(toWGS84.getX()); //jarjestys 0
				muunnetutkoordinaatit.add(toWGS84.getY()); //jarjestys 1, koska tama jarjestys oletetaan seuraavassa vaiheessa
				picMetadata.geometry.coordinates = muunnetutkoordinaatit;
				
				String generatedString = UUID.randomUUID().toString().replace("-", "");
				// save location format: date/maintainer/<random_string>id<id>
				String savelocation= new SimpleDateFormat("dd.MM.yyyy").format(new Date()) +"/" + System.getenv("Maintainer").toLowerCase()+ "/"+ generatedString+"id"+picMetadata.id+".json";
				
				Runnable s3writer= new S3Writer(gson.toJson(picMetadata), s3Client, System.getenv("s3Bucket"),savelocation,context,failedThread);
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
		s3Client.shutdown();
		if (failedThread)
		System.exit(-1);
		return "Success";
	}
}