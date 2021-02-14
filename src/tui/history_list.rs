use crate::tui::EventHandler;
use crate::{Ending, TimeTrackingItem};
use chrono::{DateTime, Local, Utc};
use crossterm::event::{KeyCode, KeyEvent};
use regex::Regex;
use std::collections::HashSet;
use tui::buffer::Buffer;
use tui::layout::Rect;
use tui::style::{Color, Style};
use tui::widgets::ListState;
use tui::widgets::{Block, Borders, List, ListItem};
use tui::widgets::{StatefulWidget, Widget};

#[derive(Default)]
pub struct HistoryListState {
    scroll_y: usize,
    page_size: u16,
    selected: Option<usize>,
    indices: Vec<usize>,
}

pub struct HistoryList<'a> {
    items: &'a [TimeTrackingItem],
    filter: Option<Regex>,
    pub show_indices: bool,
}

impl EventHandler for HistoryListState {
    fn handle_event(&mut self, event: KeyEvent) {
        match event.code {
            KeyCode::Up => {
                self.selected = Some(
                    self.selected
                        .map(|i| i.saturating_sub(1))
                        .unwrap_or(0),
                )
            }
            KeyCode::Down => self.selected = Some(self.selected.map(|i| i + 1).unwrap_or(0)),
            KeyCode::PageUp => {
                self.selected = Some(
                    self.selected
                        .map(|i| {
                            if i >= self.page_size as usize {
                                i - self.page_size as usize
                            } else {
                                0
                            }
                        })
                        .unwrap_or(0),
                )
            }
            KeyCode::PageDown => {
                self.selected = Some(
                    self.selected
                        .map(|i| i + self.page_size as usize)
                        .unwrap_or(0),
                )
            }
            KeyCode::Home => self.selected = Some(0),
            KeyCode::End => self.selected = Some(usize::MAX),
            _ => (),
        }
    }
}

impl HistoryListState {
    pub fn reset(&mut self) {
        self.selected = None;
    }

    pub fn get_selected_item(&self) -> Option<usize> {
        self.selected
    }

    pub fn set_selected_item(&mut self, selected: Option<usize>) {
        self.selected = selected;
    }

    pub fn get_item_by_index(&self, index: usize) -> Option<usize> {
        self.indices.get(index).cloned()
    }
}

impl<'a> HistoryList<'a> {
    pub fn new(items: &'a [TimeTrackingItem]) -> Self {
        Self {
            items,
            filter: None,
            show_indices: false,
        }
    }

    pub fn set_filter(mut self, filter: Option<Regex>) -> Self {
        self.filter = filter;
        self
    }
}

impl<'a> StatefulWidget for HistoryList<'a> {
    type State = HistoryListState;

    fn render(self, area: Rect, buf: &mut Buffer, state: &mut Self::State) {
        if area.height < 3 {
            return;
        }
        state.page_size = area.height - 2;
        let mut list_state = ListState::default();
        let filtered: Vec<_> = if let Some(regex) = &self.filter {
            self.items
                .iter()
                .enumerate()
                .filter(|(_, i)| regex.find(&i.activity).is_some())
                .collect()
        } else {
            self.items.iter().enumerate().collect()
        };
        if let Some(ref mut index) = state.selected {
            *index = (*index).min(filtered.len().saturating_sub(1));
            let index = *index;
            if index < state.scroll_y {
                state.scroll_y = index;
            }
            if index >= state.scroll_y + state.page_size as usize {
                state.scroll_y = index - state.page_size as usize + 1;
            }
            list_state.select(Some(index - state.scroll_y));
        }
        state.scroll_y = state
            .scroll_y
            .min(filtered.len().saturating_sub(state.page_size as usize));
        let visible = &filtered[state.scroll_y..filtered.len().min(state.scroll_y + state.page_size as usize)];
        let mut seen_activities = HashSet::<&str>::new();
        let mut indices = vec![];
        let list_items = visible
            .iter()
            .map(|(i, e)| {
                let ending = match e.end {
                    Ending::Open => {
                        let delta = Local::now().signed_duration_since(e.start);
                        let (delta, msg) = if delta < chrono::Duration::zero() {
                            (-delta, "in")
                        } else {
                            (delta, "for")
                        };
                        format!(
                            "{} {}",
                            msg,
                            (DateTime::<Utc>::from(std::time::UNIX_EPOCH) + delta)
                                .format("%H:%M:%S")
                        )
                    }
                    Ending::At(t) => format!("{}", DateTime::<Local>::from(t).format("%F %X")),
                };
                let mut lines = e.activity.lines();
                let first_line = lines.next().unwrap_or("");

                ListItem::new(format!(
                    "{} -> {:>19} {}|{}{}",
                    DateTime::<Local>::from(e.start).format("%F %X"),
                    ending,
                    if self.show_indices && indices.len() < 9 {
                        let added = seen_activities.insert(&e.activity);
                        if added {
                            indices.push(*i);
                            format!("[{}]", indices.len())
                        } else {
                            "   ".to_owned()
                        }
                    } else if self.show_indices {
                        "   ".to_owned()
                    } else {
                        "".to_owned()
                    },
                    if lines.next().is_some() { "#" } else { " " },
                    first_line
                ))
            })
            .collect::<Vec<_>>();
        state.indices = indices;
        let list = List::new(list_items)
            .highlight_symbol(">>")
            .highlight_style(Style::default().fg(Color::Black).bg(Color::Green))
            .block(Block::default().title("History").borders(Borders::ALL));
        StatefulWidget::render(list, area, buf, &mut list_state);
        let footer = Block::default().title(format!(
            "{}% {}",
            100 * state.scroll_y / self.items.len().max(1),
            self.filter
                .as_ref()
                .map(|r| format!("search-regex: {}", r))
                .unwrap_or_else(|| "".to_owned())
        ));
        let mut footer_area = area;
        footer_area.y += footer_area.height - 1;
        footer_area.height = 1;
        footer_area.x += 2;
        footer_area.width -= 2;
        footer.render(footer_area, buf);
    }
}
