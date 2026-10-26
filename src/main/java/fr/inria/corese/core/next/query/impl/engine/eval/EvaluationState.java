package fr.inria.corese.core.next.query.impl.engine.eval;

/**
 * Encapsulates the execution state, metrics, stack depth, and cooperative cancellation
 * flag for SPARQL query evaluation in {@link Eval}.
 *
 * <p>Evaluation counters (calls, edges, results, max stack depth) are confined to the
 * single evaluation thread. The cancellation flag {@link #isStopped()} is volatile to
 * support concurrent interruption by a query timeout watchdog.</p>
 */
public final class EvaluationState {

    private int nbEdge = 0;
    private int nbCall = 0;
    private int nbResult = 0;
    private int backjump = -1;
    private int maxLevel = -1;
    private int limit = Integer.MAX_VALUE;
    private volatile boolean stopped = false;

    // --- Counters & Metrics

    public void incrementEdge() {
        nbEdge++;
    }

    public void incrementCall() {
        nbCall++;
    }

    public void incrementResult() {
        nbResult++;
    }

    public int getNbEdge() {
        return nbEdge;
    }

    public void setNbEdge(int nbEdge) {
        this.nbEdge = nbEdge;
    }

    public int getNbCall() {
        return nbCall;
    }

    public void setNbCall(int nbCall) {
        this.nbCall = nbCall;
    }

    public int getNbResult() {
        return nbResult;
    }

    public void setNbResult(int nbResult) {
        this.nbResult = nbResult;
    }

    // --- Backjumping & Depth

    public int getBackjump() {
        return backjump;
    }

    public void setBackjump(int level) {
        this.backjump = level;
    }

    public void clearBackjump() {
        this.backjump = -1;
    }

    public boolean hasBackjump() {
        return backjump != -1;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void updateMaxLevel(int level) {
        if (level > maxLevel) {
            maxLevel = level;
        }
    }

    // --- Limit

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    // --- Cooperative Interruption

    public boolean isStopped() {
        return stopped;
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    public void stop() {
        this.stopped = true;
    }

    public void reset() {
        nbEdge = 0;
        nbCall = 0;
        nbResult = 0;
        backjump = -1;
        maxLevel = -1;
        limit = Integer.MAX_VALUE;
        stopped = false;
    }
}
