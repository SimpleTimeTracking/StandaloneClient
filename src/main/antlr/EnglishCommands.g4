grammar EnglishCommands;

@header {
import org.joda.time.*;
import org.joda.time.format.*;
import org.stt.model.*;
}

anyToken: DAYS | HOURS | MINUTES | SECONDS | NUMBER | ID | AGO | SINCE | COLON | FROM | TO | DOT | AT | FIN | MINUS | UNTIL;
comment: anyToken*?;

timeUnit:
	HOURS
	| 	MINUTES
	| 	SECONDS;


agoFormat returns [int amount] :
	SINCE a=NUMBER timeUnit AGO? { $amount = $a.int; }
	| a=NUMBER timeUnit AGO { $amount = $a.int; };

date returns [int year, int month, int day]: y=NUMBER (DOT|MINUS) m=NUMBER (DOT|MINUS) d=NUMBER
	{ $year = $y.int; $month = $m.int; $day = $d.int; };

dateTime returns [ int hour, int minute, int second]: date? h=NUMBER COLON m=NUMBER (COLON s=NUMBER { $second = $s.int; })?
	{ $hour = $h.int; $minute = $m.int; };

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

itemWithComment returns [String text]:c=comment result=timeFormat { $text = $c.text; };

command:
	(	finCommand
 		|	itemWithComment
			
	) EOF;


// CLI stuff

justDate returns [DateTime result]: txt=date { $result = DateTimeFormat.forPattern("yyyy-MM-dd").parseDateTime($txt.text); };

fromToDateFormat returns [DateTime start, DateTime end]: FROM? s=justDate TO e=justDate {
	$start = $s.result;
	$end = $e.result;
};

reportStart returns [DateTime from_date, DateTime to_date]:
		SINCE d=date {
		$from_date = DateTimeFormat.forPattern("yyyy-MM-dd").parseDateTime($d.text); $to_date = DateTime.now();
		 }
	|  	n=NUMBER DAYS { $from_date = new DateTime().minusDays($n.int); $to_date = DateTime.now();}
	|   AT d=date {
	    $from_date = DateTimeFormat.forPattern("yyyy-MM-dd").parseDateTime($d.text); $to_date=$from_date;
	    }
	|   fromTo = fromToDateFormat { $from_date = $fromTo.start; $to_date = $fromTo.end; }
	|	anyToken*;



// WHEN ADDING TOKENS: ADD THEM to anyToken above!!
// SYMBOLS
COLON: ':';
DOT: '.';
MINUS: '-';

// WORDS
AGO: 'ago';
AT: 'at';
FIN: 'fin';
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