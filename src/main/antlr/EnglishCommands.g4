grammar EnglishCommands;

anyToken: HOURS | MINUTES | SECONDS | NUMBER | ID | AGO | SINCE | COLON;
comment: anyToken+?;
timeFormat: (NUMBER (HOURS | MINUTES | SECONDS) AGO 
	| SINCE NUMBER COLON NUMBER COLON NUMBER) {System.out.println("Detected time");};
command: c=comment { System.out.println($c.text);} timeFormat? EOF;

HOURS: 'h' | 'hr' | 'hrs' | 'hour' | 'hours';
MINUTES: 'min' | 'mins' | 'minute' | 'minutes';
SECONDS: 's' | 'sec' | 'secs' | 'second' | 'seconds';
COLON: ':';
AGO: 'ago';
SINCE: 'since';
WS: [ \t\r\n]+ -> channel(HIDDEN);
fragment DIGIT: [0-9];
NUMBER: DIGIT+;
ID: ~[ \t\r\n0-9]+;