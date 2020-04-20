use chrono::format::{Item, ParseResult, Parsed};
use chrono::prelude::*;
use chrono::serde::ts_seconds;
use lazy_static::lazy_static;
use serde::Deserializer;
use serde::Serializer;
use serde::{Deserialize, Serialize};
use std::fmt::Display;
use std::str::FromStr;

#[derive(Deserialize, Serialize)]
pub struct TimeTrackingItem {
    #[serde(with = "ts_seconds")]
    start: DateTime<Utc>,
    #[serde(
        default,
        deserialize_with = "de_opt_date_time",
        serialize_with = "ser_opt_date_time"
    )]
    end: Option<DateTime<Utc>>,
    activity: String,
}

fn de_opt_date_time<'de, D>(deserializer: D) -> Result<Option<DateTime<Utc>>, D::Error>
where
    D: Deserializer<'de>,
{
    chrono::serde::ts_seconds::deserialize(deserializer).map(Some)
}

fn ser_opt_date_time<S>(t: &Option<DateTime<Utc>>, serializer: S) -> Result<S::Ok, S::Error>
where
    S: Serializer,
{
    if let Some(dt) = t {
        chrono::serde::ts_seconds::serialize(dt, serializer)
    } else {
        serializer.serialize_none()
    }
}

impl TimeTrackingItem {
    fn starting_at(start: DateTime<Local>, activity: &str) -> Self {
        TimeTrackingItem {
            start: DateTime::<Utc>::from(start.with_nanosecond(0).unwrap()),
            end: Option::None,
            activity: activity.to_string(),
        }
    }

    fn interval(
        start: DateTime<Local>,
        end: DateTime<Local>,
        activity: &str,
    ) -> Result<Self, ItemError> {
        if end < start {
            Err(ItemError(ItemErrorKind::StartAfterEnd))
        } else {
            Ok(TimeTrackingItem {
                start: DateTime::<Utc>::from(start.with_nanosecond(0).unwrap()),
                end: Some(DateTime::<Utc>::from(end.with_nanosecond(0).unwrap())),
                activity: activity.to_string(),
            })
        }
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
                    &s.get(40..)
                        .ok_or(ItemError(ItemErrorKind::ActivityMissing))?,
                )
            } else {
                Ok(TimeTrackingItem::starting_at(
                    start,
                    &s.get(20..)
                        .ok_or(ItemError(ItemErrorKind::ActivityMissing))?,
                ))
            }
        } else {
            Ok(TimeTrackingItem::starting_at(
                start,
                &s.get(20..)
                    .ok_or(ItemError(ItemErrorKind::ActivityMissing))?,
            ))
        }
    }
}

impl TimeTrackingItem {
    fn to_storage_line(&self) -> String {
        let start = DateTime::<Local>::from(self.start).format_with_items(FORMAT.iter().cloned());
        if let Some(end) = self.end {
            format!(
                "{start} {end} {activity}",
                start = start,
                end = DateTime::<Local>::from(end).format_with_items(FORMAT.iter().cloned()),
                activity = self.activity
            )
        } else {
            format!(
                "{start} {activity}",
                start = start,
                activity = self.activity
            )
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
    use crate::*;
    use chrono::prelude::*;

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
        assert_ne!(sut.end.unwrap(), end);
    }

    #[test]
    fn should_parse_interval() {
        // GIVEN

        // WHEN
        let result =
            TimeTrackingItem::from_str("2017-07-01_21:06:03 2017-07-01_21:06:11 bla bla blub blub")
                .unwrap();

        // THEN
        assert_eq!(result.start, Local.ymd(2017, 7, 1).and_hms(21, 6, 3));
        assert_eq!(
            result.end.unwrap(),
            Local.ymd(2017, 7, 1).and_hms(21, 6, 11)
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
        assert_eq!(result.end, None);
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
    fn without_end_should_serialize_to_json() {
        // GIVEN
        let item = TimeTrackingItem::starting_at(Local.ymd(2022, 11, 1).and_hms(11, 6, 3), "test");

        // WHEN
        let line = serde_json::to_string(&item).unwrap();

        // THEN
        assert_eq!(
            "{\"start\":1667297163,\"end\":null,\"activity\":\"test\"}",
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
            "{\"start\":1667297163,\"end\":1672567319,\"activity\":\"test again\"}",
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
}
