package fr.inria.corese.core.next.query.impl.engine.path;

import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Synchronized buffer to put/get path edges
 * Edges are consumed by an iterator
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 *
 */
// A producer/consumer stream is consumed once; replay would require unbounded buffering.
@SuppressWarnings("java:S4348")
public class PathMappingBuffer implements Iterable<Mapping>, Iterator<Mapping> {

	private Mapping map;
	private boolean hasNext = true;
	private boolean available = false;

	public synchronized Mapping next() {
        if (!hasNext()) {
            throw new NoSuchElementException("Path enumeration has ended");
        }
		available = false;
		notifyAll();
		return map;
	}

	public synchronized boolean hasNext() {
		while (!available) {
			try {
				wait();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
		notifyAll();
		return hasNext;
	}

	public synchronized void put(Mapping val, boolean next) {
		while (available) {
			try {
				wait();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
		map = val;
		hasNext = next;
		available = true;
		notifyAll();
	}

	@Override
	@SuppressWarnings("NullableProblems")
	public Iterator<Mapping> iterator() {
		return this;
	}

}
