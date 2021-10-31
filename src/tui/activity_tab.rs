use crate::tti::*;
use crate::tui::{component::*, history_list::*, textfield::*};
use chrono::{self, DateTime, Datelike, Local, Utc};
use crossterm::event::{KeyCode, KeyEvent, KeyModifiers};
use regex::Regex;
use tui::{
    backend::Backend,
    buffer::Buffer,
    layout::{Constraint, Layout, Rect},
    style::{Modifier, Style},
    widgets::{Block, Borders, Paragraph, StatefulWidget, Widget},
};

#[derive(Default)]
pub struct ActivityTabState {
    activity_field: TextFieldState,
    history_list: HistoryListState,
    mode: Mode,
    filter_field: TextFieldState,
}

#[derive(PartialEq)]
enum Mode {
    Normal,
    EnteringActivity,
    SearchHistory,
}

impl Default for Mode {
    fn default() -> Self {
        Mode::Normal
    }
}

impl ActivityTabState {
    pub fn into_frame(self, items: &[TimeTrackingItem]) -> ActivityTabFrame {
        let cloned_items_iter = items.iter().cloned();
        let filter = self.filter_field.get_text();
        let filtered: Vec<_> = if !filter.is_empty() {
            let regex = Regex::new(&format!("(?i){}", regex::escape(&filter)));
            if let Ok(regex) = regex {
                cloned_items_iter
                    .filter(|i| regex.find(&i.activity).is_some())
                    .collect()
            } else {
                cloned_items_iter.collect()
            }
        } else {
            cloned_items_iter.collect()
        };

        let now = Local::now();
        let start_of_day = now.date().and_hms(0, 0, 0);
        let start_of_week =
            start_of_day - chrono::Duration::days(now.weekday().num_days_from_monday() as i64);
        let duration_acc = |acc, a: &TimeTrackingItem| {
            acc - a.start.signed_duration_since(match a.end {
                Ending::Open => now,
                Ending::At(time) => time.into(),
            })
        };
        let pause_matcher = Regex::new("(?i).*pause.*").unwrap();
        let work_time_today = items
            .iter()
            .take_while(|a| a.end > start_of_day)
            .filter(|a| !pause_matcher.is_match(&a.activity))
            .fold(chrono::Duration::zero(), duration_acc);
        let work_time_week = items
            .iter()
            .take_while(|a| a.end > start_of_week)
            .filter(|a| !pause_matcher.is_match(&a.activity))
            .fold(chrono::Duration::zero(), duration_acc);
        ActivityTabFrame {
            activity_field: self.activity_field,
            history_list: self.history_list.into_frame(filtered),
            mode: self.mode,
            filter_field: self.filter_field,
            work_time_today,
            work_time_week,
        }
    }
}

pub struct ActivityTabFrame {
    activity_field: TextFieldState,
    history_list: HistoryListFrame,
    mode: Mode,
    filter_field: TextFieldState,
    work_time_today: chrono::Duration,
    work_time_week: chrono::Duration,
}

impl Frame<ActivityTabState> for ActivityTabFrame {
    fn into_state(self) -> ActivityTabState {
        ActivityTabState {
            activity_field: self.activity_field,
            history_list: self.history_list.into_state(),
            mode: self.mode,
            filter_field: self.filter_field,
        }
    }
}

impl EventHandler for ActivityTabFrame {
    fn set_focus<B: Backend>(&self, frame: &mut tui::Frame<B>) {
        match self.mode {
            Mode::EnteringActivity => self.activity_field.set_focus(frame),
            Mode::SearchHistory => self.filter_field.set_focus(frame),
            _ => (),
        }
    }

    fn handle_event(&mut self, event: KeyEvent) -> Consumed {
        match self.mode {
            _ if event.code == KeyCode::Esc => {
                self.history_list.reset();
                self.filter_field.clear();
                if self.mode != Mode::Normal {
                    self.mode = Mode::Normal;
                    Consumed::Consumed
                } else {
                    Consumed::NotConsumed
                }
            }
            Mode::Normal | Mode::SearchHistory
                if event.code == KeyCode::Enter
                    && self.history_list.get_selected_item().is_some() =>
            {
                self.mode = Mode::EnteringActivity;
                self.activity_field
                    .set_text(&self.history_list.get_selected_item().unwrap().activity);
                self.activity_field.end();
                self.history_list.reset();
                Consumed::Consumed
            }
            Mode::Normal => match event.code {
                KeyCode::Char(index @ '1'..='9')
                    if self
                        .history_list
                        .get_item_with_index(index as usize - '1' as usize)
                        .is_some() =>
                {
                    self.mode = Mode::EnteringActivity;
                    self.activity_field.set_text(
                        &self
                            .history_list
                            .get_item_with_index(index as usize - '1' as usize)
                            .unwrap()
                            .activity,
                    );
                    self.activity_field.end();
                    self.history_list.reset();
                    Consumed::Consumed
                }
                KeyCode::Char('/') => {
                    self.history_list.reset();
                    self.filter_field.clear();
                    self.mode = Mode::SearchHistory;
                    Consumed::Consumed
                }
                KeyCode::Char('n') => {
                    self.history_list.reset();
                    self.mode = Mode::EnteringActivity;
                    self.activity_field.clear();
                    Consumed::Consumed
                }
                KeyCode::Char('i') => {
                    self.history_list.reset();
                    self.mode = Mode::EnteringActivity;
                    Consumed::Consumed
                }
                _ => self.history_list.handle_event(event),
            },
            Mode::EnteringActivity => match event.code {
                KeyCode::Enter | KeyCode::Char('\r')
                    if event
                        .modifiers
                        .intersects(KeyModifiers::ALT | KeyModifiers::CONTROL) =>
                {
                    crate::commands::command(&self.activity_field.get_text())
                        .map(|(_, command)| {
                            self.activity_field.clear();
                            Consumed::Command(command)
                        })
                        .unwrap_or(Consumed::NotConsumed)
                }
                _ => self.activity_field.handle_event(event),
            },
            Mode::SearchHistory => {
                let result = self.history_list.handle_event(event);
                if !result.is_consumed() {
                    if event.code == KeyCode::Enter {
                        self.mode = Mode::Normal;
                        Consumed::Consumed
                    } else {
                        self.filter_field.handle_event(event)
                    }
                } else {
                    result
                }
            }
        }
    }
}

pub struct ActivityTab;

impl StatefulWidget for ActivityTab {
    type State = ActivityTabFrame;

    fn render(self, area: Rect, buf: &mut Buffer, state: &mut Self::State) {
        let show_filter = state.mode == Mode::SearchHistory || state.filter_field.has_text();
        let mut chunks = Layout::default()
            .constraints([
                Constraint::Length(5),
                Constraint::Min(0),
                Constraint::Length(1),
            ])
            .split(area)
            .into_iter();

        let textfield_block_area = chunks.next().unwrap();
        let textfield_block = Block::default().title("Activity").borders(Borders::ALL);
        let textfield_block = if state.mode == Mode::EnteringActivity {
            textfield_block.border_style(Style::default().add_modifier(Modifier::BOLD))
        } else {
            textfield_block
        };
        let textfield_area = textfield_block.inner(textfield_block_area);
        textfield_block.render(textfield_block_area, buf);

        let mut activity_status_area = textfield_area;
        activity_status_area.y += activity_status_area.height;
        activity_status_area.x += 1;
        activity_status_area.height = 1;
        activity_status_area.width -= 1;
        let work_time_today =
            (DateTime::<Utc>::from(std::time::UNIX_EPOCH) + state.work_time_today).format("%T");
        let activity_status_line = Block::default().title(format!(
            "Tracked time today: {} week: {}:{:02}:{:02}",
            work_time_today,
            state.work_time_week.num_hours(),
            state.work_time_week.num_minutes() % 60,
            state.work_time_week.num_seconds() % 60
        ));
        activity_status_line.render(activity_status_area, buf);

        let textfield = TextField;
        if state.mode == Mode::EnteringActivity {}
        textfield.render(textfield_area, buf, &mut state.activity_field);

        let history_list = HistoryList;
        state
            .history_list
            .set_show_indices(state.mode != Mode::EnteringActivity);
        history_list.render(chunks.next().unwrap(), buf, &mut state.history_list);

        let mut area = chunks.next().unwrap();
        if show_filter {
            let search_field = TextField;
            buf.set_string(area.x, area.y, "/", Style::default());
            area.x += 1;
            area.width -= 1;
            search_field.render(area, buf, &mut state.filter_field);
        } else {
            let text = match state.mode {
                Mode::Normal => {
                    "Esc: Exit mode, [1]-[9]: Use activity, '/': Filter, up/down: Select, Enter: Use selected, 'i': insert, 'n': new"
                }
                Mode::EnteringActivity => "Esc: Exit mode, Ctrl/Alt+Enter: Apply",
                Mode::SearchHistory => panic!("Should not try to render status line for search"),
            };
            let status_line = Paragraph::new(text);
            status_line.render(area, buf);
        }
    }
}
