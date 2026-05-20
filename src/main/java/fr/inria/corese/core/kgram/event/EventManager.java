package fr.inria.corese.core.kgram.event;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Event Manager to trace KGRAM execution
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 *
 */
public class EventManager implements Iterable<EventListener> {

    boolean isEval = false;

    List<EventListener> observers = new Vector<EventListener>();


    public void addEventListener(EventListener el) {
        observers.add(el);
        isEval = isEval || (el.handle(Event.START));

    }


    @Override
    public Iterator<EventListener> iterator() {
        return observers.iterator();
    }

    public boolean send(Event event) {
        boolean res = true;
        for (EventListener el : observers) {
            if (el.handle(event.getSort())) {
                boolean b = el.send(event);
                res = res && b;
            }
        }
        return res;
    }

}
