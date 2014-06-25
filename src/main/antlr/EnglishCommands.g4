grammar EnglishCommands;

@header {
import org.joda.time.*;
import org.joda.time.format.*;
import org.stt.model.*;
}

@parser::members {
	private static final DateTimeFormatter FORMAT_HOUR_MINUTES_SECONDS = DateTimeFormat
			.forPattern("HH:mm:ss");

	static final DateTimeFormatter FORMAT_YEAR_MONTH_HOUR_MINUTES_SECONDS = DateTimeFormat
			.forPattern("yyyy.MM.dd HH:mm:ss");

	private static final DateTimeFormatter FORMAT_HOUR_MINUTES = DateTimeFormat
			.forPattern("HH:mm");
			
	private DateTime parseDateTime(String timeString) {
		DateTime result = parseTimeWithFormatterOrReturnNull(timeString, FORMAT_YEAR_MONTH_HOUR_MINUTES_SECONDS);
		if (result != null) {
			return result;
		}
		DateTime now = DateTime.now();
		result = parseTimeWithFormatterOrReturnNull(timeString, FORMAT_HOUR_MINUTES_SECONDS);
		if (result != null) {
			return result.withDate(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth());
		}
		return FORMAT_HOUR_MINUTES.parseDateTime(timeString).withDate(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth());
	}
	
	private DateTime parseTimeWithFormatterOrReturnNull(String time,
			DateTimeFormatter formatter) {
		try {
			return formatter.parseDateTime(time);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
	
}

anyToken: HOURS | MINUTES | SECONDS | NUMBER | ID | AGO | SINCE | COLON | FROM | TO | DOT | AT | FIN;
comment: anyToken*?;

agoFormat returns [DateTime result]
@init {
	$result = DateTime.now();
}
: amount=NUMBER (
	 	HOURS {$result = $result.minusHours($amount.int); }
	| 	MINUTES {$result = $result.minusMinutes($amount.int); } 
	| 	SECONDS {$result = $result.minusSeconds($amount.int); }
	) AGO;

optDateWithTime: (NUMBER DOT NUMBER DOT NUMBER)? NUMBER COLON NUMBER (COLON NUMBER)?;

dateTime returns [DateTime result]: txt=optDateWithTime { $result = parseDateTime($txt.text); };

sinceFormat returns [DateTime result]: SINCE dt=dateTime { $result = $dt.result; };

fromToFormat returns [DateTime start, DateTime end]: FROM? s=dateTime TO e=dateTime { 
	$start = $s.result;
	$end = $e.result; 
};

timeFormat[String _comment] returns [TimeTrackingItem item]:
		ago=agoFormat { $item = new TimeTrackingItem($_comment, $ago.result); } 
	| 	since=sinceFormat { $item = new TimeTrackingItem($_comment, $since.result); }
	|	fromTo=fromToFormat { $item = new TimeTrackingItem($_comment, $fromTo.start, $fromTo.end); }
	|	{ $item = new TimeTrackingItem($_comment, DateTime.now()); };
	
command returns [TimeTrackingItem newItem, DateTime fin]:
	( 
			FIN (AT? at=dateTime { $fin = $at.result; })? { 
		 		if ($fin == null)
		 			$fin = DateTime.now(); 
		 	}
 		|	c=comment result=timeFormat[$c.text] { $newItem = $result.item; }
			
	) EOF;


// WHEN ADDING TOKENS: ADD THEM to anyToken above!!
// SYMBOLS
COLON: ':';
DOT: '.';

// WORDS
AGO: 'ago';
AT: 'at';
FIN: 'fin';
FROM: 'from';
HOURS: 'h' | 'hr' | 'hrs' | 'hour' | 'hours';
MINUTES: 'min' | 'mins' | 'minute' | 'minutes';
SINCE: 'since';
SECONDS: 's' | 'sec' | 'secs' | 'second' | 'seconds';
TO: 'to';

WS: [ \t\r\n]+ -> channel(HIDDEN);
fragment DIGIT: [0-9];
NUMBER: DIGIT+;
ID: ~[ \t\r\n0-9]+;