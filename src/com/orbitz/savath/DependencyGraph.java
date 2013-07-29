package com.orbitz.savath;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.GraphPathImpl;
import org.jgrapht.graph.Pseudograph;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import dk.aaue.sna.alg.AllPaths;

public class DependencyGraph implements Serializable {
	private static Graph<String, DefaultEdge> g;

	public DependencyGraph() {
		setG(new Pseudograph<String, DefaultEdge>(DefaultEdge.class));
		getG().addVertex("Root");
	}

	public static void main(String args[]) throws Exception {

		DependencyGraph d = new DependencyGraph();
		String repDir = "/opt/orbitz/code/.savant_repository";

		if (args.length > 0) {
			repDir = args[0];
		}
		System.out.println("Base repository :" + repDir);
		System.out.print("\nPlease wait... Generating the dependency graph based on " + repDir);

		d.process(new File(repDir));

		System.err.println("\nSyntax:path <depedency1> <dependency2>");
		System.err.println("For example:path sun/j2ee/j2ee-1.4.jar orbitz/slapp-host-itsb/orbitz-host-itsb-34.139.jar");
		boolean isDone = false;
		while (!isDone) {
			try {
				System.out.print("\n>");
				Scanner sc = new Scanner(System.in);
				String cmd = sc.next();
				if ("q".equalsIgnoreCase(cmd)) {
					System.exit(0);
				} else if ("path".equalsIgnoreCase(cmd)) {
					String source = sc.next();
					String destination = sc.next();
					List<DefaultEdge> r = null;
					try {
						r = DijkstraShortestPath.findPathBetween(g, source, destination);
					} catch (Exception e) {
						System.out.println(e.getMessage());
					}
					if (r != null && !r.isEmpty()) {
						Map<String, String> map = new HashMap<String, String>();
						for (DefaultEdge e : r) {
							map.put(e.getSource().toString(), e.getTarget().toString());
							map.put(e.getTarget().toString(), e.getSource().toString());
						}
						StringBuilder buf = new StringBuilder();
						String key = source;
						buf.append(source);
						do {
							buf.append(" --> ");
							key = map.get(key);
							buf.append(key);
						} while (!key.equals(destination));
						System.err.println("\nShortest Path:"
								+ buf.toString().replaceAll("Root", "").replaceAll("-->  -->", "-->"));
					} else {
						System.err.println("No dependency found.");
					}
				} else if ("fullpath".equalsIgnoreCase(cmd)) {
					String source = sc.next();
					String destination = sc.next();
				} else {
					System.err.println("No such command! Type q to exit.");
				}
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}
	}

	public List<DefaultEdge> shortestPath(String source, String destination) {
		List<DefaultEdge> r = DijkstraShortestPath.findPathBetween(getG(), source, destination);
		return r;
	}

	public void process(File node) throws Exception {
		if (node.isDirectory()) {
			String[] subNote = node.list();
			for (String filename : subNote) {
				String v = node.getAbsolutePath() + "/" + filename.replaceAll(".deps", "");
				v = v.replaceAll("/opt/orbitz/code/.savant_repository/", "");
				if (filename.endsWith("deps")) {
					NodeList nodeList = parseDepsFile(node, filename);
					for (int i = 0; i < nodeList.getLength(); i++) {
						DepEntry entry = getDepEntry(nodeList.item(i).getAttributes());
						if (!getG().containsVertex(v)) {
							getG().addVertex(v);
						}
						String targetVertext = entry.getNode();
						if (!getG().containsVertex(targetVertext)) {
							getG().addVertex(targetVertext);
						}

						DefaultEdge t = getG().addEdge(v, targetVertext);
					}

					if (!getG().containsVertex(v)) {
						getG().addVertex(v);
						getG().addEdge("Root", v);
					}
				} else {
					process(new File(node, filename));
				}
				process(new File(node, filename));
			}
		}
	}

	private NodeList parseDepsFile(File node, String filename) throws ParserConfigurationException, SAXException,
			IOException, FileNotFoundException, XPathExpressionException {
		String f = node.getAbsolutePath() + "/" + filename;
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		builder = builderFactory.newDocumentBuilder();
		Document document = builder.parse(new FileInputStream(f));
		XPath xPath = XPathFactory.newInstance().newXPath();
		String expression = "/dependencies/artifactgroup/artifact";

		// read a string value
		NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(document, XPathConstants.NODESET);
		return nodeList;
	}

	private DepEntry getDepEntry(NamedNodeMap attributes) {
		String name = attributes.getNamedItem("name").getNodeValue();
		String group = attributes.getNamedItem("group").getNodeValue();
		Node project = attributes.getNamedItem("project");
		if (project == null) {
			project = attributes.getNamedItem("projectname");
		}
		String projectStr = project.getNodeValue();
		String version = attributes.getNamedItem("version").getNodeValue();
		return new DepEntry(name, group, projectStr, version);
	}

	public static Graph<String, DefaultEdge> getG() {
		return g;
	}

	public static void setG(Graph<String, DefaultEdge> g) {
		DependencyGraph.g = g;
	}

}