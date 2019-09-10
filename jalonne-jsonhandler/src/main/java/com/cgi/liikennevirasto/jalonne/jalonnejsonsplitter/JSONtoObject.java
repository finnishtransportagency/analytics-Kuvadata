package com.cgi.liikennevirasto.jalonne.jalonnejsonsplitter;

import java.util.List;

import com.google.gson.Gson;

public class JSONtoObject {

	
	
	public PictureMetadata[] json2Object(String jsonString) {
		Gson gson = new Gson();
		gson.serializeNulls();;
		FeatureCollection collection = gson.fromJson(jsonString, FeatureCollection.class);
		return collection.features;
	}
	
	class FeatureCollection {
		String type;
		PictureMetadata[] features;
	}
	
	class Attitude {
		double bearing;
		double elevation;
		double bank;

	}
	class PictureMetadata{
		String type;
		String id;
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
