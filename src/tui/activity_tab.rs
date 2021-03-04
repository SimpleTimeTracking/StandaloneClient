use crate::tti::*;
use crate::tui::{component::*, history_list::*, textfield::*};
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
    pub fn into_frame<I: IntoIterator<Item = TimeTrackingItem>>(
        self,
        items: I,
    ) -> ActivityTabFrame {
        let items = items.into_iter();
        let filter = self.filter_field.get_text();
        let filtered: Vec<_> = if !filter.is_empty() {
            let regex = Regex::new(&format!("(?i){}", filter));
            if let Ok(regex) = regex {
                items
                    .filter(|i| regex.find(&i.activity).is_some())
                    .collect()
            } else {
                items.collect()
            }
        } else {
            items.collect()
        };

        ActivityTabFrame {
            activity_field: self.activity_field,
            history_list: self.history_list.into_frame(filtered),
            mode: self.mode,
            filter_field: self.filter_field,
        }
    }
}

pub struct ActivityTabFrame {
    activity_field: TextFieldState,
    history_list: HistoryListFrame,
    mode: Mode,
    filter_field: TextFieldState,
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
        let index = if let KeyCode::Char(index @ '1'..='9') = event.code {
            Some(index as usize - '1' as usize)
        } else {
            None
        };
        match self.mode {
            _ if event.code == KeyCode::Esc => {
                self.history_list.reset();
                self.filter_field.clear();
                self.mode = Mode::Normal;
                Consumed::NotConsumed
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
            Mode::Normal | Mode::SearchHistory
                if index
                    .map(|i| self.history_list.get_item_with_index(i))
                    .flatten()
                    .is_some() =>
            {
                self.mode = Mode::EnteringActivity;
                self.activity_field.set_text(
                    &self
                        .history_list
                        .get_item_with_index(index.unwrap())
                        .unwrap()
                        .activity,
                );
                self.activity_field.end();
                self.history_list.reset();
                Consumed::Consumed
            }
            Mode::Normal => match event.code {
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
                if show_filter {
                    Constraint::Length(1)
                } else {
                    Constraint::Length(0)
                },
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

        let textfield = TextField;
        if state.mode == Mode::EnteringActivity {}
        textfield.render(textfield_area, buf, &mut state.activity_field);

        let history_list = HistoryList;
        state
            .history_list
            .set_show_indices(state.mode != Mode::EnteringActivity);
        history_list.render(chunks.next().unwrap(), buf, &mut state.history_list);

        if show_filter {
            let search_field =
                Block::default().title(format!("r? {}", state.filter_field.get_text()));
            search_field.render(chunks.next().unwrap(), buf);
        }
    }
}
