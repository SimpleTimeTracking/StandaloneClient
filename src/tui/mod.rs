mod calendar;
mod daily_report;
mod event_handler;
mod history_list;
mod textfield;
mod date_picker;

use crate::tui::history_list::{HistoryList, HistoryListState};
use crate::tui::{
    daily_report::{DailyReport, DailyReportState},
    event_handler::EventHandler,
    date_picker::{DatePicker, DatePickerState}
};
use crate::{
    commands::{self, Command, TimeSpec},
    Connection, Database, Ending, TimeTrackingItem,
};
use chrono::{DateTime, Datelike, Local, Utc};
use crossterm::{
    cursor::{DisableBlinking, EnableBlinking},
    event::{poll, read, Event, KeyCode, KeyModifiers},
    execute,
    terminal::{disable_raw_mode, enable_raw_mode, EnterAlternateScreen, LeaveAlternateScreen},
};
use regex::Regex;
use std::{error::Error, io::stdout, time::Duration};
use textfield::{TextField, TextFieldState};
use tui::{
    backend::CrosstermBackend,
    layout::{Constraint, Layout},
    style::{Modifier, Style},
    text::Spans,
    widgets::{Block, BorderType, Borders, Tabs},
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
    history_list: HistoryListState,
    activity_field: TextFieldState,
    search_field: TextFieldState,
    daily_report: DailyReportState,
}

pub fn duration_to_string(duration: &chrono::Duration) -> String {
    format!(
        "{}:{:02}:{:02}",
        duration.num_hours(),
        duration.num_minutes() % 60,
        duration.num_seconds() % 60
    )
}

impl App {
    fn new() -> Self {
        Self {
            mode: Mode::EnterActivity,
            history_list: HistoryListState::default(),
            activity_field: TextFieldState::new(),
            search_field: TextFieldState::new(),
            daily_report: DailyReportState::new(),
        }
    }

    fn select_as_activity<'a, T: Into<Option<&'a TimeTrackingItem>>>(&mut self, item: T) {
        if let Some(selected) = item.into() {
            self.mode = Mode::EnterActivity;
            self.activity_field.set_text(&selected.activity);
            self.activity_field.end();
            self.history_list.reset();
        }
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

    let mut app = App::new();
    let pause_matcher = Regex::new("(?i).*pause.*")?;

    'main: loop {
        let current_list: Vec<_> = connection.query().iter().map(|&i| i.clone()).collect();
        terminal.draw(|f| {
            let mut chunks = Layout::default()
                .constraints([
                    if app.mode == Mode::Normal {Constraint::Length(1) } else {Constraint::Length(0)},
                    Constraint::Length(5),
                    Constraint::Min(0),
                    Constraint::Length(1),
                ])
                .split(f.size()).into_iter();


            let tab_area = chunks.next().unwrap();
            if app.mode == Mode::Normal {
                let tab = Tabs::new(vec![Spans::from("Activities"), Spans::from("Daily Report")])
                .highlight_style(Style::default().add_modifier(Modifier::BOLD));
                f.render_widget(tab, tab_area);
            }
            let activity_area = chunks.next().unwrap();
            if app.mode == Mode::DailyReport {
                app.daily_report.condensed_list.set_items(connection.query_n(20));
                let daily_report = DailyReport::new();
//                f.render_stateful_widget(daily_report, activity_area, &mut app.daily_report);
                let picker = DatePicker::default();
                let mut state = DatePickerState::new(2021, 2);
                state.set_selected(Some(chrono::NaiveDate::from_ymd(2021, 2, 14)));
                f.render_stateful_widget(picker, f.size(), &mut state);
                return;
            }
            let mut block = Block::default().title("Activity").borders(Borders::ALL);
            if app.mode == Mode::EnterActivity {
                block = block.border_type(BorderType::Thick);
            }
            let txt = block.inner(activity_area);
            f.render_widget(block, activity_area);

            let text_field = TextField::default();
            f.render_stateful_widget(text_field, txt, &mut app.activity_field);
            if app.mode == Mode::EnterActivity {
                f.set_cursor(app.activity_field.cursor_screen_pos.0, app.activity_field.cursor_screen_pos.1);
            }

            let mut activity_status_area = activity_area;
            activity_status_area.y += activity_status_area.height - 1;
            activity_status_area.x += 2;
            activity_status_area.height = 1;
            activity_status_area.width -= 2;
            let now = Local::now();
            let start_of_day = now.date().and_hms(0, 0, 0);
            let start_of_week = start_of_day - chrono::Duration::days(now.weekday().num_days_from_monday() as i64);
            let duration_acc = |acc, a: &TimeTrackingItem| acc - a.start.signed_duration_since(match a.end { Ending::Open => now, Ending::At(time) => time.into()});
            let work_time_today = current_list.iter()
                .take_while(|a| a.end > start_of_day)
                .filter(|a| !pause_matcher.is_match(&a.activity))
                .fold(chrono::Duration::zero(), duration_acc);
            let work_time_today = (DateTime::<Utc>::from(std::time::UNIX_EPOCH) + work_time_today)
                .format("%T");
            let work_time_week = current_list.iter()
            .take_while(|a| a.end > start_of_week)
            .filter(|a| !pause_matcher.is_match(&a.activity))
            .fold(chrono::Duration::zero(), duration_acc);
            let activity_status_line = Block::default().title(format!("Tracked time today: {} week: {}:{:02}:{:02}", work_time_today, work_time_week.num_hours(), work_time_week.num_minutes() % 60, work_time_week.num_seconds() % 60));
            f.render_widget(activity_status_line, activity_status_area);

            let mut history_list = HistoryList::new(&current_list)
                .set_filter(
                    if app.search_field.first_line().is_empty() {
                        None
                    } else {
                        Regex::new(&format!(
                            "(?i){}",
                            regex::escape(app.search_field.first_line())
                        ))
                        .ok() 
                },
            );

            history_list.show_indices = app.mode == Mode::Normal;
            f.render_stateful_widget(history_list, chunks.next().unwrap(), &mut app.history_list);

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
                let text_field = TextField::default();
                f.render_stateful_widget(text_field, my_area, &mut app.search_field);
                if app.mode == Mode::SearchHistory {
                    f.set_cursor(app.search_field.cursor_screen_pos.0, app.search_field.cursor_screen_pos.1);
                }
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
                                        .map(|i| &current_list[i])
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
                                    app.select_as_activity(
                                        app.history_list
                                            .get_selected_item()
                                            .map(|i| &current_list[i]),
                                    );
                                }
                                KeyCode::Char('/') => {
                                    app.mode = Mode::SearchHistory;
                                    app.search_field.clear();
                                }
                                KeyCode::Char('r') => {
                                    if let Some(mut item) = app
                                        .history_list
                                        .get_selected_item()
                                        .map(|i| current_list[i].clone())
                                    {
                                        item.activity = app.activity_field.get_text();
                                        connection.insert_item(item);
                                        database.flush();
                                        connection = database.open_connection();
                                        app.activity_field.clear();
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
                                }
                            },
                            Mode::DailyReport => {
                                app.daily_report.handle_event(event);
                            }
                        },
                    },
                    Event::Mouse(_event) => (),
                    Event::Resize(_width, _height) => (),
                }
                if !poll(Duration::from_secs(0))? {
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
