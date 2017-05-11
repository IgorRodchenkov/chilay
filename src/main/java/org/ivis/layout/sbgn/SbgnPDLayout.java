package org.ivis.layout.sbgn;

import java.util.*;

import org.ivis.layout.*;
import org.ivis.layout.cose.CoSELayout;
import org.ivis.layout.cose.CoSENode;
import org.ivis.layout.fd.FDLayoutConstants;
import org.ivis.layout.fd.FDLayoutEdge;
import org.ivis.layout.fd.FDLayoutNode;
import org.ivis.layout.sbgn.SbgnProcessNode.Orientation;
import org.ivis.layout.util.MemberPack;
import org.ivis.layout.util.RectProc;
import org.ivis.util.IGeometry;
import org.ivis.util.PointD;
import org.ivis.util.RectangleD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the layout process of SBGN notation.
 * 
 * @author Begum Genc
 * 
 *         Copyright: i-Vis Research Group, Bilkent University, 2007 - present
 */
public class SbgnPDLayout extends CoSELayout
{
	private static final Logger log = LoggerFactory.getLogger(SbgnPDLayout.class);

	/**
	 * For remembering contents of a complex.
	 */
	Map<SbgnPDNode, LGraph> childGraphMap;

	/**
	 * Used during Tiling.
	 */
	Map<SbgnPDNode, MemberPack> memberPackMap;

	/**
	 * List of dummy complexes (a dummy complex for all degree zero nodes at a
	 * level)
	 */
	LinkedList<SbgnPDNode> dummyComplexList;

	/**
	 * List of removed complexes (was created for degree zero nodes). They store
	 * the dummy complex and its child graph
	 */
	Map<SbgnPDNode, LGraph> emptiedDummyComplexMap;

	/**
	 * This list stores the complex molecules as a result of DFS. The first
	 * element corresponds to the deep-most node.
	 */
	LinkedList<SbgnPDNode> complexOrder;

	/**
	 * This parameter indicates the chosen compaction method.
	 */
	private DefaultCompactionAlgorithm compactionMethod;

	/**
	 * This parameter stores the properly oriented total edge count of all
	 * process nodes.
	 */
	public double properlyOrientedEdgeCount;

	/**
	 * This parameter stores the total neighboring edge count of all process
	 * nodes.
	 */
	public double totalEdgeCountToBeOriented;

	/**
	 * This parameter indicates the phase number (1 or 2)
	 */
	private int phaseNumber;

	// for results
	public int phase1IterationCount;
	public int phase2IterationCount;

	/**
	 * 0 for roulette wheel selection, 1 for random selection.
	 */
	public int rotationRandomizationMethod;

	public ArrayList<SbgnProcessNode> processNodeList;

	public double successRatio;
	public double enhancedRatio;
	
	public int totalEffCount;

	public SbgnPDLayout()
	{
		rotationRandomizationMethod = 1;
		enhancedRatio = 0;
		totalEffCount = 0;
		compactionMethod = DefaultCompactionAlgorithm.TILING;
		childGraphMap = new HashMap<SbgnPDNode, LGraph>();
		complexOrder = new LinkedList<SbgnPDNode>();
		dummyComplexList = new LinkedList<SbgnPDNode>();
		emptiedDummyComplexMap = new HashMap<SbgnPDNode, LGraph>();
		processNodeList = new ArrayList<SbgnProcessNode>();
		if (compactionMethod == DefaultCompactionAlgorithm.TILING)
			memberPackMap = new HashMap<SbgnPDNode, MemberPack>();
	}

	/**
	 * This method performs the actual layout on the l-level compound
	 * graph. An update() needs to be called for changes to be
	 * propagated to the v-level compound graph.
	 */
	@Override
	public void runSpringEmbedder()
	{
		log.info("SBGN-PD Layout phase1...");
		phaseNumber = 1;
		doPhase1();

		log.info("SBGN-PD Layout phase2...");
		phaseNumber = 2;
		doPhase2();

		// used to calculate - to make sure
		recalcProperlyOrientedEdges(true);
		
		log.info("success ratio: " + successRatio);

		finalEnhancement();
		
		log.info("enhanced ratio: " + enhancedRatio);

		removeDummyCompounds();
	}

	/**
	 * At this phase, CoSE is applied for a number of iterations.
	 */
	private void doPhase1()
	{
		maxIterations = SbgnPDConstants.PHASE1_MAX_ITERATION_COUNT;
		totalIterations = 0;

		do
		{
			totalIterations++;
			if (totalIterations
					% FDLayoutConstants.CONVERGENCE_CHECK_PERIOD == 0)
			{
				if (isConverged())
				{
					break;
				}

				coolingFactor = initialCoolingFactor
						* ((maxIterations - totalIterations) / (double) maxIterations);
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
		phase1IterationCount = totalIterations;
	}

	/**
	 * At this phase, location of single nodes are approximated occasionally.
	 * Rotational forces are applied. Cooling factor starts from a small value
	 * to prevent huge changes.
	 */
	private void doPhase2()
	{
		// dynamic max iteration
		maxIterations = (int) Math.log(getAllEdges().length
				+ getAllNodes().length) * 400;

		// cooling fac is small
		initialCoolingFactor = SbgnPDConstants.PHASE2_INITIAL_COOLINGFACTOR;
		coolingFactor = initialCoolingFactor;

		totalIterations = 0;

		do
		{
			totalIterations++;

			if (totalIterations % FDLayoutConstants.CONVERGENCE_CHECK_PERIOD == 0)
			{
				successRatio = properlyOrientedEdgeCount
						/ totalEdgeCountToBeOriented;

				if (isConverged() && successRatio >= SbgnPDConstants.ROTATIONAL_FORCE_CONVERGENCE)
				{
					break;
				}

				coolingFactor = initialCoolingFactor * ((maxIterations - totalIterations) / (double) maxIterations);
			}

			totalDisplacement = 0;

			graphManager.updateBounds();

			calcSpringForces();
			calcRepulsionForces(); //TODO: this may take many minutes...
			calcGravitationalForces();
			moveNodes();

			animate();

		} while (totalIterations < maxIterations && totalIterations < 2500); //10000 was too much (~1e+25 nodes+edges :))

		phase2IterationCount = totalIterations;
		graphManager.updateBounds();
	}

	@Override
	public void moveNodes()
	{
		properlyOrientedEdgeCount = 0;
		totalEdgeCountToBeOriented = 0;

		// only change single node positions on early stages
		if (hasApproximationPeriodReached() && coolingFactor > 0.02)
		{
			for (SbgnProcessNode p : processNodeList)
				p.applyApproximations();
		}

		for (SbgnProcessNode p : processNodeList)
		{
			// calculate rotational forces for phase 2 only
			if (phaseNumber == 2)
			{
				p.calcRotationalForces();

				properlyOrientedEdgeCount += p.properEdgeCount;
				totalEdgeCountToBeOriented += (p.consumptionEdges.size()
						+ p.productEdges.size() + p.effectorEdges.size());
				successRatio = properlyOrientedEdgeCount
						/ totalEdgeCountToBeOriented;
			}
			p.transferForces();

			p.resetForces();
			p.inputPort.resetForces();
			p.outputPort.resetForces();

		}

		// each time, rotate one process that wants to rotate
		if (totalIterations
				% SbgnPDConstants.ROTATIONAL_FORCE_ITERATION_COUNT == 0
				&& phaseNumber == 2)
			rotateAProcess();

		super.moveNodes();
	}

	private boolean hasApproximationPeriodReached()
	{
		if(totalIterations % 100 == SbgnPDConstants.APPROXIMATION_PERIOD)
			return true;
		else
			return false;
	}

	private void rotateAProcess()
	{
		ArrayList<SbgnProcessNode> processNodesToBeRotated = new ArrayList<SbgnProcessNode>();

		for (SbgnProcessNode p : processNodeList)
		{
			if (p.isRotationNecessary())
				processNodesToBeRotated.add(p);
		}

		// random selection
		if (processNodesToBeRotated.size() > 0)
		{
			int randomIndex = 0;

			if (rotationRandomizationMethod == 0)
			{
				randomIndex = rouletteWheelSelection(processNodesToBeRotated);
				if (randomIndex == -1)
					log.error("ERROR: no nodes have been selected for rotation");
			}
			else
			{
				randomIndex = (int) (Math.random() * processNodesToBeRotated
						.size());
			}

			SbgnProcessNode p = processNodesToBeRotated.get(randomIndex);
			p.applyRotation();
		}
	}

	/*
	 * This method iterates over the process nodes and checks if there exists
	 * another orientation which maximizes the total number of proper edges.
	 * If there is, the orientation is changed.
	 */
	private void finalEnhancement()
	{
		List<Orientation> orientationList;
		double bestStepResult;
		Orientation bestOrientation = null;
		double stepAppropriateEdgeCnt = 0;
		double totalProperEdges = 0;
		double angle;

		orientationList = new ArrayList<Orientation>();
		orientationList.add(Orientation.LEFT_TO_RIGHT);
		orientationList.add(Orientation.RIGHT_TO_LEFT);
		orientationList.add(Orientation.TOP_TO_BOTTOM);
		orientationList.add(Orientation.BOTTOM_TO_TOP);

		for (SbgnProcessNode p : processNodeList)
		{
			bestStepResult = p.properEdgeCount;
			bestOrientation = null;
			List<Boolean> rememberPropList;
			List<Boolean> bestPropList = new ArrayList<Boolean>();

			for (Orientation orient : orientationList)
			{
				stepAppropriateEdgeCnt = 0;

				PointD inputPortTarget = p.findPortTargetPoint(true, orient);
				PointD outputPortTarget = p.findPortTargetPoint(false, orient);

				rememberPropList = new ArrayList<Boolean>();

				for (SbgnPDEdge edge : p.consumptionEdges)
				{
					SbgnPDNode node = (SbgnPDNode) edge.getSource();
					angle = IGeometry.calculateAngle(inputPortTarget,
							p.inputPort.getCenter(), node.getCenter());
					if (angle <= SbgnPDConstants.ANGLE_TOLERANCE)
					{
						stepAppropriateEdgeCnt++;
						rememberPropList.add(true);
					}
					else
						rememberPropList.add(false);
				}

				for (SbgnPDEdge edge : p.productEdges)
				{
					SbgnPDNode node = (SbgnPDNode) edge.getTarget();
					angle = IGeometry.calculateAngle(outputPortTarget,
							p.outputPort.getCenter(), node.getCenter());

					if (angle <= SbgnPDConstants.ANGLE_TOLERANCE)
					{
						stepAppropriateEdgeCnt++;
						rememberPropList.add(true);

					}
					else
						rememberPropList.add(false);
				}

				for (SbgnPDEdge edge : p.effectorEdges)
				{
					SbgnPDNode node = (SbgnPDNode) edge.getSource();
					angle = calcEffectorAngle(orient, p.getCenter(), node);

					if (angle <= SbgnPDConstants.EFFECTOR_ANGLE_TOLERANCE)
					{
						stepAppropriateEdgeCnt++;
						rememberPropList.add(true);

					}
					else
						rememberPropList.add(false);
				}

				if (stepAppropriateEdgeCnt > bestStepResult)
				{
					bestStepResult = stepAppropriateEdgeCnt;
					bestOrientation = orient;
					bestPropList = rememberPropList;
				}
			}
			totalProperEdges += bestStepResult;

			// it means a better position has been found
			if (bestStepResult > p.properEdgeCount)
			{
				p.setOrientation(bestOrientation);
				p.properEdgeCount = bestStepResult;

				// mark edges with best known configuration values
				for (int i = 0; i < p.consumptionEdges.size(); i++)
				{
					p.consumptionEdges.get(i).isProperlyOriented = bestPropList
							.get(i);
				}
				for (int i = 0; i < p.productEdges.size(); i++)
				{
					p.productEdges.get(i).isProperlyOriented = bestPropList
							.get(i + p.consumptionEdges.size());
				}
				for (int i = 0; i < p.effectorEdges.size(); i++)
				{
					p.effectorEdges.get(i).isProperlyOriented = bestPropList
							.get(i
									+ (p.consumptionEdges.size() + p.productEdges
											.size()));
				}
			}
		}

		properlyOrientedEdgeCount = totalProperEdges;
		enhancedRatio = totalProperEdges / totalEdgeCountToBeOriented;
		
		for(SbgnProcessNode p : processNodeList)
		{
			totalEffCount += p.effectorEdges.size();
		}
	}

	/**
	 * If a process node has higher netRotationalForce, it has more chance to be
	 * rotated
	 */
	private int rouletteWheelSelection(
			ArrayList<SbgnProcessNode> processNodesToBeRotated)
	{
		double randomNumber = Math.random();
		double[] fitnessValues = new double[processNodesToBeRotated.size()];
		double totalSum = 0, sumOfProbabilities = 0;
		int i = 0;

		for (SbgnProcessNode p : processNodesToBeRotated)
			totalSum += Math.abs(p.netRotationalForce);

		// normalize all between 0..1
		for (SbgnProcessNode p : processNodesToBeRotated)
		{
			fitnessValues[i] = sumOfProbabilities
					+ (Math.abs(p.netRotationalForce) / totalSum);
			sumOfProbabilities = fitnessValues[i];
			i++;
		}

		if (randomNumber < fitnessValues[0])
			return 0;
		else
		{
			for (int j = 0; j < fitnessValues.length - 1; j++)
			{
				if (randomNumber >= fitnessValues[j]
						&& randomNumber < fitnessValues[j + 1])
					return j + 1;
			}
		}

		return -1;
	}

	private double calcEffectorAngle(Orientation orient, PointD centerPt, CoSENode eff)
	{
		PointD targetPnt = new PointD();
		PointD centerPnt = centerPt;

		// find target point
		if (orient.equals(Orientation.LEFT_TO_RIGHT)
				|| orient.equals(Orientation.RIGHT_TO_LEFT))
		{
			targetPnt.x = centerPnt.x;

			if (eff.getCenterY() > centerPnt.y)
				targetPnt.y = centerPnt.y + idealEdgeLength;
			else
				targetPnt.y = centerPnt.y - idealEdgeLength;
		}
		else if (orient.equals(Orientation.BOTTOM_TO_TOP)
				|| orient.equals(Orientation.TOP_TO_BOTTOM))
		{
			targetPnt.y = centerPnt.y;

			if (eff.getCenterX() > centerPnt.x)
				targetPnt.x = centerPnt.x + idealEdgeLength;
			else
				targetPnt.x = centerPnt.x - idealEdgeLength;
		}

		double angle = IGeometry.calculateAngle(targetPnt, centerPnt,
				eff.getCenter());

		return angle;
	}

	/**
	 * Recursively calculate if the node or its child nodes have any edges to
	 * other nodes. Return the total number of edges.
	 */
	private int calcGraphDegree(SbgnPDNode parentNode)
	{
		int degree = 0;
		if (parentNode.getChild() == null)
		{
			degree = parentNode.getEdges().size();
			return degree;
		}

		for (Object o : parentNode.getChild().getNodes())
		{
			degree = degree + parentNode.getEdges().size()
					+ calcGraphDegree((SbgnPDNode) o);
		}

		return degree;
	}

	private void recalcProperlyOrientedEdges(boolean isLastIteration)
	{
		properlyOrientedEdgeCount = 0.0;
		totalEdgeCountToBeOriented = 0;
		// get all process nodes
		for (SbgnProcessNode p : processNodeList)
		{
			p.calcProperlyOrientedEdges();
			properlyOrientedEdgeCount += p.properEdgeCount;
			totalEdgeCountToBeOriented += (p.consumptionEdges.size()
					+ p.productEdges.size() + p.effectorEdges.size());
			successRatio = properlyOrientedEdgeCount
					/ totalEdgeCountToBeOriented;
		}
	}

	/**
	 * This method finds all the zero degree nodes in the graph which are not
	 * owned by a complex node. Zero degree nodes at each level are grouped
	 * together and placed inside a dummy complex to reduce bounds of root
	 * graph.
	 */
	private void groupZeroDegreeMembers()
	{
		Map<SbgnPDNode, LGraph> childComplexMap = new HashMap<SbgnPDNode, LGraph>();
		for (Object graphObj : getGraphManager().getGraphs())
		{
			ArrayList<SbgnPDNode> zeroDegreeNodes = new ArrayList<SbgnPDNode>();
			LGraph ownerGraph = (LGraph) graphObj;

			// do not process complex nodes (their members are already owned)
			if (ownerGraph.getParent().type != null
					&& ((SbgnPDNode) ownerGraph.getParent()).isComplex())
				continue;

			for (Object nodeObj : ownerGraph.getNodes())
			{
				SbgnPDNode node = (SbgnPDNode) nodeObj;

				if (calcGraphDegree(node) == 0)
				{
					zeroDegreeNodes.add(node);
				}
			}

			if (zeroDegreeNodes.size() > 1)
			{
				// create a new dummy complex
				SbgnPDNode complex = (SbgnPDNode) newNode(null);
				complex.type = SbgnPDConstants.COMPLEX;
				complex.label = "DummyComplex_" + ownerGraph.getParent().label;

				ownerGraph.add(complex);

				LGraph childGraph = newGraph(null);

				for (SbgnPDNode zeroNode : zeroDegreeNodes)
				{
					ownerGraph.remove(zeroNode);
					childGraph.add(zeroNode);
				}
				dummyComplexList.add(complex);
				childComplexMap.put(complex, childGraph);
			}
		}

		for (SbgnPDNode complex : dummyComplexList)
			graphManager.add(childComplexMap.get(complex), complex);

		getGraphManager().updateBounds();
	}

	/**
	 * This method creates two port nodes and a compound for each process nodes
	 * and adds them to graph.
	 */
	private void createPortNodes()
	{
		for (Object o : getAllNodes())
		{
			SbgnPDNode originalProcessNode = (SbgnPDNode) o;

			if (originalProcessNode.type.equals(SbgnPDConstants.PROCESS))
			{
				LGraph ownerGraph = originalProcessNode.getOwner();

				// create new nodes and graphs
				SbgnProcessNode processNode = (SbgnProcessNode) newProcessNode(null);
				SbgnPDNode inputPort = (SbgnPDNode) newPortNode(null,
						SbgnPDConstants.INPUT_PORT);
				SbgnPDNode outputPort = (SbgnPDNode) newPortNode(null,
						SbgnPDConstants.OUTPUT_PORT);

				// create a dummy compound
				SbgnPDNode compoundNode = (SbgnPDNode) newNode(null);
				compoundNode.type = SbgnPDConstants.DUMMY_COMPOUND;

				// add labels
				compoundNode.label = "DummyCompound_"
						+ originalProcessNode.label;
				inputPort.label = "InputPort_" + originalProcessNode.label;
				outputPort.label = "OutputPort_" + originalProcessNode.label;

				// create child graph (= 2port+process) to be set as child to
				// dummy compound
				LGraph childGraph = newGraph(null);
				ownerGraph.add(processNode);

				// convert the process node to SbgnProcessNode
				processNode.copyFromSBGNPDNode(originalProcessNode,
						getGraphManager());

				processNode.connectNodes(compoundNode, inputPort, outputPort);

				// create rigid edges, change edge connections
				processNode.reconnectEdges(idealEdgeLength);

				SbgnPDEdge rigidToProduction = (SbgnPDEdge) newRigidEdge(null);
				rigidToProduction.label = ""
						+ (graphManager.getAllEdges().length + 1);

				SbgnPDEdge rigidToConsumption = (SbgnPDEdge) newRigidEdge(null);
				rigidToConsumption.label = ""
						+ (graphManager.getAllEdges().length + 2);

				ownerGraph.remove(processNode);

				// organize child graph
				childGraph.add(processNode);
				childGraph.add(inputPort);
				childGraph.add(outputPort);
				childGraph.add(rigidToProduction, inputPort, processNode);
				childGraph.add(rigidToConsumption, outputPort, processNode);

				// organize the compound node
				compoundNode.setOwner(ownerGraph);
				compoundNode.setCenter(processNode.getCenterX(),
						processNode.getCenterY());
				ownerGraph.add(compoundNode);
				graphManager.add(childGraph, compoundNode);

				// remove the original process node
				ownerGraph.remove(originalProcessNode);

				processNodeList.add(processNode);
				graphManager.updateBounds();
			}
		}

		// important to reset -
		graphManager.resetAllNodes();
		graphManager.resetAllEdges();
		graphManager.resetAllNodesToApplyGravitation();
	}

	/*
	 * Checks whether there exist process nodes in the graph.
	 * If there are, it is assumed that the given graph respects our structure.
	 *
	 * TODO: likely, this method does not work properly. Never tested...
	 */
	private boolean arePortNodesCreated()
	{
		boolean flag = false;

		// if there are any process nodes, check for port nodes
		for (Object o : getAllNodes())
		{
			SbgnPDNode s = (SbgnPDNode) o;
			if (SbgnPDConstants.PROCESS.equals(s.type))
			{
				flag = true;
				break;
			}
		}

		// if there are no process nodes, no need to check for port nodes
		if (!flag) {
			return true;
		}
		else
		{
			// check for the port nodes. if any found, return true.
			for (Object o : getAllNodes())
			{
				if (((SbgnPDNode) o).type.equals(SbgnPDConstants.INPUT_PORT)
						|| ((SbgnPDNode) o).type.equals(SbgnPDConstants.OUTPUT_PORT))
					return true;
			}
		}

		return false;
	}

	/**
	 * This method is used to remove the dummy compounds (previously created for
	 * each process node) from the graph.
	 */
	private void removeDummyCompounds()
	{
		for (SbgnProcessNode processNode : processNodeList)
		{
			SbgnPDNode dummyNode = processNode.parentCompound;
			LGraph childGraph = dummyNode.getChild();
			LGraph owner = dummyNode.getOwner();

			// add children to original parent
			for (Object s : childGraph.getNodes())
				owner.add((SbgnPDNode) s);

			for (Object e : childGraph.getEdges())
			{
				SbgnPDEdge edge = (SbgnPDEdge) e;
//				childGraph.remove(edge); //commented out because assertions fail there...
				owner.add(edge, edge.getSource(), edge.getTarget());
			}

			// add effectors / remaining edges back to the process
			for (int i = 0; i < dummyNode.getEdges().size(); i++)
			{
				SbgnPDEdge edge = (SbgnPDEdge) dummyNode.getEdges().get(i);
				dummyNode.getEdges().remove(edge);

				edge.setTarget(processNode);
				processNode.getEdges().add(edge);
				i--;
			}

			// remove the graph
			getGraphManager().getGraphs().remove(childGraph);
			dummyNode.setChild(null);
			owner.remove(dummyNode);
		}
	}

	// ********************* SECTION : TILING METHODS *********************

	private void clearComplex(SbgnPDNode comp)
	{
		MemberPack pack = null;
		LGraph childGr = comp.getChild();
		childGraphMap.put(comp, childGr);

		if (childGr == null)
			return;

		if (compactionMethod == DefaultCompactionAlgorithm.POLYOMINO_PACKING)
		{
			applyPolyomino(comp);
		}
		else if (compactionMethod == DefaultCompactionAlgorithm.TILING)
		{
			pack = new MemberPack(childGr);
			memberPackMap.put(comp, pack);
		}

		if (dummyComplexList.contains(comp))
		{
			for (Object o : comp.getChild().getNodes())
			{
				clearDummyComplexGraphs((SbgnPDNode) o);
			}
		}

		getGraphManager().getGraphs().remove(childGr);
		comp.setChild(null);

		if (compactionMethod == DefaultCompactionAlgorithm.TILING)
		{
			comp.setWidth(pack.getWidth());
			comp.setHeight(pack.getHeight());
		}

		// Redirect the edges of complex members to the complex.
		if (childGr != null)
		{
			for (Object ch : childGr.getNodes())
			{
				SbgnPDNode chNd = (SbgnPDNode) ch;

				for (Object obj : new ArrayList(chNd.getEdges()))
				{
					LEdge edge = (LEdge) obj;
					if (edge.getSource() == chNd)
					{
						chNd.getEdges().remove(edge);
						edge.setSource(comp);
						comp.getEdges().add(edge);
					}
					else if (edge.getTarget() == chNd)
					{
						chNd.getEdges().remove(edge);
						edge.setTarget(comp);
						comp.getEdges().add(edge);
					}
				}
			}
		}
	}

	/**
	 * This method searched unmarked complex nodes recursively, because they may
	 * contain complex children. After the order is found, child graphs of each
	 * complex node are cleared.
	 */
	private void applyDFSOnComplexes()
	{
		// LGraph>();
		for (Object o : getAllNodes())
		{
			if (!(o instanceof SbgnPDNode) || !((SbgnPDNode) o).isComplex())
				continue;

			SbgnPDNode comp = (SbgnPDNode) o;

			// complex is found, recurse on it until no visited complex remains.
			if (!comp.visited)
				DFSVisitComplex(comp);
		}

		// clear each complex
		for (SbgnPDNode o : complexOrder)
		{
			clearComplex(o);
		}

		getGraphManager().updateBounds();
	}

	/**
	 * This method recurses on the complex objects. If a node does not contain
	 * any complex nodes or all the nodes in the child graph is already marked,
	 * it is reported. (Depth first)
	 * 
	 */
	private void DFSVisitComplex(SbgnPDNode node)
	{
		if (node.getChild() != null)
		{
			for (Object n : node.getChild().getNodes())
			{
				SbgnPDNode sbgnChild = (SbgnPDNode) n;
				DFSVisitComplex(sbgnChild);
			}
		}

		if (node.isComplex() && !node.containsUnmarkedComplex())
		{
			complexOrder.add(node);
			node.visited = true;
			return;
		}
	}

	/**
	 * This method tiles the given list of nodes by using polyomino packing
	 * algorithm.
	 */
	private void applyPolyomino(SbgnPDNode parent)
	{
		RectangleD r;
		LGraph childGr = parent.getChild();

		if (childGr == null)
		{
			log.info("Child graph is empty (Polyomino)");
		}
		else
		{
			// packing takes the input as an array. put the members in an array.
			SbgnPDNode[] mpArray = new SbgnPDNode[childGr.getNodes().size()];
			for (int i = 0; i < childGr.getNodes().size(); i++)
			{
				SbgnPDNode s = (SbgnPDNode) childGr.getNodes().get(i);
				mpArray[i] = s;
			}

			// pack rectangles
			RectProc.packRectanglesMino(
					SbgnPDConstants.COMPLEX_MEM_HORIZONTAL_BUFFER,
					mpArray.length, mpArray);

			// apply compaction
			Compaction c = new Compaction(
					(ArrayList<SbgnPDNode>) childGr.getNodes());
			c.perform();

			// get the resulting rectangle and set parent's (complex) width &
			// height
			r = calculateBounds(true,
					(ArrayList<SbgnPDNode>) childGr.getNodes());

			parent.setWidth(r.getWidth());
			parent.setHeight(r.getHeight());
		}
	}

	/**
	 * Reassigns the complex content. The outermost complex is placed first.
	 */
	protected void repopulateComplexes()
	{
		for (SbgnPDNode comp : emptiedDummyComplexMap.keySet())
		{
			LGraph chGr = emptiedDummyComplexMap.get(comp);
			comp.setChild(chGr);
			getGraphManager().getGraphs().add(chGr);
		}

		for (int i = complexOrder.size() - 1; i >= 0; i--)
		{
			SbgnPDNode comp = complexOrder.get(i);
			LGraph chGr = childGraphMap.get(comp);

			// repopulate the complex
			comp.setChild(chGr);

			// if the child graph is not null, adjust the positions of members
			if (chGr != null)
			{
				// adjust the positions of the members
				if (compactionMethod == DefaultCompactionAlgorithm.POLYOMINO_PACKING)
				{
					adjustLocation(comp, chGr);
					getGraphManager().getGraphs().add(chGr);
				}
				else if (compactionMethod == DefaultCompactionAlgorithm.TILING)
				{
					getGraphManager().getGraphs().add(chGr);

					MemberPack pack = memberPackMap.get(comp);
					pack.adjustLocations(comp.getLeft(), comp.getTop());
				}
			}
		}
		for (SbgnPDNode comp : emptiedDummyComplexMap.keySet())
		{
			LGraph chGr = emptiedDummyComplexMap.get(comp);

			adjustLocation(comp, chGr);
		}

		removeDummyComplexes();
	}

	/**
	 * Adjust locations of children of given complex wrt. the location of the
	 * complex
	 */
	private void adjustLocation(SbgnPDNode comp, LGraph chGr)
	{
		RectangleD rect = calculateBounds(false,
				(ArrayList<SbgnPDNode>) chGr.getNodes());

		int differenceX = (int) (rect.x - comp.getLeft());
		int differenceY = (int) (rect.y - comp.getTop());

		// if the parent graph is a compound, add compound margins
		if (!comp.type.equals(SbgnPDConstants.COMPLEX))
		{
			differenceX -= LayoutConstants.COMPOUND_NODE_MARGIN;
			differenceY -= LayoutConstants.COMPOUND_NODE_MARGIN;
		}

		for (int j = 0; j < chGr.getNodes().size(); j++)
		{
			SbgnPDNode s = (SbgnPDNode) chGr.getNodes().get(j);

			s.setLocation(s.getLeft() - differenceX
					+ SbgnPDConstants.COMPLEX_MEM_HORIZONTAL_BUFFER, s.getTop()
					- differenceY + SbgnPDConstants.COMPLEX_MEM_VERTICAL_BUFFER);

			if (s.getChild() != null)
				adjustLocation(s, s.getChild());
		}
	}

	/**
	 * Recursively removes all dummy complex nodes (previously created to tile
	 * group degree-zero nodes) from the graph.
	 */
	private void clearDummyComplexGraphs(SbgnPDNode comp)
	{
		if (comp.getChild() == null || comp.isDummyCompound)
		{
			return;
		}
		for (Object o : comp.getChild().getNodes())
		{
			SbgnPDNode childNode = (SbgnPDNode) o;
			if (childNode.getChild() != null
					&& childNode.getEdges().size() == 0)
				clearDummyComplexGraphs(childNode);
		}
		if (graphManager.getGraphs().contains(comp.getChild()))
		{
			if (calcGraphDegree(comp) == 0)
			{
				emptiedDummyComplexMap.put(comp, comp.getChild());

				getGraphManager().getGraphs().remove(comp.getChild());
				comp.setChild(null);
			}
		}
	}

	/**
	 * Dummy complexes (placed in the "dummyComplexList") are removed from the
	 * graph.
	 */
	private void removeDummyComplexes()
	{
		// remove dummy complexes and connect children to original parent
		for (SbgnPDNode dummyComplex : dummyComplexList)
		{
			LGraph childGraph = dummyComplex.getChild();
			LGraph owner = dummyComplex.getOwner();

			getGraphManager().getGraphs().remove(childGraph);
			dummyComplex.setChild(null);

			owner.remove(dummyComplex);

			for (Object s : childGraph.getNodes())
				owner.add((SbgnPDNode) s);
		}
	}

	/**
	 * This method returns the bounding rectangle of the given set of nodes with
	 * or without the margins
	 */
	protected RectangleD calculateBounds(boolean isMarginIncluded,
			ArrayList<SbgnPDNode> nodes)
	{
		int boundLeft = Integer.MAX_VALUE;
		int boundRight = Integer.MIN_VALUE;
		int boundTop = Integer.MAX_VALUE;
		int boundBottom = Integer.MIN_VALUE;
		int nodeLeft;
		int nodeRight;
		int nodeTop;
		int nodeBottom;

		Iterator<SbgnPDNode> itr = nodes.iterator();

		while (itr.hasNext())
		{
			LNode lNode = itr.next();
			nodeLeft = (int) (lNode.getLeft());
			nodeRight = (int) (lNode.getRight());
			nodeTop = (int) (lNode.getTop());
			nodeBottom = (int) (lNode.getBottom());

			if (boundLeft > nodeLeft)
				boundLeft = nodeLeft;

			if (boundRight < nodeRight)
				boundRight = nodeRight;

			if (boundTop > nodeTop)
				boundTop = nodeTop;

			if (boundBottom < nodeBottom)
				boundBottom = nodeBottom;
		}

		if (isMarginIncluded)
		{
			return new RectangleD(boundLeft
					- SbgnPDConstants.COMPLEX_MEM_MARGIN, boundTop
					- SbgnPDConstants.COMPLEX_MEM_MARGIN, boundRight
					- boundLeft + 2 * SbgnPDConstants.COMPLEX_MEM_MARGIN,
					boundBottom - boundTop + 2
							* SbgnPDConstants.COMPLEX_MEM_MARGIN);
		}
		else
		{
			return new RectangleD(boundLeft, boundTop, boundRight - boundLeft,
					boundBottom - boundTop);
		}
	}

	/**
	 * calculates usedArea/totalArea inside the complexes and prints them out.
	 */
	protected void calculateFullnessOfComplexes()
	{
		SbgnPDNode largestComplex = null;
		double totalArea = 0;
		double usedArea = 0;
		double maxArea = Double.MIN_VALUE;

		// find the largest complex -> area
		for (int i = 0; i < getAllNodes().length; i++)
		{
			SbgnPDNode s = (SbgnPDNode) getAllNodes()[i];
			if (s.type.equals(SbgnPDConstants.COMPLEX)
					&& s.getWidth() * s.getHeight() > maxArea)
			{
				maxArea = s.getWidth() * s.getHeight();
				largestComplex = s;
			}
		}

		usedArea = calculateUsedArea(largestComplex);

		totalArea = largestComplex.getWidth() * largestComplex.getHeight();

		if (compactionMethod == DefaultCompactionAlgorithm.TILING)
			log.info("Tiling results");
		else if (compactionMethod == DefaultCompactionAlgorithm.POLYOMINO_PACKING)
			log.info("Polyomino Packing results");

		log.info(" = " + usedArea / totalArea);
	}

	/**
	 * This method calculates the used area of a given complex node's children
	 */
	protected double calculateUsedArea(SbgnPDNode parent)
	{
		int totalArea = 0;
		if (parent.getChild() == null)
			return 0.0;

		for (int i = 0; i < parent.getChild().getNodes().size(); i++)
		{
			SbgnPDNode node = (SbgnPDNode) parent.getChild().getNodes().get(i);

			if (!node.type.equalsIgnoreCase(SbgnPDConstants.COMPLEX))
			{
				totalArea += node.getWidth() * node.getHeight();
			}
			else
			{
				totalArea += calculateUsedArea(node);
			}
		}
		return totalArea;
	}

	public enum DefaultCompactionAlgorithm
	{
		TILING, POLYOMINO_PACKING
	};

	// ********************* SECTION : OVERRIDEN METHODS *********************

	/**
	 * This method creates a new node associated with the input view node.
	 */
	public LNode newNode(Object vNode)
	{
		return new SbgnPDNode(graphManager, vNode);
	}

	/**
	 * This method creates a new edge associated with the input view edge.
	 */
	public LEdge newEdge(Object vEdge)
	{
		return new SbgnPDEdge(null, null, vEdge);
	}

	/**
	 * This method performs layout on constructed l-level graph.
	 * It returns true on success, false otherwise.
	 */
	public boolean layout()
	{
		groupZeroDegreeMembers();
		applyDFSOnComplexes();

		//run CoSE layout
		boolean b = super.layout();

		repopulateComplexes();

		getAllNodes();

		return b;
	}

	/**
	 * This method uses classic layout method (without multi-scaling)
	 * @return
	 */
	protected boolean classicLayout()
	{
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

		if (!arePortNodesCreated()) {
			createPortNodes();
		}

		calculateNodesToApplyGravitationTo();

		initSpringEmbedder();
		runSpringEmbedder();

		//reset or it looks no good
		getGraphManager().resetAllNodes();
		getGraphManager().resetAllNodesToApplyGravitation();
		getGraphManager().resetAllEdges();

		log.info("Classic CoSE layout finished after " + totalIterations + " iterations");

		return true;
	}

	@Override
	/**
	 * This method calculates the spring forces for the ends of each node.
	 * Modification: do not calculate spring force for rigid edges
	 */
	public void calcSpringForces()
	{
		Object[] lEdges = getAllEdges();
		FDLayoutEdge edge;

		for (int i = 0; i < lEdges.length; i++)
		{
			edge = (FDLayoutEdge) lEdges[i];

			if (!SbgnPDConstants.RIGID_EDGE.equals(edge.type))
				calcSpringForce(edge, edge.idealLength);
		}
	}

	@Override
	/**	 
	 * This method calculates the repulsion forces for each pair of nodes.
	 * Modification: Do not calculate repulsion for port & process nodes
	 */
	public void calcRepulsionForces()
	{
		int i, j;
		FDLayoutNode nodeA, nodeB;
		Object[] lNodes = getAllNodes();
		HashSet<FDLayoutNode> processedNodeSet;

		if (useFRGridVariant)
		{
			// grid is a vector matrix that holds CoSENodes.
			// be sure to convert the Object type to CoSENode.

			if (totalIterations
					% FDLayoutConstants.GRID_CALCULATION_CHECK_PERIOD == 1)
			{
				grid = calcGrid(graphManager.getRoot());

				// put all nodes to proper grid cells
				for (i = 0; i < lNodes.length; i++)
				{
					nodeA = (FDLayoutNode) lNodes[i];
					addNodeToGrid(nodeA, grid, graphManager
							.getRoot().getLeft(), graphManager.getRoot()
							.getTop());
				}
			}

			processedNodeSet = new HashSet<FDLayoutNode>();

			// calculate repulsion forces between each nodes and its surrounding
			for (i = 0; i < lNodes.length; i++)
			{
				nodeA = (FDLayoutNode) lNodes[i];
				calculateRepulsionForceOfANode(grid, nodeA,
						processedNodeSet);
				processedNodeSet.add(nodeA);
			}
		}
		else
		{
			for (i = 0; i < lNodes.length; i++)
			{
				nodeA = (FDLayoutNode) lNodes[i];

				for (j = i + 1; j < lNodes.length; j++)
				{
					nodeB = (FDLayoutNode) lNodes[j];

					// If both nodes are not members of the same graph, skip.
					if (nodeA.getOwner() != nodeB.getOwner())
					{
						continue;
					}

					if (nodeA.type != null
							&& nodeB.type != null
							&& nodeA.getOwner().equals(nodeB.getOwner())
							&& (nodeA.type.equals(SbgnPDConstants.INPUT_PORT)
									|| nodeA.type
											.equals(SbgnPDConstants.OUTPUT_PORT)
									|| nodeB.type
											.equals(SbgnPDConstants.INPUT_PORT) || nodeB.type
										.equals(SbgnPDConstants.OUTPUT_PORT)))
					{
						continue;
					}

					calcRepulsionForce(nodeA, nodeB);
				}
			}
		}
	}

	@Override
	/**
	 * This method finds surrounding nodes of nodeA in repulsion range.
	 * And calculates the repulsion forces between nodeA and its surrounding.
	 * During the calculation, ignores the nodes that have already been processed.
	 * Modification: Do not calculate repulsion for port & process nodes
	 */
	protected void calculateRepulsionForceOfANode(Vector[][] grid,
			FDLayoutNode nodeA, HashSet<FDLayoutNode> processedNodeSet)
	{
		int i, j;

		if (totalIterations % FDLayoutConstants.GRID_CALCULATION_CHECK_PERIOD == 1)
		{
			HashSet<Object> surrounding = new HashSet<Object>();
			FDLayoutNode nodeB;

			for (i = (nodeA.startX - 1); i < (nodeA.finishX + 2); i++)
			{
				for (j = (nodeA.startY - 1); j < (nodeA.finishY + 2); j++)
				{
					if (!((i < 0) || (j < 0) || (i >= grid.length) || (j >= grid[0].length)))
					{
						for (Object obj : grid[i][j])
						{
							nodeB = (FDLayoutNode) obj;

							// If both nodes are not members of the same graph,
							// or both nodes are the same, skip.
							if ((nodeA.getOwner() != nodeB.getOwner())
									|| (nodeA == nodeB))
							{
								continue;
							}

							if (nodeA.type != null
									&& nodeB.type != null
									&& nodeA.getOwner()
											.equals(nodeB.getOwner())
									&& (nodeA.type
											.equals(SbgnPDConstants.INPUT_PORT)
											|| nodeA.type
													.equals(SbgnPDConstants.OUTPUT_PORT)
											|| nodeB.type
													.equals(SbgnPDConstants.INPUT_PORT) || nodeB.type
												.equals(SbgnPDConstants.OUTPUT_PORT)))
							{
								continue;
							}

							// check if the repulsion force between
							// nodeA and nodeB has already been calculated
							if (!processedNodeSet.contains(nodeB)
									&& !surrounding.contains(nodeB))
							{
								double distanceX = Math.abs(nodeA.getCenterX()
										- nodeB.getCenterX())
										- ((nodeA.getWidth() / 2) + (nodeB.getWidth() / 2));
								double distanceY = Math.abs(nodeA.getCenterY()
										- nodeB.getCenterY())
										- ((nodeA.getHeight() / 2) + (nodeB.getHeight() / 2));

								// if the distance between nodeA and nodeB
								// is less then calculation range
								if ((distanceX <= repulsionRange)
										&& (distanceY <= repulsionRange))
								{
									// then add nodeB to surrounding of nodeA
									surrounding.add(nodeB);
								}
							}

						}
					}
				}
			}
			nodeA.surrounding = surrounding.toArray();
		}

		for (i = 0; i < nodeA.surrounding.length; i++) {
			calcRepulsionForce(nodeA, (FDLayoutNode) nodeA.surrounding[i]);
		}
	}

	/**
	 * This method creates a port node with the associated type (input/output
	 * port)
	 */
	public LNode newPortNode(Object vNode, String type)
	{
		SbgnPDNode n = new SbgnPDNode(graphManager, vNode);
		n.type = type;
		n.setWidth(SbgnPDConstants.PORT_NODE_DEFAULT_WIDTH);
		n.setHeight(SbgnPDConstants.PORT_NODE_DEFAULT_HEIGHT);

		return n;
	}

	/**
	 * This method creates an SBGNProcessNode object
	 */
	public LNode newProcessNode(Object vNode)
	{
		return new SbgnProcessNode(graphManager, vNode);
	}

	/**
	 * This method creates a rigid edge.
	 */
	public LEdge newRigidEdge(Object vEdge)
	{
		SbgnPDEdge e = new SbgnPDEdge(null, null, vEdge);
		e.type = SbgnPDConstants.RIGID_EDGE;
		return e;
	}

}
