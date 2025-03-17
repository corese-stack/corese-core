package fr.inria.corese.core.approximate.strategy;

import fr.inria.corese.core.compiler.parser.Pragma;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static fr.inria.corese.core.approximate.strategy.AlgType.*;
import static fr.inria.corese.core.approximate.strategy.StrategyType.*;

/**
 * Define the strategies
 *
 * @author Fuqi Song, Wimmics Inria I3S
 * @date 5 oct. 2015
 */
public class ApproximateStrategy {

    public static final String SEPARATOR = "-";
    private static final Logger logger = LoggerFactory.getLogger(ApproximateStrategy.class);
    //default strategy-algorithm map
    private static final Map<StrategyType, List<AlgType>> defaultStrategyMap = new EnumMap<>(StrategyType.class);
    private static final List<StrategyType> mergableStrategy; //the strateiges that can be merged into filter
    private static final List<StrategyType> defaultStrategy;

    static {
        //*** DEFAULT MAP STRATEGY - ALGORITHMS
        defaultStrategyMap.put(URI_LEX, Arrays.asList(NG, JW));//S P O
        defaultStrategyMap.put(URI_WN, Collections.singletonList(WN));//S P O
        defaultStrategyMap.put(URI_EQUALITY, Collections.singletonList(EQ));//S P O

        defaultStrategyMap.put(PROPERTY_EQUALITY, Collections.singletonList(EQ));//P
        defaultStrategyMap.put(CLASS_HIERARCHY, Collections.singletonList(CH));//A rdf:type B

        defaultStrategyMap.put(LITERAL_WN, Collections.singletonList(WN));//O@literal@xsd:string@en
        defaultStrategyMap.put(LITERAL_LEX, Arrays.asList(NG, JW));//O@literal@xsd:string

        mergableStrategy = Arrays.asList(URI_LEX, URI_WN, CLASS_HIERARCHY, LITERAL_WN, LITERAL_LEX);

        defaultStrategy = new ArrayList<>();
        defaultStrategy.add(URI_LEX);
        defaultStrategy.add(LITERAL_LEX);
    }

    //real strategy-algorithm map applied
    private Map<StrategyType, List<AlgType>> strategyMap = new EnumMap<>(StrategyType.class);
    private List<StrategyType> strategyList = null; //strategies to be applied
    private List<AlgType> algorithmList = null; //algorithm list to be applied

    /**
     * Convert a string to a list of algorithm instance
     *
     * @param algs
     * @return
     */
    public static List<AlgType> getAlgorithmList(String algs) {
        List<AlgType> list = new ArrayList<>();

        if (algs == null || algs.isEmpty()) {
            return list;
        }

        String[] algsArray = algs.split(SEPARATOR);
        for (String aa : algsArray) {
            AlgType at = valueOf(aa);
            if (at != null) {
                list.add(at);
            }
        }

        return list;
    }

    /**
     * Convert a string name to an instance of algorithm
     *
     * @param alg
     * @return
     */
    public static AlgType valueOf(String alg) {
        try {
            return AlgType.valueOf(alg);
        } catch (IllegalArgumentException e) {
            logger.warn("Illegal algorithm name '" + alg + "'. \n" + e.getMessage());
        }
        return null;
    }

    /**
     * Initialize the strategy and algorithms using Pragma from AST
     *
     * @param ast
     */
    public void init(ASTQuery ast) {
        //kg:strategy, option: strategies used
        List<String> strategyOption = ast.getApproximateSearchOptions(Pragma.STRATEGY);
        strategyList = parse(strategyOption, StrategyType.class);

        // kg:algorithm, option: algorithms to use
        List<String> algorithmOption = ast.getApproximateSearchOptions(Pragma.ALGORITHM);
        algorithmList = parse(algorithmOption, AlgType.class);

        // kg:priority, option: algorithm priortiy **
        List<String> priorityAlgorithmOption = ast.getApproximateSearchOptions(Pragma.PRIORITY_ALGORITHM);
        List<Double> algorithmPriorities = parse(priorityAlgorithmOption, Double.class);
        Priority.init(algorithmPriorities, algorithmList);

        // filter algorithms according to the settings above
        filter(strategyList, algorithmList);
    }

    //setup the real strategy-algorithm to be applied, according to the default
    //strategy-algorithm list and options from Pragma@AST
    private void filter(List<StrategyType> ls, List<AlgType> la) {
        strategyMap = new EnumMap<>(StrategyType.class);

        for (StrategyType st : ls) {
            if (!defaultStrategyMap.containsKey(st)) {
                continue;
            }
            List<AlgType> algs = new ArrayList<>();
            for (AlgType alg : defaultStrategyMap.get(st)) {
                if (la.contains(alg)) {
                    algs.add(alg);
                }
            }
            if (!algs.isEmpty()) {
                strategyMap.put(st, algs);
            }
        }
    }

    /**
     * Get strategies by group and only return the strategies appeared in the
     * given list
     *
     * @param filter
     * @return
     */
    public List<StrategyType> getMergableStrategies(List<StrategyType> filter) {
        List<StrategyType> lst = new ArrayList<>();
        for (StrategyType st : mergableStrategy) {
            if (filter.contains(st) && check(st)) {
                lst.add(st);
            }
        }
        return lst;
    }

    /**
     * Get list of algorithms that can be used by one strategy
     *
     * @param strategy
     * @return
     */
    public List<AlgType> getAlgorithmTypes(StrategyType strategy) {
        return strategyMap.containsKey(strategy) ? strategyMap.get(strategy) : new ArrayList<>();
    }

    public String getAlgorithmString(List<StrategyType> lst) {

        List<AlgType> types = new ArrayList<>();
        for (StrategyType st : lst) {
            types.addAll(strategyMap.get(st));
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < types.size(); i++) {
            sb.append(types.get(i).name());
            if (i < types.size() - 1) {
                sb.append(SEPARATOR);
            }
        }

        return sb.toString();
    }

    /**
     * parse a list of strings to the specified type T
     *
     * @param <T>
     * @param options
     * @param type
     * @return
     */
    private <T> List<T> parse(List<String> options, Class<T> type) {
        List<T> list;

        if (options == null || options.isEmpty()) {
            if (type.getName().equals(StrategyType.class.getName())) {
                list = (List<T>) defaultStrategy;
            } else if (type.getName().equals(AlgType.class.getName())) {
                list = (List<T>) AlgType.allValues();
            } else {
                list = null;
            }

            return list;
        }

        list = new ArrayList<>();
        for (String aa : options) {
            T t;
            try {
                if (type.getName().equals(StrategyType.class.getName())) {
                    t = (T) StrategyType.valueOf(aa);
                } else if (type.getName().equals(AlgType.class.getName())) {
                    t = (T) AlgType.valueOf(aa);
                } else {
                    t = (T) Double.valueOf(aa);
                }
                list.add(t);
            } catch (IllegalArgumentException e) {
                logger.warn("Approximate search: '" + aa + "' is not defined");
            }
        }
        return list;
    }

    /**
     * Check whether a given strategy is authroized to use
     *
     * @param strategy
     * @return
     */
    public boolean check(StrategyType strategy) {
        if (!strategyList.contains(strategy)) {
            return false;
        }

        for (AlgType at : getAlgorithmTypes(strategy)) {
            if (!algorithmList.contains(at)) {
                return false;
            }
        }
        return true;
    }
}
