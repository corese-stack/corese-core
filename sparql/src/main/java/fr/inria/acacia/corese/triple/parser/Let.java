package fr.inria.acacia.corese.triple.parser;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2015
 *
 */
public class Let extends Statement {

     Let (Expression def, Expression body) {
        super(Processor.LET, def, body);
    }
     
      /**
       * let (var = exp){ exp }
       * @return 
       */
        @Override
      public Variable getVariable(){
          return getArg(0).getArg(0).getVariable();
      }
        
        @Override
        public Expression getDefinition(){
            return getArg(0).getArg(1);
        }
        
        @Override
        public Expression getBody(){
            return getArg(1);
        }
    
     @Override
    public StringBuffer toString(StringBuffer sb) {         
        sb.append(Processor.LET);
        Expression def = getArg(0);
        sb.append(" (");        
        getDefinition().toString(sb);
        sb.append(" as "); 
        // may be match() after parsing ...
        getArg(0).getArg(0).toString(sb);
        sb.append(") {");
        sb.append(NL);
        sb.append("  ");
        getBody().toString(sb);
        sb.append(NL);
        sb.append("}");
        return sb;
    }
    
}
