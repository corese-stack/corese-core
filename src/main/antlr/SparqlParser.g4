/*
 * Copyright 2007 the original author or authors.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * Neither the name of the author or authors nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Simone Tripodi   (simone)
 * @author Michele Mostarda (michele)
 * @version $Id: Sparql.g 5 2007-10-30 17:20:36Z simone $
 */
/*
 * Ported to Antlr4 by Tom Everett
 * Updated to SPARQL 1.1
 */

// $antlr-format alignTrailingComments true, columnLimit 150, minEmptyLines 1, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine false, allowShortBlocksOnASingleLine true, alignSemicolons hanging, alignColons hanging

parser grammar SparqlParser;

options {
    tokenVocab = SparqlLexer;
}

query
    : queryUnit EOF
    | updateUnit EOF
    ;

queryUnit
    : prologue (selectQuery | constructQuery | describeQuery | askQuery) valuesClause
    ;

updateUnit
    : update
    ;

prologue
    : (baseDecl | prefixDecl)*
    ;

baseDecl
    : BASE IRI_REF
    ;

prefixDecl
    : PREFIX PNAME_NS IRI_REF
    ;

selectQuery
    : selectClause datasetClause* whereClause solutionModifier
    ;

subSelect
    : selectClause whereClause solutionModifier valuesClause
    ;

selectClause
    : SELECT (DISTINCT | REDUCED)? (selectVar+ | STAR)
    ;

selectVar
    : var_
    | L_PAREN expression AS var_ R_PAREN
    ;

constructQuery
    : CONSTRUCT (constructTemplate datasetClause* whereClause solutionModifier | datasetClause* WHERE L_CURLY triplesTemplate? R_CURLY solutionModifier)
    ;

describeQuery
    : DESCRIBE (varOrIri+ | STAR) datasetClause* whereClause? solutionModifier
    ;

askQuery
    : ASK datasetClause* whereClause solutionModifier
    ;

datasetClause
    : FROM (defaultGraphClause | namedGraphClause)
    ;

defaultGraphClause
    : sourceSelector
    ;

namedGraphClause
    : NAMED sourceSelector
    ;

sourceSelector
    : iriRef
    ;

whereClause
    : WHERE? groupGraphPattern
    ;

solutionModifier
    : groupClause? havingClause? orderClause? limitOffsetClauses?
    ;

groupClause
    : GROUP BY groupCondition+
    ;

groupCondition
    : builtInCall
    | functionCall
    | L_PAREN expression (AS var_)? R_PAREN
    | var_
    ;

havingClause
    : HAVING havingCondition+
    ;

havingCondition
    : constraint
    ;

orderClause
    : ORDER BY orderCondition+
    ;

orderCondition
    : (ASC | DESC) brackettedExpression
    | constraint
    | var_
    ;

limitOffsetClauses
    : limitClause offsetClause?
    | offsetClause limitClause?
    ;

limitClause
    : LIMIT INTEGER
    ;

offsetClause
    : OFFSET INTEGER
    ;

valuesClause
    : (VALUES dataBlock)?
    ;

update
    : prologue (update1 (SEMICOLON update)?)?
    ;

update1
    : load
    | clear
    | drop
    | add
    | move
    | copy
    | create
    | insertData
    | deleteData
    | deleteWhere
    | modify
    ;

load
    : LOAD SILENT? iriRef (INTO graphRef)?
    ;

clear
    : CLEAR SILENT? graphRefAll
    ;

drop
    : DROP SILENT? graphRefAll
    ;

create
    : CREATE SILENT? graphRef
    ;

add
    : ADD SILENT? graphOrDefault TO graphOrDefault
    ;

move
    : MOVE SILENT? graphOrDefault TO graphOrDefault
    ;

copy
    : COPY SILENT? graphOrDefault TO graphOrDefault
    ;

insertData
    : INSERT DATA quadData
    ;

deleteData
    : DELETE DATA quadData
    ;

deleteWhere
    : DELETE WHERE quadPattern
    ;

modify
    : (WITH iriRef)? (deleteClause insertClause? | insertClause) usingClause* WHERE groupGraphPattern
    ;

deleteClause
    : DELETE quadPattern
    ;

insertClause
    : INSERT quadPattern
    ;

usingClause
    : USING iriRef
    | USING NAMED iriRef
    ;

graphOrDefault
    : DEFAULT
    | GRAPH? iriRef
    ;

graphRef
    : GRAPH iriRef
    ;

graphRefAll
    : graphRef
    | DEFAULT
    | NAMED
    | ALL
    ;

quadPattern
    : L_CURLY quads R_CURLY
    ;

quadData
    : L_CURLY quads R_CURLY
    ;

quads
    : triplesTemplate? (quadsNotTriples DOT? triplesTemplate?)*
    ;

quadsNotTriples
    : GRAPH varOrIri L_CURLY triplesTemplate? R_CURLY
    ;

triplesTemplate
    : triplesSameSubject (DOT triplesTemplate?)?
    ;

groupGraphPattern
    : L_CURLY (subSelect | groupGraphPatternSub) R_CURLY
    ;

groupGraphPatternSub
    : triplesBlock? (graphPatternNotTriples DOT? triplesBlock?)*
    ;

triplesBlock
    : triplesSameSubjectPath (DOT triplesBlock?)?
    ;

graphPatternNotTriples
    : groupOrUnionGraphPattern
    | optionalGraphPattern
    | minusGraphPattern
    | graphGraphPattern
    | serviceGraphPattern
    | filter_
    | bind
    | inlineData
    ;

optionalGraphPattern
    : OPTIONAL groupGraphPattern
    ;

graphGraphPattern
    : GRAPH varOrIri groupGraphPattern
    ;

serviceGraphPattern
    : SERVICE SILENT? varOrIri groupGraphPattern
    ;

bind
    : BIND L_PAREN expression AS var_ R_PAREN
    ;

inlineData
    : VALUES dataBlock
    ;

dataBlock
    : inlineDataOneVar
    | inlineDataFull
    ;

inlineDataOneVar
    : var_ L_CURLY dataBlockValue* R_CURLY
    ;

inlineDataFull
    : NIL L_CURLY dataBlockValues* R_CURLY
    | L_PAREN var_* R_PAREN L_CURLY dataBlockValues* R_CURLY
    ;

dataBlockValues
    : L_PAREN dataBlockValue* R_PAREN
    | NIL
    ;

dataBlockValue
    : iriRef
    | rdfLiteral
    | numericLiteral
    | booleanLiteral
    | UNDEF
    ;

minusGraphPattern
    : MINUS groupGraphPattern
    ;

groupOrUnionGraphPattern
    : groupGraphPattern (UNION groupGraphPattern)*
    ;

filter_
    : FILTER constraint
    ;

constraint
    : brackettedExpression
    | builtInCall
    | functionCall
    ;

functionCall
    : iriRef argList
    ;

argList
    : NIL
    | L_PAREN DISTINCT? expression (COMMA expression)* R_PAREN
    ;

expressionList
    : NIL
    | L_PAREN expression (COMMA expression)* R_PAREN
    ;

constructTemplate
    : L_CURLY constructTriples? R_CURLY
    ;

constructTriples
    : triplesSameSubject (DOT constructTriples?)?
    ;

triplesSameSubject
    : varOrTerm propertyListNotEmpty
    | triplesNode propertyList
    ;

propertyList
    : propertyListNotEmpty?
    ;

propertyListNotEmpty
    : verb objectList (SEMICOLON (verb objectList)?)*
    ;

verb
    : varOrIri
    | A
    ;

objectList
    : object_ (COMMA object_)*
    ;

object_
    : graphNode
    ;

triplesSameSubjectPath
    : varOrTerm propertyListPathNotEmpty
    | triplesNodePath propertyListPath
    ;

propertyListPath
    : propertyListPathNotEmpty?
    ;

propertyListPathNotEmpty
    : (verbPath | verbSimple) objectListPath (SEMICOLON ((verbPath | verbSimple) objectListPath)?)*
    ;

verbPath
    : path
    ;

verbSimple
    : var_
    ;

objectListPath
    : objectPath (COMMA objectPath)*
    ;

objectPath
    : graphNodePath
    ;

path
    : pathAlternative
    ;

pathAlternative
    : pathSequence (PIPE pathSequence)*
    ;

pathSequence
    : pathEltOrInverse (SLASH pathEltOrInverse)*
    ;

pathElt
    : pathPrimary pathMod?
    ;

pathEltOrInverse
    : pathElt
    | CARET pathElt
    ;

pathMod
    : QUESTION
    | STAR
    | PLUS
    ;

pathPrimary
    : iriRef
    | A
    | EXCLAMATION pathNegatedPropertySet
    | L_PAREN path R_PAREN
    ;

pathNegatedPropertySet
    : pathOneInPropertySet
    | L_PAREN (pathOneInPropertySet (PIPE pathOneInPropertySet)*)? R_PAREN
    ;

pathOneInPropertySet
    : iriRef
    | A
    | CARET (iriRef | A)
    ;

triplesNode
    : collection
    | blankNodePropertyList
    ;

blankNodePropertyList
    : L_SQUARE propertyListNotEmpty R_SQUARE
    ;

triplesNodePath
    : collectionPath
    | blankNodePropertyListPath
    ;

blankNodePropertyListPath
    : L_SQUARE propertyListPathNotEmpty R_SQUARE
    ;

collection
    : L_PAREN graphNode+ R_PAREN
    ;

collectionPath
    : L_PAREN graphNodePath+ R_PAREN
    ;

graphNode
    : varOrTerm
    | triplesNode
    ;

graphNodePath
    : varOrTerm
    | triplesNodePath
    ;

varOrTerm
    : var_
    | graphTerm
    ;

varOrIri
    : var_
    | iriRef
    ;

var_
    : VAR1
    | VAR2
    ;

graphTerm
    : iriRef
    | rdfLiteral
    | numericLiteral
    | booleanLiteral
    | blankNode
    | NIL
    ;

expression
    : conditionalOrExpression
    ;

conditionalOrExpression
    : conditionalAndExpression (DOUBLE_BAR conditionalAndExpression)*
    ;

conditionalAndExpression
    : valueLogical (DOUBLE_AMP valueLogical)*
    ;

valueLogical
    : relationalExpression
    ;

relationalExpression
    : numericExpression (
        (EQUAL | NOT_EQUAL | LESS | GREATER | LESS_OR_EQUAL | GREATER_OR_EQUAL) numericExpression
        | IN expressionList
        | NOT IN expressionList
    )?
    ;

numericExpression
    : additiveExpression
    ;

additiveExpression
    : multiplicativeExpression (
        (PLUS | MINUS_SIGN) multiplicativeExpression
        | numericLiteralPositive
        | numericLiteralNegative
    )*
    ;

multiplicativeExpression
    : unaryExpression ((STAR | SLASH) unaryExpression)*
    ;

unaryExpression
    : (EXCLAMATION | PLUS | MINUS_SIGN)? primaryExpression
    ;

primaryExpression
    : brackettedExpression
    | builtInCall
    | iriRefOrFunction
    | rdfLiteral
    | numericLiteral
    | booleanLiteral
    | var_
    ;

brackettedExpression
    : L_PAREN expression R_PAREN
    ;

builtInCall
    : aggregate
    | STR L_PAREN expression R_PAREN
    | LANG L_PAREN expression R_PAREN
    | LANGMATCHES L_PAREN expression COMMA expression R_PAREN
    | DATATYPE L_PAREN expression R_PAREN
    | BOUND L_PAREN var_ R_PAREN
    | IRI L_PAREN expression R_PAREN
    | URI L_PAREN expression R_PAREN
    | BNODE (L_PAREN expression R_PAREN | NIL)
    | RAND NIL
    | ABS L_PAREN expression R_PAREN
    | CEIL L_PAREN expression R_PAREN
    | FLOOR L_PAREN expression R_PAREN
    | ROUND L_PAREN expression R_PAREN
    | CONCAT L_PAREN expression (COMMA expression)* R_PAREN
    | subStringExpression
    | strReplaceExpression
    | STRLEN L_PAREN expression R_PAREN
    | UCASE L_PAREN expression R_PAREN
    | LCASE L_PAREN expression R_PAREN
    | ENCODE_FOR_URI L_PAREN expression R_PAREN
    | CONTAINS L_PAREN expression COMMA expression R_PAREN
    | STRSTARTS L_PAREN expression COMMA expression R_PAREN
    | STRENDS L_PAREN expression COMMA expression R_PAREN
    | STRBEFORE L_PAREN expression COMMA expression R_PAREN
    | STRAFTER L_PAREN expression COMMA expression R_PAREN
    | YEAR L_PAREN expression R_PAREN
    | MONTH L_PAREN expression R_PAREN
    | DAY L_PAREN expression R_PAREN
    | HOURS L_PAREN expression R_PAREN
    | MINUTES L_PAREN expression R_PAREN
    | SECONDS L_PAREN expression R_PAREN
    | TIMEZONE L_PAREN expression R_PAREN
    | TZ L_PAREN expression R_PAREN
    | NOW NIL
    | UUID NIL
    | STRUUID NIL
    | MD5 L_PAREN expression R_PAREN
    | SHA1 L_PAREN expression R_PAREN
    | SHA256 L_PAREN expression R_PAREN
    | SHA384 L_PAREN expression R_PAREN
    | SHA512 L_PAREN expression R_PAREN
    | COALESCE L_PAREN expression (COMMA expression)* R_PAREN
    | IF L_PAREN expression COMMA expression COMMA expression R_PAREN
    | STRLANG L_PAREN expression COMMA expression R_PAREN
    | STRDT L_PAREN expression COMMA expression R_PAREN
    | SAME_TERM L_PAREN expression COMMA expression R_PAREN
    | IS_IRI L_PAREN expression R_PAREN
    | IS_URI L_PAREN expression R_PAREN
    | IS_BLANK L_PAREN expression R_PAREN
    | IS_LITERAL L_PAREN expression R_PAREN
    | IS_NUMERIC L_PAREN expression R_PAREN
    | regexExpression
    | existsFunc
    | notExistsFunc
    ;

regexExpression
    : REGEX L_PAREN expression COMMA expression (COMMA expression)? R_PAREN
    ;

subStringExpression
    : SUBSTR L_PAREN expression COMMA expression (COMMA expression)? R_PAREN
    ;

strReplaceExpression
    : REPLACE L_PAREN expression COMMA expression COMMA expression (COMMA expression)? R_PAREN
    ;

existsFunc
    : EXISTS groupGraphPattern
    ;

notExistsFunc
    : NOT EXISTS groupGraphPattern
    ;

aggregate
    : COUNT L_PAREN DISTINCT? (STAR | expression) R_PAREN
    | SUM L_PAREN DISTINCT? expression R_PAREN
    | MIN L_PAREN DISTINCT? expression R_PAREN
    | MAX L_PAREN DISTINCT? expression R_PAREN
    | AVG L_PAREN DISTINCT? expression R_PAREN
    | SAMPLE L_PAREN DISTINCT? expression R_PAREN
    | GROUP_CONCAT L_PAREN DISTINCT? expression (SEMICOLON SEPARATOR EQUAL string_)? R_PAREN
    ;

iriRefOrFunction
    : iriRef argList?
    ;

rdfLiteral
    : string_ (LANGTAG | DOUBLE_CARET iriRef)?
    ;

numericLiteral
    : numericLiteralUnsigned
    | numericLiteralPositive
    | numericLiteralNegative
    ;

numericLiteralUnsigned
    : INTEGER
    | DECIMAL
    | DOUBLE
    ;

numericLiteralPositive
    : INTEGER_POSITIVE
    | DECIMAL_POSITIVE
    | DOUBLE_POSITIVE
    ;

numericLiteralNegative
    : INTEGER_NEGATIVE
    | DECIMAL_NEGATIVE
    | DOUBLE_NEGATIVE
    ;

booleanLiteral
    : TRUE
    | FALSE
    ;

string_
    : STRING_LITERAL1
    | STRING_LITERAL2
    | STRING_LITERAL_LONG1
    | STRING_LITERAL_LONG2
    ;

iriRef
    : IRI_REF
    | prefixedName
    ;

prefixedName
    : PNAME_LN
    | PNAME_NS
    ;

blankNode
    : BLANK_NODE_LABEL
    | ANON
    ;
