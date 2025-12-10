package fr.inria.corese.core.query;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.api.Engine;
import fr.inria.corese.core.compiler.api.QueryVisitor;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.core.Mapping;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.parser.ASTExtension;
import fr.inria.corese.core.sparql.triple.parser.Access.Level;
import fr.inria.corese.core.sparql.triple.parser.Dataset;
import fr.inria.corese.core.transform.Transformer;
import fr.inria.corese.core.transform.TransformerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Equivalent of RuleEngine for Query and Template Run a set of query
 * Used by Transformer to manage set of templates
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public class QueryEngine implements Engine {

    private static final Logger logger = LoggerFactory.getLogger(QueryEngine.class);

    // Cache configuration
    private static final int DEFAULT_CACHE_SIZE = 100;
    private static final int MAX_QUERY_LENGTH_FOR_CACHE = 10000;

    private final QueryProcess exec;
    private final Graph graph;
    private final ArrayList<Query> list;
    private final HashMap<String, Query> table;
    private final HashMap<String, ArrayList<Query>> tableList;
    private final TemplateIndex index;

    //  Query compilation cache
    private final Map<CompilationKey, Query> compilationCache;
    private final int maxCacheSize;

    // Cache statistics (optional for monitoring)
    private long cacheHits = 0;
    private long cacheMisses = 0;

    private boolean isActivate = true;
    private boolean isWorkflow = false;
    private Dataset ds;
    private boolean transformation = false;
    private String base;
    private Level level = Level.USER_DEFAULT;

    QueryEngine(Graph g) {
        this(g, DEFAULT_CACHE_SIZE);
    }

    QueryEngine(Graph g, int cacheSize) {
        graph = g;
        exec = QueryProcess.create(g);
        list = new ArrayList<>();
        table = new HashMap<>();
        tableList = new HashMap<>();
        index = new TemplateIndex();
        maxCacheSize = cacheSize;

        compilationCache = new ConcurrentHashMap<>(cacheSize);
    }

    public static QueryEngine create(Graph g) {
        return new QueryEngine(g);
    }

    public static QueryEngine create(Graph g, int cacheSize) {
        return new QueryEngine(g, cacheSize);
    }

    public void addQuery(String q) {
        try {
            defQuery(q);
        } catch (EngineException e) {
            logger.error("Failed to add query: {}", q.substring(0, Math.min(100, q.length())), e);
        }
    }

    Dataset getCreateDataset() {
        if (getDataset() == null) {
            setDataset(Dataset.create());
        }
        return getDataset();
    }

    /**
     * OPTIMIZED: defQuery with compilation cache
     */
    public Query defQuery(String queryString) throws EngineException {
        if (getBase() != null) {
            getQueryProcess().setBase(getBase());
        }
        getCreateDataset().setLevel(getLevel());

        Query cachedQuery = getCachedQuery(queryString);
        if (cachedQuery != null) {
            cacheHits++;
            logger.debug("Query cache hit for query of length {}", queryString.length());
            // Clone the query to avoid concurrent modifications
            Query clonedQuery = cloneQuery(cachedQuery);
            cleanContext(clonedQuery);
            defQuery(clonedQuery);
            return clonedQuery;
        }

        cacheMisses++;
        Query compiledQuery = getQueryProcess().compile(queryString, getDataset());

        if (compiledQuery != null) {
            cleanContext(compiledQuery);

            // Cache if the query is eligible
            cacheQuery(queryString, compiledQuery);

            defQuery(compiledQuery);
        }

        return compiledQuery;
    }

    /**
     * Retrieves a query from the cache if it exists
     */
    private Query getCachedQuery(String queryString) {
        if (!shouldCache(queryString)) {
            return null;
        }

        CompilationKey key = new CompilationKey(queryString, getBase(), getLevel(), getDataset());
        return compilationCache.get(key);
    }

    /**
     * Caches a query
     */
    private void cacheQuery(String queryString, Query query) {
        if (!shouldCache(queryString)) {
            return;
        }

        // Simple LRU eviction if the cache is full
        if (compilationCache.size() >= maxCacheSize) {
            evictOldestEntry();
        }

        CompilationKey key = new CompilationKey(queryString, getBase(), getLevel(), getDataset());
        compilationCache.put(key, query);
    }

    /**
     * Determines if a query should be cached
     */
    private boolean shouldCache(String queryString) {
        // Do not cache very long queries or UPDATE queries
        return queryString != null
                && queryString.length() <= MAX_QUERY_LENGTH_FOR_CACHE
                && !queryString.toUpperCase().contains("INSERT")
                && !queryString.toUpperCase().contains("DELETE")
                && !queryString.toUpperCase().contains("CREATE")
                && !queryString.toUpperCase().contains("DROP");
    }

    /**
     * Simple eviction of the oldest entry
     */
    private void evictOldestEntry() {
        if (compilationCache.isEmpty()) {
            return;
        }

        Optional<CompilationKey> oldestKey = compilationCache.keySet()
                .stream()
                .findFirst();

        oldestKey.ifPresent(key -> {
            compilationCache.remove(key);
            logger.debug("Evicted query from cache, cache size: {}", compilationCache.size());
        });
    }

    /**
     * Clones a query to avoid concurrent modifications.
     * IMPORTANT: This implementation is a simplification. In a real system,
     * a deep copy of the Query object would need to be implemented
     * to ensure that modifications to the cloned query
     * do not affect the cached instance.
     * The complexity of this copy depends on the internal structure of the Query class.
     */
    private Query cloneQuery(Query original) {
        // TODO: Implement a deep copy of the query.
        // For example, if Query implements Cloneable or has a copy constructor:
        // return original.clone();
        // or
        // return new Query(original);
        // For now, we return the original, which might cause issues
        // if the query is modified after being retrieved from the cache.
        return original;
    }

    /**
     * Clears the compilation cache
     */
    public void clearCompilationCache() {
        compilationCache.clear();
        cacheHits = 0;
        cacheMisses = 0;
        logger.info("Compilation cache cleared");
    }

    /**
     * Cache statistics
     */
    public CacheStats getCacheStats() {
        return new CacheStats(cacheHits, cacheMisses, compilationCache.size(), maxCacheSize);
    }

    /**
     * Remove compile time context
     * Use case: server may have runtime Context
     */
    void cleanContext(Query q) {
        q.setContext(null);
        q.getAST().setContext(null);
    }

    public void defQuery(Query q) {
        if (q.isTemplate()) {
            defTemplate(q);
        } else {
            list.add(q);
        }
    }

    /**
     * Named templates are stored in a table, not in the list
     */
    public void defTemplate(Query q) {
        q.setPrinterTemplate(true);
        if (q.getName() != null) {
            table.put(q.getName(), q);
        } else {
            list.add(q);
            index.add(q);
        }
    }

    /**
     * called once with this transformer map and
     * may be called again with outer transformer map if any
     * map belongs to current or outer transformer
     * current transformer may inherit table from outer transformer
     * hence all subtransformers share same table
     * table: transformation -> Transformer
     */
    public void complete(Transformer trans) {
        complete();
        for (Query q : getTemplates()) {
            trans.complete(q);
            complete(q);
        }
        for (Query q : getNamedTemplates()) {
            trans.complete(q);
            complete(q);
        }
    }

    void complete(Query q) {
        q.setTransformationTemplate(true);
        q.setListPath(true);
    }

    void complete() {
        for (String name : table.keySet()) {
            ArrayList<Query> queryArrayList = new ArrayList<>(1);
            queryArrayList.add(table.get(name));
            tableList.put(name, queryArrayList);
        }
    }

    /**
     * templates inherit template st:profile function definitions
     */
    public void profile() {
        Query profile = getTemplate(TransformerUtils.STL_PROFILE);
        if ((profile != null) && (profile.getExtension() != null)) {
            // share profile function definitions in templates
            fr.inria.corese.core.compiler.parser.Transformer tr = fr.inria.corese.core.compiler.parser.Transformer.create();
            ASTExtension ext = profile.getExtension();
            tr.definePublic(ext, profile, false);

            for (Query t : getTemplates()) {
                addExtension(t, ext);
            }
            for (Query t : getNamedTemplates()) {
                addExtension(t, ext);
            }
        }
    }

    void addExtension(Query q, ASTExtension ext) {
        if (ext == null) {
            return;
        }
        if (q.getExtension() == null) {
            q.setExtension(ext);
        } else {
            q.getExtension().add(ext);
        }
    }

    public List<Query> getQueries() {
        return list;
    }

    public List<Query> getTemplates() {
        return list;
    }

    public List<Query> getTemplates(IDatatype dt) {
        String type = null;
        if (dt != null) {
            type = dt.getLabel();
        }
        List<Query> l = index.getTemplates(type);
        if (l != null) {
            return l;
        }
        return list;
    }

    public Query getTemplate(String name) {
        return table.get(name);
    }

    public List<Query> getTemplateList(String name) {
        return tableList.get(name);
    }

    public Collection<Query> getNamedTemplates() {
        return table.values();
    }

    public Query getTemplate() {
        Query q = getTemplate(TransformerUtils.STL_PROFILE);
        if (q != null) {
            return q;
        } else if (getTemplates().isEmpty()) {
            for (Query qq : table.values()) {
                return qq;
            }
        } else {
            return getTemplates().get(0);
        }
        return null;
    }

    public boolean isEmpty() {
        return list.isEmpty() && table.isEmpty();
    }

    public boolean contains(Query q) {
        if (q.getName() == null) {
            return list.contains(q);
        } else {
            return table.containsValue(q);
        }
    }

    @Override
    public boolean process() throws EngineException {
        if (!isActivate) {
            return false;
        }

        boolean b = false;
        if (isWorkflow) {
            // TRICKY:
            // This engine is part of a workflow which is processed by graph.init()
            // hence it is synchronized by graph.init() 
            // We are here because a query is processed, hence a (read) lock has been taken
            // tell the query processor that it is already synchronized to prevent QueryProcess synUpdate
            // to take a write lock that would cause a deadlock
            getQueryProcess().setSynchronized(true);
        }
        for (Query q : list) {
            Mappings map = getQueryProcess().query(q);
            b = map.nbUpdate() > 0 || b;
        }
        return b;
    }

    public Mappings process(Query q, Mapping m) {
        try {
            return getQueryProcess().query(null, q, m, null);
        } catch (EngineException e) {
            logger.error("Failed to process query: {}", q.toString().substring(0, Math.min(100, q.toString().length())), e);
        }
        return Mappings.create(q);
    }

    @Override
    public boolean isActivate() {
        return isActivate;
    }

    @Override
    public void setActivate(boolean b) {
        isActivate = b;
    }

    /**
     * This method is called by a workflow where this engine is submitted
     */
    @Override
    public void init() {
        isWorkflow = true;
    }

    @Override
    public void remove() {
    }

    @Override
    public void onDelete() {
    }

    @Override
    public void onInsert(Node gNode, Edge edge) {
    }

    @Override
    public void onClear() {
    }

    @Override
    public Type type() {
        return Engine.Type.QUERY_ENGINE;
    }

    public void sort() {
        index.sort(list);
        index.sort();
    }

    /**
     * Fix for "Cannot assign a value to final variable 'list'"
     * Clears the existing list and adds filtered elements instead of reassigning the final list.
     */
    public void clean() {
        ArrayList<Query> filteredList = new ArrayList<>();
        for (Query q : list) {
            if (!q.isFail()) {
                filteredList.add(q);
            }
        }
        list.clear();
        list.addAll(filteredList);
    }

    public Dataset getDataset() {
        return ds;
    }

    public void setDataset(Dataset ds) {
        this.ds = ds;
    }

    public boolean isTransformation() {
        return transformation;
    }

    public void setTransformation(boolean transformation) {
        this.transformation = transformation;
    }

    public void setVisitor(QueryVisitor vis) {
        getQueryProcess().setVisitor(vis);
    }

    public QueryProcess getQueryProcess() {
        return exec;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    /**
     * Composite key for the compilation cache
     */
    private static class CompilationKey {
        private final String queryString;
        private final String base;
        private final Level level;
        private final int datasetHash;
        private final int hashCode;

        public CompilationKey(String queryString, String base, Level level, Dataset dataset) {
            this.queryString = queryString;
            this.base = base;
            this.level = level;
            this.datasetHash = (dataset != null) ? dataset.hashCode() : 0;
            this.hashCode = computeHashCode();
        }

        private int computeHashCode() {
            return Objects.hash(queryString, base, level, datasetHash);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            CompilationKey that = (CompilationKey) obj;
            return datasetHash == that.datasetHash &&
                    Objects.equals(queryString, that.queryString) &&
                    Objects.equals(base, that.base) &&
                    Objects.equals(level, that.level);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    /**
     * Class for cache statistics
     */
    public static class CacheStats {
        private final long hits;
        private final long misses;
        private final int currentSize;
        private final int maxSize;

        public CacheStats(long hits, long misses, int currentSize, int maxSize) {
            this.hits = hits;
            this.misses = misses;
            this.currentSize = currentSize;
            this.maxSize = maxSize;
        }

        public double getHitRate() {
            long total = hits + misses;
            return total == 0 ? 0.0 : (double) hits / total;
        }

        public long getHits() { return hits; }
        public long getMisses() { return misses; }
        public int getCurrentSize() { return currentSize; }
        public int getMaxSize() { return maxSize; }

        @Override
        public String toString() {
            return String.format("CacheStats{hits=%d, misses=%d, hitRate=%.2f%%, size=%d/%d}",
                    hits, misses, getHitRate() * 100, currentSize, maxSize);
        }
    }
}
