use crate::tui::component::{Consumed, EventHandler};
use crate::{Ending, TimeTrackingItem};
use chrono::{DateTime, Local, Utc};
use crossterm::event::{KeyCode, KeyEvent};
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
    selected: Option<usize>,
}

impl HistoryListState {
    pub fn into_frame<I: Into<Vec<TimeTrackingItem>>>(self, items: I) -> HistoryListFrame {
        HistoryListFrame {
            state: self,
            items: items.into(),
            ..Default::default()
        }
    }
}

#[derive(Default)]
pub struct HistoryListFrame {
    state: HistoryListState,
    items: Vec<TimeTrackingItem>,
    indices: Vec<usize>,
    page_size: u16,
    show_indices: bool,
}

impl HistoryListFrame {
    pub fn reset(&mut self) {
        self.state.scroll_y = 0;
        self.state.selected = None;
    }

    pub fn into_state(self) -> HistoryListState {
        self.state
    }

    pub fn set_show_indices(&mut self, show: bool) {
        self.show_indices = show;
    }

    pub fn get_selected_item(&self) -> Option<TimeTrackingItem> {
        self.state
            .selected
            .map(|i| self.items.get(i))
            .flatten()
            .cloned()
    }

    pub fn get_item_with_index(&self, index: usize) -> Option<TimeTrackingItem> {
        self.indices
            .get(index)
            .map(|&index| self.items.get(index))
            .flatten()
            .cloned()
    }
}

pub struct HistoryList;

impl StatefulWidget for HistoryList {
    type State = HistoryListFrame;

    fn render(self, area: Rect, buf: &mut Buffer, state: &mut Self::State) {
        let HistoryListFrame {
            state,
            items,
            indices,
            page_size,
            show_indices,
        } = state;
        if area.height < 3 {
            return;
        }
        *page_size = area.height - 2;
        let mut list_state = ListState::default();
        if let Some(ref mut index) = state.selected {
            *index = (*index).min(items.len().saturating_sub(1));
            let index = *index;
            if index < state.scroll_y {
                state.scroll_y = index;
            }
            if index >= state.scroll_y + *page_size as usize {
                state.scroll_y = index - *page_size as usize + 1;
            }
            list_state.select(Some(index - state.scroll_y));
        }
        state.scroll_y = state
            .scroll_y
            .min(items.len().saturating_sub(*page_size as usize));
        let visible = &items[state.scroll_y..items.len().min(state.scroll_y + *page_size as usize)];
        let mut seen_activities = HashSet::<&str>::new();
        let list_items = visible
            .iter()
            .enumerate()
            .map(|(i, e)| {
                let i = i + state.scroll_y;
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
                    if *show_indices && indices.len() < 9 {
                        let added = seen_activities.insert(&e.activity);
                        if added {
                            indices.push(i);
                            format!("[{}]", indices.len())
                        } else {
                            "   ".to_owned()
                        }
                    } else if *show_indices {
                        "   ".to_owned()
                    } else {
                        "".to_owned()
                    },
                    if lines.next().is_some() { "#" } else { " " },
                    first_line
                ))
            })
            .collect::<Vec<_>>();
        let list = List::new(list_items)
            .highlight_symbol(">>")
            .highlight_style(Style::default().fg(Color::Black).bg(Color::Green))
            .block(Block::default().title("History").borders(Borders::ALL));
        StatefulWidget::render(list, area, buf, &mut list_state);
        let footer =
            Block::default().title(format!("{}%", 100 * state.scroll_y / items.len().max(1)));
        let mut footer_area = area;
        footer_area.y += footer_area.height - 1;
        footer_area.height = 1;
        footer_area.x += 2;
        footer_area.width -= 2;
        footer.render(footer_area, buf);
    }
}

impl EventHandler for HistoryListFrame {
    fn handle_event(&mut self, event: KeyEvent) -> Consumed {
        match event.code {
            KeyCode::Up => {
                self.state.selected = Some(
                    self.state
                        .selected
                        .map(|i| i.saturating_sub(1))
                        .unwrap_or(0),
                )
            }
            KeyCode::Down => {
                self.state.selected = Some(self.state.selected.map(|i| i + 1).unwrap_or(0))
            }
            KeyCode::PageUp => {
                self.state.selected = Some(
                    self.state
                        .selected
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
                self.state.selected = Some(
                    self.state
                        .selected
                        .map(|i| i + self.page_size as usize)
                        .unwrap_or(0),
                )
            }
            KeyCode::Home => self.state.selected = Some(0),
            KeyCode::End => self.state.selected = Some(usize::MAX),
            _ => (),
        };
        Consumed::NotConsumed
    }
}
