package com.cgi.liikennevirasto.jalonne.jalonnejsonsplitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;


import com.amazonaws.services.lambda.runtime.Context;

public class readJsonFromURL {


	public String urlReader(String urlString, Context context ) throws IOException {
		StringBuilder sb= new StringBuilder();
		sb.append("");
		try {
			URL url = new URL(urlString);
			BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
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
	
