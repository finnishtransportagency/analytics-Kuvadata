package com.cgi.liikennevirasto.jalonne.jalonnejsonsplitter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

public class S3Writer implements Runnable{

	private String json;
	private AmazonS3 s3Client;
	private String bucketName;
	private String savePathAndFileName;
	private Context context;
	@SuppressWarnings("unused")
	private Boolean failed; //tells main thread if one of the writer has failed, and alert us if we should check logs

	public S3Writer(String json,AmazonS3 s3Client, String bucketName, String savePath,Context context, Boolean failed){
		this.json=json;
		this.s3Client=s3Client;
		this.bucketName=bucketName;
		this.savePathAndFileName=savePath;
		this.context = context;
		this.failed=failed;
	}

	public void run(){
		try {
			byte[] stringbytearray= json.getBytes("UTF-8");
			InputStream byteString = new ByteArrayInputStream(stringbytearray);
			ObjectMetadata oMetadata = new ObjectMetadata();
			oMetadata.setContentType("plain/text");
			oMetadata.setContentLength(stringbytearray.length);
			//if (!System.getenv("Maintainer").equals("Test")) //in test mode we dont want to test writing 
				s3Client.putObject(bucketName,savePathAndFileName, byteString,oMetadata);
			System.out.println("Json splitted successfully");
		} catch (UnsupportedEncodingException e) {
			String errorMessage="Failure to generate random string for picture id: " + savePathAndFileName;
			context.getLogger().log(errorMessage);
			failed=true;
			System.err.println(errorMessage);
			e.printStackTrace();
		}
		catch (Exception e) {
			failed=true;
			String errorMessage="Error: S3 write error " + savePathAndFileName;
			context.getLogger().log(errorMessage);
			System.err.println(errorMessage);
			e.printStackTrace();
		}
	}

}
