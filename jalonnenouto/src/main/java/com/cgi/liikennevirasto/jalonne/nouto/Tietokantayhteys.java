package com.cgi.liikennevirasto.jalonne.nouto;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3URI;
import com.cgi.liikennevirasto.jalonne.nouto.JSONtoObject.PictureMetadata;

public class Tietokantayhteys {
    //String url = System.getenv("dbUrl");
	String dbAddress = System.getenv("dbWriteURL");
	String dbPort = System.getenv("dbport");
    String username = System.getenv("dbUser");
    String password = System.getenv("dbPassword");
    Connection conn = null;
    
    //Lambda containerille mahdollisuus kierrattaa luokkaa ja yhteytta
    public Tietokantayhteys(){
    	try {
    		System.out.println("## JDBC connection attempt");
    		System.out.println("## URL: " +getDbUrl());
    		System.out.println("## User:" + username);
			conn = DriverManager.getConnection(getDbUrl(), username, password);
			System.out.println("## Connection success!");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
    
    String getDbUrl() {
    	return "jdbc:postgresql://" + dbAddress + ":" + dbPort + "/kuvatieto";
    }
	
	public int updateKuvadata(Context context, PictureMetadata picture, AmazonS3URI uri) {
		LambdaLogger logger = context.getLogger();
	    logger.log("## updateKuvadata kutsuttu");
	    int result = -1;
	    
	    try {
	    	if(conn == null) conn = DriverManager.getConnection(getDbUrl(), username, password);
	        PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(pic_aws_id)"
	        		+ "FROM kuvatieto.pic_metadata");
	        //TODO:
	        //result = stmt.executeUpdate("UPSERT kuva");
	        ResultSet resultSet = stmt.executeQuery();
	        int kuvia = 0;
	        if(resultSet.next()) {
	        	kuvia = resultSet.getInt("count");
	        }
	        
	        result = kuvia;

	        logger.log("## Tiedot viety tietokantaan");

	      } catch (Exception e) {
	        logger.log("## Tietokantayhteydessa virhe: " + e.getMessage());
	      }
	    
		return result;
	}

}
