package com.amazonaws.lambda.nouto;

import java.util.List;


import com.google.gson.Gson;

public class JSONtoObject {

	
	
	public PictureMetadata json2Object(String jsonString) {
		Gson gson = new Gson();
		gson.serializeNulls();;
	return	gson.fromJson(jsonString, PictureMetadata.class);
	}
	
	class Attitude {
		int bearing;
		int elevation;
		int bank;

	}
	class PictureMetadata{
		String type;
		int id;
		Geometry geometry;
		String maintainer;
		Properties properties;
		String url;
	}


	class Properties{
		String time;
		Digest digest;
		Attitude attitude;
	}

	class Digest{
		String sha256;
	}

	class Geometry {
		String type;
		List<Double> coordinates;	
	}

	
}
