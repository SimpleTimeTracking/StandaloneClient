use crate::{tui::duration_to_string, Ending, TimeTrackingItem};
use chrono::{Duration, Local};
use clipboard::ClipboardProvider;
use crossterm::event::{KeyCode, KeyEvent, KeyModifiers};
use itertools::Itertools;
use tui::{
    layout::{Constraint, Direction, Layout, Rect},
    style::{Color, Modifier, Style},
    widgets::{Row, StatefulWidget, Table, TableState},
};

pub struct DailyReportState {
    pub condensed_list: CondensedActivityListState,
}

impl DailyReportState {
    pub fn new() -> Self {
        Self {
            condensed_list: CondensedActivityListState::new(),
        }
    }

    pub fn handle_event(&mut self, event: KeyEvent) {
        self.condensed_list.handle_event(event);
    }
}

pub struct DailyReport;

impl DailyReport {
    pub fn new() -> Self {
        Self
    }
}

impl StatefulWidget for DailyReport {
    type State = DailyReportState;

    fn render(self, area: Rect, buf: &mut tui::buffer::Buffer, state: &mut Self::State) {
        let mut chunks = Layout::default()
            .constraints([Constraint::Min(3 * 7), Constraint::Min(0)])
            .direction(Direction::Vertical)
            .split(area)
            .into_iter();

        let list = CondensedActivityList::new();
        list.render(chunks.next().unwrap(), buf, &mut state.condensed_list)
    }
}

struct ActivityWithDuration {
    activity: String,
    duration: Duration,
}

pub struct CondensedActivityListState {
    selected: Option<usize>,
    items: Vec<ActivityWithDuration>,
}

impl CondensedActivityListState {
    pub fn new() -> Self {
        Self {
            selected: None,
            items: vec![],
        }
    }

    pub fn set_items(&mut self, mut items: Vec<&TimeTrackingItem>) {
        let now = Local::now();
        let to_duration = |a: &TimeTrackingItem| {
            (match a.end {
                Ending::Open => now,
                Ending::At(time) => time.into(),
            })
            .signed_duration_since(a.start)
        };
        items.sort_by(|a, b| a.activity.cmp(&b.activity));
        self.items = items
            .iter()
            .map(|a| (&a.activity, to_duration(a)))
            .group_by(|a| a.0)
            .into_iter()
            .map(|(activity, i)| ActivityWithDuration {
                activity: activity.to_owned(),
                duration: i.fold(Duration::zero(), |acc, a| acc + a.1),
            })
            .collect();
    }

    pub fn select(&mut self, selected: Option<usize>) {
        self.selected = selected;
    }

    pub fn handle_event(&mut self, event: KeyEvent) {
        match event.code {
            KeyCode::Down => self.selected = Some(self.selected.map(|i| i + 1).unwrap_or(0)),
            KeyCode::Up => {
                self.selected = Some(self.selected.map(|i| i.saturating_sub(1)).unwrap_or(0))
            }
            KeyCode::Char('d') => {
                if let Some(item) = self.selected.map(|i| self.items.get(i)).flatten() {
                    let mut ctx: clipboard::ClipboardContext = ClipboardProvider::new().unwrap();
                    ctx.set_contents(duration_to_string(&item.duration))
                        .unwrap()
                }
            }
            KeyCode::Char('a') => {
                if let Some(item) = self.selected.map(|i| self.items.get(i)).flatten() {
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
        }
    }
}

pub struct CondensedActivityList;

impl CondensedActivityList {
    pub fn new() -> Self {
        Self
    }
}

impl StatefulWidget for CondensedActivityList {
    type State = CondensedActivityListState;

    fn render(self, area: Rect, buf: &mut tui::buffer::Buffer, state: &mut Self::State) {
        let rows: Vec<_> = state
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
        if let Some(index) = state.selected {
            state.select(Some(index.min(num_rows - 1)));
        }
        let mut table_state = TableState::default();
        table_state.select(state.selected);
        StatefulWidget::render(table, area, buf, &mut table_state);
    }
}
