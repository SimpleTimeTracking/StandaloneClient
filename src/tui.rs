use crate::{
    commands::{self, Command, TimeSpec},
    Connection, Database, Ending, TimeTrackingItem,
};
use chrono::{DateTime, Local};
use crossterm::{
    cursor::{DisableBlinking, EnableBlinking},
    event::{
        poll, read, DisableMouseCapture, EnableMouseCapture, Event, KeyCode, KeyEvent, KeyModifiers,
    },
    execute,
    terminal::{disable_raw_mode, enable_raw_mode, EnterAlternateScreen, LeaveAlternateScreen},
};
use regex::Regex;
use std::{
    error::Error,
    io::{stdout, Write},
    time::{Duration, SystemTime},
};
use tui::{
    backend::{self, CrosstermBackend},
    layout::{Constraint, Layout, Rect},
    terminal::Frame,
    text::{Spans},
    widgets::{Block, BorderType, Borders, List, ListItem, ListState, Paragraph},
    Terminal,
};

struct App {
    mode: Mode,
    history_list: HistoryList,
    activity_field: TextField,
    search_field: TextField,
}

impl App {
    fn new(history_list: HistoryList) -> Self {
        Self {
            mode: Mode::EnterActivity,
            history_list,
            activity_field: TextField::new(),
            search_field: TextField::new(),
        }
    }

    fn select_as_activity(&mut self, item: Option<TimeTrackingItem>) {
        if let Some(selected) = item {
            self.mode = Mode::EnterActivity;
            self.activity_field.set_text(&selected.activity);
            self.activity_field.end();
            self.history_list.reset();
        }
    }
}

struct TextField {
    cursor: (usize, usize),
    scroll: (u16, u16),
    lines: Vec<String>,
    focus: bool,
}

impl TextField {
    fn new() -> Self {
        TextField {
            cursor: (0, 0),
            scroll: (0, 0),
            lines: vec![],
            focus: false,
        }
    }

    fn clear(&mut self) {
        self.lines.clear();
        self.cursor = (0, 0);
        self.scroll = (0, 0);
    }

    fn set_text(&mut self, text: &str) {
        self.lines.clear();
        self.lines.push(text.to_owned());
    }

    fn end(&mut self) {
        if let Some(last) = self.lines.last() {
            self.cursor = (last.len(), self.lines.len() - 1);
        } else {
            self.cursor = (0, 0);
        }
    }

    fn current_line_len(&self) -> usize {
        self.lines.get(self.cursor.1).map(|l| l.len()).unwrap_or(0)
    }

    fn handle_event(&mut self, event: KeyEvent) {
        match event.code {
            KeyCode::Backspace => {
                if self.cursor.0 > 0 {
                    self.cursor.0 -= 1;
                    self.lines[self.cursor.1].remove(self.cursor.0);
                } else if !self.lines.is_empty() {
                    let line = self.lines.remove(self.cursor.1);
                    if self.cursor.1 > 0 {
                        self.cursor.1 -= 1;
                    }
                    self.cursor.0 = self.current_line_len();
                    if !self.lines.is_empty() {
                        self.lines[self.cursor.1].push_str(&line);
                    }
                }
            }
            KeyCode::Delete => {
                if !self.lines.is_empty() {
                    let line = self.lines.get_mut(self.cursor.1).unwrap();
                    if line.len() > self.cursor.0 {
                        line.remove(self.cursor.0);
                    } else if self.lines.len() > self.cursor.1 + 1 {
                        let line = self.lines.remove(self.cursor.1 + 1);
                        self.lines[self.cursor.1].push_str(&line);
                    }
                }
            }
            KeyCode::Enter => {
                if self.lines.is_empty() {
                    self.lines.push(String::new());
                    self.lines.push(String::new());
                    self.cursor.1 = 1;
                } else {
                    let split = self.lines[self.cursor.1].split_at(self.cursor.0);
                    let split = (split.0.to_owned(), split.1.to_owned());
                    self.lines[self.cursor.1] = split.0;
                    self.cursor.1 += 1;
                    self.cursor.0 = 0;
                    self.lines.insert(self.cursor.1, split.1);
                }
            }
            KeyCode::Char(c) => {
                if self.lines.is_empty() {
                    self.lines.push(String::from(c));
                } else {
                    self.lines[self.cursor.1].insert(self.cursor.0, c);
                }
                self.cursor.0 += 1;
            }
            KeyCode::Up => {
                if self.cursor.1 > 0 {
                    self.cursor.1 -= 1;
                    self.cursor.0 = self.current_line_len().min(self.cursor.0);
                }
            }
            KeyCode::Down => {
                if self.cursor.1 + 1 < self.lines.len() {
                    self.cursor.1 += 1;
                    self.cursor.0 = self.current_line_len().min(self.cursor.0);
                }
            }
            KeyCode::Left => {
                if self.cursor.0 > 0 {
                    self.cursor.0 -= 1;
                }
            }
            KeyCode::Right => {
                if self.cursor.0 < self.current_line_len() {
                    self.cursor.0 += 1;
                }
            }
            KeyCode::End => {
                self.cursor.0 = self.current_line_len();
            }
            KeyCode::Home => {
                self.cursor.0 = 0;
            }
            _ => (),
        }
    }

    fn render<B: backend::Backend>(&mut self, f: &mut Frame<'_, B>, area: Rect) {
        self.scroll.1 = self.scroll.1.min(self.cursor.1 as u16);
        self.scroll.0 = self.scroll.0.min(self.cursor.0 as u16);

        if self.cursor.0 as u16 >= self.scroll.0 + area.width {
            self.scroll.0 = self.cursor.0 as u16 - area.width + 1;
        }

        if self.cursor.1 as u16 >= self.scroll.1 + area.height {
            self.scroll.1 = self.cursor.1 as u16 - area.height + 1;
        }

        let para = Paragraph::new(
            self.lines
                .iter()
                .map(|s| Spans::from(s.to_owned()))
                .collect::<Vec<_>>(),
        ) // (x, y) _> (y, x)
        .scroll((self.scroll.1, self.scroll.0));
        f.render_widget(para, area);
        if self.focus {
            f.set_cursor(
                area.x + self.cursor.0 as u16 - self.scroll.0,
                area.y + self.cursor.1 as u16 - self.scroll.1,
            );
        }
    }
}

struct HistoryList {
    start: usize,
    page_size: u16,
    selected: Option<usize>,
    items: Vec<TimeTrackingItem>,
    filter: Option<Regex>,
    visible: Vec<TimeTrackingItem>,
    show_indices: bool,
}

impl HistoryList {
    fn new(items: Vec<TimeTrackingItem>) -> Self {
        HistoryList {
            start: 0,
            page_size: 0,
            selected: None,
            items,
            filter: None,
            visible: vec![],
            show_indices: false,
        }
    }

    fn get_selected_item(&self) -> Option<TimeTrackingItem> {
        if let Some(index) = self.selected {
            self.visible.get(index).cloned()
        } else {
            None
        }
    }

    fn get_item(&self, index: usize) -> Option<TimeTrackingItem> {
        self.visible.get(index).cloned()
    }

    fn set_filter(&mut self, filter: Option<Regex>) {
        self.filter = filter;
        self.start = 0;
        self.selected = None;
    }

    fn reset(&mut self) {
        self.selected = None;
        self.filter = None;
    }

    fn handle_event(&mut self, event: KeyEvent) {
        match event.code {
            KeyCode::Up => {
                self.selected = Some(
                    self.selected
                        .map(|i| if i > 0 { i - 1 } else { 0 })
                        .unwrap_or(0),
                )
            }
            KeyCode::Down => {
                self.selected = Some(
                    self.selected
                        .map(|i| (i + 1).min(self.items.len() - 1))
                        .unwrap_or(0),
                )
            }
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
                        .map(|i| (i + self.page_size as usize).min(self.items.len() - 1))
                        .unwrap_or(0),
                )
            }
            _ => (),
        }
    }

    fn render<B: backend::Backend>(&mut self, f: &mut Frame<'_, B>, area: Rect) {
        self.page_size = area.height - 2;
        let mut state = ListState::default();
        if let Some(index) = self.selected {
            if index < self.start {
                self.start = index;
            }
            if index >= self.start + self.page_size as usize {
                self.start = index - self.page_size as usize + 1;
            }
            state.select(Some(index - self.start));
        }
        let symbol = ["-", "\\", "|", "/"][SystemTime::now()
            .duration_since(SystemTime::UNIX_EPOCH)
            .unwrap()
            .as_millis() as usize
            / 100
            % 4];
        self.visible = if let Some(regex) = &self.filter {
            self.items
                .iter()
                .filter(|i| regex.find(&i.activity).is_some())
                .skip(self.start)
                .take(self.page_size as usize)
                .cloned()
                .collect()
        } else {
            self.items
                .iter()
                .skip(self.start)
                .take(self.page_size as usize)
                .cloned()
                .collect()
        };
        let list_items = self
            .visible
            .iter()
            .enumerate()
            .map(|(i, e)| {
                let ending = match e.end {
                    Ending::Open => symbol.to_string(),
                    Ending::At(t) => format!("{}", DateTime::<Local>::from(t).format("%F %X")),
                };
                ListItem::new(format!(
                    "{} -> {:<19} {}| {}",
                    DateTime::<Local>::from(e.start).format("%F %X"),
                    ending,
                    if self.show_indices && i < 9 {
                        format!("[{}] ", i + 1)
                    } else if self.show_indices {
                        "    ".to_owned()
                    } else {
                        "".to_owned()
                    },
                    e.activity
                ))
            })
            .collect::<Vec<_>>();
        let list = List::new(list_items)
            .highlight_symbol(">>")
            .block(Block::default().title("History").borders(Borders::ALL));
        f.render_stateful_widget(list, area, &mut state);
        let footer =
            Block::default().title(format!("{}%", 100 * self.start / self.items.len().max(1)));
        let mut footer_area = area;
        footer_area.y += footer_area.height - 1;
        footer_area.height = 1;
        footer_area.x += 2;
        footer_area.width -= 2;
        f.render_widget(footer, footer_area);
    }
}

#[derive(PartialEq)]
enum Mode {
    Normal,
    EnterActivity,
    BrowseHistory,
    SearchHistory,
}

pub fn run() -> Result<(), Box<dyn Error>> {
    enable_raw_mode()?;

    let mut stdout = stdout();
    execute!(
        stdout,
        EnterAlternateScreen,
        EnableMouseCapture,
        EnableBlinking
    )?;

    let backend = CrosstermBackend::new(stdout);
    let mut terminal = Terminal::new(backend)?;

    terminal.clear()?;

    let mut database = Database::open().unwrap();
    let mut connection = database.open_connection();

    let history_list = HistoryList::new(
        connection
            .query_n(10000)
            .iter()
            .map(|&i| i.clone())
            .collect(),
    );

    let mut app = App::new(history_list);

    loop {
        terminal.draw(|f| {
            let chunks = Layout::default()
                .constraints([
                    Constraint::Length(5),
                    Constraint::Min(0),
                    Constraint::Length(1),
                ])
                .split(f.size());
            let mut block = Block::default().title("Activity").borders(Borders::ALL);
            if app.mode == Mode::EnterActivity {
                block = block.border_type(BorderType::Thick);
            }
            let txt = block.inner(chunks[0]);
            f.render_widget(block, chunks[0]);
            app.activity_field.focus = app.mode == Mode::EnterActivity;
            app.activity_field.render(f, txt);
            app.history_list.show_indices = app.mode == Mode::Normal;
            app.history_list.render(f, chunks[1]);

            let status_text = match app.mode {
                Mode::EnterActivity => "Esc - leave insert mode",
                Mode::Normal => "q - exit  i - edit  n - clear & edit  up/down/pgUp/pgDown - browse history  / - search  1-9 - quick select previous item",
                Mode::BrowseHistory => "Esc - leave browsing mode  q - exit  i - insert  up/down/pgUp/pgDown browse history",
                Mode::SearchHistory => "Esc - leave search  up/down/pgUp/pgDown - browse history ?"
            }
            .to_owned();
            let status_len = status_text.len() as u16;
            let text = Block::default().title(status_text);
            f.render_widget(text, chunks[2]);
            if app.mode == Mode::SearchHistory {
                let mut my_area = chunks[2];
                my_area.x += status_len;
                my_area.width -= status_len;
                app.search_field.focus = app.mode == Mode::SearchHistory;
                app.search_field.render(f, my_area);
            }
        })?;

        if poll(Duration::from_millis(20))? {
            match read()? {
                Event::Key(event) => match event.code {
                    KeyCode::Esc => {
                        app.mode = Mode::Normal;
                        app.history_list.reset();
                    }
                    _ => match app.mode {
                        Mode::EnterActivity => {
                            if (event.code == KeyCode::Enter || event.code == KeyCode::Char('\r'))
                                && event.modifiers.contains(KeyModifiers::ALT)
                            {
                                match commands::command(&app.activity_field.lines.join("\n")) {
                                    Ok((_, Command::StartActivity(time, activity))) => {
                                        let item = match time {
                                            TimeSpec::Interval { from, to } => {
                                                TimeTrackingItem::interval(from, to, &activity)
                                                    .unwrap()
                                            }
                                            _ => TimeTrackingItem::starting_at(
                                                time.to_date_time(),
                                                &activity,
                                            ),
                                        };
                                        connection.insert_item(item);
                                    }
                                    Ok((_, Command::Fin(time))) => {
                                        let item = connection.query_latest();
                                        if let Some(item) = item {
                                            let mut item = item.clone();
                                            connection.delete_item(&item).unwrap();
                                            item.end = Ending::At(time.to_date_time().into());
                                            connection.insert_item(item);
                                        }
                                    }
                                    _ => (),
                                }
                                database.flush();
                                connection = database.open_connection();
                                app.activity_field.clear();
                                app.history_list.items = connection
                                    .query_n(10000)
                                    .iter()
                                    .map(|&i| i.clone())
                                    .collect();
                            } else {
                                app.activity_field.handle_event(event);
                            }
                        }
                        Mode::Normal | Mode::BrowseHistory => match event.code {
                            KeyCode::Char('q') => break,
                            KeyCode::Char(n @ '1'..='9') => {
                                app.mode = Mode::EnterActivity;
                                app.select_as_activity(
                                    app.history_list.get_item((n as u8 - b'1') as usize),
                                );
                            }
                            KeyCode::Char('n') => {
                                app.mode = Mode::EnterActivity;
                                app.activity_field.clear();
                            }
                            KeyCode::Char('i') => app.mode = Mode::EnterActivity,
                            KeyCode::Down | KeyCode::Up | KeyCode::PageDown | KeyCode::PageUp => {
                                app.mode = Mode::BrowseHistory;
                                app.history_list.handle_event(event)
                            }
                            KeyCode::Char(' ') | KeyCode::Enter => {
                                app.select_as_activity(app.history_list.get_selected_item());
                            }
                            KeyCode::Char('/') => {
                                app.mode = Mode::SearchHistory;
                                app.search_field.clear();
                            }
                            _ => (),
                        },
                        Mode::SearchHistory => match event.code {
                            KeyCode::Down | KeyCode::Up | KeyCode::PageDown | KeyCode::PageUp => {
                                app.history_list.handle_event(event)
                            }
                            KeyCode::Enter => {
                                app.select_as_activity(app.history_list.get_selected_item());
                            }
                            _ => {
                                app.search_field.handle_event(event);
                                app.history_list
                                    .set_filter(app.search_field.lines.get(0).map(|l| {
                                        Regex::new(&format!("(?i){}", regex::escape(l))).unwrap()
                                    }));
                            }
                        },
                    },
                },
                Event::Mouse(_event) => (),
                Event::Resize(_width, _height) => (),
            }
        }
    }
    execute!(
        terminal.backend_mut(),
        LeaveAlternateScreen,
        DisableMouseCapture,
        DisableBlinking
    )?;
    disable_raw_mode()?;
    Ok(())
}
