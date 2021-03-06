package org.ivis.layout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.awt.Dimension;
import java.awt.Point;

import org.ivis.util.Transform;
import org.ivis.util.PointD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class lays out the associated graph model (LGraphManager, LGraph, LNode,
 * and LEdge). The framework also lets the users associate each l-level node and
 * edge with a view node and edge, respectively. It makes the necessary
 * callbacks (update methods) so that the results can be copied back to the
 * associated view object when layout is finished. Users are also given an
 * opportunity to perform any pre and post layout operations with respective
 * methods.
 *
 * @author Ugur Dogrusoz
 * @author Cihan Kucukkececi
 * @author Selcuk Onur Sumer
 *
 * Copyright: i-Vis Research Group, Bilkent University, 2007 - present
 */
public abstract class Layout {

	private static final Logger log = LoggerFactory.getLogger(Layout.class);

	/**
	 * Layout Quality: 0:proof, 1:default, 2:draft
	 */
	public int layoutQuality = LayoutConstants.DEFAULT_QUALITY;

	/**
	 * Whether layout should create bendpoints as needed or not
	 */
	public boolean createBendsAsNeeded = LayoutConstants.DEFAULT_CREATE_BENDS_AS_NEEDED;

	/**
	 * Whether layout should be incremental or not
	 */
	public boolean incremental = LayoutConstants.DEFAULT_INCREMENTAL;

	/**
	 * Whether we animate from before to after layout node positions
	 */
	public boolean animationOnLayout = LayoutConstants.DEFAULT_ANIMATION_ON_LAYOUT;

	/**
	 * Whether we animate the layout process or not
	 */
	public boolean animationDuringLayout = LayoutConstants.DEFAULT_ANIMATION_DURING_LAYOUT;

	/**
	 * Number iterations that should be done between two successive animations
	 */
	public int animationPeriod = LayoutConstants.DEFAULT_ANIMATION_PERIOD;

	/**
	 * Whether or not leaf nodes (non-compound nodes) are of uniform sizes. When
	 * they are, both spring and repulsion forces between two leaf nodes can be
	 * calculated without the expensive clipping point calculations, resulting
	 * in major speed-up.
	 */
	public boolean uniformLeafNodeSizes = LayoutConstants.DEFAULT_UNIFORM_LEAF_NODE_SIZES;

	/*
	 * Geometric abstraction of the compound graph
	 */
	protected LGraphManager graphManager;

	/*
	 * Whether layout is finished or not
	 */
	private boolean isLayoutFinished;

	/*
	 * Whether this layout is a sub-layout of another one (e.g. CoSE called
	 * within CiSE for laying out the cluster graph)
	 */
	public boolean isSubLayout;
	
	/**
	 * This is used for creation of bendpoints by using dummy nodes and edges.
	 * Maps an LEdge to its dummy bendpoint path.
	 */
	protected HashMap edgeToDummyNodes = new HashMap();

	/**
	 * Indicates whether the layout is called remotely or not.
	 */
	protected boolean isRemoteUse;

// -----------------------------------------------------------------------------
// Section: Constructors and initializations
// -----------------------------------------------------------------------------
	/**
	 * The constructor creates and associates with this layout a new graph
	 * manager as well.
	 */
	public Layout()
	{
		this.graphManager = this.newGraphManager();
		this.isLayoutFinished = false;
		this.isSubLayout = false;
		this.isRemoteUse = false;
		assert (this.graphManager != null);
	}

// -----------------------------------------------------------------------------
// Section: Accessor methods
// -----------------------------------------------------------------------------
	/**
	 * This method returns the associated graph manager.
	 * @return graph manager
	 */
	public LGraphManager getGraphManager()
	{
		return this.graphManager;
	}

	/**
	 * This method returns the array of all nodes in associated graph manager.
	 * @return array of all nodes
	 */
	public Object[] getAllNodes()
	{
		return graphManager.getAllNodes();
	}

	/**
	 * This method returns the array of all edges in associated graph manager.
	 * @return array of all edges
	 */
	public Object[] getAllEdges()
	{
		return graphManager.getAllEdges();
	}

	/**
	 * This method returns the array of all nodes to which gravitation should be
	 * applied.
	 *
	 * @return array of nodes to apply gravitation
	 */
	public Object[] getAllNodesToApplyGravitation()
	{
		return graphManager.getAllNodesToApplyGravitation();
	}

// -----------------------------------------------------------------------------
// Section: Topology related
// -----------------------------------------------------------------------------
	/*
	 * This method creates a new graph manager associated with this layout.
	 *
	 * @return new graph manager instance (is set for the private field)
	 */
	protected LGraphManager newGraphManager()
	{
		LGraphManager gm = new LGraphManager(this);
		graphManager = gm;
		return gm; //why to return the gm value when it also sets graphManager?
	}

	/**
	 * This method creates a new graph associated with the input view graph.
	 *
	 * @param vGraph view graph
	 * @return new graph
	 */
	public LGraph newGraph(Object vGraph)
	{
		return new LGraph(null, graphManager, vGraph);
	}

	/**
	 * This method creates a new node associated with the input view node.
	 *
	 * @param vNode a view node
	 * @return new node
	 */
	public LNode newNode(Object vNode)
	{
		return new LNode(graphManager, vNode);
	}

	/**
	 * This method creates a new edge associated with the input view edge.
	 *
	 * @param vEdge a view edge
	 * @return new edge
	 */
	public LEdge newEdge(Object vEdge)
	{
		return new LEdge(null, null, vEdge);
	}

// -----------------------------------------------------------------------------
// Section: Remaining methods
// -----------------------------------------------------------------------------
	/**
	 * This method coordinates the layout operation. It returns true upon
	 * success, false otherwise.
	 * @return true if the operation succeeded; otherwise - false
	 */
	public boolean runLayout()
	{
		isLayoutFinished = false;

		if (!isSubLayout)
		{
			doPreLayout();
		}

		initParameters();
		boolean isLayoutSuccessfull;
		
		if ((graphManager.getRoot() == null)
			|| graphManager.getRoot().getNodes().size() == 0
			|| graphManager.includesInvalidEdge())
		{
			isLayoutSuccessfull = false;
		}
		else
		{
			// calculate execution time
			long startTime = 0;
			
			if (!isSubLayout)
			{
				startTime = System.currentTimeMillis();
			}
			
			isLayoutSuccessfull = layout();
			
			if (!isSubLayout)
			{
				long endTime = System.currentTimeMillis();
				long excTime = endTime - startTime;
				log.info("Total execution time: " + excTime + " miliseconds.");
			}
		}
		
		if (isLayoutSuccessfull)
		{
			if (!isSubLayout)
			{
				doPostLayout();
			}
		}

		isLayoutFinished = true;

		return isLayoutSuccessfull;
	}

	/**
	 * This method performs the operations required before layout.
	 */
	public void doPreLayout()
	{ //TODO: Who did forget to implement; why it's empty?
	}

	/**
	 * This method performs the operations required after layout.
	 */
	public void doPostLayout()
	{
		assert !isSubLayout : "Should not be called on sub-layout!";
		// Propagate geometric changes to v-level objects
		transform();
		update();
	}

	/**
	 * This method is the main method of the layout algorithm; each new layout
	 * algorithm must implement this method. It should return whether layout is
	 * successful or not.
	 */
	public abstract boolean layout();

	/**
	 * This method updates the geometry of the target graph according to
	 * calculated layout.
	 */
	public void update() {
		// update bend points
		if(createBendsAsNeeded)
		{
			createBendpointsFromDummyNodes();

			// reset all edges, since the topology has changed
			graphManager.resetAllEdges();
		}

		// perform edge, node and root updates
		LEdge edge;
		for (Object obj : graphManager.getAllEdges())
		{
			edge = (LEdge) obj;
			update(edge);
		}

		LNode node;
		for (Object obj : graphManager.getRoot().getNodes())
		{
			node = (LNode) obj;
			update(node);
		}

		update(graphManager.getRoot());
	}
	
	/**
	 * This method is called for updating the geometry of the view node
	 * associated with the input node when layout finishes.
	 * @param node node to update view of
	 */
	public void update(LNode node)
	{
		if (node.getChild() != null)
		{
			// since node is compound, recursively update child nodes
			for (Object obj : node.getChild().getNodes())
			{
				update((LNode) obj);
			}
		}

		Object gObj = node.vGraphObject;
		if (gObj instanceof Updatable)
		{
			((Updatable) gObj).update(node);
		}
	}

	/**
	 * This method is called for updating the geometry of the view edge
	 * associated with the input edge when layout finishes.
	 * @param edge edge to update view of
	 */
	public void update(LEdge edge)
	{
		Object gObj = edge.vGraphObject;
		if (gObj instanceof Updatable)
		{
			((Updatable) gObj).update(edge);
		}
	}

	/**
	 * This method is called for updating the geometry of the view graph
	 * associated with the input graph when layout finishes.
	 * @param graph graph to update view of
	 */
	public void update(LGraph graph)
	{
		Object gObj = graph.vGraphObject;
		if (gObj instanceof Updatable)
		{
			((Updatable) gObj).update(graph);
		}
	}

	/**
	 * This method is used to set all layout parameters to default values
	 * determined at compile time.
	 */
	public void initParameters()
	{
		if (!isSubLayout)
		{
			LayoutOptionsPack.General layoutOptionsPack =
				LayoutOptionsPack.getInstance().getGeneral();

			layoutQuality = layoutOptionsPack.layoutQuality;

			animationDuringLayout =
				layoutOptionsPack.animationDuringLayout;
			animationPeriod =
				(int) transform(layoutOptionsPack.animationPeriod,
					LayoutConstants.DEFAULT_ANIMATION_PERIOD);
			animationOnLayout = layoutOptionsPack.animationOnLayout;

			incremental = layoutOptionsPack.incremental;
			createBendsAsNeeded = layoutOptionsPack.createBendsAsNeeded;
			uniformLeafNodeSizes =
				layoutOptionsPack.uniformLeafNodeSizes;
		}

		if (animationDuringLayout)
		{
			animationOnLayout = false;
		}
	}

	/**
	 * This method transforms the LNodes in the associated LGraphManager so that
	 * upper-left corner of the drawing is (0, 0). The goal is to avoid negative
	 * coordinates that are not allowed when displaying by shifting the drawing
	 * as necessary.
	 */
	public void transform()
	{
		transform(new PointD(0, 0));
	}

	/**
	 * This method transforms the LNodes in the associated LGraphManager so that
	 * upper-left corner of the drawing starts at the input coordinate.
	 *
	 * @param newLeftTop new left-top coordinate point
	 */
	public void transform(PointD newLeftTop)
	{
		// create a transformation object (from Eclipse to layout). When an
		// inverse transform is applied, we get upper-left coordinate of the
		// drawing or the root graph at given input coordinate (some margins
		// already included in calculation of left-top).

		Transform trans = new Transform();
		Point leftTop = graphManager.getRoot().updateLeftTop();

		if (leftTop != null)
		{
			trans.setWorldOrgX(newLeftTop.x);
			trans.setWorldOrgY(newLeftTop.y);

			trans.setDeviceOrgX(leftTop.x);
			trans.setDeviceOrgY(leftTop.y);

			Object[] nodes = getAllNodes();
			LNode node;

			for (int i = 0; i < nodes.length; i++)
			{
				node = (LNode) nodes[i];
				node.transform(trans);
			}
		}
	}

	/**
	 * This method determines the initial positions of leaf nodes in the
	 * associated l-level compound graph structure randomly. Non-empty compound
	 * nodes get their initial positions (and dimensions) from their contents,
	 * thus no calculations should be done for them!
	 */
	public void positionNodesRandomly()
	{
		assert !incremental;
		positionNodesRandomly(getGraphManager().getRoot());
		getGraphManager().getRoot().updateBounds(true);
	}

	/**
	 * Auxiliary method for positioning nodes randomly.
	 */
	private void positionNodesRandomly(LGraph graph)
	{
		LNode lNode;
		LGraph childGraph;

		for (Object obj : graph.getNodes())
		{
			lNode = (LNode)obj;
			childGraph = lNode.getChild();

			if (childGraph == null)
			{
				lNode.scatter();
			}
			else if (childGraph.getNodes().size() == 0)
			{
				lNode.scatter();
			}
			else
			{
				positionNodesRandomly(childGraph);
				lNode.updateBounds();
			}
		}
	}

	/**
	 * This method returns a list of trees where each tree is represented as a
	 * list of l-nodes. The method returns a list of size 0 when:
	 * - the graph is not flat or
	 * - one of the component(s) of the graph is not a tree.
	 *
	 * @return list of trees (forest)
	 */
	public List<List<LNode>> getFlatForest()
	{
		List<List<LNode>> flatForest = new ArrayList<List<LNode>>();
		boolean isForest = true;

		// Quick reference for all nodes in the graph manager associated with
		// this layout. The list should not be changed.
		final List<LNode> allNodes = graphManager.getRoot().getNodes();

		// First be sure that the graph is flat
		boolean isFlat = true;

		for (int i = 0; i < allNodes.size(); i++)
		{
			if (allNodes.get(i).getChild() != null)
			{
				isFlat = false;
			}
		}

		// Return empty forest if the graph is not flat.
		if (!isFlat)
		{
			return flatForest;
		}

		// Run BFS for each component of the graph.

		Set<LNode> visited = new HashSet<LNode>();
		LinkedList<LNode> toBeVisited = new LinkedList<LNode>();
		HashMap<LNode, LNode> parents = new HashMap<LNode, LNode>();
		LinkedList<LNode> unProcessedNodes = new LinkedList<LNode>();

		unProcessedNodes.addAll(allNodes);

		// Each iteration of this loop finds a component of the graph and
		// decides whether it is a tree or not. If it is a tree, adds it to the
		// forest and continued with the next component.

		while (unProcessedNodes.size() > 0 && isForest)
		{
			toBeVisited.add(unProcessedNodes.getFirst());

			// Start the BFS. Each iteration of this loop visits a node in a
			// BFS manner.
			while (!toBeVisited.isEmpty() && isForest)
			{
				LNode currentNode = toBeVisited.poll();
				visited.add(currentNode);

				// Traverse all neighbors of this node
				List<LEdge> neighborEdges = currentNode.getEdges();

				for (int i = 0; i < neighborEdges.size(); i++)
				{
					LNode currentNeighbor =
						neighborEdges.get(i).getOtherEnd(currentNode);

					// If BFS is not growing from this neighbor.
					if (parents.get(currentNode) != currentNeighbor)
					{
						// We haven't previously visited this neighbor.
						if (!visited.contains(currentNeighbor))
						{
							toBeVisited.addLast(currentNeighbor);
							parents.put(currentNeighbor, currentNode);
						}
						// Since we have previously visited this neighbor and
						// this neighbor is not parent of currentNode, given
						// graph contains a component that is not tree, hence
						// it is not a forest.
						else
						{
							isForest = false;
							break;
						}
					}
				}
			}

			if (!isForest)
			// The graph contains a component that is not a tree. Empty
			// previously found trees. The method will end.
			{
				flatForest.clear();
			}
			else
			// Save currently visited nodes as a tree in our forest. Reset
			// visited and parents lists. Continue with the next component of
			// the graph, if any.
			{
				flatForest.add(new ArrayList<LNode>(visited));
				unProcessedNodes.removeAll(visited);
				visited.clear();
				parents.clear();
			}
		}

		return flatForest;
	}

	/**
	 * This method creates dummy nodes (an l-level node with minimal dimensions)
	 * for the given edge (one per bendpoint). The existing l-level structure
	 * is updated accordingly.
	 *
	 * @param edge given edge
	 *
	 * @return list of dummy nodes for bendpoints
	 */
	public List createDummyNodesForBendpoints(LEdge edge)
	{
		List dummyNodes = new ArrayList();
		LNode prev = edge.source;
		
		LGraph graph = graphManager.
			calcLowestCommonAncestor(edge.source, edge.target);

		for (int i = 0; i < edge.bendpoints.size(); i++)
		{
			// create new dummy node
			LNode dummyNode = newNode(null);
			dummyNode.setRect(new Point(0,0), new Dimension(1,1));
			
			graph.add(dummyNode);

			// create new dummy edge between prev and dummy node
			LEdge dummyEdge = newEdge(null);
			graphManager.add(dummyEdge, prev, dummyNode);

			dummyNodes.add(dummyNode);
			prev = dummyNode;
		}

		LEdge dummyEdge = newEdge(null);
		graphManager.add(dummyEdge, prev, edge.target);

		edgeToDummyNodes.put(edge, dummyNodes);
		
		// remove real edge from graph manager if it is inter-graph
		if (edge.isInterGraph())
		{
			graphManager.remove(edge);
		}
		// else, remove the edge from the current graph
		else
		{
			graph.remove(edge);
		}
		
		return dummyNodes;
	}
	
	/**
	 * This method creates bendpoints for edges from the dummy nodes
	 * at l-level.
	 */
	public void createBendpointsFromDummyNodes()
	{
		List edges = new ArrayList();
		edges.addAll(Arrays.asList(graphManager.getAllEdges()));
		edges.addAll(0, edgeToDummyNodes.keySet());

		for (int k = 0 ; k < edges.size(); k++)
		{
			LEdge lEdge = (LEdge) edges.get(k);

			if (lEdge.bendpoints.size() > 0)
			{
				List path = (List) edgeToDummyNodes.get(lEdge);

				for (int i = 0; i < path.size(); i++)
				{
					LNode dummyNode = ((LNode)path.get(i));
					PointD p = new PointD(dummyNode.getCenterX(),
							dummyNode.getCenterY());

					// update bendpoint's location according to dummy node
					
					PointD ebp = lEdge.bendpoints.get(i);
					ebp.x = p.x;
					ebp.y = p.y;
					
					// remove the dummy node, dummy edges incident with this
					// dummy node is also removed (within the remove method)
					dummyNode.getOwner().remove(dummyNode);					
				}
				
				// add the real edge to graph
				graphManager.add(lEdge, lEdge.source, lEdge.target);
			}
		}
	}
	
// -----------------------------------------------------------------------------
// Section: Class methods
// -----------------------------------------------------------------------------
	/**
	 * This method transforms the input slider value into actual parameter
	 * value using two separate linear functions (one from 0 to 50, other from
	 * 50 to 100), where default slider value (50) maps to the default value of
	 * the associated actual parameter. Minimum and maximum slider values map to
	 * 1/10 and 10 fold of this default value, respectively.
	 *
	 * @param sliderValue slider current value
	 * @param defaultValue slider default actual parameter value
	 *
	 * @return actual parameter value
	 */
	public static double transform(int sliderValue, double defaultValue)
	{
		double a, b;

		if (sliderValue <= 50)
		{
			a = 9.0 * defaultValue / 500.0;
			b = defaultValue / 10.0;
		}
		else
		{
			a = 9.0 * defaultValue / 50.0;
			b = -8 * defaultValue;
		}

		return (a * sliderValue + b);
	}

	public static double transform(int sliderValue, 
		double defaultValue,
		double minDiv,
		double maxMul)
	{
		double value = defaultValue;
		
		if (sliderValue <= 50)
		{
			double minValue = defaultValue / minDiv;
			value -= ((defaultValue - minValue) / 50) * (50 - sliderValue);
		}
		else
		{
			double maxValue = defaultValue * maxMul;
			value += ((maxValue - defaultValue) / 50) * (sliderValue - 50);
		}
		
		return value;
	}
	
	/**
	 * This method takes a list of lists, where each list contains l-nodes of a
	 * tree. Center of each tree is return as a list of.
	 *
	 * @param listofLists list of lists of l-nodes of a tree
	 *
	 * @return list of centre nodes
	 */
	public static List<LNode> findCenterOfEachTree(List<List> listofLists)
	{
		ArrayList<LNode> centers = new ArrayList<LNode>();

		for (int i = 0; i < listofLists.size(); i++)
		{
			List<LNode> list = listofLists.get(i);
			LNode center = findCenterOfTree(list);
			centers.add(i, center);
		}

		return centers;
	}

	/**
	 * This method finds and returns the center of the given nodes, assuming
	 * that the given nodes form a tree in themselves.
	 *
	 * @param nodes list of nodes
	 *
	 * @return the centre node of the tree
	 */
	public static LNode findCenterOfTree(List<LNode> nodes)
	{
		ArrayList<LNode> list = new ArrayList<LNode>();
		list.addAll(nodes);

		ArrayList<LNode> removedNodes = new ArrayList<LNode>();
		HashMap<LNode, Integer> remainingDegrees =
			new HashMap<LNode, Integer>();
		boolean foundCenter = false;
		LNode centerNode = null;

		if (list.size() == 1 || list.size() == 2)
		{
			foundCenter = true;
			centerNode = list.get(0);
		}

		Iterator<LNode> iter = list.iterator();

		while (iter.hasNext())
		{
			LNode node = iter.next();
			Integer degree = new Integer(node.getNeighborsList().size());
			remainingDegrees.put(node , degree);

			if (degree.intValue() == 1)
			{
				removedNodes.add(node);
			}
		}

		ArrayList<LNode> tempList = new ArrayList<LNode>();
		tempList.addAll(removedNodes);

		while (!foundCenter)
		{
			ArrayList<LNode> tempList2 = new ArrayList<LNode>();
			tempList2.addAll(tempList);
			tempList.removeAll(tempList);
			iter = tempList2.iterator();

			while (iter.hasNext())
			{
				LNode node = iter.next();
				list.remove(node);

				Set<LNode> neighbours = node.getNeighborsList();

				for (LNode neighbor: neighbours)
				{
					if (!removedNodes.contains(neighbor))
					{
						Integer otherDegree = remainingDegrees.get(neighbor);
						Integer newDegree =
							new Integer(otherDegree.intValue() - 1);

						if (newDegree.intValue() == 1)
						{
							tempList.add(neighbor);
						}

						remainingDegrees.put(neighbor, newDegree);
					}
				}
			}

			removedNodes.addAll(tempList);

			if (list.size() == 1 || list.size() == 2)
			{
				foundCenter = true;
				centerNode = list.get(0);
			}
		}

		return centerNode;
	}

// -----------------------------------------------------------------------------
// Section: Class variables
// -----------------------------------------------------------------------------
	/**
	 * Used for deterministic results on consecutive executions of layout.
	 */
	public static final long RANDOM_SEED = 1;
	
// -----------------------------------------------------------------------------
// Section: Coarsening
// -----------------------------------------------------------------------------
	/**
	 * During the coarsening process, this layout may be referenced by two graph managers
	 * this setter function grants access to change the currently being used graph manager.
	 *
	 * @param gm graph manager
	 */
	public void setGraphManager (LGraphManager gm)
	{
		this.graphManager = gm;
	}
}