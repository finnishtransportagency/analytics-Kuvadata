package com.amazonaws.lambda.nouto;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import com.amazonaws.lambda.nouto.JSONtoObject.PictureMetadata;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3URI;

public class Tietokantayhteys {
    String url = System.getenv("dbUrl");
    String username = System.getenv("dbUsername");
    String password = System.getenv("dbPassword");
    Connection conn = null;
    
    //Lambda containerille mahdollisuus kierrattaa luokkaa ja yhteytta
    public Tietokantayhteys(){
    	try {
			conn = DriverManager.getConnection(url, username, password);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public int updateKuvadata(Context context, PictureMetadata picture, AmazonS3URI uri) {
		LambdaLogger logger = context.getLogger();
	    logger.log("## updateKuvadata kutsuttu");
	    int result = -1;
	    
	    try {
	    	if(conn == null) conn = DriverManager.getConnection(url, username, password);
	        Statement stmt = conn.createStatement();
	        result = stmt.executeUpdate("UPSERT kuva");

	        logger.log("## Tiedot viety tietokantaan");

	      } catch (Exception e) {
	        logger.log("## Tietokantayhteydessa virhe: " + e.getMessage());
	      }
	    
		return result;
	}

}
