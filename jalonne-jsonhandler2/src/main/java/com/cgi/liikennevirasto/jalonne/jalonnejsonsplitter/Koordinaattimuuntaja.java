package com.cgi.liikennevirasto.jalonne.jalonnejsonsplitter;

import java.awt.geom.Point2D;

import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.osgeo.proj4j.ProjCoordinate;

public class Koordinaattimuuntaja {

	private static final CoordinateTransform transformerFromEtrs89Tm35FinToWgs84;
	private static final boolean DEBUG = false;
	
	static {
        CRSFactory crsFactory = new CRSFactory();

        // WGS84: http://spatialreference.org/ref/epsg/4326/ -> Proj4
        CoordinateReferenceSystem wgs84 = crsFactory.createFromName("EPSG:4326");

        // ETRS89-TM35FIN/EUREF-FIN http://spatialreference.org/ref/epsg/etrs89-etrs-tm35fin/ -> Proj4
        CoordinateReferenceSystem etrs89tm35fin = crsFactory.createFromName("EPSG:3067");

        CoordinateTransformFactory coordinateTransformFactory = new CoordinateTransformFactory();
        // ETRS89-TM35FIN to WGS84 transformer
        transformerFromEtrs89Tm35FinToWgs84 = coordinateTransformFactory.createTransform(etrs89tm35fin, wgs84);
    }
	
    public static Point2D.Double convertFromETRS89ToWGS84(Point2D.Double fromETRS89) {
    	if(fromETRS89 == null) throw new IllegalArgumentException("Muunnettava karttapiste ei voi olla null");
        return convert(fromETRS89, transformerFromEtrs89Tm35FinToWgs84);
    }
	
	private static Point2D.Double convert(Point2D.Double fromETRS89, final CoordinateTransform transformer) {
        ProjCoordinate to = new ProjCoordinate();
        ProjCoordinate from = new ProjCoordinate(fromETRS89.getX(),fromETRS89.getY());
        transformer.transform(from, to);
        Point2D.Double point = new Point2D.Double(to.x, to.y);

        if (DEBUG) {
            System.out.println("From: " + fromETRS89 + " to " + point);
        }
        return point;
    }
	
}
