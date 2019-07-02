package com.cgi.liikennevirasto.jalonne.nouto;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import org.postgis.Geometry;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgresql.geometric.PGpoint;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3URI;
import com.cgi.liikennevirasto.jalonne.nouto.JSONtoObject.PictureMetadata;

public class Tietokantayhteys {
	String dbAddress = System.getenv("dbWriteURL");
	String dbPort = System.getenv("dbport");
    String username = System.getenv("dbUser");
    String password = System.getenv("dbPassword");
    Connection conn = null;
    static final String insertSql = 
    		"INSERT INTO kuvatieto.pic_metadata (pic_orig_id, pic_orig_file, pic_aws_file, pic_date,"
    		+ " pic_longitude, pic_latitude, pic_bearing, pic_bank, pic_aws_load_timestamp, pic_geometry,"
    		+ " pic_time, pic_anonymized) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    //Lambda containerille mahdollisuus kierrattaa luokkaa ja yhteytta
    public Tietokantayhteys(){
    	try {
    		System.out.println("## JDBC connection attempt");
    		System.out.println("## URL: " +getDbUrl());
    		System.out.println("## User:" + username);
			conn = DriverManager.getConnection(getDbUrl(), username, password);
			((org.postgresql.PGConnection)conn).addDataType("geometry", org.postgis.PGgeometry.class);
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

		Date now = new Date();
	    
	    try {
	    	if(conn == null) conn = DriverManager.getConnection(getDbUrl(), username, password);
	        PreparedStatement stmt = conn.prepareStatement(insertSql);
			Point point = getPoint(picture); 
			PGgeometry pGgeometry = new PGgeometry(point);
			Time kuvanaika = getSqlTime(picture.properties.time);
			java.sql.Date kuvanpvm = getSqlDate(picture.properties.time);
			
	        stmt.setInt(1, picture.id);
	        stmt.setString(2, "orig_file");
	        stmt.setString(3, "aws_file");
	        stmt.setDate(4, kuvanpvm);
	        stmt.setDouble(5, point.x);
	        stmt.setDouble(6, point.y);
	        stmt.setInt(7, picture.properties.attitude.bearing);
	        stmt.setInt(8, picture.properties.attitude.bank);
	        stmt.setTimestamp(9, new Timestamp(now.getTime()));
	        stmt.setObject(10, pGgeometry);
	        stmt.setTime(11, kuvanaika);
	        stmt.setBoolean(12, false);
	        
	        result = stmt.executeUpdate();

	        logger.log("## SQL: " + insertSql);
	        logger.log("## SQL parametrit: " + picture.id + ",orig_file" 
	        + ",aws_file" + ","+ kuvanpvm + ","+point.x + ","+point.y + ","+picture.properties.attitude.bearing 
	        + ","+picture.properties.attitude.bank + ","+now.getTime() + ","+point.toString() + ","+kuvanaika + ",false");
	        logger.log("## Tiedot viety tietokantaan: " + result);

	      } catch (SQLTimeoutException e) {
	    	  logger.log("## Aikakatkaisu tietokantaoperaatiossa: " + e.getMessage());
	      } catch (SQLException e) {
	    	  logger.log("## Tietokantayhteydessa virhe: " + e.getMessage());
	      } catch (Exception e) {
	    	  logger.log("## Odottamaton poikkeus tietokantatallennuksessa: " + e.getMessage());
	      }
	    
		return result;
	}
	
	static Point getPoint(PictureMetadata picture) {
		List<Double> koordinaatit = picture.geometry.coordinates;
		Point point = null;
		try {
			point = new Point(koordinaatit.get(0), koordinaatit.get(1));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return point;
	}
	
	static java.sql.Date getSqlDate(String sqldate) {
		java.sql.Date pvm = null;
		try {
			LocalDateTime localDateTime;
			localDateTime = LocalDateTime.parse(sqldate, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			pvm = java.sql.Date.valueOf(localDateTime.toLocalDate());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return pvm;
	}
	
	static Time getSqlTime(String sqltime) {
		Time aika = null;
		try {
			LocalDateTime localDateTime;
			localDateTime = LocalDateTime.parse(sqltime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			aika = Time.valueOf(localDateTime.toLocalTime());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return aika;
	}
	
	public static void main(String[] args) {
		String sqldate = "2018-05-14T14:05:40+00:00";
		String sqltime = "2018-05-14T14:05:40+00:00";
		
		System.out.println("Paivamaara: " + Tietokantayhteys.getSqlDate(sqldate));
		System.out.println("Aika: " + Tietokantayhteys.getSqlTime(sqltime));
	}

}
