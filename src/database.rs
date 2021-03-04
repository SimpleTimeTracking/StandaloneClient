use crate::tti::*;
use chrono::prelude::*;
use std::fs::File;
use std::io::{BufRead, BufReader, BufWriter, Write};
use std::path::PathBuf;
use std::str::FromStr;
#[cfg(LOG_TIMES)]
use std::time::Instant;

pub trait Connection
where
    Self: IntoIterator<Item = TimeTrackingItem>,
{
    fn insert_item(&mut self, item: TimeTrackingItem);
    fn delete_item(&mut self, item: &TimeTrackingItem) -> Result<(), String>;
    fn query_n(&self, limit: usize) -> Vec<&TimeTrackingItem>;
    fn query_latest(&self) -> Option<&TimeTrackingItem>;
    fn query(&self) -> Vec<&TimeTrackingItem>;
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
        let file = match File::open(&stt_file) {
            Ok(file) => file,
            Err(_) => {
                return Ok(Database {
                    stt_file,
                    content: vec![],
                })
            }
        };
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

    pub fn flush(&self) {
        let parent = self.stt_file.parent().unwrap();
        std::fs::create_dir_all(parent).unwrap();
        let file = File::create(&self.stt_file).unwrap();
        let mut writer = BufWriter::new(file);
        #[cfg(LOG_TIMES)]
        let i = Instant::now();
        for line in &self.content {
            writeln!(writer, "{}", line.to_storage_line()).unwrap();
        }
        #[cfg(LOG_TIMES)]
        println!("Writing DB: {} ms", i.elapsed().as_millis());
    }
}

impl Drop for Database {
    fn drop(&mut self) {
        self.flush();
    }
}

trait Items<'a>: Iterator<Item = &'a TimeTrackingItem> + Sized {}

impl Connection for Vec<TimeTrackingItem> {
    fn query(&self) -> Vec<&TimeTrackingItem> {
        self.iter().rev().collect()
    }

    fn query_n(&self, limit: usize) -> Vec<&TimeTrackingItem> {
        self.iter().rev().take(limit).collect()
    }

    fn query_latest(&self) -> Option<&TimeTrackingItem> {
        self.last()
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

    fn delete_item(&mut self, item: &TimeTrackingItem) -> Result<(), String> {
        for i in 0..self.len() {
            if self[i] == *item {
                self.remove(i);
                let len = self.len();
                let last_item = &mut self[i - 1];
                if i > 0 && last_item.end == item.start {
                    match item.end {
                        Ending::Open => (),
                        Ending::At(end) => {
                            if i < len && item.start.num_days_from_ce() == end.num_days_from_ce() {
                                last_item.end = item.end;
                            }
                        }
                    }
                }
                return Ok(());
            }
        }
        Err(format!("Item to delete {:?} not found!", item))
    }
}

#[cfg(test)]
mod test {
    use crate::database::*;

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
        times.delete_item(&times[1].clone()).unwrap();

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
        times.delete_item(&times[1].clone()).unwrap();

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
        times.delete_item(&times[1].clone()).unwrap();

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
        times.delete_item(&times[1].clone()).unwrap();

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
}
