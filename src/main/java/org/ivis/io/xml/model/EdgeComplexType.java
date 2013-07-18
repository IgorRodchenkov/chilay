package org.ivis.io.xml.model;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for edgeComplexType complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType name="edgeComplexType">
 *   &lt;complexContent>
 *     &lt;extension base="{}graphObjectComplexType">
 *       &lt;sequence>
 *         &lt;element name="sourceNode">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="targetNode">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="bendPointList" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="bendPoint" maxOccurs="unbounded" minOccurs="0">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;attribute name="x" type="{http://www.w3.org/2001/XMLSchema}double" />
 *                           &lt;attribute name="y" type="{http://www.w3.org/2001/XMLSchema}double" />
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "edgeComplexType", propOrder =
{ "sourceNode", "targetNode", "bendPointList", "type" })
public class EdgeComplexType extends GraphObjectComplexType
{

	@XmlElement(required = true)
	protected EdgeComplexType.SourceNode sourceNode;
	@XmlElement(required = true)
	protected EdgeComplexType.TargetNode targetNode;
	protected EdgeComplexType.BendPointList bendPointList;
	protected EdgeComplexType.Type type;

	/**
	 * Gets the value of the sourceNode property.
	 * 
	 * @return possible object is {@link EdgeComplexType.SourceNode }
	 * 
	 */
	public EdgeComplexType.SourceNode getSourceNode()
	{
		return sourceNode;
	}

	public EdgeComplexType.Type getTypeInfo()
	{
		return type;
	}

	/**
	 * Sets the value of the sourceNode property.
	 * 
	 * @param value
	 *            allowed object is {@link EdgeComplexType.SourceNode }
	 * 
	 */
	public void setSourceNode(EdgeComplexType.SourceNode value)
	{
		this.sourceNode = value;
	}

	/**
	 * Gets the value of the targetNode property.
	 * 
	 * @return possible object is {@link EdgeComplexType.TargetNode }
	 * 
	 */
	public EdgeComplexType.TargetNode getTargetNode()
	{
		return targetNode;
	}

	/**
	 * Sets the value of the targetNode property.
	 * 
	 * @param value
	 *            allowed object is {@link EdgeComplexType.TargetNode }
	 * 
	 */
	public void setTargetNode(EdgeComplexType.TargetNode value)
	{
		this.targetNode = value;
	}

	/**
	 * Gets the value of the bendPointList property.
	 * 
	 * @return possible object is {@link EdgeComplexType.BendPointList }
	 * 
	 */
	public EdgeComplexType.BendPointList getBendPointList()
	{
		return bendPointList;
	}

	/**
	 * Sets the value of the bendPointList property.
	 * 
	 * @param value
	 *            allowed object is {@link EdgeComplexType.BendPointList }
	 * 
	 */
	public void setBendPointList(EdgeComplexType.BendPointList value)
	{
		this.bendPointList = value;
	}

	/**
	 * <p>
	 * Java class for anonymous complex type.
	 * 
	 * <p>
	 * The following schema fragment specifies the expected content contained
	 * within this class.
	 * 
	 * <pre>
	 * &lt;complexType>
	 *   &lt;complexContent>
	 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
	 *       &lt;sequence>
	 *         &lt;element name="bendPoint" maxOccurs="unbounded" minOccurs="0">
	 *           &lt;complexType>
	 *             &lt;complexContent>
	 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
	 *                 &lt;attribute name="x" type="{http://www.w3.org/2001/XMLSchema}double" />
	 *                 &lt;attribute name="y" type="{http://www.w3.org/2001/XMLSchema}double" />
	 *               &lt;/restriction>
	 *             &lt;/complexContent>
	 *           &lt;/complexType>
	 *         &lt;/element>
	 *       &lt;/sequence>
	 *     &lt;/restriction>
	 *   &lt;/complexContent>
	 * &lt;/complexType>
	 * </pre>
	 * 
	 * 
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder =
	{ "bendPoint" })
	public static class BendPointList
	{

		protected List<EdgeComplexType.BendPointList.BendPoint> bendPoint;

		/**
		 * Gets the value of the bendPoint property.
		 * 
		 * <p>
		 * This accessor method returns a reference to the live list, not a
		 * snapshot. Therefore any modification you make to the returned list
		 * will be present inside the JAXB object. This is why there is not a
		 * <CODE>set</CODE> method for the bendPoint property.
		 * 
		 * <p>
		 * For example, to add a new item, do as follows:
		 * 
		 * <pre>
		 * getBendPoint().add(newItem);
		 * </pre>
		 * 
		 * 
		 * <p>
		 * Objects of the following type(s) are allowed in the list
		 * {@link EdgeComplexType.BendPointList.BendPoint }
		 * 
		 * 
		 */
		public List<EdgeComplexType.BendPointList.BendPoint> getBendPoint()
		{
			if (bendPoint == null)
			{
				bendPoint = new ArrayList<EdgeComplexType.BendPointList.BendPoint>();
			}
			return this.bendPoint;
		}

		/**
		 * <p>
		 * Java class for anonymous complex type.
		 * 
		 * <p>
		 * The following schema fragment specifies the expected content
		 * contained within this class.
		 * 
		 * <pre>
		 * &lt;complexType>
		 *   &lt;complexContent>
		 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
		 *       &lt;attribute name="x" type="{http://www.w3.org/2001/XMLSchema}double" />
		 *       &lt;attribute name="y" type="{http://www.w3.org/2001/XMLSchema}double" />
		 *     &lt;/restriction>
		 *   &lt;/complexContent>
		 * &lt;/complexType>
		 * </pre>
		 * 
		 * 
		 */
		@XmlAccessorType(XmlAccessType.FIELD)
		@XmlType(name = "")
		public static class BendPoint
		{

			@XmlAttribute
			protected Double x;
			@XmlAttribute
			protected Double y;

			/**
			 * Gets the value of the x property.
			 * 
			 * @return possible object is {@link Double }
			 * 
			 */
			public Double getX()
			{
				return x;
			}

			/**
			 * Sets the value of the x property.
			 * 
			 * @param value
			 *            allowed object is {@link Double }
			 * 
			 */
			public void setX(Double value)
			{
				this.x = value;
			}

			/**
			 * Gets the value of the y property.
			 * 
			 * @return possible object is {@link Double }
			 * 
			 */
			public Double getY()
			{
				return y;
			}

			/**
			 * Sets the value of the y property.
			 * 
			 * @param value
			 *            allowed object is {@link Double }
			 * 
			 */
			public void setY(Double value)
			{
				this.y = value;
			}

		}

	}

	/**
	 * <p>
	 * Java class for anonymous complex type.
	 * 
	 * <p>
	 * The following schema fragment specifies the expected content contained
	 * within this class.
	 * 
	 * <pre>
	 * &lt;complexType>
	 *   &lt;complexContent>
	 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
	 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
	 *     &lt;/restriction>
	 *   &lt;/complexContent>
	 * &lt;/complexType>
	 * </pre>
	 * 
	 * 
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "")
	public static class SourceNode
	{

		@XmlAttribute
		protected String id;

		/**
		 * Gets the value of the id property.
		 * 
		 * @return possible object is {@link String }
		 * 
		 */
		public String getId()
		{
			return id;
		}

		/**
		 * Sets the value of the id property.
		 * 
		 * @param value
		 *            allowed object is {@link String }
		 * 
		 */
		public void setId(String value)
		{
			this.id = value;
		}

	}

	/**
	 * <p>
	 * Java class for anonymous complex type.
	 * 
	 * <p>
	 * The following schema fragment specifies the expected content contained
	 * within this class.
	 * 
	 * <pre>
	 * &lt;complexType>
	 *   &lt;complexContent>
	 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
	 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
	 *     &lt;/restriction>
	 *   &lt;/complexContent>
	 * &lt;/complexType>
	 * </pre>
	 * 
	 * 
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "")
	public static class TargetNode
	{

		@XmlAttribute
		protected String id;

		/**
		 * Gets the value of the id property.
		 * 
		 * @return possible object is {@link String }
		 * 
		 */
		public String getId()
		{
			return id;
		}

		/**
		 * Sets the value of the id property.
		 * 
		 * @param value
		 *            allowed object is {@link String }
		 * 
		 */
		public void setId(String value)
		{
			this.id = value;
		}

	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "")
	public static class Type
	{

		@XmlAttribute
		protected String value;

		/**
		 * Gets the value of the id property.
		 * 
		 * @return possible object is {@link String }
		 * 
		 */
		public String getValue()
		{
			return value;
		}

		/**
		 * Sets the value of the id property.
		 * 
		 * @param value
		 *            allowed object is {@link String }
		 * 
		 */
		public void setValue(String value)
		{
			this.value = value;
		}

	}
}