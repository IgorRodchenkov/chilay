package org.ivis.layout.cose;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.awt.*;

import org.ivis.layout.*;
import org.ivis.layout.fd.*;
import org.ivis.util.*;

/**
 * This class implements the overall layout process for the CoSE algorithm.
 * Details of this algorithm can be found in the following article:
 * 		U. Dogrusoz, E. Giral, A. Cetintas, A. Civril, and E. Demir,
 * 		"A Layout Algorithm For Undirected Compound Graphs",
 * 		Information Sciences, 179, pp. 980-994, 2009.
 *
 * @author Ugur Dogrusoz
 * @author Erhan Giral
 * @author Cihan Kucukkececi
 * @author Alper Karacelik
 * 
 * Copyright: i-Vis Research Group, Bilkent University, 2007 - present
 */
public class CoSELayout extends FDLayout
{
// -----------------------------------------------------------------------------
// Section: Instance variables
// -----------------------------------------------------------------------------
	/**
	 * Whether or not multi-level scaling should be used to speed up layout
	 */
	public boolean useMultiLevelScaling =
		CoSEConstants.DEFAULT_USE_MULTI_LEVEL_SCALING;
	
	/**
	 * Level of the current graph manager in the coarsening process
	 */
	private int level;
	
	/**
	 * Total level number
	 */
	private int noOfLevels;
	
	/**
	 * Holds all graph managers (M0 to Mk)
	 */
	ArrayList<CoSEGraphManager> MList;

// -----------------------------------------------------------------------------
// Section: Constructors and initializations
// -----------------------------------------------------------------------------
	/**
	 * The constructor creates and associates with this layout a new graph
	 * manager as well.
	 */
	public CoSELayout()
	{
		super();
	}

	/**
	 * This method creates a new graph manager associated with this layout.
	 */
	protected LGraphManager newGraphManager()
	{
		LGraphManager gm = new CoSEGraphManager(this);
		graphManager = gm;
		return gm;
	}
	
	/**
	 * This method creates a new graph associated with the input view graph.
	 */
	public LGraph newGraph(Object vGraph)
	{
		return new CoSEGraph(null, graphManager, vGraph);
	}

	/**
	 * This method creates a new node associated with the input view node.
	 */
	public LNode newNode(Object vNode)
	{
		return new CoSENode(graphManager, vNode);
	}

	/**
	 * This method creates a new edge associated with the input view edge.
	 */
	public LEdge newEdge(Object vEdge)
	{
		return new CoSEEdge(null, null, vEdge);
	}

	/**
	 * This method is used to set all layout parameters to default values.
	 */
	public void initParameters()
	{
		super.initParameters();

		if (!isSubLayout)
		{
			LayoutOptionsPack.CoSE layoutOptionsPack =
				LayoutOptionsPack.getInstance().getCoSE();

			if (layoutOptionsPack.idealEdgeLength < 10)
			{
				idealEdgeLength = 10;
			}
			else
			{
				idealEdgeLength = layoutOptionsPack.idealEdgeLength;
			}

			useSmartIdealEdgeLengthCalculation =
				layoutOptionsPack.smartEdgeLengthCalc;
			useMultiLevelScaling =
				layoutOptionsPack.multiLevelScaling;
			springConstant =
				transform(layoutOptionsPack.springStrength,
					FDLayoutConstants.DEFAULT_SPRING_STRENGTH, 5.0, 5.0);
			repulsionConstant =
				transform(layoutOptionsPack.repulsionStrength,
					FDLayoutConstants.DEFAULT_REPULSION_STRENGTH, 5.0, 5.0);
			gravityConstant =
				transform(layoutOptionsPack.gravityStrength,
					FDLayoutConstants.DEFAULT_GRAVITY_STRENGTH);
			compoundGravityConstant =
				transform(layoutOptionsPack.compoundGravityStrength,
					FDLayoutConstants.DEFAULT_COMPOUND_GRAVITY_STRENGTH);
			gravityRangeFactor =
				transform(layoutOptionsPack.gravityRange,
					FDLayoutConstants.DEFAULT_GRAVITY_RANGE_FACTOR);
			compoundGravityRangeFactor =
				transform(layoutOptionsPack.compoundGravityRange,
					FDLayoutConstants.DEFAULT_COMPOUND_GRAVITY_RANGE_FACTOR);
		}
	}
		
// -----------------------------------------------------------------------------
// Section: Layout!
// -----------------------------------------------------------------------------

	/**
	 * This method performs layout on constructed l-level graph. It returns true
	 * on success, false otherwise.
	 */
	public boolean layout()
	{
		boolean createBendsAsNeeded = LayoutOptionsPack.getInstance().getGeneral().createBendsAsNeeded;

		if (createBendsAsNeeded) {
			createBendpoints();
			// reset edge list, since the topology has changed
			graphManager.resetAllEdges();
		}
		
		if (useMultiLevelScaling && !incremental) {
			return multiLevelScalingLayout();
		}
		else {
			level = 0;
			return classicLayout();
		}
	}

	/**
	 * This method applies multi-level scaling during layout
	 */
	private boolean multiLevelScalingLayout ()
	{
		CoSEGraphManager gm = (CoSEGraphManager)graphManager;

		// Start coarsening process
		
		// save graph managers M0 to Mk in an array list
		MList = gm.coarsenGraph();

		noOfLevels = MList.size()-1;
		level = noOfLevels;
		
		while (level >= 0)
		{
			graphManager = gm = MList.get(level);
			
//			System.out.print("@" + level + "th level, with " + gm.getRoot().getNodes().size() + " nodes. ");
			classicLayout();

			// after finishing layout of first (coarsest) level,
			incremental = true;
			
			if (level >= 1)
			{	
				uncoarsen(); // also makes initial placement for Mi-1
			}
			
			// reset total iterations
			totalIterations = 0;
			
			level--;
		}
		
		incremental = false;
		return true;
	}
	
	/**
	 * This method uses classic layout method (without multi-scaling)
	 * @return
	 */
	protected boolean classicLayout ()
	{
		calculateNodesToApplyGravitationTo();
	
		graphManager.calcLowestCommonAncestors();
		graphManager.calcInclusionTreeDepths();
	
		graphManager.getRoot().calcEstimatedSize();
		calcIdealEdgeLengths();
	
		if (!incremental)
		{
			List<List<LNode>> forest = getFlatForest();
	
			if (forest.size() > 0)
			// The graph associated with this layout is flat and a forest
			{
				positionNodesRadially(forest);
			}
			else
			// The graph associated with this layout is not flat or a forest
			{
				positionNodesRandomly();
			}
		}
	
		initSpringEmbedder();
		runSpringEmbedder();
	
//		System.out.println("Classic CoSE layout finished after " +
//			totalIterations + " iterations");
		
		return true;
	}
	/**
	 * This method performs the actual layout on the l-level compound graph. An
	 * update() needs to be called for changes to be propogated to the v-level
	 * compound graph.
	 */
	public void runSpringEmbedder()
	{
//		if (!incremental)
//		{
//			CoSELayout.randomizedMovementCount = 0;
//			CoSELayout.nonRandomizedMovementCount = 0;
//		}

//		updateAnnealingProbability();

		do
		{
			totalIterations++;

			if (totalIterations % FDLayoutConstants.CONVERGENCE_CHECK_PERIOD == 0)
			{
				if (isConverged())
				{
					break;
				}

				coolingFactor = initialCoolingFactor *
					((maxIterations - totalIterations) / (double)maxIterations);
				
//				updateAnnealingProbability();
			}

			totalDisplacement = 0;

			graphManager.updateBounds();
			calcSpringForces();
			calcRepulsionForces();
			calcGravitationalForces();
			moveNodes();

			animate();
		}
		while (totalIterations < maxIterations);
		
		graphManager.updateBounds();
	}

	/**
	 * This method finds and forms a list of nodes for which gravitation should
	 * be applied. For connected graphs (root graph or compounds / child graphs)
	 * there is no need to apply gravitation. While doing so, each graph in the
	 * associated graph manager is marked as connected or not.
	 */
	public void calculateNodesToApplyGravitationTo()
	{
		LinkedList nodeList = new LinkedList();
		LGraph graph;

		for (Object obj : graphManager.getGraphs())
		{
			graph = (LGraph) obj;

			graph.updateConnected();

			if (!graph.isConnected())
			{
				nodeList.addAll(graph.getNodes());
			}
		}

		graphManager.setAllNodesToApplyGravitation(nodeList);

//		// Use this to apply the idea for flat graphs only
//		if (graphManager.getGraphs().size() == 1)
//		{
//			LGraph root = graphManager.getRoot();
//			assert graphManager.getGraphs().get(0) == root;
//
//			root.updateConnected();
//
//			if (!root.isConnected())
//			{
//				graphManager.setAllNodesToApplyGravitation(
//					graphManager.getAllNodes());
//			}
//			else
//			{
//				graphManager.setAllNodesToApplyGravitation(new LinkedList());
//			}
//		}
//		else
//		{
//			graphManager.setAllNodesToApplyGravitation(
//				graphManager.getAllNodes());
//		}
	}

	/**
	 * This method creates bendpoints multi-edges which are incident to same  
	 * source and target nodes, and for all edges that have the same node as 
	 * both source and target.
	 */
	private void createBendpoints()
	{
		List edges = new ArrayList();
		edges.addAll(Arrays.asList(graphManager.getAllEdges()));
		Set visited = new HashSet();

		for (int i = 0; i < edges.size(); i++)
		{
			LEdge edge = (LEdge) edges.get(i);

			if (!visited.contains(edge))
			{
				LNode source = edge.getSource();
				LNode target = edge.getTarget();

				if (source == target)
				{
					edge.getBendpoints().add(new PointD());
					edge.getBendpoints().add(new PointD());
					createDummyNodesForBendpoints(edge);
					visited.add(edge);
				}
				else
				{
					List edgeList = new ArrayList();
					
					edgeList.addAll(source.getEdgeListToNode(target));
					edgeList.addAll(target.getEdgeListToNode(source));

					if (!visited.contains(edgeList.get(0)))
					{
						if (edgeList.size() > 1)
						{
							for(int k = 0; k < edgeList.size(); k++)
							{
								LEdge multiEdge = (LEdge)edgeList.get(k);
								multiEdge.getBendpoints().add(new PointD());
								createDummyNodesForBendpoints(multiEdge);
							}
						}

						visited.addAll(edgeList);
					}
				}
			}

			if (visited.size() == edges.size())
			{
				break;
			}
		}
	}
	
	/**
	 * This method performs initial positioning of given forest radially. The
	 * final drawing should be centered at the gravitational center.
	 */
	protected void positionNodesRadially(List<List<LNode>> forest)
	{
		// We tile the trees to a grid row by row; first tree starts at (0,0)
		Point currentStartingPoint = new Point(0, 0);
		int numberOfColumns = (int) Math.ceil(Math.sqrt(forest.size()));
		int height = 0;
		int currentY = 0;
		int currentX = 0;
		PointD point = new PointD(0, 0);

		for (int i = 0; i < forest.size(); i++)
		{
			if (i % numberOfColumns == 0)
			{
				// Start of a new row, make the x coordinate 0, increment the
				// y coordinate with the max height of the previous row
				currentX = 0;
				currentY = height;

				if (i !=0)
				{
					currentY += CoSEConstants.DEFAULT_COMPONENT_SEPERATION;
				}

				height = 0;
			}

			List<LNode> tree = forest.get(i);

			// Find the center of the tree
			LNode centerNode = Layout.findCenterOfTree(tree);

			// Set the staring point of the next tree
			currentStartingPoint.x = currentX;
			currentStartingPoint.y = currentY;

			// Do a radial layout starting with the center
			point =
				CoSELayout.radialLayout(tree, centerNode, currentStartingPoint);

			if (point.y > height)
			{
				height = (int) point.y;
			}

			currentX = (int)
				(point.x + CoSEConstants.DEFAULT_COMPONENT_SEPERATION);
		}

		transform(
			new PointD(LayoutConstants.WORLD_CENTER_X - point.x / 2,
				LayoutConstants.WORLD_CENTER_Y - point.y / 2));
	}

	/**
	 * This method positions given nodes according to a simple radial layout
	 * starting from the center node. The top-left of the final drawing is to be
	 * at given location. It returns the bottom-right of the bounding rectangle
	 * of the resulting tree drawing.
	 */
	private static PointD radialLayout(List<LNode> tree,
		LNode centerNode,
		Point startingPoint)
	{
		double radialSep = Math.max(maxDiagonalInTree(tree),
			CoSEConstants.DEFAULT_RADIAL_SEPARATION);
		CoSELayout.branchRadialLayout(centerNode, null, 0, 359, 0, radialSep);
		Rectangle bounds = LGraph.calculateBounds(tree);

		Transform transform = new Transform();
		transform.setDeviceOrgX(bounds.getMinX());
		transform.setDeviceOrgY(bounds.getMinY());
		transform.setWorldOrgX(startingPoint.x);
		transform.setWorldOrgY(startingPoint.y);

		for (int i = 0; i < tree.size(); i++)
		{
			LNode node = tree.get(i);
			node.transform(transform);
		}

		PointD bottomRight =
			new PointD(bounds.getMaxX(), bounds.getMaxY());

		return transform.inverseTransformPoint(bottomRight);
	}

	/**
	 * This method is recursively called for radial positioning of a node,
	 * between the specified angles. Current radial level is implied by the
	 * distance given. Parent of this node in the tree is also needed.
	 */
	private static void branchRadialLayout(LNode node,
		LNode parentOfNode,
		double startAngle, double endAngle,
		double distance, double radialSeparation)
	{
		// First, position this node by finding its angle.
		double halfInterval = ((endAngle - startAngle) + 1) / 2;

		if (halfInterval < 0)
		{
			halfInterval += 180;
		}

		double nodeAngle = (halfInterval + startAngle) % 360;
		double teta = (nodeAngle * IGeometry.TWO_PI) / 360;

		// Make polar to java cordinate conversion.
		double x = distance * Math.cos(teta);
		double y = distance * Math.sin(teta);

		node.setCenter(x, y);

		// Traverse all neighbors of this node and recursively call this
		// function.

		List<LEdge> neighborEdges = new LinkedList<LEdge>(node.getEdges());
		int childCount = neighborEdges.size();

		if (parentOfNode != null)
		{
			childCount--;
		}

		int branchCount = 0;

		int incEdgesCount = neighborEdges.size();
		int startIndex;

		List edges = node.getEdgesBetween(parentOfNode);

		// If there are multiple edges, prune them until there remains only one
		// edge.
		while (edges.size() > 1)
		{
			neighborEdges.remove(edges.remove(0));
			incEdgesCount--;
			childCount--;
		}

		if (parentOfNode != null)
		{
			assert edges.size() == 1;
			startIndex =
				(neighborEdges.indexOf(edges.get(0)) + 1) % incEdgesCount;
		}
		else
		{
			startIndex = 0;
		}

		double stepAngle = Math.abs(endAngle - startAngle) / childCount;

		for (int i = startIndex;
			branchCount != childCount ;
			i = (++i) % incEdgesCount)
		{
			LNode currentNeighbor =
				neighborEdges.get(i).getOtherEnd(node);

			// Don't back traverse to root node in current tree.
			if (currentNeighbor == parentOfNode)
			{
				continue;
			}

			double childStartAngle =
				(startAngle + branchCount * stepAngle) % 360;
			double childEndAngle = (childStartAngle + stepAngle) % 360;

			branchRadialLayout(currentNeighbor,
					node,
					childStartAngle, childEndAngle,
					distance + radialSeparation, radialSeparation);

			branchCount++;
		}
	}

	/**
	 * This method finds the maximum diagonal length of the nodes in given tree.
	 */
	private static double maxDiagonalInTree(List<LNode> tree)
	{
		double maxDiagonal = Double.MIN_VALUE;

		for (int i = 0; i < tree.size(); i++)
		{
			LNode node = tree.get(i);
			double diagonal = node.getDiagonal();

			if (diagonal > maxDiagonal)
			{
				maxDiagonal = diagonal;
			}
		}

		return maxDiagonal;
	}

	// -----------------------------------------------------------------------------
	// Section: Multi-level Scaling
	// -----------------------------------------------------------------------------
	
	/**
	 * This method un-coarsens Mi to Mi-1 and makes initial placement for Mi-1
	 */
	public void uncoarsen ()
	{
		for (Object obj: graphManager.getAllNodes())
		{
			CoSENode v = (CoSENode) obj;
			// set positions of v.pred1 and v.pred2
			v.getPred1().setLocation(v.getLeft(), v.getTop());
			
			if (v.getPred2() != null)
			{
				// TODO: check (what?)
				/*
				double w = v.getPred1().getRect().width;
				double l = idealEdgeLength;
				v.getPred2().setLocation((v.getPred1().getLeft()+w+l), (v.getPred1().getTop()+w+l));
				*/
				v.getPred2().setLocation(v.getLeft()+idealEdgeLength,
					v.getTop()+idealEdgeLength);
			}
		}
	}
	
// -----------------------------------------------------------------------------
// Section: FR-Grid Variant Repulsion Force Calculation
// -----------------------------------------------------------------------------
	
	/**
	 * This method calculates the repulsion range
	 * Also it can be used to calculate the height of a grid's edge
	 */
	@Override
	protected double calcRepulsionRange()
	{
		// formula is 2 x (level + 1) x idealEdgeLength
		return (2 * ( level+1 ) * idealEdgeLength);
	}
	
// -----------------------------------------------------------------------------
// Section: Temporary methods (especially for testing)
// -----------------------------------------------------------------------------

//	/**
//	 * This method checks if there is a node with null vGraphObject
//	 */
//	private boolean checkVGraphObjects()
//	{
//		if (graphManager.getAllEdges() == null)
//		{
//			System.out.println("Edge list is null!");
//		}
//		if (graphManager.getAllNodes() == null)
//		{
//			System.out.println("Node list is null!");
//		}
//		if (graphManager.getGraphs() == null)
//		{
//			System.out.println("Graph list is null!");
//		}
//		for (Object obj: graphManager.getAllEdges())
//		{
//			CoSEEdge e = (CoSEEdge) obj;
//			//NodeModel nm = (NodeModel) v.vGraphObject;
//
//			if (e.vGraphObject == null)
//			{
//				System.out.println("Edge(Source): " + e.getSource().getRect() + " has null vGraphObject!");
//				return false;
//			}
//		}
//
//		for (Object obj: graphManager.getAllNodes())
//		{
//			CoSENode v = (CoSENode) obj;
//			//NodeModel nm = (NodeModel) v.vGraphObject;
//
//			if (v.vGraphObject == null)
//			{
//				System.out.println("Node: " + v.getRect() + " has null vGraphObject!");
//				return false;
//			}
//		}
//
//		for (Object obj: graphManager.getGraphs())
//		{
//			LGraph l = (LGraph) obj;
//			if (l.vGraphObject == null)
//			{
//				System.out.println("Graph with " + l.getNodes().size() + " nodes has null vGraphObject!");
//				return false;
//			}
//		}
//		return true;
//	}
//	private void updateAnnealingProbability()
//	{
//		CoSELayout.annealingProbability = Math.pow(Math.E,
//			CoSELayout.annealingConstant / coolingFactor);
//	}
}