package com.cgi.liikennevirasto.jalonne.nouto;

import java.net.URI;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.cgi.liikennevirasto.jalonne.nouto.JSONtoObject.PictureMetadata;

public class LambdaFunctionHandler implements RequestHandler<S3Event, String> {
	final String dstBucketImages = System.getenv("s3BucketImages");
	final String dstBucketJobs = System.getenv("s3BucketJobs");
	//Lambda jaadyttaa ja mahdollisesti kierrattaa handlerin ulkopuolisia muuttujia (ja yhteyksia)
	//Yhtaikaisten yhteyksien riittavyys varmistetaan taman lambdan max 100 yhtaikaisella suorituksella
	Tietokantayhteys tietokantayhteys = new Tietokantayhteys();

	private AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();

	public LambdaFunctionHandler() {
	}

	// Test purpose only.
	LambdaFunctionHandler(AmazonS3 s3) {
		this.s3 = s3;
	}

	@Override
	public String handleRequest(S3Event event, Context context) {
		LambdaLogger logger = context.getLogger();
		logger.log("## Received event: " + event);

		// Get the object from the event and show its content type
		String bucket = event.getRecords().get(0).getS3().getBucket().getName();
		String key = event.getRecords().get(0).getS3().getObject().getKey();
		S3jobReader s3reader = new S3jobReader();
		JSONtoObject j20 = new JSONtoObject();
		
		boolean updateok = false;
		try {
			//Hae tapahtuman laukaissut tiedosto ja muunna se jsoniksi
			S3Object response = s3.getObject(new GetObjectRequest(bucket, key));
			String contentType = response.getObjectMetadata().getContentType();
			context.getLogger().log("## CONTENT TYPE: " + contentType);
			String json = s3reader.urlReader(response.getObjectContent(), context);
			
			//Muuna json, hae metatietojen urlia vastaava tiedosto
			PictureMetadata pictureMetadata = j20.json2Object(json);
			URI srcPictureURL = new URI(pictureMetadata.url);
			context.getLogger().log("## kuvan url: " + srcPictureURL);
			AmazonS3URI amazonS3URI = new AmazonS3URI(srcPictureURL);
			String srcBucket = amazonS3URI.getBucket();
			String srcKey = amazonS3URI.getKey();
			context.getLogger().log("## s3 lahde: " + srcBucket + ", " + srcKey);
			//sama kuin lahde
			String dstKey = amazonS3URI.getKey();
			
			//Kopioi kuva s3 bucketien kesken
			CopyObjectResult copyObjectResult = s3.copyObject(srcBucket, srcKey, dstBucketImages, dstKey);

			//TODO: tallenna metatieto tietokantaan, jos kuvan kopiointi onnistunut
			String dstPictureURL = "s3://" + dstBucketImages + dstKey;
			AmazonS3URI amazonS3URI2 = new AmazonS3URI(dstPictureURL);
			
			int rows = tietokantayhteys.updateKuvadata(context, pictureMetadata, amazonS3URI2);
			if(rows > 0) updateok = true;
		} catch (Exception e) {
			e.printStackTrace();
			logger.log(String.format("Error getting object %s from bucket %s. Make sure they exist and"
					+ " your bucket is in the same region as this function.", key, bucket));
			try {
				throw e;
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		if(!updateok) System.exit(-1);
		return "Success";
	}

}