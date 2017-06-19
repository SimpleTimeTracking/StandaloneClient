grammar EnglishCommands;

@header {
package org.stt.grammar;

import java.time.*;
import java.time.format.*;
import org.stt.model.*;
}

@members {
private static final DateTimeFormatter LOCAL_DATE_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd");
}

anyToken: DAYS | HOURS | MINUTES | SECONDS | NUMBER | ID | AGO | SINCE | COLON | FROM | TO | DOT | AT | FIN | MINUS | UNTIL | RESUME | LAST | SLASH;
activity: anyToken*?;

timeUnit:
	HOURS
	| 	MINUTES
	| 	SECONDS;


agoFormat returns [int amount] :
	SINCE a=NUMBER timeUnit AGO? { $amount = $a.int; }
	| a=NUMBER timeUnit AGO { $amount = $a.int; };

date: (NUMBER|DOT|MINUS|SLASH)+;

dateTimeInternal: (NUMBER|DOT|MINUS|SLASH|COLON)+;

dateTime returns [String text]: txt=dateTimeInternal { $text = $txt.text; };

sinceFormat: (SINCE|AT) start=dateTime
	(UNTIL end=dateTime)?;

fromToFormat:
	FROM start=dateTime (TO end=dateTime)?
	| 	start=dateTime TO end=dateTime;


timeFormat:
		ago=agoFormat
	| 	since=sinceFormat
	|	fromTo=fromToFormat
	|	;

finCommand: FIN (AT? at=dateTime)?;

resumeLastCommand: RESUME LAST;

itemWithComment returns [String text]:c=activity result=timeFormat { $text = $c.text; };

command:
	(	finCommand
	    |   resumeLastCommand
 		|	itemWithComment
			
	) EOF;


// CLI stuff

justDate returns [LocalDate result]: txt=date { $result = LocalDate.parse($txt.text, LOCAL_DATE_PATTERN); };

fromToDateFormat returns [LocalDate start, LocalDate end]: FROM? s=justDate TO e=justDate {
	$start = $s.result;
	$end = $e.result;
};

reportStart returns [LocalDate from_date, LocalDate to_date]:
		SINCE d=date {
		$from_date = LocalDate.parse($d.text, LOCAL_DATE_PATTERN); $to_date = LocalDate.now();
		 }
	|  	n=NUMBER DAYS { $from_date = LocalDate.now().minusDays($n.int); $to_date = LocalDate.now();}
	|   AT d=date {
	    $from_date = LocalDate.parse($d.text, LOCAL_DATE_PATTERN); $to_date=$from_date;
	    }
	|   fromTo = fromToDateFormat { $from_date = $fromTo.start; $to_date = $fromTo.end; }
	|	anyToken*;



// WHEN ADDING TOKENS: ADD THEM to anyToken above!!
// SYMBOLS
COLON: ':';
DOT: '.';
MINUS: '-';
SLASH: '/';

// WORDS
AGO: 'ago';
AT: 'at';
FIN: 'fin';
RESUME: 'resume';
LAST: 'last';
FROM: 'from';
DAYS: 'days';
HOURS: 'h' | 'hr' | 'hrs' | 'hour' | 'hours';
MINUTES: 'min' | 'mins' | 'minute' | 'minutes';
SINCE: 'since';
SECONDS: 's' | 'sec' | 'secs' | 'second' | 'seconds';
TO: 'to';
UNTIL: 'until';

WS: [ \t\r\n]+ -> channel(HIDDEN);
fragment DIGIT: [0-9];
NUMBER: DIGIT+;
ID: ~[ \t\r\n0-9]+;