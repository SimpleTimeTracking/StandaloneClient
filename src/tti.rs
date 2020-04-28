use chrono::format::{Item, ParseResult, Parsed};
use chrono::prelude::*;
use chrono::serde::ts_seconds;
use lazy_static::lazy_static;
use serde::{Deserialize, Serialize};
use std::cmp::Ordering;
use std::fs::File;
use std::io::{BufRead, BufReader, BufWriter, Write};
use std::path::PathBuf;
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

impl PartialEq<DateTime<Utc>> for Ending {
    fn eq(&self, other: &DateTime<Utc>) -> bool {
        match self {
            Ending::Open => false,
            Ending::At(ts) => ts == other,
        }
    }
}

impl PartialOrd<DateTime<Utc>> for Ending {
    fn partial_cmp(&self, other: &DateTime<Utc>) -> Option<Ordering> {
        match self {
            Ending::Open => Some(Ordering::Greater),
            Ending::At(ts) => Some(ts.cmp(other)),
        }
    }
}

impl PartialOrd<Ending> for DateTime<Utc> {
    fn partial_cmp(&self, other: &Ending) -> Option<Ordering> {
        match other {
            Ending::Open => None,
            Ending::At(ts) => Some(self.cmp(ts)),
        }
    }
}

impl PartialEq<Ending> for DateTime<Utc> {
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
    pub fn starting_at(start: DateTime<Local>, activity: &str) -> Self {
        TimeTrackingItem {
            start: DateTime::<Utc>::from(start.with_nanosecond(0).unwrap()),
            end: Ending::Open,
            activity: activity.to_string(),
        }
    }

    pub fn interval(
        start: DateTime<Local>,
        end: DateTime<Local>,
        activity: &str,
    ) -> Result<Self, ItemError> {
        if end < start {
            Err(ItemError(ItemErrorKind::StartAfterEnd))
        } else {
            Ok(TimeTrackingItem {
                start: DateTime::<Utc>::from(start.with_nanosecond(0).unwrap()),
                end: Ending::At(DateTime::<Utc>::from(end.with_nanosecond(0).unwrap())),
                activity: activity.to_string(),
            })
        }
    }

    fn to_storage_line(&self) -> String {
        let start = DateTime::<Local>::from(self.start).format_with_items(FORMAT.iter().cloned());
        if let Ending::At(end) = self.end {
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

fn parse_stt_date_time(s: &str) -> ParseResult<DateTime<Local>> {
    let mut parsed = Parsed::new();
    chrono::format::parse(&mut parsed, s, FORMAT.iter().cloned())?;
    parsed.to_datetime_with_timezone(&Local)
}

pub trait Connection
where
    Self: IntoIterator<Item = TimeTrackingItem>,
{
    fn insert_item(&mut self, item: TimeTrackingItem);
    fn delete_item(&mut self, item: &TimeTrackingItem);
    fn query_n(&self, limit: usize) -> Vec<TimeTrackingItem>;
}

pub struct Database {
    stt_file: PathBuf,
    content: Vec<TimeTrackingItem>,
}

impl Database {
    pub fn open() -> std::io::Result<Database> {
        let mut stt_file = dirs::home_dir().unwrap();
        stt_file.push(".stt");
        stt_file.push("activities");
        let file = File::open(&stt_file)?;
        let reader = BufReader::new(file);
        let mut content: Vec<TimeTrackingItem> = reader
            .lines()
            .filter_map(|l| TimeTrackingItem::from_str(&l.unwrap()).ok())
            .collect();
        content.sort_by(|a, b| a.start.partial_cmp(&b.start).unwrap());
        Ok(Database { stt_file, content })
    }

    pub fn open_connection(&mut self) -> &mut impl Connection {
        &mut self.content
    }
}

impl Drop for Database {
    fn drop(&mut self) {
        let file = File::create(&self.stt_file).unwrap();
        let mut writer = BufWriter::new(file);
        for line in &self.content {
            writeln!(writer, "{}", line.to_storage_line()).unwrap();
        }
    }
}

impl Connection for Vec<TimeTrackingItem> {
    fn query_n(&self, limit: usize) -> Vec<TimeTrackingItem> {
        let mut query_result = self.clone();
        query_result.reverse();
        query_result.truncate(limit);
        query_result
    }

    fn insert_item(&mut self, item: TimeTrackingItem) {
        let mut i = 0;
        while i < self.len() && self[i].end < item.start {
            i += 1;
        }
        if i < self.len() {
            let last = &mut self[i];
            if last.start < item.start && last.end > item.start {
                let mut after = last.clone();
                last.end = Ending::At(item.start);
                i += 1;
                if let Ending::At(end) = item.end {
                    if after.end > end {
                        after.start = end;
                        self.insert(i, after);
                    }
                }
            } else if last.start >= item.start && last.end <= item.end {
                self.remove(i);
            } else if item.start >= last.end {
                i += 1;
            }
        }
        let item_end = item.end;
        self.insert(i, item);
        i += 1;
        if i < self.len() {
            let last = &mut self[i];
            if let Ending::At(end) = item_end {
                if last.start < item_end && last.end > end {
                    last.start = end;
                }
            }
        }
        while i < self.len() && self[i].end <= item_end {
            self.remove(i);
        }
    }

    fn delete_item(&mut self, item: &TimeTrackingItem) {
        for i in 0..self.len() {
            if self[i] == *item {
                self.remove(i);
                if i > 0 && i < self.len() && self[i - 1].end == item.start {
                    match item.end {
                        Ending::Open => (),
                        Ending::At(end) => {
                            if item.start.num_days_from_ce() == end.num_days_from_ce() {
                                self[i - 1].end = item.end;
                            }
                        }
                    }
                }
                break;
            }
        }
    }
}

#[cfg(test)]
mod test {
    use crate::tti::*;
    use chrono::prelude::*;

    #[test]
    fn should_modify_end_on_overlap() {
        // GIVEN
        let mut times = vec![TimeTrackingItem::starting_at(
            Local.ymd(2020, 10, 10).and_hms(10, 10, 10),
            "test",
        )];
        let to_insert =
            TimeTrackingItem::starting_at(Local.ymd(2020, 11, 10).and_hms(10, 10, 10), "test 2");

        // WHEN
        times.insert_item(to_insert);

        // THEN
        assert_eq!(
            times[0].end,
            Ending::At(Local.ymd(2020, 11, 10).and_hms(10, 10, 10).into())
        );
    }

    #[test]
    fn should_modify_start_on_overlap() {
        // GIVEN
        let mut times = vec![TimeTrackingItem::starting_at(
            Local.ymd(2020, 10, 10).and_hms(10, 10, 10),
            "test",
        )];
        let to_insert = TimeTrackingItem::interval(
            Local.ymd(2020, 10, 10).and_hms(9, 10, 10),
            Local.ymd(2020, 10, 10).and_hms(11, 10, 10),
            "test 2",
        )
        .unwrap();

        // WHEN
        times.insert_item(to_insert);

        // THEN
        assert_eq!(
            times[1].start,
            DateTime::<Utc>::from(Local.ymd(2020, 10, 10).and_hms(11, 10, 10))
        );
    }

    #[test]
    fn should_split_up_overlap() {
        // GIVEN
        let mut times = vec![TimeTrackingItem::starting_at(
            Local.ymd(2020, 10, 10).and_hms(10, 10, 10),
            "test",
        )];
        let to_insert = TimeTrackingItem::interval(
            Local.ymd(2020, 10, 10).and_hms(11, 10, 10),
            Local.ymd(2020, 10, 10).and_hms(12, 10, 10),
            "test 2",
        )
        .unwrap();

        // WHEN
        times.insert_item(to_insert);

        // THEN
        assert_eq!(
            times[0].end,
            Ending::At(Local.ymd(2020, 10, 10).and_hms(11, 10, 10).into())
        );
        assert_eq!(
            times[2].start,
            DateTime::<Utc>::from(Local.ymd(2020, 10, 10).and_hms(12, 10, 10))
        );
    }

    #[test]
    fn should_not_modify_others_on_non_overlapping() {
        // GIVEN
        let mut times = vec![
            TimeTrackingItem::interval(
                Local.ymd(2020, 10, 10).and_hms(10, 10, 10),
                Local.ymd(2020, 10, 10).and_hms(11, 10, 10),
                "test",
            )
            .unwrap(),
            TimeTrackingItem::interval(
                Local.ymd(2020, 10, 10).and_hms(13, 10, 10),
                Local.ymd(2020, 10, 10).and_hms(14, 10, 10),
                "test",
            )
            .unwrap(),
        ];
        let to_insert = TimeTrackingItem::interval(
            Local.ymd(2020, 10, 10).and_hms(11, 10, 10),
            Local.ymd(2020, 10, 10).and_hms(13, 10, 10),
            "test 2",
        )
        .unwrap();

        // WHEN
        times.insert_item(to_insert);

        // THEN
        assert_eq!(
            vec![
                TimeTrackingItem::interval(
                    Local.ymd(2020, 10, 10).and_hms(10, 10, 10),
                    Local.ymd(2020, 10, 10).and_hms(11, 10, 10),
                    "test",
                )
                .unwrap(),
                TimeTrackingItem::interval(
                    Local.ymd(2020, 10, 10).and_hms(11, 10, 10),
                    Local.ymd(2020, 10, 10).and_hms(13, 10, 10),
                    "test 2",
                )
                .unwrap(),
                TimeTrackingItem::interval(
                    Local.ymd(2020, 10, 10).and_hms(13, 10, 10),
                    Local.ymd(2020, 10, 10).and_hms(14, 10, 10),
                    "test",
                )
                .unwrap(),
            ],
            times
        );
    }

    #[test]
    fn should_delete_covered_items() {
        // GIVEN
        let mut times = vec![
            TimeTrackingItem::interval(
                Local.ymd(2020, 10, 10).and_hms(10, 10, 10),
                Local.ymd(2020, 10, 10).and_hms(11, 10, 10),
                "test",
            )
            .unwrap(),
            TimeTrackingItem::interval(
                Local.ymd(2020, 10, 10).and_hms(12, 10, 10),
                Local.ymd(2020, 10, 10).and_hms(13, 10, 10),
                "test 3",
            )
            .unwrap(),
        ];
        let to_insert =
            TimeTrackingItem::starting_at(Local.ymd(2020, 10, 10).and_hms(9, 10, 10), "test 2");

        // WHEN
        times.insert_item(to_insert);

        // THEN
        assert_eq!(
            vec![TimeTrackingItem::starting_at(
                Local.ymd(2020, 10, 10).and_hms(9, 10, 10),
                "test 2",
            )],
            times
        );
    }

    #[test]
    fn should_delete_covered_open_items() {
        // GIVEN
        let mut times = vec![
            TimeTrackingItem::interval(
                Local.ymd(2020, 10, 10).and_hms(12, 10, 10),
                Local.ymd(2020, 10, 10).and_hms(13, 10, 10),
                "test 3",
            )
            .unwrap(),
            TimeTrackingItem::starting_at(Local.ymd(2020, 10, 10).and_hms(14, 10, 10), "test"),
        ];
        let to_insert =
            TimeTrackingItem::starting_at(Local.ymd(2020, 10, 10).and_hms(9, 10, 10), "test 2");

        // WHEN
        times.insert_item(to_insert);

        // THEN
        assert_eq!(
            vec![TimeTrackingItem::starting_at(
                Local.ymd(2020, 10, 10).and_hms(9, 10, 10),
                "test 2",
            )],
            times
        );
    }

    #[test]
    fn should_delete_item() {
        // GIVEN
        let mut times = vec![
            TimeTrackingItem::interval(
                Local.ymd(2020, 10, 10).and_hms(10, 10, 10),
                Local.ymd(2020, 10, 10).and_hms(11, 10, 10),
                "test",
            )
            .unwrap(),
            TimeTrackingItem::interval(
                Local.ymd(2020, 10, 10).and_hms(11, 20, 10),
                Local.ymd(2020, 10, 10).and_hms(13, 00, 10),
                "test 2",
            )
            .unwrap(),
            TimeTrackingItem::interval(
                Local.ymd(2020, 10, 10).and_hms(13, 10, 10),
                Local.ymd(2020, 10, 10).and_hms(14, 10, 10),
                "test",
            )
            .unwrap(),
        ];
        // WHEN
        times.delete_item(&times[1].clone());

        // THEN
        assert_eq!(
            vec![
                TimeTrackingItem::interval(
                    Local.ymd(2020, 10, 10).and_hms(10, 10, 10),
                    Local.ymd(2020, 10, 10).and_hms(11, 10, 10),
                    "test",
                )
                .unwrap(),
                TimeTrackingItem::interval(
                    Local.ymd(2020, 10, 10).and_hms(13, 10, 10),
                    Local.ymd(2020, 10, 10).and_hms(14, 10, 10),
                    "test",
                )
                .unwrap(),
            ],
            times
        );
    }

    #[test]
    fn should_close_new_gap_on_delete() {
        // GIVEN
        let mut times = vec![
            TimeTrackingItem::interval(
                Local.ymd(2020, 10, 10).and_hms(10, 10, 10),
                Local.ymd(2020, 10, 10).and_hms(11, 10, 10),
                "test",
            )
            .unwrap(),
            TimeTrackingItem::interval(
                Local.ymd(2020, 10, 10).and_hms(11, 10, 10),
                Local.ymd(2020, 10, 10).and_hms(13, 10, 10),
                "test 2",
            )
            .unwrap(),
            TimeTrackingItem::interval(
                Local.ymd(2020, 10, 10).and_hms(13, 10, 10),
                Local.ymd(2020, 10, 10).and_hms(14, 10, 10),
                "test",
            )
            .unwrap(),
        ];
        // WHEN
        times.delete_item(&times[1].clone());

        // THEN
        assert_eq!(
            vec![
                TimeTrackingItem::interval(
                    Local.ymd(2020, 10, 10).and_hms(10, 10, 10),
                    Local.ymd(2020, 10, 10).and_hms(13, 10, 10),
                    "test",
                )
                .unwrap(),
                TimeTrackingItem::interval(
                    Local.ymd(2020, 10, 10).and_hms(13, 10, 10),
                    Local.ymd(2020, 10, 10).and_hms(14, 10, 10),
                    "test",
                )
                .unwrap(),
            ],
            times
        );
    }

    #[test]
    fn should_not_close_gap_crossing_midnight() {
        // GIVEN
        let mut times = vec![
            TimeTrackingItem::interval(
                Local.ymd(2020, 10, 10).and_hms(10, 10, 10),
                Local.ymd(2020, 10, 10).and_hms(11, 10, 10),
                "test",
            )
            .unwrap(),
            TimeTrackingItem::interval(
                Local.ymd(2020, 10, 10).and_hms(11, 10, 10),
                Local.ymd(2020, 10, 11).and_hms(13, 10, 10),
                "test 2",
            )
            .unwrap(),
            TimeTrackingItem::interval(
                Local.ymd(2020, 10, 11).and_hms(13, 10, 10),
                Local.ymd(2020, 10, 11).and_hms(14, 10, 10),
                "test",
            )
            .unwrap(),
        ];
        // WHEN
        times.delete_item(&times[1].clone());

        // THEN
        assert_eq!(
            vec![
                TimeTrackingItem::interval(
                    Local.ymd(2020, 10, 10).and_hms(10, 10, 10),
                    Local.ymd(2020, 10, 10).and_hms(11, 10, 10),
                    "test",
                )
                .unwrap(),
                TimeTrackingItem::interval(
                    Local.ymd(2020, 10, 11).and_hms(13, 10, 10),
                    Local.ymd(2020, 10, 11).and_hms(14, 10, 10),
                    "test",
                )
                .unwrap(),
            ],
            times
        );
    }

    #[test]
    fn should_not_close_gap_for_last_item() {
        // GIVEN
        let mut times = vec![
            TimeTrackingItem::interval(
                Local.ymd(2020, 10, 10).and_hms(10, 10, 10),
                Local.ymd(2020, 10, 10).and_hms(11, 10, 10),
                "test",
            )
            .unwrap(),
            TimeTrackingItem::interval(
                Local.ymd(2020, 10, 10).and_hms(11, 10, 10),
                Local.ymd(2020, 10, 10).and_hms(14, 10, 10),
                "test",
            )
            .unwrap(),
        ];
        // WHEN
        times.delete_item(&times[1].clone());

        // THEN
        assert_eq!(
            vec![TimeTrackingItem::interval(
                Local.ymd(2020, 10, 10).and_hms(10, 10, 10),
                Local.ymd(2020, 10, 10).and_hms(11, 10, 10),
                "test",
            )
            .unwrap(),],
            times
        );
    }

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
        let result =
            TimeTrackingItem::from_str("2017-07-01_21:06:03 2017-07-01_21:06:11 bla bla blub blub")
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
