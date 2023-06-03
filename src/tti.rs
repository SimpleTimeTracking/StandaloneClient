use chrono::format::{Item, ParseResult, Parsed};
use chrono::naive::serde::ts_seconds;
use chrono::prelude::*;
use chrono::NaiveDateTime;
use core::fmt::Display;
use lazy_static::lazy_static;
use serde::{Deserialize, Serialize};
use std::cmp::Ordering;
use std::str::FromStr;

#[derive(Deserialize, Serialize, Clone, Debug, Eq, PartialEq)]
pub struct TimeTrackingItem {
    #[serde(with = "ts_seconds")]
    pub start: NaiveDateTime,
    pub end: Ending,
    pub activity: String,
}

#[derive(Deserialize, Serialize, Copy, Clone, Debug, Eq, PartialEq)]
pub enum Ending {
    Open,
    At(#[serde(with = "ts_seconds")] NaiveDateTime),
}

impl Ending {
    pub fn as_date_time(&self) -> Option<NaiveDateTime> {
        match self {
            Ending::Open => None,
            Ending::At(dt) => Some(*dt),
        }
    }
}

impl PartialEq<NaiveDateTime> for Ending {
    fn eq(&self, other: &NaiveDateTime) -> bool {
        match self {
            Ending::Open => false,
            Ending::At(ts) => ts == other,
        }
    }
}

impl PartialOrd<NaiveDateTime> for Ending {
    fn partial_cmp(&self, other: &NaiveDateTime) -> Option<Ordering> {
        match self {
            Ending::Open => Some(Ordering::Greater),
            Ending::At(ts) => Some(ts.timestamp().cmp(&other.timestamp())),
        }
    }
}

impl PartialEq<Ending> for NaiveDateTime {
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
    pub fn starting_at<S: Into<String>>(start: NaiveDateTime, activity: S) -> Self {
        TimeTrackingItem {
            start: start.with_nanosecond(0).unwrap(),
            end: Ending::Open,
            activity: activity.into(),
        }
    }

    pub fn interval<S: Into<String>>(
        start: NaiveDateTime,
        end: NaiveDateTime,
        activity: S,
    ) -> Result<Self, ItemError> {
        if end < start {
            Err(ItemError(ItemErrorKind::StartAfterEnd))
        } else {
            Ok(TimeTrackingItem {
                start: start.with_nanosecond(0).unwrap(),
                end: Ending::At(end.with_nanosecond(0).unwrap()),
                activity: activity.into(),
            })
        }
    }

    pub fn to_storage_line(&self) -> String {
        let start = self.start.format_with_items(FORMAT.iter().cloned());
        let activity_escaped = self.activity.replace('\\', "\\\\").replace('\n', "\\n");
        if let Ending::At(end) = self.end {
            format!(
                "{start} {end} {activity}",
                start = start,
                end = end.format_with_items(FORMAT.iter().cloned()),
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
        let start = parse_stt_date_time(s.get(0..19).ok_or(ItemError(ItemErrorKind::BadFormat))?)
            .map_err(|_| ItemError(ItemErrorKind::BadFormat))?;
        if let Some(end) = s.get(20..39) {
            let end = parse_stt_date_time(end);
            if let Ok(end) = end {
                TimeTrackingItem::interval(
                    start,
                    end,
                    Self::unescape(
                        s.get(40..)
                            .ok_or(ItemError(ItemErrorKind::ActivityMissing))?,
                    ),
                )
            } else {
                Ok(TimeTrackingItem::starting_at(
                    start,
                    Self::unescape(
                        s.get(20..)
                            .ok_or(ItemError(ItemErrorKind::ActivityMissing))?,
                    ),
                ))
            }
        } else {
            Ok(TimeTrackingItem::starting_at(
                start,
                Self::unescape(
                    s.get(20..)
                        .ok_or(ItemError(ItemErrorKind::ActivityMissing))?,
                ),
            ))
        }
    }
}

fn parse_stt_date_time(s: &str) -> ParseResult<NaiveDateTime> {
    let mut parsed = Parsed::new();
    chrono::format::parse(&mut parsed, s, FORMAT.iter().cloned())?;
    parsed.to_naive_datetime_with_offset(0)
}

#[cfg(test)]
mod test {
    use crate::tti::*;

    #[test]
    fn should_round_start_to_second() {
        // GIVEN
        let start = NaiveDateTime::from_timestamp_opt(0, 1).unwrap();

        // WHEN
        let sut = TimeTrackingItem::starting_at(start, "");

        // THEN
        assert_ne!(sut.start, start);
    }

    #[test]
    fn should_round_interval_to_second() {
        // GIVEN
        let start = NaiveDateTime::from_timestamp_opt(9999, 9999).unwrap();
        let end = NaiveDateTime::from_timestamp_opt(99999999, 9999999).unwrap();

        // WHEN
        let sut = TimeTrackingItem::interval(start, end, "").unwrap();

        // THEN
        assert_ne!(sut.start, start);
        assert_ne!(sut.end, Ending::At(end));
    }

    #[test]
    fn should_parse_interval() {
        // GIVEN

        // WHEN
        let result: TimeTrackingItem = "2017-07-01_21:06:03 2017-07-01_21:06:11 bla bla blub blub"
            .parse()
            .unwrap();

        // THEN
        assert_eq!(
            result.start,
            NaiveDate::from_ymd_opt(2017, 7, 1)
                .unwrap()
                .and_hms_opt(21, 6, 3)
                .unwrap()
        );
        assert_eq!(
            result.end,
            Ending::At(
                NaiveDate::from_ymd_opt(2017, 7, 1)
                    .unwrap()
                    .and_hms_opt(21, 6, 11)
                    .unwrap()
                    .into()
            )
        );
        assert_eq!(result.activity, "bla bla blub blub")
    }

    #[test]
    fn should_parse_start_only() {
        // GIVEN

        // WHEN
        let result = TimeTrackingItem::from_str("2022-11-01_11:06:03 bla").unwrap();

        // THEN
        assert_eq!(
            result.start,
            NaiveDate::from_ymd_opt(2022, 11, 1)
                .unwrap()
                .and_hms_opt(11, 6, 3)
                .unwrap()
        );
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
            NaiveDate::from_ymd_opt(2022, 11, 1)
                .unwrap()
                .and_hms_opt(11, 6, 3)
                .unwrap(),
            NaiveDate::from_ymd_opt(2020, 11, 1)
                .unwrap()
                .and_hms_opt(11, 6, 3)
                .unwrap(),
            "test",
        )
        .unwrap();

        //THEN
        // expected fail
    }

    #[test]
    fn should_print_in_storage_format() {
        // GIVEN
        let item = TimeTrackingItem::starting_at(
            NaiveDate::from_ymd_opt(2022, 11, 1)
                .unwrap()
                .and_hms_opt(11, 6, 3)
                .unwrap(),
            "test",
        );

        // WHEN
        let line = item.to_storage_line();

        // THEN
        assert_eq!("2022-11-01_11:06:03 test", line);
    }

    #[test]
    fn should_escape_cr_in_storage_format() {
        // GIVEN
        let item = TimeTrackingItem::starting_at(
            NaiveDate::from_ymd_opt(2022, 11, 1)
                .unwrap()
                .and_hms_opt(11, 6, 3)
                .unwrap(),
            "test\ntest\\",
        );

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
        let item = TimeTrackingItem::starting_at(
            NaiveDate::from_ymd_opt(2022, 11, 1)
                .unwrap()
                .and_hms_opt(11, 6, 3)
                .unwrap(),
            "test",
        );

        // WHEN
        let line = serde_json::to_string(&item).unwrap();

        // THEN
        assert_eq!(
            "{\"start\":1667300763,\"end\":\"Open\",\"activity\":\"test\"}",
            line
        );
    }

    #[test]
    fn with_end_should_serialize_to_json() {
        // GIVEN
        let item = TimeTrackingItem::interval(
            NaiveDate::from_ymd_opt(2022, 11, 1)
                .unwrap()
                .and_hms_opt(11, 6, 3)
                .unwrap(),
            NaiveDate::from_ymd_opt(2023, 1, 1)
                .unwrap()
                .and_hms_opt(11, 1, 59)
                .unwrap(),
            "test again",
        )
        .unwrap();

        // WHEN
        let line = serde_json::to_string(&item).unwrap();

        // THEN
        assert_eq!(
            "{\"start\":1667300763,\"end\":{\"At\":1672570919},\"activity\":\"test again\"}",
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
            Ending::Open.partial_cmp(
                &NaiveDate::from_ymd_opt(99999, 12, 31)
                    .unwrap()
                    .and_hms_opt(12, 12, 12)
                    .unwrap()
            ),
            Some(Ordering::Greater)
        );
    }

    #[test]
    fn ending_at_should_be_comparable() {
        assert_eq!(
            Ending::At(
                NaiveDate::from_ymd_opt(1, 1, 1)
                    .unwrap()
                    .and_hms_opt(1, 1, 1)
                    .unwrap()
            )
            .partial_cmp(&Ending::At(
                NaiveDate::from_ymd_opt(99999, 12, 31)
                    .unwrap()
                    .and_hms_opt(12, 12, 12)
                    .unwrap()
            )),
            Some(Ordering::Less)
        );
    }
}
