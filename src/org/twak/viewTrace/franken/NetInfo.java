package org.twak.viewTrace.franken;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.twak.utils.ImageU;
import org.twak.utils.geom.DRectangle;

public class NetInfo {

	public String netName;
	public int sizeZ;
	public int resolution;
	public String name;
	public Color emptyColour;
	public BufferedImage icon;
	public boolean visible;
	
	public NetInfo( String name, int sizeZ, int resolution, Color empty, String icon, boolean visible ) {
		this.name = name;
		this.sizeZ = sizeZ;
		this.resolution = resolution;
		this.emptyColour = empty;
		this.icon = ImageU.cacheResource.get( "/org/twak/tweed/resources/"+icon );
		this.visible = visible;
	}
	
	public final static Map  <Class<? extends App> , NetInfo> index = new HashMap<>();
	public final static List <Class<? extends App>> evaluationOrder = new ArrayList<>();
	
	static {
		index.put ( d( BlockApp.class        ), new NetInfo ( "block"          , 0, 0  , null      , "block.png"          , true ) );
		index.put ( d( BuildingApp.class     ), new NetInfo ( "building"       , 0, 0  , null      , "building.png"       , true ) );
		
		index.put ( d( FacadeLabelApp.class  ), new NetInfo ( "facade labels"  , 8, 256, Color.blue, "facade_label.png"   , true ) );
		index.put ( d( FacadeTexApp.class    ), new NetInfo ( "facade textures", 8, 256, Color.blue, "facade_tex.png"     , true ) );
		index.put ( d( FacadeGreebleApp.class), new NetInfo ( "facade greebles", 0, 256, Color.blue, "facade_greeble.png" , true ) );
		
		index.put ( d( RoofGreebleApp.class  ), new NetInfo ( "roof greebles"  , 8, 512, Color.blue, "roof_label.png"     , true ) );
		index.put ( d( RoofTexApp.class      ), new NetInfo ( "roof textures"  , 8, 512, Color.blue, "roof_tex.png"       , true ) );
		
		index.put ( d( VeluxTexApp.class     ), new NetInfo ( "pane textures"  , 8, 256, Color.red,  "roof_tex.png"       , false) );
		
		index.put ( d( PanesLabelApp.class   ), new NetInfo ( "pane labels"    , 8, 256, Color.red , "pane_label.png"     , true ) );
		index.put ( d( PanesTexApp.class     ), new NetInfo ( "pane textures"  , 8, 256, Color.red , "pane_tex.png"       , true ) );
		
		index.put ( d( DoorTexApp.class      ), new NetInfo  ( "door textures" , 8, 256, Color.red , "door_tex.png"       , true ) );
		
		index.put ( d( FacadeSuperApp.class  ), new NetInfo ( "facade super"   , 8, 256, null      , "facade_super.png"   , true ) );
		index.put ( d( RoofSuperApp.class    ), new NetInfo ( "roof super"     , 8, 256, null      , "roof_super.png"     , true ) );
	}
	
	private static Class<? extends App> d (Class<? extends App> k) {
		evaluationOrder.add( k );
		return k;
	}
	
	public static NetInfo get( App exemplar ) {
		return index.get(exemplar.getClass());
	}
	
	public static NetInfo get( Class exemplar ) {
		return index.get(exemplar);
	}

	public DRectangle rect() {
		return new DRectangle(resolution, resolution);
	}
}
