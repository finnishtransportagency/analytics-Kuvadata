package com.cgi.liikennevirasto.jalonne.jalonnejsonsplitter;

import com.amazonaws.services.s3.model.ObjectMetadata;

public class SavedObject {
	public String json;
	public String s3Path;
	public String bucketName;
	public ObjectMetadata oMetadata;

	public SavedObject (String json, String s3Path, String bucketName,ObjectMetadata oMetadata) {
		this.json=json;
		this.s3Path=s3Path;
		this.bucketName=bucketName;
		this.oMetadata=oMetadata;
	}
}
