package com.amazonaws.lambda.nouto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import com.amazonaws.services.lambda.runtime.Context;

public class S3jobReader {

	public String urlReader(InputStream is, Context context ) throws IOException {
		StringBuilder sb= new StringBuilder();
		sb.append("");
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String str = "";
			while (null != (str = br.readLine())) {
				sb.append(str);
			}			
		} catch (Exception ex) {
			String errorMessage="Error: Failed to read json from URL";
			context.getLogger().log(errorMessage);
			System.err.println(errorMessage);
			ex.printStackTrace();
		}
		return sb.toString();
		}
	
}
