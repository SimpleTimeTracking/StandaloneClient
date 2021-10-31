use crate::{
    tui::{
        component::EventHandler,
        date_picker::{DatePicker, DatePickerState},
        duration_to_string, Consumed, Frame,
    },
    Ending, TimeTrackingItem,
};
use chrono::{DateTime, Duration, Local};
use clipboard::ClipboardProvider;
use crossterm::event::{KeyCode, KeyEvent, KeyModifiers};
use itertools::Itertools;
use tui::{
    buffer::Buffer,
    layout::{Constraint, Direction, Layout, Rect},
    style::{Color, Modifier, Style},
    widgets::{Block, Borders, Row, StatefulWidget, Table, TableState},
};

pub struct DailyReportFrame {
    duration_activity_list: DurationActivityListFrame,
    date_picker_state: DatePickerState,
}

impl Frame<DailyReportState> for DailyReportFrame {
    fn into_state(self) -> DailyReportState {
        DailyReportState {
            duration_activity_list_state: self.duration_activity_list.into_state(),
            date_picker_state: self.date_picker_state,
        }
    }
}

pub struct DailyReport;

impl StatefulWidget for DailyReport {
    type State = DailyReportFrame;

    fn render(self, area: Rect, buf: &mut Buffer, state: &mut Self::State) {
        let mut chunks = Layout::default()
            .constraints([Constraint::Length(3 * 7), Constraint::Min(0)])
            .direction(Direction::Horizontal)
            .split(area)
            .into_iter();

        let date_picker = DatePicker;
        date_picker.render(chunks.next().unwrap(), buf, &mut state.date_picker_state);
        let duration_activity_list = DurationActivityList;
        duration_activity_list.render(
            chunks.next().unwrap(),
            buf,
            &mut state.duration_activity_list,
        )
    }
}

#[derive(Default)]
pub struct DailyReportState {
    pub duration_activity_list_state: DurationActivityListState,
    pub date_picker_state: DatePickerState,
}

impl EventHandler for DailyReportFrame {
    fn handle_event(&mut self, event: KeyEvent) -> Consumed {
        let result = self.date_picker_state.handle_event(event);
        if result.is_consumed() {
            return result;
        }
        self.duration_activity_list.handle_event(event)
    }
}

impl DailyReportState {
    pub fn into_frame(self, items: &[TimeTrackingItem]) -> DailyReportFrame {
        let items: Vec<_> = items
            .iter()
            .filter(|a| a.start.date().naive_local() == self.date_picker_state.get_selected())
            .cloned()
            .collect();
        DailyReportFrame {
            duration_activity_list: self.duration_activity_list_state.into_frame(&items),
            date_picker_state: self.date_picker_state,
        }
    }
}

struct ActivityWithDuration {
    activity: String,
    duration: Duration,
}

#[derive(Default)]
pub struct DurationActivityListState {
    selected: Option<usize>,
}

pub struct DurationActivityListFrame {
    state: DurationActivityListState,
    items: Vec<ActivityWithDuration>,
    start: Option<DateTime<Local>>,
    end: Option<DateTime<Local>>,
}

impl Frame<DurationActivityListState> for DurationActivityListFrame {
    fn into_state(self) -> DurationActivityListState {
        self.state
    }
}

impl DurationActivityListState {
    pub fn select(&mut self, selected: Option<usize>) {
        self.selected = selected;
    }

    pub fn into_frame(self, items: &[TimeTrackingItem]) -> DurationActivityListFrame {
        let now = Local::now();
        let to_duration = |a: &TimeTrackingItem| {
            (match a.end {
                Ending::Open => now,
                Ending::At(time) => time.into(),
            })
            .signed_duration_since(a.start)
        };
        let start = items.last().map(|i| DateTime::<Local>::from(i.start));
        let end = items
            .first()
            .map(|i| i.end.as_date_time().map(DateTime::<Local>::from))
            .flatten();
        let items: Vec<_> = items
            .iter()
            .sorted_by(|a, b| a.activity.cmp(&b.activity))
            .map(|a| (&a.activity, to_duration(a)))
            .group_by(|a| a.0)
            .into_iter()
            .map(|(activity, i)| ActivityWithDuration {
                activity: activity.to_owned(),
                duration: Duration::seconds(
                    (i.fold(Duration::zero(), |acc, a| acc + a.1).num_seconds() + 5 * 30) / 5 / 60
                        * 5
                        * 60,
                ),
            })
            .collect();
        DurationActivityListFrame {
            state: self,
            items,
            start,
            end,
        }
    }
}

pub struct DurationActivityList;

impl EventHandler for DurationActivityListFrame {
    fn handle_event(&mut self, event: KeyEvent) -> Consumed {
        match event.code {
            KeyCode::Down => {
                self.state.selected = Some(self.state.selected.map(|i| i + 1).unwrap_or(0))
            }
            KeyCode::Up => {
                self.state.selected = Some(
                    self.state
                        .selected
                        .map(|i| i.saturating_sub(1))
                        .unwrap_or(0),
                )
            }
            KeyCode::Char('d') => {
                if let Some(item) = self.state.selected.map(|i| self.items.get(i)).flatten() {
                    let mut ctx: clipboard::ClipboardContext = ClipboardProvider::new().unwrap();
                    ctx.set_contents(duration_to_string(&item.duration))
                        .unwrap()
                }
            }
            KeyCode::Char('a') => {
                if let Some(item) = self.state.selected.map(|i| self.items.get(i)).flatten() {
                    let mut ctx: clipboard::ClipboardContext = ClipboardProvider::new().unwrap();
                    ctx.set_contents(item.activity.to_owned()).unwrap()
                }
            }
            KeyCode::Char(n @ '1'..='9') => {
                let index = n as usize - '1' as usize;
                if let Some(item) = self.items.get(index) {
                    let mut ctx: clipboard::ClipboardContext = ClipboardProvider::new().unwrap();
                    if event
                        .modifiers
                        .intersects(KeyModifiers::CONTROL | KeyModifiers::ALT)
                    {
                        ctx.set_contents(item.activity.to_owned()).unwrap()
                    } else {
                        ctx.set_contents(duration_to_string(&item.duration))
                            .unwrap()
                    }
                }
            }
            KeyCode::Char('s') => {
                if let Some(start) = self.start {
                    let mut ctx: clipboard::ClipboardContext = ClipboardProvider::new().unwrap();
                    ctx.set_contents(format!("{}", start.time())).unwrap();
                }
            }
            KeyCode::Char('e') => {
                if let Some(end) = self.end {
                    let mut ctx: clipboard::ClipboardContext = ClipboardProvider::new().unwrap();
                    ctx.set_contents(format!("{}", end.time())).unwrap();
                }
            }
            _ => return Consumed::NotConsumed,
        };
        Consumed::Consumed
    }
}

impl StatefulWidget for DurationActivityList {
    type State = DurationActivityListFrame;

    fn render(self, area: Rect, buf: &mut Buffer, state: &mut Self::State) {
        let mut rows: Vec<_> = state
            .items
            .iter()
            .enumerate()
            .map(|(index, i)| {
                Row::new(vec![
                    format!("[{}]", index + 1),
                    format!("{:>10}", duration_to_string(&i.duration)),
                    i.activity.to_owned(),
                ])
            })
            .collect();
        if let Some(start) = state.start {
            rows.push(Row::new(vec![
                "[s]".to_owned(),
                format!("{:>10}", start.time()),
                "*** START OF DAY ***".to_owned(),
            ]));
        }
        if let Some(end) = state.end {
            rows.push(Row::new(vec![
                "[e]".to_owned(),
                format!("{:>10}", end.time()),
                "*** END OF DAY ***".to_owned(),
            ]));
        }
        let num_rows = rows.len();
        let table = Table::new(rows)
            .block(
                Block::default()
                    .title("Aggregated Times")
                    .borders(Borders::ALL),
            )
            .header(
                Row::new(vec!["Index", "Duration", "Activity"])
                    .style(Style::default().add_modifier(Modifier::BOLD | Modifier::UNDERLINED)),
            )
            .widths(&[
                Constraint::Length(5),
                Constraint::Min(12),
                Constraint::Min(10),
            ])
            .highlight_style(Style::default().bg(Color::Green));
        state
            .state
            .select(state.state.selected.map(|index| index.min(num_rows - 1)));
        let mut table_state = TableState::default();
        table_state.select(state.state.selected);
        StatefulWidget::render(table, area, buf, &mut table_state);
    }
}
