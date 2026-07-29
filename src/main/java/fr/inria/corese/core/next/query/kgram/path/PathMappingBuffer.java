package fr.inria.corese.core.next.query.kgram.path;

import fr.inria.corese.core.next.query.kgram.core.Mapping;

import java.util.Iterator;

/**
 * Synchronized buffer to put/get path edges
 * Edges are consumed by an iterator
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 *
 */
public class PathMappingBuffer implements Iterable<Mapping>, Iterator<Mapping> {

	private Mapping map;
	private boolean hasNext = true;
	private boolean available = false;

	public synchronized Mapping next() {
		while (!available) {
			try {
				wait();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			}
		}
		available = false;
		notify();
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
		notify();
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
		notify();
	}

	@Override
	@SuppressWarnings("NullableProblems")
	public Iterator<Mapping> iterator() {
		return this;
	}

	@Override
	@Deprecated
	public void remove() {
		throw new UnsupportedOperationException("Remove operation is not supported");
	}
}
