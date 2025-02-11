/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.inria.corese.fr.inria.corese.kgraph.core;

import java.text.NumberFormat;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Node;

/**
 *
 * @author edemairy
 */
public class GraphStressTest {

	public GraphStressTest() {
	}

	public void graphCreation() {
		int size = 1;
		while (size < 100_000_000) {
			Graph graph = Graph.create();

			for (int nbNode = 0; nbNode < size; nbNode++) {
				Node n = graph.createNode("node_" + nbNode);
				graph.add(n);
			}
			displayMemoryState();
			size *= 2;
		}
	}

	private void displayMemoryState() {
		Runtime runtime = Runtime.getRuntime();

		NumberFormat format = NumberFormat.getInstance();

		StringBuilder sb = new StringBuilder();
		long maxMemory = runtime.maxMemory();
		long allocatedMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();

		long usedMemory = allocatedMemory - freeMemory;
		sb.append("used memory: " + format.format(usedMemory / 1024));
	}


	public static void main(String[] args) {
		GraphStressTest tester = new GraphStressTest();
		tester.graphCreation();
	}
}
