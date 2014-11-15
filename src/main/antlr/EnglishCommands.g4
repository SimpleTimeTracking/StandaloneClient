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

anyToken: DAYS | HOURS | MINUTES | SECONDS | NUMBER | ID | AGO | SINCE | COLON | FROM | TO | DOT | AT | FIN | MINUS | UNTIL;
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

date: NUMBER (DOT|MINUS) NUMBER (DOT|MINUS) NUMBER;

optDateWithTime: date? NUMBER COLON NUMBER (COLON NUMBER)?;

dateTime returns [DateTime result]: txt=optDateWithTime { $result = parseDateTime($txt.text); };

sinceFormat returns [DateTime start, DateTime end]: (SINCE|AT) s=dateTime 
	(UNTIL e=dateTime { $end = $e.result; })? {
$start = $s.result;
};

fromToFormat returns [DateTime start, DateTime end]: FROM? s=dateTime TO e=dateTime { 
	$start = $s.result;
	$end = $e.result; 
};

//the same as above but just for date without time
justDate returns [DateTime result]: txt=date { $result = DateTimeFormat.forPattern("yyyy-MM-dd").parseDateTime($txt.text); };

fromToDateFormat returns [DateTime start, DateTime end]: FROM? s=justDate TO e=justDate { 
	$start = $s.result;
	$end = $e.result;
};

timeFormat[String _comment] returns [TimeTrackingItem item]:
		ago=agoFormat { $item = new TimeTrackingItem($_comment, $ago.result); } 
	| 	since=sinceFormat {
	if ($since.end == null) {
		$item = new TimeTrackingItem($_comment, $since.start);
	} else {
		$item = new TimeTrackingItem($_comment, $since.start, $since.end);
}
		}
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


// CLI stuff

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