package de.unipaderborn.visuflow.ui.graph;

import static de.unipaderborn.visuflow.model.DataModel.EA_TOPIC_DATA_FILTER_GRAPH;
import static de.unipaderborn.visuflow.model.DataModel.EA_TOPIC_DATA_MODEL_CHANGED;
import static de.unipaderborn.visuflow.model.DataModel.EA_TOPIC_DATA_SELECTION;
import static de.unipaderborn.visuflow.model.DataModel.EA_TOPIC_DATA_UNIT_CHANGED;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.graphstream.algorithm.Toolkit;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.layout.Layout;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.google.common.base.Optional;

import de.unipaderborn.visuflow.debug.handlers.NavigationHandler;
import de.unipaderborn.visuflow.model.DataModel;
import de.unipaderborn.visuflow.model.VFClass;
import de.unipaderborn.visuflow.model.VFEdge;
import de.unipaderborn.visuflow.model.VFMethod;
import de.unipaderborn.visuflow.model.VFMethodEdge;
import de.unipaderborn.visuflow.model.VFNode;
import de.unipaderborn.visuflow.model.VFUnit;
import de.unipaderborn.visuflow.model.graph.ControlFlowGraph;
import de.unipaderborn.visuflow.model.graph.ICFGStructure;
import de.unipaderborn.visuflow.util.ServiceUtil;

public class GraphManager implements Runnable, ViewerListener, EventHandler {

	Graph graph;
	String styleSheet;
	int maxLength;
	private Viewer viewer;
	private ViewPanel view;
	List<VFClass> analysisData;

	Container panel;
	JApplet applet;
	JButton zoomInButton, zoomOutButton, viewCenterButton, toggleLayout, showICFGButton, btColor;
	JToolBar settingsBar;
	JTextField searchText;

	JDialog dialog;
	JPanel panelColor;
	JColorChooser jcc;

	double zoomInDelta, zoomOutDelta, maxZoomPercent, minZoomPercent, panXDelta, panYDelta;

	boolean autoLayoutEnabled = false;

	Layout graphLayout = new SpringBox();

	private JToolTip tip;
	private JButton panLeftButton;
	private JButton panRightButton;
	private JButton panUpButton;
	private JButton panDownButton;
	private BufferedImage imgLeft;
	private BufferedImage imgRight;
	private BufferedImage imgUp;
	private BufferedImage imgDown;
	private BufferedImage imgPlus;
	private BufferedImage imgMinus;
	private boolean CFG;

	public GraphManager(String graphName, String styleSheet)
	{
		System.setProperty("sun.awt.noerasebackground", "true");
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		this.panXDelta = 2;
		this.panYDelta = 2;
		this.zoomInDelta = .075;
		this.zoomOutDelta = .075;
		this.maxZoomPercent = 0.2;
		this.minZoomPercent = 1.0;
		this.maxLength = 45;
		this.styleSheet = styleSheet;
		createGraph(graphName);
		createUI();

		renderICFG(ServiceUtil.getService(DataModel.class).getIcfg());
	}

	public Container getApplet() {
		return applet.getRootPane();
	}

	private void registerEventHandler() {
		String [] topics = new String[] {
				EA_TOPIC_DATA_FILTER_GRAPH,
				EA_TOPIC_DATA_SELECTION,
				EA_TOPIC_DATA_MODEL_CHANGED,
				EA_TOPIC_DATA_UNIT_CHANGED,
				"GraphReady"
		};
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(EventConstants.EVENT_TOPIC, topics);
		ServiceUtil.registerService(EventHandler.class, this, properties);
	}

	void createGraph(String graphName)
	{
		graph = new MultiGraph(graphName);
		graph.addAttribute("ui.stylesheet", styleSheet);
		graph.setStrict(true);
		graph.setAutoCreate(true);
		graph.addAttribute("ui.quality");
		graph.addAttribute("ui.antialias");

		viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
		viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.CLOSE_VIEWER);

		view = viewer.addDefaultView(false);
		view.getCamera().setAutoFitView(true);
		//		view.removeMouseMotionListener(view.getMouseMotionListeners()[0]);
	}

	private void reintializeGraph() throws Exception
	{
		if(graph != null)
		{
			graph.clear();
			graph.addAttribute("ui.stylesheet", styleSheet);
			graph.setStrict(true);
			graph.setAutoCreate(true);
			graph.addAttribute("ui.quality");
			graph.addAttribute("ui.antialias");
		}
		else
			throw new Exception("Graph is null");
	}

	private void createUI() {
		createIcons();
		createZoomControls();
		createShowICFGButton();
		createPanningButtons();
		createViewListeners();
		createToggleLayoutButton();
		createSearchText();
		createSettingsBar();
		createPanel();
		createAppletContainer(); 
		//		colorNode();
	}

	private void panUp()
	{
		Point3 currCenter = view.getCamera().getViewCenter();
		view.getCamera().setViewCenter(currCenter.x, currCenter.y + panYDelta, 0);
	}

	private void panDown()
	{
		Point3 currCenter = view.getCamera().getViewCenter();
		view.getCamera().setViewCenter(currCenter.x, currCenter.y - panYDelta, 0);
	}

	private void panLeft()
	{
		Point3 currCenter = view.getCamera().getViewCenter();
		view.getCamera().setViewCenter(currCenter.x - panXDelta, currCenter.y, 0);
	}

	private void panRight()
	{
		Point3 currCenter = view.getCamera().getViewCenter();
		view.getCamera().setViewCenter(currCenter.x + panXDelta, currCenter.y, 0);
	}

	private void panToNode(String nodeId)
	{
		view.getCamera().resetView();
		Node panToNode = graph.getNode(nodeId);
		double[] pos = Toolkit.nodePosition(panToNode);
		double currPosition = view.getCamera().getViewCenter().y;
		while(pos[1] > currPosition)
		{
			try {
				Thread.sleep(40);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			view.getCamera().setViewCenter(pos[0], currPosition++, 0.0);
		}
	}

	private void defaultPanZoom() {
		int count = 0;
		if(graph.getNodeCount() > 10)
			count = 10;
		for (int i = 0; i < count; i++) {
			try {
				Thread.sleep(40);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					GraphManager.this.zoomIn();
				}
			});
		}
	}

	private void createPanningButtons() {
		panLeftButton = new JButton("");
		panRightButton = new JButton("");
		panUpButton = new JButton("");
		panDownButton = new JButton("");

		panLeftButton.setIcon(new ImageIcon(getScaledImage(imgLeft, 20, 20)));
		panRightButton.setIcon(new ImageIcon(getScaledImage(imgRight, 20, 20)));
		panUpButton.setIcon(new ImageIcon(getScaledImage(imgUp, 20, 20)));
		panDownButton.setIcon(new ImageIcon(getScaledImage(imgDown, 20, 20)));

		panLeftButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				panLeft();
			}
		});

		panRightButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				panRight();
			}
		});

		panUpButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				panUp();
			}
		});

		panDownButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				panDown();
			}
		});
	}

	private void createIcons() {
		try {
			ClassLoader loader = GraphManager.class.getClassLoader();
			imgLeft = ImageIO.read(loader.getResource("/icons/left.png"));
			imgRight = ImageIO.read(loader.getResource("/icons/right.png"));
			imgUp = ImageIO.read(loader.getResource("/icons/up.png"));
			imgDown = ImageIO.read(loader.getResource("/icons/down.png"));
			imgPlus = ImageIO.read(loader.getResource("/icons/plus.png"));
			imgMinus = ImageIO.read(loader.getResource("/icons/minus.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Image getScaledImage(Image srcImg, int w, int h){
		BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = resizedImg.createGraphics();

		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(srcImg, 0, 0, w, h, null);
		g2.dispose();

		return resizedImg;
	}

	private void createShowICFGButton() {
		showICFGButton = new JButton("Show ICFG");
		showICFGButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				renderICFG(ServiceUtil.getService(DataModel.class).getIcfg());
			}
		});
	}

	private void createSearchText()
	{
		this.searchText = new JTextField("Search graph");
		searchText.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String searchString = searchText.getText().toLowerCase();
				ArrayList<VFNode> vfNodes = new ArrayList<>();
				ArrayList<VFUnit> vfUnits = new ArrayList<>();
				for (Node node : graph) {
					if(node.getAttribute("ui.label").toString().toLowerCase().contains((searchString))){
						vfNodes.add((VFNode) node.getAttribute("nodeUnit"));
						vfUnits.add(((VFNode) node.getAttribute("nodeUnit")).getVFUnit());
					}
				}
				
				try {
					DataModel model = ServiceUtil.getService(DataModel.class);
					model.filterGraph(vfNodes, true, null);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				
				NavigationHandler handler = new NavigationHandler();
				handler.HighlightJimpleLine(vfUnits);
			}
		});
	}

	private void createAppletContainer() {
		applet = new JApplet();
		applet.add(panel);
	}

	private void createSettingsBar() {
		settingsBar = new JToolBar("ControlsBar", JToolBar.HORIZONTAL);

		settingsBar.add(zoomInButton);
		settingsBar.add(zoomOutButton);
		settingsBar.add(showICFGButton);
		settingsBar.add(viewCenterButton);
		settingsBar.add(toggleLayout);
		settingsBar.add(btColor);
		settingsBar.add(panLeftButton);
		settingsBar.add(panRightButton);
		settingsBar.add(panUpButton);
		settingsBar.add(panDownButton);
		settingsBar.add(searchText);
	}

	private void createPanel() {
		JFrame temp = new JFrame();
		temp.setLayout(new BorderLayout());
		panel = temp.getContentPane();
		panel.add(view);
		panel.add(settingsBar, BorderLayout.PAGE_END);
	}

	private void createViewListeners() {
		view.addMouseWheelListener(new MouseWheelListener() {

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				int rotationDirection = e.getWheelRotation();
				if(rotationDirection > 0)
					zoomIn();
				else
					zoomOut();
			}
		});

		view.addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseMoved(MouseEvent event) {

				GraphicElement curElement = view.findNodeOrSpriteAt(event.getX(), event.getY());

				if(curElement == null && tip != null) {
					tip.setVisible(false);
					setTip(null);
					view.repaint();
				}

				if(curElement != null && tip == null) {
					Node node = graph.getNode(curElement.getId());
					String result = "<html><table>";
					int maxToolTipLength = 0;
					int height = 0;
					for(String key:node.getEachAttributeKey()) {
						if(key.startsWith("nodeData")){
							height++;
							Object value = node.getAttribute(key);
							String tempVal = key.substring(key.lastIndexOf(".") + 1) + " : " + value.toString();
							if(tempVal.length() > maxToolTipLength){
								maxToolTipLength = tempVal.length();
							}

							result += "<tr><td>" + key.substring(key.lastIndexOf(".") + 1) + "</td>"+"<td>" + value.toString() + "</td></tr>";
						}
					}
					result += "</table></html>";
					tip = new JToolTip();
					String tipText = result;
					tip.setTipText(tipText);
					tip.setBounds(event.getX() - tipText.length()*3 + 1, event.getY(), maxToolTipLength*3+3,height*30 );
					setTip(tip);
					tip.setVisible(true);

					if(tipText.length() > 10) {
						tip.setLocation(event.getX() - 15, event.getY());
					}

					view.add(tip);
					tip.repaint();
				}
			}

			@Override
			public void mouseDragged(MouseEvent e) {

			}
		});

		view.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
				//noop
			}

			@Override
			public void mousePressed(MouseEvent e) {
				//noop
			}

			@Override
			public void mouseExited(MouseEvent e) {
				//noop
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				//noop
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				DataModel dataModel = ServiceUtil.getService(DataModel.class);
				GraphicElement curElement = view.findNodeOrSpriteAt(e.getX(), e.getY());
				if(curElement == null)
					return;
				Node curr = graph.getNode(curElement.getId());
				if(e.getButton() == MouseEvent.BUTTON1 && !CFG && !((e.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK))
				{
					Object node = curr.getAttribute("nodeMethod");
					if(node instanceof VFMethod)
					{
						VFMethod selectedMethod = (VFMethod) node;
						try {
							if(selectedMethod.getControlFlowGraph() == null)
								throw new Exception("CFG Null Exception");
							else
							{
								dataModel.setSelectedMethod(selectedMethod, true);
							}
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}
				}
				else if(e.getButton() == MouseEvent.BUTTON1 && CFG && !((e.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK))
				{
					Object node = curr.getAttribute("nodeUnit");
					NavigationHandler handler = new NavigationHandler();
					if(node instanceof VFNode)
					{
						ArrayList<VFUnit> units = new ArrayList<>();
						units.add(((VFNode) node).getVFUnit());
						handler.HighlightJimpleLine(units);
						handler.NavigateToSource(units.get(0));

						ArrayList<VFNode> nodes = new ArrayList<>();
						nodes.add((VFNode) node);
						try {
							dataModel.filterGraph(nodes, true, null);
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}
				}
				else if(e.getButton() == MouseEvent.BUTTON1 && (e.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK)
				{
					String id = curr.getId();
					if(id != null)
						toggleNode(id);
				}
				/*else
				{
					Object node = curr.getAttribute("nodeUnit");
					if(node instanceof VFNode)
					{
						dataModel.HighlightJimpleUnit((VFNode) node);
						if(((Stmt)((VFNode) node).getUnit()).containsInvokeExpr()){
							callInvokeExpr(((Stmt)((VFNode) node).getUnit()).getInvokeExpr());
						}
					}
				}*/
			}
		});
	}

	/*private void callInvokeExpr(InvokeExpr expr){
		if(expr == null) return;
		DataModel dataModel = ServiceUtil.getService(DataModel.class);
		System.out.println(expr);
		VFMethod selectedMethod = dataModel.getVFMethodByName(expr.getMethod());
		try {
			if(selectedMethod.getControlFlowGraph() == null)
				throw new Exception("CFG Null Exception");
			else
			{
				dataModel.setSelectedMethod(selectedMethod, true);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}*/

	private void zoomIn()
	{
		double viewPercent = view.getCamera().getViewPercent();
		if(viewPercent > maxZoomPercent)
			view.getCamera().setViewPercent(viewPercent - zoomInDelta);
	}

	private void zoomOut()
	{
		double viewPercent = view.getCamera().getViewPercent();
		if(viewPercent < minZoomPercent)
			view.getCamera().setViewPercent(viewPercent + zoomOutDelta);
	}

	private void createZoomControls() {
		zoomInButton = new JButton();
		zoomOutButton = new JButton();
		viewCenterButton = new JButton("reset");

		zoomInButton.setIcon(new ImageIcon(getScaledImage(imgPlus, 20, 20)));
		zoomOutButton.setIcon(new ImageIcon(getScaledImage(imgMinus, 20, 20)));

		zoomInButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				zoomIn();
			}
		});

		zoomOutButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				zoomOut();
			}
		});

		viewCenterButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				view.getCamera().resetView();
			}
		});

		colorNode(); 
	}

	private void createToggleLayoutButton()
	{
		toggleLayout = new JButton();
		toggleAutoLayout();
		toggleLayout.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
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
				//				viewer.enableAutoLayout(graphLayout);
				experimentalLayout();
			}
			else if(viewer != null)
			{
				//				viewer.enableAutoLayout();
				experimentalLayout();
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

	private void colorNode(){
		jcc = new JColorChooser(Color.BLUE);
		jcc.getSelectionModel().addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				Color color = jcc.getColor();
				System.out.println("color:"+ color);

			}
		});

		dialog = JColorChooser.createDialog(null, "Color Chooser", true, jcc, null, null);
		panelColor = new JPanel(new GridLayout(0, 2));       

		btColor = new JButton ("Color nodes");
		btColor.addActionListener(new ActionListener() {


			@Override
			public void actionPerformed(ActionEvent e) {
				colorTheGraph();

			}
		});
	}

	private void filterGraphNodes(List<VFNode> nodes, boolean selected, String uiClassName)
	{
		boolean panned = false;
		if(uiClassName == null)
			uiClassName = "filter";
		Iterable<? extends Node> graphNodes = graph.getEachNode();
		for (Node node : graphNodes) {
			if(node.hasAttribute("ui.class"))
				node.removeAttribute("ui.class");
			for (VFNode vfNode : nodes) {
				if(node.getAttribute("nodeData.unit").toString().contentEquals(vfNode.getUnit().toString()))
				{
					if(selected)
						node.setAttribute("ui.class", uiClassName);
					if(!panned)
					{
						this.panToNode(node.getId());
						panned = true;
					}
				}
			}
		}
	}

	private void renderICFG(ICFGStructure icfg) {
		if(icfg == null) {
			return;
		}

		Iterator<VFMethodEdge> iterator = icfg.listEdges.iterator();
		try {
			reintializeGraph();
		} catch (Exception e) {
			e.printStackTrace();
		}
		while(iterator.hasNext())
		{
			VFMethodEdge curr = iterator.next();

			VFMethod src = curr.getSourceMethod();
			VFMethod dest = curr.getDestMethod();

			createGraphMethodNode(src);
			createGraphMethodNode(dest);
			createGraphMethodEdge(src, dest);
		}
		this.CFG = false;
		experimentalLayout();
	}

	private void createGraphMethodEdge(VFMethod src, VFMethod dest) {
		if(graph.getEdge("" + src.getId() + dest.getId()) == null)
		{
			graph.addEdge(src.getId() + "" + dest.getId(), src.getId() + "", dest.getId() + "", true);
		}
	}

	private void createGraphMethodNode(VFMethod src) {
		if(graph.getNode(src.getId() + "") == null)
		{
			Node createdNode = graph.addNode(src.getId() + "");
			createdNode.setAttribute("ui.label", src.getSootMethod().getName().toString());
			createdNode.setAttribute("nodeData.methodName", src.getSootMethod().getName());
			createdNode.setAttribute("nodeData.methodSignature", src.getSootMethod().getSignature());
			createdNode.setAttribute("nodeMethod", src);
		}
	}

	private void renderMethodCFG(ControlFlowGraph interGraph, boolean panToNode) throws Exception
	{
		if(interGraph == null)
			throw new Exception("GraphStructure is null");

		this.reintializeGraph();
		ListIterator<VFEdge> edgeIterator = interGraph.listEdges.listIterator();

		while(edgeIterator.hasNext())
		{
			VFEdge currEdgeIterator = edgeIterator.next();

			VFNode src = currEdgeIterator.getSource();
			VFNode dest = currEdgeIterator.getDestination();

			createGraphNode(src);
			createGraphNode(dest);
			createGraphEdge(src,dest);
		}
		this.CFG = true;
		experimentalLayout();

		if(panToNode)
		{
			defaultPanZoom();
			panToNode(graph.getNodeIterator().next().getId());
		}
	}

	private void createGraphEdge(VFNode src, VFNode dest) {
		if(graph.getEdge("" + src.getId() + dest.getId()) == null)
		{
			Edge createdEdge = graph.addEdge(src.getId() + "" + dest.getId(), src.getId() + "", dest.getId() + "", true);
			VFUnit unit = src.getVFUnit();
			createdEdge.addAttribute("ui.label", Optional.fromNullable(unit.getOutSet()).or("").toString());
			createdEdge.addAttribute("edgeData.outSet", Optional.fromNullable(unit.getInSet()).or("").toString());
		}
	}

	private void createGraphNode(VFNode node) {
		if(graph.getNode(node.getId() + "") == null)
		{
			Node createdNode = graph.addNode(node.getId() + "");
			if(node.getUnit().toString().length() > maxLength) {
				createdNode.setAttribute("ui.label", node.getUnit().toString().substring(0, maxLength) + "...");
			} else {
				createdNode.setAttribute("ui.label", node.getUnit().toString());
			}
			createdNode.setAttribute("nodeData.unit", node.getUnit().toString());
			createdNode.setAttribute("nodeData.unitType", node.getUnit().getClass());
			createdNode.setAttribute("nodeData.inSet", Optional.fromNullable(node.getVFUnit().getInSet()).or("n/a").toString());
			createdNode.setAttribute("nodeData.outSet", Optional.fromNullable(node.getVFUnit().getInSet()).or("n/a").toString());
			createdNode.setAttribute("nodeData.line", node.getUnit().getJavaSourceStartLineNumber());
			createdNode.setAttribute("nodeData.column", node.getUnit().getJavaSourceStartColumnNumber());
			createdNode.setAttribute("nodeUnit", node);
		}
	}

	private void experimentalLayout()
	{
		if(!CFG)
		{
			viewer.enableAutoLayout(new SpringBox());
			view.getCamera().resetView();
			return;
		}
		viewer.disableAutoLayout();

		double rowSpacing = 3.0;
		double columnSpacing = 3.0;
		Iterator<Node> nodeIterator = graph.getNodeIterator();
		int totalNodeCount = graph.getNodeCount();
		int currNodeIndex = 0;
		while(nodeIterator.hasNext())
		{
			Node curr = nodeIterator.next();
			if(curr.hasAttribute("layout.visited"))
			{
				continue;
			}
			int currEdgeCount = curr.getOutDegree();
			int currEdgeIndex = 0;
			if(currEdgeCount > 1)
			{
				Iterator<Edge> currEdgeIterator = curr.getEdgeIterator();
				curr.setAttribute("xyz", 0.0, ((totalNodeCount * rowSpacing) - currNodeIndex), 0.0);
				curr.setAttribute("layout.visited");
				currNodeIndex++;
				while(currEdgeIterator.hasNext())
				{
					Node temp = currEdgeIterator.next().getOpposite(curr);
					temp.setAttribute("xyz", ((columnSpacing- currEdgeIndex) * currEdgeCount), ((totalNodeCount * columnSpacing) - currNodeIndex), 0.0);
					curr.setAttribute("layout.visited");
					currEdgeIndex++;
				}
				currNodeIndex++;
			}
			curr.setAttribute("xyz", 0.0, ((totalNodeCount * rowSpacing) - currNodeIndex), 0.0);
			curr.setAttribute("layout.visited");
			currNodeIndex++;
		}

		for (Node node : graph) {
			double[] pos = Toolkit.nodePosition(graph, node.getId());
			int inDegree = node.getInDegree();
			int outDegree = node.getOutDegree();
			if(inDegree == 0)
				continue;
			/*if(inDegree == 1 && outDegree == 1)
			{
				Node temp = node.getEdgeIterator().next().getOpposite(node);
				temp.setAttribute("xyz", pos[0], pos[1] + rowSpacing, 0.0);
			}*/
			if(inDegree > outDegree)
			{
				node.setAttribute("xyz", pos[0] - rowSpacing, pos[1], 0.0);
			}
			if(outDegree > inDegree)
			{
				node.setAttribute("xyz", pos[0] + rowSpacing, pos[1], 0.0);
			}
			if(inDegree>1 && outDegree>1 && inDegree == outDegree)
			{
				/*Node parent = node.getEnteringEdgeIterator().next().getOpposite(node);
				double[] parentPos = Toolkit.nodePosition(parent);
				node.setAttribute("xyz", parentPos[0], pos[1], 0.0);*/
				node.setAttribute("xyz", pos[0] - rowSpacing, pos[1], 0.0);
				//continue;
			}
			else
				continue;
		}

		view.getCamera().resetView();
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
					//					m.setAttribute("x", ((double) pos[0]) + Math.random() * 0.0001);
					//					m.setAttribute("y", ((double) pos[1]) + Math.random() * 0.0001);

					m.setAttribute("xyz", ((double) pos[0]) + Math.random() * 0.0001, ((double) pos[1]) + Math.random() * 0.0001, 0.0);

					m.setAttribute("ui.hide");
				}

			}
		}
		experimentalLayout();
		panToNode(id);
	}

	@Override
	public void run() {
		this.registerEventHandler();
		System.out.println("GraphManager ---> registered for events");



		//		No need to have the following code.

		/*ViewerPipe fromViewer = viewer.newViewerPipe();
		fromViewer.addViewerListener(this);
		fromViewer.addSink(graph);

		// FIXME the Thread.sleep slows down the loop, so that it does not eat up the CPU
		// but this really should be implemented differently. isn't there an event listener
		// or something we can use, so that we call pump() only when necessary
		while(true) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
			}
			fromViewer.pump();
		}*/
	}

	@Override
	public void buttonPushed(String id) {
		//noop
	}

	@Override
	public void buttonReleased(String id) {
		toggleNode(id);
		experimentalLayout();
	}

	@Override
	public void viewClosed(String id) {
		//noop
	}

	protected void setTip(JToolTip toolTip) {
		this.tip = toolTip;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void handleEvent(Event event) {
		if(event.getTopic().contentEquals(DataModel.EA_TOPIC_DATA_MODEL_CHANGED))
		{
			renderICFG((ICFGStructure) event.getProperty("icfg"));
		}
		if(event.getTopic().contentEquals(DataModel.EA_TOPIC_DATA_SELECTION))
		{
			VFMethod selectedMethod = (VFMethod) event.getProperty("selectedMethod");
			boolean panToNode = (boolean) event.getProperty("panToNode");
			try {
				renderMethodCFG(selectedMethod.getControlFlowGraph(), panToNode);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if(event.getTopic().contentEquals(DataModel.EA_TOPIC_DATA_FILTER_GRAPH))
		{
			filterGraphNodes((List<VFNode>) event.getProperty("nodesToFilter"), (boolean) event.getProperty("selection"), (String) event.getProperty("uiClassName"));
		}
		if(event.getTopic().equals(DataModel.EA_TOPIC_DATA_UNIT_CHANGED))
		{
			VFUnit unit = (VFUnit) event.getProperty("unit");

			for (Edge edge : graph.getEdgeSet()) {
				Node src = edge.getSourceNode();
				VFNode vfNode = src.getAttribute("nodeUnit");
				if(vfNode != null) {
					VFUnit currentUnit = vfNode.getVFUnit();
					if(unit.getFullyQualifiedName().equals(currentUnit.getFullyQualifiedName())) {
						String outset = Optional.fromNullable(unit.getOutSet()).or("").toString();
						edge.setAttribute("ui.label", outset);
						edge.setAttribute("edgeData.outSet", outset);
						src.addAttribute("nodeData.inSet", unit.getInSet());
						src.addAttribute("nodeData.outSet", unit.getOutSet());
						System.out.println("GraphManager: Unit changed: " + unit.getFullyQualifiedName());
						System.out.println("GraphManager: Unit in-set: " + unit.getInSet());
						System.out.println("GraphManager: Unit out-set: " + unit.getOutSet());
					} 
				}
			}
		}
	}

	private boolean inPanel( String btName, JPanel panel){
		boolean exist = false;

		for(Component c : panel.getComponents()){

			if (!(c.getName()== null)) {
				if ((c.getClass().toString().equals("class javax.swing.JButton"))) {
					JButton bt = (JButton) c;
					System.out.println("Button's name = " + c.getName());
					if (bt.getName().equals(btName)) {
						System.out.println("btName = " + btName + " , component name = " + bt.getName());
						exist = true;
						break;
					}
				}
			}
		}

		return exist;
	}

	public void colorTheGraph(){

		panelColor.removeAll();

		for(Node n: graph.getEachNode()){
			if(!(n.getAttribute("nodeData.unitType") == null)){
				JLabel l = new JLabel("Change the color of this unit");
				JButton bt = new JButton(n.getAttribute("nodeData.unitType").toString());
				//            	 bt.setName(n.getAttribute("nodeData.methodName").toString());
				bt.setName(n.getAttribute("nodeData.unitType").toString());

				bt.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						dialog.setVisible(true);
						System.out.println("Button's name is : " + bt.getName());
						System.out.println("Button's name is..." );
						for(Node n: graph.getEachNode()){
							//							if(n.getAttribute("nodeData.methodName").toString().equals(bt.getName())){
							//							if(n.getAttribute("ui.label").toString().equals(bt.getName())){
							if(n.getAttribute("nodeData.unitType").toString().equals(bt.getName())){
								n.setAttribute("ui.color", jcc.getColor());
								//								n.setAttribute("ui.text-size", "20%");
								System.out.println(jcc.getColor());
							}

						}


					}
				});

				if(!(inPanel(bt.getName(), panelColor))){
					panelColor.add(l);
					panelColor.add(bt);
				}

			}
			if(!(n.getAttribute("nodeData.methodName")== null)){
				JLabel l = new JLabel("Change the color of this unit");
				JButton bt = new JButton(n.getAttribute("nodeData.methodName").toString());
				//            	 bt.setName(n.getAttribute("nodeData.methodName").toString());
				bt.setName(n.getAttribute("nodeData.methodName").toString());

				bt.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						dialog.setVisible(true);
						System.out.println("Button's name is : " + bt.getName());
						System.out.println("Button's name is..." );
						for(Node n: graph.getEachNode()){
							//							if(n.getAttribute("nodeData.methodName").toString().equals(bt.getName())){
							//							if(n.getAttribute("ui.label").toString().equals(bt.getName())){
							if(n.getAttribute("nodeData.methodName").toString().equals(bt.getName())){
								n.setAttribute("ui.color", jcc.getColor());
								System.out.println(jcc.getColor());
							}

						}


					}
				});

				if(!(inPanel(bt.getName(), panelColor))){
					panelColor.add(l);
					panelColor.add(bt);
				}

			}



		}

		int result = JOptionPane.showConfirmDialog(null, panelColor, "Test", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION) {

		}else{
			System.out.println("Cancelled");
		}
	}

}