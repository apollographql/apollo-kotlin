grammar GraphSDL;

@header {
package com.apollographql.apollo.compiler.parser.antlr;
}

document
  : (schemaDefinition | typeDefinition)+
  ;

schemaDefinition
  : description? SCHEMA directives? operationTypesDefinition
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

typeDefinition
  : enumTypeDefinition
  | objectTypeDefinition
  | interfaceTypeDefinition
  | inputObjectDefinition
  | unionTypeDefinition
  | scalarTypeDefinition
  | directiveDefinition
  | typeSystemExtension
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
  : '(' argumentDefinition* ')'
  ;

argumentDefinition
  : description? name ':' type defaultValue? directives?
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
  : description? INPUT name directives? inputValuesDefinition
  ;

inputValuesDefinition
  : '{' inputValueDefinition* '}'
  ;

inputValueDefinition
  : description? name ':' type defaultValue? directives?
  ;

directiveDefinition
  : description? DIRECTIVE '@' name argumentsDefinition? REPEATABLE? ON_KEYWORD directiveLocations
  ;

directiveLocations
  : directiveLocation
  | directiveLocations '|' directiveLocation
  ;

directiveLocation: name;

typeSystemExtension
  : schemaExtension | typeExtension
  ;

schemaExtension
  : EXTEND SCHEMA directives? operationTypesDefinition
  | EXTEND SCHEMA directives+
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
  : EXTEND INTERFACE name directives? fieldsDefinition
  | EXTEND INTERFACE name directives?
  ;

unionTypeExtensionDefinition
  : EXTEND UNION name directives? unionMemberTypes
  | EXTEND UNION name directives?
  ;

scalarTypeExtensionDefinition
  : EXTEND SCALAR name directives
  ;

enumTypeExtensionDefinition
  : EXTEND ENUM name directives? enumValuesDefinition
  | EXTEND ENUM name directives?
  ;

inputObjectTypeExtensionDefinition
  : EXTEND INPUT name directives? inputValuesDefinition
  | EXTEND INPUT name directives?
  ;

name
  : NAME
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
  | ON_KEYWORD
  | EXTEND
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
  | nonNullType '!'
  ;

description
  : stringValue
  ;

defaultValue
  : '=' value
  ;

value
  : intValue
	| floatValue
	| stringValue
	| booleanValue
	| nullValue
	| enumValue
	| listValue
	| objectValue
	;

intValue
  : INT
  ;

floatValue
  : FLOAT
  ;

booleanValue
  :	'true'
  |	'false'
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
  : '[' ']'
  | '[' value+ ']'
  ;

objectValue
  : '{' objectField* '}'
  ;

objectField
  : name ':' value
  ;

directives
  : directive+
  ;

directive
  : '@' name directiveArguments?
  ;

directiveArguments
  : '(' directiveArgument+ ')'
  ;

directiveArgument
  : name ':' value
  ;

//Start lexer

//keywords
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
REPEATABLE: 'repeatable';
ON_KEYWORD: 'on';
EXTEND: 'extend';

NAME: [_A-Za-z] [_0-9A-Za-z]*;
STRING: '"' CHARACTER* '"';
BLOCK_STRING:   '"""' .*? '"""';

fragment CHARACTER: ( ESC | ~ ["\\]);
fragment ESC: '\\' ( ["\\/bfnrt] | UNICODE);
fragment UNICODE: 'u' HEX HEX HEX HEX;
fragment HEX: [0-9a-fA-F];
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

WS: [ \t\n\r]+ -> skip;
COMMA: ',' -> skip;
COMMENT: '#' ~[\n\r]* -> skip;
UNICODE_BOM: (UTF8_BOM | UTF16_BOM | UTF32_BOM) -> skip;
UTF8_BOM: '\uEFBBBF';
UTF16_BOM: '\uFEFF';
UTF32_BOM: '\u0000FEFF';
