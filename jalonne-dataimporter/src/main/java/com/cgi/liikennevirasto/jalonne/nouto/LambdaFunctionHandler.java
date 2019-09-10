package com.cgi.liikennevirasto.jalonne.nouto;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.cgi.liikennevirasto.jalonne.nouto.JSONtoObject.PictureMetadata;

public class LambdaFunctionHandler implements RequestHandler<S3Event, String> {
	private final String JPEG_TYPE = (String) "jpeg";
	private final String JPEG_MIME = (String) "image/jpeg";
	private final String JPG_TYPE = (String) "jpg";
    private final String JPG_MIME = (String) "image/jpeg";
    private final String PNG_TYPE = (String) "png";
    private final String PNG_MIME = (String) "image/png";
	final String dstBucketImages = System.getenv("imagesS3Bucket");
	final String dstFolderImages = System.getenv("imagesFolder");

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
			logger.log("## Hae s3 objekti: " + bucket + ", " + key);
			//Hae tapahtuman laukaissut tiedosto ja muunna se jsoniksi
			S3Object response = s3.getObject(new GetObjectRequest(bucket, key));
			String contentType = response.getObjectMetadata().getContentType();
			context.getLogger().log("## CONTENT TYPE: " + contentType);
			String json = s3reader.urlReader(response.getObjectContent(), context);
			
			//Muuna json, hae metatietojen urlia vastaava kuvatiedosto
			PictureMetadata pictureMetadata = j20.json2Object(json);
			URI srcPictureURL = new URI(pictureMetadata.url);
			logger.log("## kuvan url: " + srcPictureURL);
			String filename = FilenameUtils.getName(srcPictureURL.getPath());
			
			// Ei tehdakaan tiedostyypin tarkastusta paatteen perusteella,
			// silla lahdetiedostot ovat ilman paatetta, mutta sovitusti jpg-kuvia... -9.9.2019
			
			// Tallennetaan tiedostot kuitenkin aws:aan tiedostopaatteella, 
			// jotta tableau ym osaavat hakea ja nayttaa kuvan oikein
			String awsFilename = filename + ".jpg";
	        
	        String imageType = JPG_TYPE;
			
			// siirrettavat kuvat eivat olekaan s3:ssa, vaan missa vaan palvelimella
			BufferedImage img = ImageIO.read(srcPictureURL.toURL());
			ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(img, imageType, os);
            InputStream is = new ByteArrayInputStream(os.toByteArray());
            // Set Content-Length and Content-Type
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(os.size());
            if (JPG_TYPE.equals(imageType)) {
                meta.setContentType(JPG_MIME);
            }
            if (PNG_TYPE.equals(imageType)) {
                meta.setContentType(PNG_MIME);
            }
            if (JPEG_TYPE.equals(imageType)) {
            	meta.setContentType(JPEG_MIME);
            }
            // tallennettaan kuva
            logger.log("## tallennetaan kuva s3 buckettiin: " + dstBucketImages + ", " + dstFolderImages + ", " + awsFilename);
			PutObjectResult putObjectResult = s3.putObject(dstBucketImages, dstFolderImages + awsFilename, is, meta);

			// tallenna metatieto tietokantaan, jos kuvan kopiointi onnistunut
			String dstPictureURL = "https://s3-eu-central-1.amazonaws.com/" + dstBucketImages + "/" + dstFolderImages + "/" + awsFilename;
			logger.log("## s3 url: " + dstPictureURL);
			AmazonS3URI amazonS3URI = new AmazonS3URI(dstPictureURL);
			
			int rows = tietokantayhteys.updateKuvadata(context, pictureMetadata, amazonS3URI);
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