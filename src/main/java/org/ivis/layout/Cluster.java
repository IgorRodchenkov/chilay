package org.ivis.layout;

import java.util.*;

import org.ivis.util.PointD;

/**
 * This class represents a cluster for layout purpose. A cluster maintains
 * a list of nodes, which belong to the cluster. Every cluster has its own name 
 * and unique ID.
 *
 * @author Shatlyk Ashyralyyev
 *
 * Copyright: i-Vis Research Group, Bilkent University, 2007 - present
 */
public class Cluster implements Comparable
{
// -----------------------------------------------------------------------------
// Section: Instance variables
// -----------------------------------------------------------------------------
	/*
	 * List of clustered objects that belong to the cluster
	 */
	protected Set<Clustered> nodes;
	
	/*
	 * Owner cluster manager.
	 */
	protected ClusterManager clusterManager;
	
	/*
	 * Unique ID of the cluster 
	 */
	protected int clusterID;
	
	/*
	 * Name of the cluster
	 */
	protected String clusterName;
	
	/*
	 * Polygon that covers all nodes of the cluster
	 */
	protected List<PointD> polygon;

// -----------------------------------------------------------------------------
// Section: Constructors
// -----------------------------------------------------------------------------
	/**
	 * Constructor for creating a Cluster from cluster name for a given 
	 * ClusterManager. Cluster ID is generated from ClusterManager.idCounter.
	 *
	 * @param clusterManager cluster manager
	 * @param clusterName name
	 */
	public Cluster(ClusterManager clusterManager, String clusterName)
	{
		this.nodes = new HashSet<Clustered>();
		this.polygon = new ArrayList<PointD>();
		
		// set the cluster manager
		this.clusterManager = clusterManager;
		this.clusterName = clusterName;
		
		// find a free clusterID
		if (this.clusterManager != null)
		{
			while (!this.clusterManager.isClusterIDUsed(ClusterManager.idCounter))
			{
				ClusterManager.idCounter++;
			}
		}
		this.clusterID = ClusterManager.idCounter;	
		
		// each cluster has its own cluster ID, counter is incremented by 1
		ClusterManager.idCounter++;
	}
	
	/**
	 * Constructor for creating a Cluster when clusterID is specified by user.
	 *
	 * @param clusterManager cluster manager
	 * @param clusterID id
	 * @param clusterName name
	 */
	public Cluster(ClusterManager clusterManager, int clusterID, String clusterName)
	{
		this.nodes = new HashSet<Clustered>();
		this.polygon = new ArrayList<PointD>();

		// set the cluster manager
		this.clusterManager = clusterManager;
		
		// check if clusterID is used before
		if (this.clusterManager != null)
		{
			// if cluster ID is used before, set the cluster id automatically
			if (this.clusterManager.isClusterIDUsed(clusterID))
			{
				// print error message
				System.err.println("Cluster ID " + clusterID + " is used" + 
						" before. ClusterID is set automatically.");
				
				// find first free clusterID that can be used
				while (this.clusterManager.isClusterIDUsed(ClusterManager.idCounter))
				{
					ClusterManager.idCounter++;
				}
				// set cluster ID automatically
				clusterID = ClusterManager.idCounter;

				// each cluster has its own cluster ID, counter is incremented by 1
				ClusterManager.idCounter++;
			}
		}
		
		// set cluster name and id
		this.clusterName = clusterName;
		this.clusterID = clusterID;
	}
	
	
// -----------------------------------------------------------------------------
// Section: Accessors
// -----------------------------------------------------------------------------
	/**
	 * This method returns a set of nodes that belong to this cluster.
	 * @return nodes
	 */
	public Set<Clustered> getNodes()
	{
		return nodes;
	}

	/**
	 * This method returns the ID of this cluster.
	 * @return id
	 */
	public int getClusterID()
	{
		return clusterID;
	}

	/**
	 * This method sets the cluster manager of this cluster
	 * @param clusterManager cluster manager
	 */
	public void setClusterManager(ClusterManager clusterManager)
	{
		this.clusterManager = clusterManager;
	}
	
	/**
	 * This method returns the name of this cluster.
	 * @return name
	 */
	public String getClusterName()
	{
		return clusterName;
	}

	/**
	 * This method sets the name of this cluster.
	 * @param clusterName name
	 */
	public void setClusterName(String clusterName)
	{
		this.clusterName = clusterName;
	}
	
	/**
	 * This method returns the polygon
	 * @return polygon (the list of points)
	 */
	public List<PointD> getPolygon()
	{
		return polygon;
	}
	
	/**
	 * This method sets the polygon
	 * @param points polygon - defined as the list of points
	 */
	public void setPolygon(List<PointD> points)
	{
		this.polygon = points;
	}

// -----------------------------------------------------------------------------
// Section: Remaining methods
// -----------------------------------------------------------------------------
	/**
	 * This method adds the given clustered node into this cluster.
	 * @param node node to add
	*/	
	public void addNode(Clustered node)
	{		
		node.addCluster(this);
	}
	
	/**
	 * This method removes the given clustered node from this cluster.
	 * @param node node to remove
	 */
	public void removeNode(Clustered node)
	{
		node.removeCluster(this);
	}
	
	/**
	 * This method deletes the cluster information from the graph.
	 */
	public void delete()
	{	
		// get copy of nodes in order to prevent pointer problems
		ArrayList<Clustered> copy = new ArrayList<Clustered>();
		copy.addAll(nodes);
		
		for (Clustered node : copy)
		{
			node.removeCluster(this);
		}

		// delete this cluster form cluster managers cluster list
		clusterManager.getClusters().remove(this);
	}
	
	/**
	 * This method calculates the convex polygon bounding all nodes of 
	 * this cluster.
	 */
	public void calculatePolygon()
	{
		if (clusterID == 0)
		{
			return;
		}
		calculateConvexHull();
	}
	
	/**
	 * This method collects all boundary points of all nodes.
	 */
	private void findPoints()
	{
		polygon.clear();

		if (nodes.isEmpty())
		{
			return;
		}
		
		Iterator<Clustered> nodeItr = nodes.iterator();
		Clustered node;
		
		while (nodeItr.hasNext())
		{
			node = nodeItr.next();
			
			double left = node.getLeft();
			double right = node.getRight();
			double top = node.getTop();
			double bottom = node.getBottom();
			
			Clustered parent = node.getParent();
			
			//calculate absolute position
			while ( parent != null )
			{
				left += parent.getLeft();
				right += parent.getLeft();
				
				top += parent.getTop();
				bottom += parent.getTop();
				
				parent = parent.getParent();
			}

			polygon.add(new PointD(left, top));
			polygon.add(new PointD(right, top));
			polygon.add(new PointD(right, bottom));
			polygon.add(new PointD(left, bottom));
		}
	}
	/**
	 * This method computes the convex hull of given points in O(N*logN) time.
	 * Very similar algorithm to Graham Scan is implemented.
	 */
	private void calculateConvexHull()
	{
		// find points
		findPoints();
		
		if (polygon.isEmpty())
		{
			return;
		}
		
		// sort points in increasing order of x coordinates, in case of tie
		// point with higher y coordinate comes first
		Collections.sort(polygon, new PointComparator());

		Stack<PointD> upperHull = new Stack<PointD>();
		Stack<PointD> lowerHull = new Stack<PointD>();
		
		int n = polygon.size();
		if ( n < 3 )
		{
			// no polygon
			return;
		}
		// push first 2 points
		upperHull.push(polygon.get(0));
		upperHull.push(polygon.get(1));
		
		// calculate upper hull
		for (int i = 2; i < polygon.size(); i++)
		{
			PointD pt3 = polygon.get(i);
			
			while (true) 
			{
				PointD pt2 = upperHull.pop();
				// 2 points should be pushed back
				if (upperHull.empty())
				{
					upperHull.push(pt2);
					upperHull.push(pt3);
					break;
				}
				
				PointD pt1 = upperHull.peek();
				
				if (rightTurn(pt1, pt2, pt3))
				{
					upperHull.push(pt2);
					upperHull.push(pt3);
					break;
				}	
			}
		}

		lowerHull.push(polygon.get(n-1));
		lowerHull.push(polygon.get(n-2));
		
		// calculate lower hull
		for (int i = n-3; i >= 0; i--)
		{
			PointD pt3 = polygon.get(i);
			
			while (true) 
			{
				PointD pt2 = lowerHull.pop();
				// 2 points should be pushed back
				if (lowerHull.empty())
				{
					lowerHull.push(pt2);
					lowerHull.push(pt3);
					break;
				}
				
				PointD pt1 = lowerHull.peek();
				
				if (rightTurn(pt1, pt2, pt3))
				{
					lowerHull.push(pt2);
					lowerHull.push(pt3);
					break;
				}	
			}
		}
		
		// construct convex hull
		polygon.clear();
		n = lowerHull.size();
		for (int i=0; i < n; i++)
		{
			polygon.add(lowerHull.pop());
		}	

		n = upperHull.size();
		for (int i=0; i < n; i++)
		{
			polygon.add(upperHull.pop());
		}
	}

	/**
	 * This method check whether it is a right turn.
	 */
	private static boolean rightTurn(PointD pt1, PointD pt2, PointD pt3)
	{
		// first vector
		double x1 = pt2.x - pt1.x;
		double y1 = - (pt2.y - pt1.y);
		
		// second vector
		double x2 = pt3.x - pt2.x;
		double y2 = - (pt3.y - pt2.y);
		
		// decide using cross product, right hand rule is applied
		if((x1* y2 - y1 * x2) <= 0){
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Method to make 2 clusters comparable by ID.
	 * @param obj object to compare to
	 * @return see {@link String#compareTo(Object)}
	 */
	public int compareTo(Object obj)
	{
		if (obj instanceof Cluster)
		{
			Cluster cluster = (Cluster) obj;
			// compare ID's of two clusters
			return ((Integer)clusterID).compareTo(cluster.getClusterID());
		}
		return 0; //TODO: this means any non-Cluster object equals this (Cluster) one...
	}
	
	/**
	 * This is a helper class for sorting PointD objects
	 */
	private class PointComparator implements Comparator<PointD>
	{
		public int compare(PointD o1, PointD o2) {
			PointD pt1 = (PointD) o1;
			PointD pt2 = (PointD) o2;
			
			if(pt1.x < pt2.x) return -1;
			else if(pt1.x > pt2.x) return 1;
			else if(Math.abs(pt1.x-pt2.x) < 1e-9  && pt1.y > pt2.y) return -1;
			else if(Math.abs(pt1.x-pt2.x) < 1e-9 && pt1.y < pt2.y) return 1;
			
			return 0;
		}
	}
}
