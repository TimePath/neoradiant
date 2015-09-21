grammar Map;

parse
    :   entity* EOF
    ;

entity
    :   '{' (property | brush | brushdef | patchdef)* '}'
    ;

property
    :   String String
    ;

brush
    :   '{' plane+ '}'
    ;

    plane
        :   triple
            triple
            triple
            File
            Number Number
            Number
            Number Number
            Number Number Number
        ;

    triple
        :   '(' Number Number Number ')'
        ;

brushdef
    :   '{' 'brushDef' '{' brushdefFragment+ '}' '}'
    ;

    brushdefFragment
        :   triple triple triple '(' triple triple ')' File Number Number Number
        ;

patchdef
    :   '{' 'patchDef2' '{' File quintuple '(' patchdefFragment+ ')' '}' '}'
    ;

    patchdefFragment
        :   '(' quintuple+ ')'
        ;

    quintuple
        :   '(' Number Number Number Number Number ')'
        ;

Number
    :   '-'? Digit+ ('.' Digit+)?
    ;

fragment
Digit
    :   [0-9]
    ;

File
    :   FileFragment ('/' FileFragment)*
    ;

fragment
FileFragment
    :   [a-zA-Z_0-9-]+
    ;

String
    :   '"' (~'"')* '"'
    ;

WS
    :
    (   [ \t]+
    |   (   '\r' '\n'?
        |   '\n'
        )
    |   '//' ~[\r\n]*
    )   -> skip
    ;
