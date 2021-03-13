use chrono::format::{Item, ParseResult, Parsed};
use chrono::prelude::*;
use chrono::serde::ts_seconds;
use core::fmt::Display;
use lazy_static::lazy_static;
use serde::{Deserialize, Serialize};
use std::cmp::Ordering;
use std::str::FromStr;

#[derive(Deserialize, Serialize, Clone, Debug, Eq, PartialEq)]
pub struct TimeTrackingItem {
    #[serde(with = "ts_seconds")]
    pub start: DateTime<Utc>,
    pub end: Ending,
    pub activity: String,
}

#[derive(Deserialize, Serialize, Copy, Clone, Debug, Eq, PartialEq)]
pub enum Ending {
    Open,
    At(#[serde(with = "ts_seconds")] DateTime<Utc>),
}

impl Ending {
    pub fn to_date_time(&self) -> Option<DateTime<Utc>> {
        match self {
            Ending::Open => None,
            Ending::At(dt) => Some(*dt),
        }
    }
}

impl<TZ: chrono::TimeZone> PartialEq<DateTime<TZ>> for Ending {
    fn eq(&self, other: &DateTime<TZ>) -> bool {
        match self {
            Ending::Open => false,
            Ending::At(ts) => ts == other,
        }
    }
}

impl<TZ: chrono::TimeZone> PartialOrd<DateTime<TZ>> for Ending {
    fn partial_cmp(&self, other: &DateTime<TZ>) -> Option<Ordering> {
        match self {
            Ending::Open => Some(Ordering::Greater),
            Ending::At(ts) => Some(ts.timestamp().cmp(&other.timestamp())),
        }
    }
}

impl<TZ: chrono::TimeZone> PartialOrd<Ending> for DateTime<TZ> {
    fn partial_cmp(&self, other: &Ending) -> Option<Ordering> {
        match other {
            Ending::Open => None,
            Ending::At(ts) => Some(self.timestamp().cmp(&ts.timestamp())),
        }
    }
}

impl<TZ: chrono::TimeZone> PartialEq<Ending> for DateTime<TZ> {
    fn eq(&self, other: &Ending) -> bool {
        match other {
            Ending::Open => false,
            Ending::At(ts) => self == ts,
        }
    }
}

impl Ord for Ending {
    fn cmp(&self, other: &Self) -> Ordering {
        match self {
            Ending::Open => {
                if *other == Ending::Open {
                    Ordering::Equal
                } else {
                    Ordering::Greater
                }
            }
            Ending::At(my_ts) => match other {
                Ending::Open => Ordering::Less,
                Ending::At(other_ts) => my_ts.cmp(other_ts),
            },
        }
    }
}

impl PartialOrd for Ending {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        Some(self.cmp(other))
    }
}

impl TimeTrackingItem {
    pub fn ending_at(mut self, end: DateTime<Local>) -> Self {
        self.end = Ending::At(DateTime::<Utc>::from(end.with_nanosecond(0).unwrap()));
        self
    }

    pub fn ending_now(self) -> Self {
        self.ending_at(Local::now())
    }

    pub fn starting_at<S: Into<String>>(start: DateTime<Local>, activity: S) -> Self {
        TimeTrackingItem {
            start: DateTime::<Utc>::from(start.with_nanosecond(0).unwrap()),
            end: Ending::Open,
            activity: activity.into(),
        }
    }

    pub fn interval<S: Into<String>>(
        start: DateTime<Local>,
        end: DateTime<Local>,
        activity: S,
    ) -> Result<Self, ItemError> {
        if end < start {
            Err(ItemError(ItemErrorKind::StartAfterEnd))
        } else {
            Ok(TimeTrackingItem {
                start: DateTime::<Utc>::from(start.with_nanosecond(0).unwrap()),
                end: Ending::At(DateTime::<Utc>::from(end.with_nanosecond(0).unwrap())),
                activity: activity.into(),
            })
        }
    }

    pub fn to_storage_line(&self) -> String {
        let start = DateTime::<Local>::from(self.start).format_with_items(FORMAT.iter().cloned());
        let activity_escaped = self.activity.replace('\\', "\\\\").replace('\n', "\\n");
        if let Ending::At(end) = self.end {
            format!(
                "{start} {end} {activity}",
                start = start,
                end = DateTime::<Local>::from(end).format_with_items(FORMAT.iter().cloned()),
                activity = activity_escaped
            )
        } else {
            format!(
                "{start} {activity}",
                start = start,
                activity = activity_escaped
            )
        }
    }

    fn unescape(activity: &str) -> String {
        activity.replace("\\n", "\n").replace("\\\\", "\\")
    }
}

lazy_static! {
    static ref FORMAT: Vec<Item<'static>> =
        chrono::format::strftime::StrftimeItems::new("%Y-%m-%d_%H:%M:%S").collect();
}

#[derive(Debug, Clone, PartialEq, Eq, Copy)]
pub struct ItemError(ItemErrorKind);

#[derive(Debug, Clone, PartialEq, Eq, Copy)]
enum ItemErrorKind {
    StartAfterEnd,
    BadFormat,
    ActivityMissing,
}

impl Display for ItemError {
    fn fmt(&self, fmt: &mut std::fmt::Formatter<'_>) -> std::result::Result<(), std::fmt::Error> {
        fmt.write_fmt(format_args!("{:?}", self))
    }
}

impl FromStr for TimeTrackingItem {
    type Err = ItemError;

    fn from_str(s: &str) -> std::result::Result<Self, Self::Err> {
        let start = parse_stt_date_time(&s.get(0..19).ok_or(ItemError(ItemErrorKind::BadFormat))?)
            .map_err(|_| ItemError(ItemErrorKind::BadFormat))?;
        if let Some(end) = s.get(20..39) {
            let end = parse_stt_date_time(end);
            if let Ok(end) = end {
                TimeTrackingItem::interval(
                    start,
                    end,
                    Self::unescape(
                        &s.get(40..)
                            .ok_or(ItemError(ItemErrorKind::ActivityMissing))?,
                    ),
                )
            } else {
                Ok(TimeTrackingItem::starting_at(
                    start,
                    Self::unescape(
                        &s.get(20..)
                            .ok_or(ItemError(ItemErrorKind::ActivityMissing))?,
                    ),
                ))
            }
        } else {
            Ok(TimeTrackingItem::starting_at(
                start,
                Self::unescape(
                    &s.get(20..)
                        .ok_or(ItemError(ItemErrorKind::ActivityMissing))?,
                ),
            ))
        }
    }
}

fn parse_stt_date_time(s: &str) -> ParseResult<DateTime<Local>> {
    let mut parsed = Parsed::new();
    chrono::format::parse(&mut parsed, s, FORMAT.iter().cloned())?;
    parsed.to_datetime_with_timezone(&Local)
}

#[cfg(test)]
mod test {
    use crate::tti::*;

    #[test]
    fn should_round_start_to_second() {
        // GIVEN
        let start = Local.timestamp(0, 1);

        // WHEN
        let sut = TimeTrackingItem::starting_at(start, "");

        // THEN
        assert_ne!(sut.start, start);
    }

    #[test]
    fn should_round_interval_to_second() {
        // GIVEN
        let start = Local.timestamp(9999, 9999);
        let end = Local.timestamp(99999999, 9999999);

        // WHEN
        let sut = TimeTrackingItem::interval(start, end, "").unwrap();

        // THEN
        assert_ne!(sut.start, start);
        assert_ne!(sut.end, Ending::At(end.into()));
    }

    #[test]
    fn should_parse_interval() {
        // GIVEN

        // WHEN
        let result: TimeTrackingItem = "2017-07-01_21:06:03 2017-07-01_21:06:11 bla bla blub blub"
            .parse()
            .unwrap();

        // THEN
        assert_eq!(result.start, Local.ymd(2017, 7, 1).and_hms(21, 6, 3));
        assert_eq!(
            result.end,
            Ending::At(Local.ymd(2017, 7, 1).and_hms(21, 6, 11).into())
        );
        assert_eq!(result.activity, "bla bla blub blub")
    }

    #[test]
    fn should_parse_start_only() {
        // GIVEN

        // WHEN
        let result = TimeTrackingItem::from_str("2022-11-01_11:06:03 bla").unwrap();

        // THEN
        assert_eq!(result.start, Local.ymd(2022, 11, 1).and_hms(11, 6, 3));
        assert_eq!(result.end, Ending::Open);
        assert_eq!(result.activity, "bla");
    }

    #[test]
    #[should_panic(expected = "ItemError(ActivityMissing)")]
    fn should_fail_without_activity_separator() {
        // GIVEN

        // WHEN
        TimeTrackingItem::from_str("2022-11-01_11:06:03").unwrap();

        // THEN
        // expected fail
    }

    #[test]
    #[should_panic(expected = "ItemError(ActivityMissing)")]
    fn should_fail_without_activity_separator2() {
        // GIVEN

        // WHEN
        TimeTrackingItem::from_str("2022-11-01_11:06:03 2022-11-01_11:06:03").unwrap();

        // THEN
        // expected fail
    }

    #[test]
    #[should_panic(expected = "ItemError(StartAfterEnd)")]
    fn should_fail_when_start_is_after_end() {
        // GIVEN

        // WHEN
        TimeTrackingItem::interval(
            Local.ymd(2022, 11, 1).and_hms(11, 6, 3),
            Local.ymd(2020, 11, 1).and_hms(11, 6, 3),
            "test",
        )
        .unwrap();

        //THEN
        // expected fail
    }

    #[test]
    fn should_print_in_storage_format() {
        // GIVEN
        let item = TimeTrackingItem::starting_at(Local.ymd(2022, 11, 1).and_hms(11, 6, 3), "test");

        // WHEN
        let line = item.to_storage_line();

        // THEN
        assert_eq!("2022-11-01_11:06:03 test", line);
    }

    #[test]
    fn should_escape_cr_in_storage_format() {
        // GIVEN
        let item =
            TimeTrackingItem::starting_at(Local.ymd(2022, 11, 1).and_hms(11, 6, 3), "test\ntest\\");

        // WHEN
        let line = item.to_storage_line();

        // THEN
        assert_eq!("2022-11-01_11:06:03 test\\ntest\\\\", line);
    }

    #[test]
    fn should_unescape_cr_in_storage_format() {
        // GIVEN
        let item = TimeTrackingItem::from_str("2022-11-01_11:06:03 test\\ntest\\\\").unwrap();

        // WHEN
        let line = item.to_storage_line();

        // THEN
        assert_eq!("2022-11-01_11:06:03 test\\ntest\\\\", line);
    }

    #[test]
    fn without_end_should_serialize_to_json() {
        // GIVEN
        let item = TimeTrackingItem::starting_at(Local.ymd(2022, 11, 1).and_hms(11, 6, 3), "test");

        // WHEN
        let line = serde_json::to_string(&item).unwrap();

        // THEN
        assert_eq!(
            "{\"start\":1667297163,\"end\":\"Open\",\"activity\":\"test\"}",
            line
        );
    }

    #[test]
    fn with_end_should_serialize_to_json() {
        // GIVEN
        let item = TimeTrackingItem::interval(
            Local.ymd(2022, 11, 1).and_hms(11, 6, 3),
            Local.ymd(2023, 1, 1).and_hms(11, 1, 59),
            "test again",
        )
        .unwrap();

        // WHEN
        let line = serde_json::to_string(&item).unwrap();

        // THEN
        assert_eq!(
            "{\"start\":1667297163,\"end\":{\"At\":1672567319},\"activity\":\"test again\"}",
            line
        );
    }

    #[test]
    fn parses_and_store_example_lines() {
        // GIVEN
        let lines = vec!["2017-03-24_18:14:27 2017-03-25_16:34:45 sdfsadfsdfsafasdsdfsadfsdfsafasdsdfsadfsdfsafasdsdfsadfsdfsafasdsdfsadfsdfsafasdsdfsadfsdfsafasdsdfsadfsdfsafasdsdfsadfsdfsafasdsdfsadfsdfsafasdsdfsadfsdfsafasd",
         "2017-03-25_16:34:45 2017-03-30_14:58:30 http://stackoverflow.com/questions/163360/regular-expression-to-match-urls-in-java",
          "2017-03-22_10:00:00 2017-03-22_20:00:00 asdfsdfi"];

        // WHEN
        let result: Vec<String> = lines
            .iter()
            .map(|l| TimeTrackingItem::from_str(l).unwrap())
            .map(|t| t.to_storage_line())
            .collect();

        // THEN
        assert_eq!(lines, result);
    }

    #[test]
    fn open_should_be_same_as_open() {
        assert_eq!(
            Ending::Open.partial_cmp(&Ending::Open),
            Some(Ordering::Equal)
        );
    }

    #[test]
    fn open_should_be_after_any_date() {
        assert_eq!(
            Ending::Open.partial_cmp(&Utc.ymd(99999, 12, 31).and_hms(12, 12, 12)),
            Some(Ordering::Greater)
        );
    }

    #[test]
    fn ending_at_should_be_comparable() {
        assert_eq!(
            Ending::At(Utc.ymd(1, 1, 1).and_hms(1, 1, 1))
                .partial_cmp(&Ending::At(Utc.ymd(99999, 12, 31).and_hms(12, 12, 12))),
            Some(Ordering::Less)
        );
    }
}
