package fr.inria.corese.core.next.query.kgram.event;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Event Manager to trace KGRAM execution
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 *
 */
public class EventManager implements Iterable<EventListener> {

    boolean isEval = false;

    List<EventListener> observers = new ArrayList<>();

    public static EventManager create() {
        return new EventManager();
    }



    public void setObject(Object obj) {
        for (EventListener el : observers) {
            el.setObject(obj);
        }
    }

    public boolean handle(int sort) {
        if (sort == Event.START) {
            return isEval;
        }
        return true;
    }

    @Override
    @SuppressWarnings("NullableProblems")
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
