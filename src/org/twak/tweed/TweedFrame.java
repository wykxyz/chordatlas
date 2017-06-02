package org.twak.tweed;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.vecmath.Vector3d;

import org.twak.tweed.gen.Gen;
import org.twak.utils.ImageU;
import org.twak.utils.PaintThing;
import org.twak.utils.WeakListener;
import org.twak.utils.geom.HalfMesh2;
import org.twak.utils.geom.ObjDump;
import org.twak.utils.ui.ListDownLayout;
import org.twak.utils.ui.ListRightLayout;
import org.twak.utils.ui.Show;
import org.twak.utils.ui.WindowManager;
import org.twak.viewTrace.SuperMeshPainter;

import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeCanvasContext;
import com.thoughtworks.xstream.XStream;

public class TweedFrame {
	
	public Tweed tweed;
	Canvas canvas;
	public JFrame frame;
	
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	
	public static boolean HEADLESS = false;
	public static TweedFrame instance;
	
	public TweedFrame() {
		
		instance = this;
		
		frame = new JFrame();
		
		WindowManager.register( frame );
		
		Dimension d3Dim = new Dimension (1024, 640);
		
		AppSettings settings = new AppSettings(true);
		
		settings.setWidth(d3Dim.width);
		settings.setHeight(d3Dim.height);
		settings.setSamples(4);
		settings.setVSync(true);
		settings.setFrameRate(60);
		
		tweed = new Tweed( this );
		tweed.setSettings(settings);
		tweed.createCanvas();
		JmeCanvasContext ctx = (JmeCanvasContext) tweed.getContext();
		ctx.setSystemListener(tweed);
		
		canvas = ctx.getCanvas();
		canvas.setPreferredSize(d3Dim);
		
		frame.setLayout(new BorderLayout());
		frame.add(buildUI(), BorderLayout.EAST);
		frame.add(canvas, BorderLayout.CENTER);
		frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		
	    frame.addWindowListener( new WindowAdapter() {
	    	public void windowClosing(WindowEvent e) {
	    		TweedSettings.save();
	    	};
		} );
	    
	    scheduler.scheduleAtFixedRate(new Runnable() {
	    	@Override
	    	public void run() {
	    		TweedSettings.save();
	    	}
	    }, 30, 30, TimeUnit.SECONDS);
	    
	    scheduler.scheduleAtFixedRate(new Runnable() {
	    	@Override
	    	public void run() {
	    		Vector3d pt = tweed.cursorPosition;
	    		if (coordLabel != null)
	    			coordLabel.setText( pt == null ? "..." : String.format( "%.4f %.4f", pt.x, pt.z) );
	    		
	    		JFrame.setDefaultLookAndFeelDecorated(true);

	    		
	    	}
	    }, 100, 100, TimeUnit.MILLISECONDS);
		
	    frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
	    frame.pack();
	    frame.setVisible(!HEADLESS);
		
		tweed.startCanvas();
	}
	
	public List<Gen> genList = new ArrayList<Gen>(); 
	JPanel genUI = new JPanel();
	JPanel layerList;
	
	public JComponent buildUI() {
		
		JPanel out = new JPanel(new BorderLayout());

		layerList = new JPanel (new ListDownLayout());
		
		JScrollPane listScroll = new JScrollPane(layerList);
		listScroll.getVerticalScrollBar().setUnitIncrement( 50 );
		listScroll.setPreferredSize(new Dimension (200, 300) );
		
		JSplitPane pane = new JSplitPane( JSplitPane.VERTICAL_SPLIT,  listScroll, genUI );
//		pane.setResizeWeight(0.2);
		
		out.add ( pane, BorderLayout.CENTER );
		
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar( menuBar );

		//Build the first menu.
		JMenu menu = new JMenu("File");
		menuBar.add(menu);

		//a group of JMenuItems
		JMenuItem save = new JMenuItem("save", KeyEvent.VK_S);
		save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
//		menu.add(save);
		
		save.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					new XStream().toXML( new TweedSave(genList), new FileOutputStream( Tweed.CONFIG +"save.gens") ) ;
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}
			}
		});
		
		JMenuItem load = new JMenuItem("open", KeyEvent.VK_S);
		load.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
//		menu.add(load);
		load.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					setGens ( (TweedSave) new XStream().fromXML( new FileInputStream ( Tweed.CONFIG +"save.gens" ) ) );
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}
			}
		});
		
		JMenuItem remove = new JMenuItem("delete gen", KeyEvent.VK_MINUS);
		remove.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ActionEvent.CTRL_MASK));
		menu.add(remove);
		remove.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (selectedGen != null)
					removeGen(selectedGen);
		    };
		});
		
		JMenuItem resetBG = new JMenuItem("reset background", KeyEvent.VK_MINUS);
		resetBG.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ActionEvent.CTRL_MASK));
		menu.add(resetBG);
		resetBG.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tweed.enqueue(new Runnable() {
					@Override
					public void run() {
						TweedFrame.this.tweed.clearBackground();
					}
				});
		    };
		});
		
		JMenuItem obj = new JMenuItem("export obj", KeyEvent.VK_O);
		obj.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, ActionEvent.CTRL_MASK));
		menu.add(obj);
		obj.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
				ObjDump dump = new ObjDump();
				
				for (Gen g : genList) 
					if (g.visible && g instanceof IDumpObjs) 
						((IDumpObjs)g).dumpObj(dump);
				
				dump.dump( new File ( Tweed.CONFIG + "all.obj") );
				
		    };
		});
		
		JMenuItem resetCam = new JMenuItem("reset view", KeyEvent.VK_R);
		resetCam.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));
		menu.add(resetCam);
		resetCam.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tweed.resetCamera();
		    };
		});
		
		JPanel toolPanel = new JPanel( new ListRightLayout() );

		tweed.addUI(toolPanel);
		
		coordLabel = new JLabel("...");
		coordLabel.setHorizontalAlignment( SwingConstants.CENTER );
		
		out.add(toolPanel, BorderLayout.NORTH);
		out.add(coordLabel, BorderLayout.SOUTH);
		
		return out;
	}
	
	JLabel coordLabel;
	
	private void setGens(TweedSave fromXML) {

		for (Gen g : genList)
			removeGen(g);
		
		layerList.removeAll();
		genList.clear();
		
		for (Gen g : fromXML.gens) {
			g.gNode = new Node();
			g.tweed = tweed;
		}
		
		for (Gen g : fromXML.gens) {
			addGen (g, true);
		}
	}

	public void removeBelowGen( Gen below ) {
		
		boolean seen = false;
		
		List<Gen> togo = new ArrayList<>();
		
		for (Gen g : genList){
			
			if (g == below)
				seen = true;
			
			
			if (seen)
				togo.add(g);
				
		}
		
		for (Gen g : togo)
			removeGen( g );
	}
	
	public void removeGen(Gen gen) {
		
		tweed.enqueue( new Runnable() {
			@Override
			public void run() {
				gen.gNode.removeFromParent();
			}
		});
		
		
		genList.remove(gen);
		
		Component togo = null;
		for (Component c : layerList.getComponents() ) {
			if (c instanceof GenListItem && ((GenListItem)c).gen == gen )
				togo = c;
		}
		
		if (selectedGen == gen)
			selectedGen = null;
		
		if (togo != null)
			layerList.remove( togo );
		
		layerList.revalidate();
		layerList.repaint();
	}
	
	public void removeGens( Class<?> klass ) {
		for ( Gen g : new ArrayList<>(genList) )
			if ( g.getClass() == klass )
				removeGen( g );
	}
	
	public void addGen(Gen gen, boolean visible) {
		
		gen.visible = visible;
		
		layerList.add(new GenListItem(gen, selectedGenListener, this, new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				setSelected(gen);
			}
		}) );
		
		genList.add(gen);
		layerList.revalidate();
		layerList.repaint();
		
		tweed.enqueue( new Runnable() {
			@Override
			public void run() {
				try {
					gen.calculate();
				} catch ( Throwable th ) {
					th.printStackTrace();
				}
				
				tweed.getRootNode().updateGeometricState();
				tweed.getRootNode().updateModelBound();
				tweed.gainFocus();
			}
		});
		
	}
	
	public Gen selectedGen;
	public WeakListener selectedGenListener = new WeakListener();
	
	public void setSelected(Gen gen) {
		
		setGenUI( gen.getUI() );
		
		if (selectedGen == gen)
			return;
		
		selectedGen = gen;
		
		selectedGenListener.fire();
	}
	
	public static void main (String[] args) throws Throwable {
		
		WindowManager.init( "chordatlas", "/org/twak/tweed/resources/icon128.png" );
		
		UIManager.put("Slider.paintValue", false);
		
		JPopupMenu.setDefaultLightWeightPopupEnabled(false); // show menus over 3d canvas
		ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
		
		PaintThing.lookup.put( HalfMesh2.class, new SuperMeshPainter() );
		
		new TweedFrame();
	}

	public void somethingChanged() {
		canvas.repaint();
	}

	public void setGenUI( JComponent ui ) {
		
		genUI.removeAll();
		genUI.add(ui);
		genUI.revalidate();
		genUI.doLayout();
		genUI.repaint();
		
	}

}