/*
    The MIT License (MIT)
    Copyright (c) 2015 Joseph T. McBride
    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:
    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
    GraphQL grammar derived from:
        GraphQL Draft Specification - July 2015
        http://facebook.github.io/graphql/
        https://github.com/facebook/graphql
*/
grammar GraphQL;

@header {
package com.apollographql.apollo3.generated.antlr;
}

document
  : definition*
  ;

definition
  : executableDefinition | typeSystemDefinition | typeSystemExtension
  ;

typeSystemDefinition
  : schemaDefinition | typeDefinition | directiveDefinition
  ;

executableDefinition
  : operationDefinition | fragmentDefinition
  ;

schemaDefinition
  : description? SCHEMA directives? operationTypesDefinition
  ;

typeDefinition
  : enumTypeDefinition
  | objectTypeDefinition
  | interfaceTypeDefinition
  | inputObjectDefinition
  | unionTypeDefinition
  | scalarTypeDefinition
  ;

enumTypeDefinition
  : description? ENUM name directives? enumValuesDefinition
  ;

enumValuesDefinition
  : '{' enumValueDefinition* '}'
  ;

enumValueDefinition
  : description? name directives?
  ;

objectTypeDefinition
  : description? TYPE name implementsInterfaces? directives? fieldsDefinition
  ;

implementsInterfaces
  : IMPLEMENTS implementsInterface*
  ;

implementsInterface
  : '&'? namedType
  ;

interfaceTypeDefinition
  : description? INTERFACE name implementsInterfaces? directives? fieldsDefinition
  ;

fieldsDefinition
  : '{' fieldDefinition* '}'
  ;

fieldDefinition
  : description? name argumentsDefinition? ':' type directives?
  ;

argumentsDefinition
  : '(' inputValueDefinition* ')'
  ;

unionTypeDefinition
  : description? UNION name directives? unionMemberTypes
  ;

unionMemberTypes
  : '=' '|'?  namedType ('|'namedType)* ;

scalarTypeDefinition
  : description? SCALAR name directives?
  ;

inputObjectDefinition
  : description? INPUT name directives? inputFieldsDefinition
  ;

inputFieldsDefinition
  : '{' inputValueDefinition* '}'
  ;

inputValueDefinition
  : description? name ':' type defaultValue? directives?
  ;

directiveDefinition
  : description? DIRECTIVE '@' name argumentsDefinition? REPEATABLE? ON_KEYWORD directiveLocations
  ;

directiveLocations
  : directiveLocation ('|' directiveLocation)*
  ;

directiveLocation: name;

typeSystemExtension
  : schemaExtension | typeExtension
  ;

schemaExtension
  : EXTEND SCHEMA directives? operationTypesDefinition
  | EXTEND SCHEMA directives
  ;

typeExtension
  : objectTypeExtensionDefinition
  | interfaceTypeExtensionDefinition
  | unionTypeExtensionDefinition
  | scalarTypeExtensionDefinition
  | enumTypeExtensionDefinition
  | inputObjectTypeExtensionDefinition
  ;

objectTypeExtensionDefinition
  : EXTEND TYPE name implementsInterfaces? directives? fieldsDefinition
  | EXTEND TYPE name implementsInterfaces? directives?
  ;

interfaceTypeExtensionDefinition
  : EXTEND INTERFACE implementsInterfaces? name directives? fieldsDefinition
  | EXTEND INTERFACE implementsInterfaces? name directives
  ;

unionTypeExtensionDefinition
  : EXTEND UNION name directives? unionMemberTypes
  | EXTEND UNION name directives
  ;

scalarTypeExtensionDefinition
  : EXTEND SCALAR name directives
  ;

enumTypeExtensionDefinition
  : EXTEND ENUM name directives? enumValuesDefinition
  | EXTEND ENUM name directives
  ;

inputObjectTypeExtensionDefinition
  : EXTEND INPUT name directives? inputFieldsDefinition
  | EXTEND INPUT name directives
  ;

operationTypesDefinition
  : '{' operationTypeDefinition* '}'
  ;

operationTypeDefinition
  : description? operationType ':' namedType
  ;

operationType
  : QUERY
  | MUTATION
  | SUBSCRIPTION
  ;

operationDefinition
  : description? operationType name? variableDefinitions? directives? selectionSet
  ;

selectionSet
  : '{' (selection)* '}'
  ;

selections
  : (selection)*
  ;

description
  : stringValue
  ;

selection
  : field | fragmentSpread | inlineFragment
  ;

field
  : alias? name arguments? directives? selectionSet?
  ;

alias
  : name ':'
  ;

arguments
  : '(' (argument)* ')'
  ;

argument
  : name ':' value
  ;

fragmentSpread
  : '...' fragmentName directives?
  ;

inlineFragment
  : '...' 'on' typeCondition directives? selectionSet
  ;

fragmentDefinition
  : description? FRAGMENT fragmentName ON_KEYWORD typeCondition directives? selectionSet
  ;

fragmentName
  : nameButNotOn
  ;

directives
  : directive+
  ;

directive
  : '@' name arguments?
  ;

typeCondition
  : namedType
  ;

variableDefinitions
  : '(' (variableDefinition)* ')'
  ;

variableDefinition
  : variable ':' type defaultValue? directives?
  ;

variable
  : '$' name
  ;

defaultValue
  : '=' value
  ;

value
  : variable | intValue | floatValue | stringValue | booleanValue | nullValue | enumValue | listValue | objectValue
  ;

intValue
  : INT
  ;

floatValue
  : FLOAT
  ;

booleanValue
  :	BOOLEAN
  ;

stringValue
  : STRING
  | BLOCK_STRING
  ;

nullValue
  : 'null'
  ;

enumValue
  : name
  ;

listValue
  : '[' value* ']'
  ;

objectValue
  : '{' objectField* '}'
  ;

objectField
  : name ':' value
  ;

type
  : namedType
  | listType
  | nonNullType
  ;

namedType
  : name
  ;

listType
  : '[' type ']'
  ;

nonNullType
  : namedType '!'
  | listType '!'
  ;

nameCommon
  : WORD
  | SCHEMA
  | QUERY
  | MUTATION
  | SUBSCRIPTION
  | ENUM
  | TYPE
  | IMPLEMENTS
  | INTERFACE
  | UNION
  | SCALAR
  | INPUT
  | DIRECTIVE
  | FRAGMENT
  | EXTEND
  | REPEATABLE
  ;


// A special case for enums
nameButNotOn
  : nameCommon
  | BOOLEAN
  | NULL
  ;

// A special case for enums
nameButNotBooleanOrNull
  : nameCommon
  | ON_KEYWORD
  ;

// We cannot make `name` a token because things like 'query', 'fragment', etc... are valid names so could potentially be used in
// field names or other places
name
  : nameButNotOn
  | ON_KEYWORD
  ;

// Begin lexer
STRING
  : '"' ( ESC | ~ ["\\] )* '"'
  ;
BLOCK_STRING:   '"""' .*? '"""';
BOOLEAN
  : 'true' | 'false'
  ;
NULL: 'null';
SCHEMA: 'schema';
QUERY: 'query';
MUTATION: 'mutation';
SUBSCRIPTION:'subscription';
ENUM: 'enum';
TYPE: 'type';
IMPLEMENTS: 'implements';
INTERFACE: 'interface';
UNION: 'union';
SCALAR: 'scalar';
INPUT: 'input';
DIRECTIVE: 'directive';
FRAGMENT: 'fragment';
ON_KEYWORD: 'on';
REPEATABLE: 'repeatable';
EXTEND: 'extend';
UTF8_BOM: '\uEFBBBF';
UTF16_BOM: '\uFEFF';
UTF32_BOM: '\u0000FEFF';
WORD
  : [_A-Za-z] [_0-9A-Za-z]*
  ;

fragment ESC
  : '\\' ( ["\\/bfnrt] | UNICODE )
  ;
fragment UNICODE
  : 'u' HEX HEX HEX HEX
  ;
fragment HEX
  : [0-9a-fA-F]
  ;

fragment NONZERO_DIGIT: [1-9];
fragment DIGIT: [0-9];
fragment FRACTIONAL_PART: '.' DIGIT+;
fragment EXPONENTIAL_PART: EXPONENT_INDICATOR SIGN? DIGIT+;
fragment EXPONENT_INDICATOR: [eE];
fragment SIGN: [+-];
fragment NEGATIVE_SIGN: '-';

FLOAT
  : INT FRACTIONAL_PART
  | INT EXPONENTIAL_PART
  | INT FRACTIONAL_PART EXPONENTIAL_PART
  ;

INT
  : NEGATIVE_SIGN? '0'
  | NEGATIVE_SIGN? NONZERO_DIGIT DIGIT*
  ;

// no leading zeros
fragment EXP
  : [Ee] [+\-]? INT
  ;
// \- since - means "range" inside [...]
WS
  : [ \t\n\r]+ -> skip
  ;
COMMENT
  : '#' ~[\n\r]* -> channel(2)
   ;
COMMA
  : ',' -> skip
   ;
UNICODE_BOM
  : (UTF8_BOM | UTF16_BOM | UTF32_BOM) -> skip;
