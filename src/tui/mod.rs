mod calendar;
mod textfield;
mod daily_report;

use crate::tui::daily_report::CondensedActivityList;
use crate::{
    commands::{self, Command, TimeSpec},
    Connection, Database, Ending, TimeTrackingItem,
};
use chrono::{DateTime, Datelike, Local, Utc};
use crossterm::{
    cursor::{DisableBlinking, EnableBlinking},
    event::{poll, read, Event, KeyCode, KeyEvent, KeyModifiers},
    execute,
    terminal::{disable_raw_mode, enable_raw_mode, EnterAlternateScreen, LeaveAlternateScreen},
};
use regex::Regex;
use std::collections::HashSet;
use std::{error::Error, io::stdout, time::Duration};
use textfield::TextField;
use tui::{
    backend::{self, CrosstermBackend},
    layout::{Constraint, Layout, Rect},
    style::{Color, Modifier, Style},
    terminal::Frame,
    text::Spans,
    widgets::{Block, BorderType, Borders, List, ListItem, ListState, Paragraph, Tabs},
    Terminal,
};

#[derive(PartialEq)]
enum Mode {
    Normal,
    EnterActivity,
    BrowseHistory,
    SearchHistory,
    DailyReport,
}

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

    fn select_as_activity<T: Into<Option<TimeTrackingItem>>>(&mut self, item: T) {
        if let Some(selected) = item.into() {
            self.mode = Mode::EnterActivity;
            self.activity_field.set_text(&selected.activity);
            self.activity_field.end();
            self.history_list.reset();
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
    indices: Vec<usize>,
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
            indices: vec![],
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

    fn get_item_by_index(&self, index: usize) -> Option<TimeTrackingItem> {
        self.indices
            .get(index)
            .map(|&i| self.visible.get(i))
            .flatten()
            .cloned()
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
            KeyCode::Home => self.selected = Some(0),
            KeyCode::End => self.selected = Some(0.max(self.items.len() - 1)),
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
        let mut seen_activities = HashSet::<&str>::new();
        let mut indices = vec![];
        let list_items = self
            .visible
            .iter()
            .enumerate()
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
                    if self.show_indices && i < 9 {
                        let added = seen_activities.insert(&e.activity);
                        if added {
                            indices.push(i);
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
        self.indices = indices;
        let list = List::new(list_items)
            .highlight_symbol(">>")
            .highlight_style(Style::default().fg(Color::Black).bg(Color::Green))
            .block(Block::default().title("History").borders(Borders::ALL));
        f.render_stateful_widget(list, area, &mut state);
        let footer = Block::default().title(format!(
            "{}% {}",
            100 * self.start / self.items.len().max(1),
            self.filter
                .as_ref()
                .map(|r| format!("search-regex: {}", r))
                .unwrap_or("".to_owned())
        ));
        let mut footer_area = area;
        footer_area.y += footer_area.height - 1;
        footer_area.height = 1;
        footer_area.x += 2;
        footer_area.width -= 2;
        f.render_widget(footer, footer_area);
    }
}

pub fn run() -> Result<(), Box<dyn Error>> {
    enable_raw_mode()?;

    let mut stdout = stdout();
    execute!(stdout, EnterAlternateScreen, EnableBlinking)?;

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
    let pause_matcher = Regex::new("(?i).*pause.*")?;

    'main: loop {
        terminal.draw(|f| {
            let mut chunks = Layout::default()
                .constraints([
                    if app.mode == Mode::Normal {Constraint::Length(1) } else {Constraint::Length(0)},
                    Constraint::Length(5),
                    Constraint::Min(0),
                    Constraint::Length(1),
                ])
                .split(f.size()).into_iter();
                /*
            let x = CondensedActivityList::new();
            f.render_widget(x, f.size());
            return;
            */
            let mut block = Block::default().title("Activity").borders(Borders::ALL);
            if app.mode == Mode::EnterActivity {
                block = block.border_type(BorderType::Thick);
            }
            let tab_area = chunks.next().unwrap();
            if app.mode == Mode::Normal {
                let tab = Tabs::new(vec![Spans::from("Activities"), Spans::from("Daily Report")])
                .highlight_style(Style::default().add_modifier(Modifier::BOLD));
                f.render_widget(tab, tab_area);
            }
            let activity_area = chunks.next().unwrap();
            let txt = block.inner(activity_area);
            f.render_widget(block, activity_area);
            app.activity_field.focus = app.mode == Mode::EnterActivity;
            app.activity_field.render(f, txt);

            let mut activity_status_area = activity_area;
            activity_status_area.y += activity_status_area.height - 1;
            activity_status_area.x += 2;
            activity_status_area.height = 1;
            activity_status_area.width -= 2;
            let now = Local::now();
            let start_of_day = now.date().and_hms(0, 0, 0);
            let start_of_week = start_of_day - chrono::Duration::days(now.weekday().num_days_from_monday() as i64);
            let duration_acc = |acc, a: &&TimeTrackingItem| acc - a.start.signed_duration_since(match a.end { Ending::Open => now, Ending::At(time) => time.into()});
            let work_time_today = connection.query().iter()
                .take_while(|a| a.end > start_of_day)
                .filter(|a| !pause_matcher.is_match(&a.activity))
                .fold(chrono::Duration::zero(), duration_acc);
            let work_time_today = (DateTime::<Utc>::from(std::time::UNIX_EPOCH) + work_time_today)
                .format("%T");
            let work_time_week = connection.query().iter()
            .take_while(|a| a.end > start_of_week)
            .filter(|a| !pause_matcher.is_match(&a.activity))
            .fold(chrono::Duration::zero(), duration_acc);
            let activity_status_line = Block::default().title(format!("Tracked time today: {} week: {}:{:02}:{:02}", work_time_today, work_time_week.num_hours(), work_time_week.num_minutes() % 60, work_time_week.num_seconds() % 60));
            f.render_widget(activity_status_line, activity_status_area);

            app.history_list.show_indices = app.mode == Mode::Normal;
            app.history_list.render(f, chunks.next().unwrap());

            let status_text = match app.mode {
                Mode::EnterActivity => "Esc - leave insert mode  Ctrl+Enter/Alt+Enter insert activity",
                Mode::Normal => "q - exit  i - edit  n - clear & edit  up/down/pgUp/pgDown - browse history  / - search  [1]-[9] - quick select previous item",
                Mode::BrowseHistory => "Esc - leave browsing mode  q - exit  i - insert  n - clear & edit  r - replace  up/down/pgUp/pgDown browse history",
                Mode::SearchHistory => "Esc - leave search  Enter - leave search/keep filter  up/down/pgUp/pgDown - browse history  ?",
                Mode::DailyReport => "Esc"
            }
            .to_owned();
            let status_len = status_text.len() as u16;
            let text = Block::default().title(status_text);
            let history_area = chunks.next().unwrap();
            f.render_widget(text, history_area);
            if app.mode == Mode::SearchHistory {
                let mut my_area = history_area;
                my_area.x += status_len;
                my_area.width -= status_len;
                app.search_field.focus = app.mode == Mode::SearchHistory;
                app.search_field.render(f, my_area);
            }
        })?;

        if poll(Duration::from_millis(500))? {
            loop {
                match read()? {
                    Event::Key(event) => match event.code {
                        KeyCode::Esc => {
                            app.mode = Mode::Normal;
                            app.history_list.reset();
                        }
                        _ => match app.mode {
                            Mode::EnterActivity => {
                                if (event.code == KeyCode::Enter
                                    || event.code == KeyCode::Char('\r'))
                                    && event
                                        .modifiers
                                        .intersects(KeyModifiers::ALT | KeyModifiers::CONTROL)
                                {
                                    match commands::command(&app.activity_field.get_text()) {
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
                                KeyCode::Char('q') => break 'main,
                                KeyCode::Char(n @ '1'..='9') => {
                                    if let Some(item) = app
                                        .history_list
                                        .get_item_by_index((n as u8 - b'1') as usize)
                                    {
                                        app.mode = Mode::EnterActivity;
                                        app.select_as_activity(item);
                                    }
                                }
                                KeyCode::Char('n') => {
                                    app.mode = Mode::EnterActivity;
                                    app.activity_field.clear();
                                }
                                KeyCode::Char('i') => app.mode = Mode::EnterActivity,
                                KeyCode::Down
                                | KeyCode::Up
                                | KeyCode::PageDown
                                | KeyCode::PageUp
                                | KeyCode::Home
                                | KeyCode::End => {
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
                                KeyCode::Char('r') => {
                                    if let Some(mut item) = app.history_list.get_selected_item() {
                                        item.activity = app.activity_field.get_text();
                                        connection.insert_item(item);
                                        database.flush();
                                        connection = database.open_connection();
                                        app.activity_field.clear();
                                        app.history_list.items = connection
                                            .query_n(10000)
                                            .iter()
                                            .map(|&i| i.clone())
                                            .collect();
                                    }
                                }
                                KeyCode::Tab => {
                                    app.mode = Mode::DailyReport;
                                }
                                _ => (),
                            },
                            Mode::SearchHistory => match event.code {
                                KeyCode::Down
                                | KeyCode::Up
                                | KeyCode::PageDown
                                | KeyCode::PageUp => app.history_list.handle_event(event),
                                KeyCode::Enter => {
                                    app.mode = Mode::Normal;
                                }
                                _ => {
                                    app.search_field.handle_event(event);
                                    app.history_list.set_filter(
                                        Regex::new(&format!(
                                            "(?i){}",
                                            regex::escape(app.search_field.first_line())
                                        ))
                                        .ok(),
                                    );
                                }
                            },
                            Mode::DailyReport => {
                                unimplemented!()
                            }
                        },
                    },
                    Event::Mouse(_event) => (),
                    Event::Resize(_width, _height) => (),
                }
                if poll(Duration::from_secs(0))? == false {
                    break;
                }
            }
        }
    }
    execute!(
        terminal.backend_mut(),
        LeaveAlternateScreen,
        DisableBlinking
    )?;
    disable_raw_mode()?;
    Ok(())
}
