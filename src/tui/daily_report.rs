use crate::{
    tui::{
        component::EventHandler, date_picker::DatePickerState, duration_to_string, Consumed,
        DatePicker,
    },
    Ending, TimeTrackingItem,
};
use chrono::{Duration, Local};
use clipboard::ClipboardProvider;
use crossterm::event::{KeyCode, KeyEvent, KeyModifiers};
use itertools::Itertools;
use tui::{
    buffer::Buffer,
    layout::{Constraint, Direction, Layout, Rect},
    style::{Color, Modifier, Style},
    widgets::{Row, StatefulWidget, Table, TableState},
};

pub struct DailyReportState {
    pub duration_activity_list_state: DurationActivityListState,
    pub date_picker_state: DatePickerState,
}

impl DailyReportState {
    pub fn new() -> Self {
        Self {
            duration_activity_list_state: DurationActivityListState::new(),
            date_picker_state: DatePickerState::new(),
        }
    }
}

pub struct DailyReport {
    duration_activity_list: DurationActivityList,
}

impl DailyReport {
    pub fn new(items: &[TimeTrackingItem], state: &DailyReportState) -> Self {
        let now = Local::now();
        let to_duration = |a: &TimeTrackingItem| {
            (match a.end {
                Ending::Open => now,
                Ending::At(time) => time.into(),
            })
            .signed_duration_since(a.start)
        };
        let items: Vec<_> = items
            .iter()
            .filter(|a| a.start.date().naive_local() == state.date_picker_state.get_selected())
            .sorted_by(|a, b| a.activity.cmp(&b.activity))
            .map(|a| (&a.activity, to_duration(a)))
            .group_by(|a| a.0)
            .into_iter()
            .map(|(activity, i)| ActivityWithDuration {
                activity: activity.to_owned(),
                duration: i.fold(Duration::zero(), |acc, a| acc + a.1),
            })
            .collect();
        Self {
            duration_activity_list: DurationActivityList::new(items),
        }
    }
}

impl EventHandler for DailyReportState {
    type State = DailyReportState;

    fn handle_event(&mut self, event: KeyEvent, state: &mut Self::State) -> Consumed {
        state.date_picker_state.handle_event(event, &mut ());
        self.duration_activity_list_state
            .handle_event(event, &mut state.duration_activity_list_state);
        Consumed::Consumed
    }
}

impl StatefulWidget for DailyReport {
    type State = DailyReportState;

    fn render(self, area: Rect, buf: &mut Buffer, state: &mut Self::State) {
        let mut chunks = Layout::default()
            .constraints([Constraint::Min(3 * 7), Constraint::Min(0)])
            .direction(Direction::Horizontal)
            .split(area)
            .into_iter();

        self.date_picker_state
            .render(chunks.next().unwrap(), buf, &mut state.date_picker_state);
        self.duration_activity_list.render(
            chunks.next().unwrap(),
            buf,
            &mut state.duration_activity_list_state,
        )
    }
}

struct ActivityWithDuration {
    activity: String,
    duration: Duration,
}

pub struct DurationActivityListState {
    selected: Option<usize>,
}

pub struct DurationActivityListEmphemeralState {
    state: DurationActivityListState,
    items: Vec<ActivityWithDuration>,
}

impl DurationActivityListState {
    pub fn new() -> Self {
        Self { selected: None }
    }

    pub fn select(&mut self, selected: Option<usize>) {
        self.selected = selected;
    }
}

pub struct DurationActivityList {}

impl DurationActivityList {
    pub fn new(items: Vec<ActivityWithDuration>) -> Self {
        Self { items }
    }
}

impl EventHandler for DurationActivityListState {
    type State = DurationActivityListEmphemeralState;

    fn handle_event(&mut self, event: KeyEvent, state: &mut Self::State) -> Consumed {
        match event.code {
            KeyCode::Down => state.selected = Some(state.selected.map(|i| i + 1).unwrap_or(0)),
            KeyCode::Up => {
                state.selected = Some(state.selected.map(|i| i.saturating_sub(1)).unwrap_or(0))
            }
            KeyCode::Char('d') => {
                if let Some(item) = state.selected.map(|i| self.items.get(i)).flatten() {
                    let mut ctx: clipboard::ClipboardContext = ClipboardProvider::new().unwrap();
                    ctx.set_contents(duration_to_string(&item.duration))
                        .unwrap()
                }
            }
            KeyCode::Char('a') => {
                if let Some(item) = state.selected.map(|i| self.items.get(i)).flatten() {
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
            _ => (),
        };
        Consumed::Consumed
    }
}

impl StatefulWidget for DurationActivityList {
    type State = DurationActivityListState;

    fn render(self, area: Rect, buf: &mut Buffer, state: &mut Self::State) {
        let rows: Vec<_> = self
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
        let num_rows = rows.len();
        let table = Table::new(rows)
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
        state.select(state.selected.map(|index| index.min(num_rows - 1)));
        let mut table_state = TableState::default();
        table_state.select(state.selected);
        StatefulWidget::render(table, area, buf, &mut table_state);
    }
}
