package de.unipaderborn.visuflow.graphdisplaycomponent;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Iterator;
import java.util.ListIterator;

import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import org.graphstream.algorithm.generator.BaseGenerator;
import org.graphstream.algorithm.generator.LobsterGenerator;
import org.graphstream.graph.Edge;
import org.graphstream.graph.ElementNotFoundException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.IdAlreadyInUseException;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.SinkAdapter;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.layout.Layout;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;

import de.visuflow.callgraph.CallGraphGenerator;
import de.visuflow.callgraph.GraphStructure;

public class GraphManager implements Runnable, ViewerListener {

	Graph graph;
	String styleSheet;
	private Viewer viewer;
	private ViewPanel view;

	ViewerPipe fromViewer;

	Container panel;
	JApplet applet;
	JButton zoomInButton, zoomOutButton, viewCenterButton, filterGraphButton, toggleLayout;
	JToolBar settingsBar;
	JTextField attribute;
	JScrollPane scrollbar;

	double zoomInDelta, zoomOutDelta, maxZoomPercent, minZoomPercent;

	boolean autoLayoutEnabled = false;

	Layout graphLayout = new SpringBox();

	public GraphManager(String graphName, String styleSheet)
	{
		System.setProperty("sun.awt.noerasebackground", "true");
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		this.zoomInDelta = .2;
		this.zoomOutDelta = .2;
		this.maxZoomPercent = .5;
		this.minZoomPercent = 2.0;
		this.styleSheet = styleSheet;
		createGraph(graphName);
		createUI();
	}

	public Container getApplet() {
		return applet.getRootPane();
	}

	void createGraph(String graphName)
	{
		graph = new MultiGraph(graphName);
		graph.addAttribute("ui.stylesheet", styleSheet);

		graph.setStrict(true);
		graph.setAutoCreate(true);
		graph.addAttribute("ui.quality");
		graph.addAttribute("ui.antialias");

		viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
		view = viewer.addDefaultView(false);

		//		view.applyComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		//		view.setAutoscrolls(true);

		//		viewer.enableAutoLayout(new HierarchicalLayout());

		viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.CLOSE_VIEWER);
	}

	private void createUI() {
		// TODO Auto-generated method stub
		createZoomControls();
		createViewListeners();
		createAttributeControls();
		createToggleLayoutButton();
		createSettingsBar();
		createPanel();
		createAppletContainer();
	}

	private void createAppletContainer() {
		// TODO Auto-generated method stub
		applet = new JApplet();

		scrollbar = new JScrollPane(panel);
		applet.add(scrollbar);
	}

	private void createAttributeControls() {
		// TODO Auto-generated method stub
		attribute = new JTextField("ui.screenshot,C:/Users/Shashank B S/Desktop/image.png");
		filterGraphButton = new JButton("SetAttribute");

		filterGraphButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				String[] newAttribute = attribute.getText().split(",");
				graph.setAttribute(newAttribute[0], newAttribute[1]);
			}
		});
	}

	private void createSettingsBar() {
		// TODO Auto-generated method stub
		settingsBar = new JToolBar("ControlsBar", JToolBar.HORIZONTAL);

		settingsBar.add(zoomInButton);
		settingsBar.add(zoomOutButton);
		settingsBar.add(viewCenterButton);
		settingsBar.add(filterGraphButton);
		settingsBar.add(attribute);
		settingsBar.add(toggleLayout);
	}

	private void createPanel() {
		// TODO Auto-generated method stub
		panel = new JFrame().getContentPane();
		panel.add(view);
		panel.add(settingsBar, BorderLayout.PAGE_START);
	}

	private void createViewListeners() {
		// TODO Auto-generated method stub
		view.addMouseWheelListener(new MouseWheelListener() {

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				// TODO Auto-generated method stub
				int rotationDirection = e.getWheelRotation();
				double viewPercent = view.getCamera().getViewPercent();
				if(rotationDirection > 0 && viewPercent > maxZoomPercent)
				{
					view.getCamera().setViewPercent(viewPercent - zoomInDelta);
				}
				else if(viewPercent < minZoomPercent)
				{
					view.getCamera().setViewPercent(viewPercent + zoomOutDelta);
				}
			}
		});

		view.addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseMoved(MouseEvent e) {
				// TODO Auto-generated method stub
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				// TODO Auto-generated method stub
				if(e.getButton() == 0)
				{
					/*Point dest = e.getPoint();
					System.out.println("dragged with button");
					System.out.println(dest);*/

					Point3 currViewCenter = view.getCamera().getViewCenter();
					System.out.println("currentViewCenter " + currViewCenter);
					System.out.println("clickCount " + e.getLocationOnScreen());

					for(int i=0; i<e.getClickCount(); i++)
					{
						view.getCamera().setViewCenter(currViewCenter.x+.2, currViewCenter.y+.2, 0);
						//						try {
						//							Thread.sleep(1000);
						//						} catch (InterruptedException e1) {
						//							// TODO Auto-generated catch block
						//							e1.printStackTrace();
						//						}
					}
				}
			}
		});
	}

	private void createZoomControls() {
		// TODO Auto-generated method stub
		zoomInButton = new JButton("+");
		zoomOutButton = new JButton("-");
		viewCenterButton = new JButton("reset");

		zoomInButton.setBackground(Color.gray);
		zoomInButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				double viewPercent = view.getCamera().getViewPercent();
				if(viewPercent > maxZoomPercent)
					view.getCamera().setViewPercent(viewPercent - zoomInDelta);
			}
		});

		zoomOutButton.setBackground(Color.gray);
		zoomOutButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				double viewPercent = view.getCamera().getViewPercent();
				if(viewPercent < minZoomPercent)
					view.getCamera().setViewPercent(viewPercent + zoomOutDelta);
			}
		});

		viewCenterButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				view.getCamera().resetView();
			}
		});
	}

	private void createToggleLayoutButton()
	{
		toggleLayout = new JButton();
		toggleAutoLayout();
		toggleLayout.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				toggleAutoLayout();
			}
		});
	}

	private void toggleAutoLayout()
	{
		if(!autoLayoutEnabled)
		{
			if(viewer != null && graphLayout != null)
			{
				viewer.enableAutoLayout(graphLayout);
			}
			else if(viewer != null)
			{
				viewer.enableAutoLayout();
			}
			autoLayoutEnabled = true;
			toggleLayout.setText("Disable Layouting");
		}
		else
		{
			viewer.disableAutoLayout();
			autoLayoutEnabled = false;
			toggleLayout.setText("Enable Layouting");
		}
	}

	void generateTestGraph()
	{
		graph.setStrict(false);
		graph.setAutoCreate( true );
		for (int i = 0; i < 50; i++) {
			String source = i + "";
			int temp = i + 1;
			String destination = temp + "";
			graph.addEdge(source+destination, source, destination);
			graph.addEdge(i+"", source, destination, true);
		}
		graph.addAttribute("ui.quality");
		graph.addAttribute("ui.antialias");
	}

	void generateGraphFromGraphStructure()
	{
		GraphStructure interGraph = new GraphStructure();
		CallGraphGenerator generator = new CallGraphGenerator();
		generator.runAnalysis(interGraph);

		System.out.println("StyleSheet " + this.styleSheet);

		ListIterator<de.visuflow.callgraph.Edge> edgeIterator = interGraph.listEdges.listIterator();

		while(edgeIterator.hasNext())
		{
			de.visuflow.callgraph.Edge curr = edgeIterator.next();

			de.visuflow.callgraph.Node src = curr.getSource();
			de.visuflow.callgraph.Node dest = curr.getDestination();
			
			try {
				if(graph.getNode(src.getId() + "") == null)
					graph.addNode(src.getId() + "").setAttribute("ui.label", src.getLabel());
				
				if(graph.getNode(dest.getId() + "") == null)
					graph.addNode(dest.getId() + "").setAttribute("ui.label", dest.getLabel());
				
				if(graph.getEdge("" + src.getId() + dest.getId()) == null)
				{
					graph.addEdge(src.getId() + "" + dest.getId(), src.getId() + "", dest.getId() + "", true);
				}
			} catch (IndexOutOfBoundsException | IdAlreadyInUseException | ElementNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				
			/*try {
				graph.addNode(src.getId() + "").setAttribute("ui.label", src.getId());
			} catch (IdAlreadyInUseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				graph.addNode(dest.getId() + "").setAttribute("ui.label", dest.getId());
			} catch (IdAlreadyInUseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				graph.addEdge(src.getId() + dest.getId() + "", src.getId() + "", dest.getId() + "", true);
				//				System.out.println("Adding edge from " + src.getId() + " " + src.getLabel() + " to " + dest.getId() + " " + dest.getLabel());
				if(src.getId() == 6)
					System.out.println("adding edge " + src.getId() + dest.getId());
			} catch (IdAlreadyInUseException | ElementNotFoundException e) {
				// TODO Auto-generated catch block
				System.out.println("..............................................................");
				e.printStackTrace();
			}*/

//			graph.addEdge("611", "6", "11", true);
		}
		experimentalLayout();
	}

	private void experimentalLayout()
	{
//		viewer.disableAutoLayout();
		int spacing = 2;
		int rowSpacing = 12;
		int nodeCount = graph.getNodeCount() * spacing;
		Iterator<Node> nodeIterator = graph.getNodeIterator();
		while(nodeIterator.hasNext())
		{
			Node curr = nodeIterator.next();

			Iterator<Edge> leavingEdgeIterator = curr.getEdgeIterator();
			int outEdges = 0;
			while(leavingEdgeIterator.hasNext())
			{
				Edge outEdge = leavingEdgeIterator.next();
				Node target = outEdge.getTargetNode();
				target.setAttribute("xyz", outEdges, nodeCount, 0);
				outEdges += rowSpacing;
			}

			curr.setAttribute("xyz", 0, nodeCount, 0);
			nodeCount -= spacing;
		}
	}
	

	void generateGraphFromGenerator()
	{
		BaseGenerator gen  = new LobsterGenerator();
		gen.setDirectedEdges(true, false);
		gen.addNodeLabels(true);
		gen.addSink(graph);

		gen.begin();
		for (int i = 0; i < 100; i++) {
			gen.nextEvents();
		}
		gen.end();

		fromViewer = viewer.newViewerPipe();
		fromViewer.addSink(graph);

		fromViewer.addSink(new SinkAdapter(){
			@Override
			public void nodeAttributeAdded(String sourceId, long timeId, String nodeId, String attribute, Object value) {
				if(attribute.equals("ui.clicked")){
					toggleNode(nodeId);
				}
			}

			@Override
			public void nodeAttributeChanged(String sourceId, long timeId, String nodeId, String attribute, Object oldValue, Object newValue) {
				if(attribute.equals("ui.clicked")){
					toggleNode(nodeId);
				}
			}
		});
	}

	void toggleNode(String id){
		Node n  = graph.getNode(id);
		Object[] pos = n.getAttribute("xyz");
		Iterator<Node> it = n.getBreadthFirstIterator(true);
		if(n.hasAttribute("collapsed")){
			n.removeAttribute("collapsed");
			while(it.hasNext()){
				Node m  =  it.next();

				for(Edge e : m.getLeavingEdgeSet()) {
					e.removeAttribute("ui.hide");
				}
				m.removeAttribute("layout.frozen");
				m.setAttribute("x",((double)pos[0])+Math.random()*0.0001);
				m.setAttribute("y",((double)pos[1])+Math.random()*0.0001);

				m.removeAttribute("ui.hide");

			}
			n.removeAttribute("ui.class");

		} else {
			n.setAttribute("ui.class", "plus");
			n.setAttribute("collapsed");

			while(it.hasNext()){
				Node m  =  it.next();

				for(Edge e : m.getLeavingEdgeSet()) {
					e.setAttribute("ui.hide");
				}
				if(n != m) {
					m.setAttribute("layout.frozen");
					m.setAttribute("x", ((double) pos[0]) + Math.random() * 0.0001);
					m.setAttribute("y", ((double) pos[1]) + Math.random() * 0.0001);

					m.setAttribute("ui.hide");
				}

			}
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		//		generateTestGraph();
		//		generateGraphFromGenerator();
		/*while(true)
			try {
				fromViewer.blockingPump();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
		//			fromViewer.pump();
		generateGraphFromGraphStructure();

		fromViewer = viewer.newViewerPipe();
		fromViewer.addViewerListener(this);
		fromViewer.addSink(graph);

		while(true)
			fromViewer.pump();
	}
	

	@Override
	public void buttonPushed(String id) {
		// TODO Auto-generated method stub
		toggleNode(id);
		experimentalLayout();
		//		Node selectedNode = graph.getNode(id);
		//		if(selectedNode.hasAttribute("ui.class"))
		//		{
		//			System.out.println("Node has attribute clicked");
		//			selectedNode.removeAttribute("ui.class");
		//		}
		//		selectedNode.addAttribute("ui.class", "clicked");
	}
	

	@Override
	public void buttonReleased(String id) {
		// TODO Auto-generated method stub

	}
	

	@Override
	public void viewClosed(String id) {
		// TODO Auto-generated method stub

	}
}
