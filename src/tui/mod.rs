mod activity_tab;
mod component;
mod daily_report;
mod date_picker;
mod history_list;
mod string_ext;
mod textfield;

use crate::tui::activity_tab::*;
use crate::tui::component::*;
use crate::tui::daily_report::*;
use crate::{
    commands::{Command, TimeSpec},
    Connection, Database, Ending, TimeTrackingItem,
};
use crossterm::{
    cursor::{DisableBlinking, EnableBlinking},
    event::{poll, read, Event, KeyCode},
    execute,
    terminal::{disable_raw_mode, enable_raw_mode, EnterAlternateScreen, LeaveAlternateScreen},
};
use std::{error::Error, io::stdout, time::Duration};
use tui::{
    backend::CrosstermBackend,
    layout::{Constraint, Layout},
    style::{Modifier, Style},
    text::Spans,
    widgets::Tabs,
    Terminal,
};

#[derive(Copy, Clone, PartialEq)]
enum Mode {
    EnterActivity,
    DailyReport,
}

struct App {
    show_tabs: bool,
    tab: Mode,
    activity: ActivityTabState,
    daily_report: DailyReportState,
}

impl App {
    fn new() -> Self {
        Self {
            show_tabs: false,
            tab: Mode::EnterActivity,
            activity: ActivityTabState::default(),
            daily_report: DailyReportState::default(),
        }
    }
}

enum FrameOrState<F, S> {
    Frame(F),
    State(S),
}

impl<F: Frame<S>, S> Frame<S> for FrameOrState<F, S> {
    fn into_state(self) -> S {
        match self {
            FrameOrState::Frame(f) => f.into_state(),
            FrameOrState::State(s) => s,
        }
    }
}

pub fn run() -> Result<(), Box<dyn Error>> {
    enable_raw_mode()?;

    let mut stdout = stdout();
    execute!(stdout, EnterAlternateScreen, EnableBlinking)?;

    let result = || -> Result<(), Box<dyn Error>> {
        let backend = CrosstermBackend::new(stdout);
        let mut terminal = Terminal::new(backend)?;

        terminal.clear()?;

        let mut database = Database::open().unwrap();
        let mut connection = database.open_connection();

        let mut app = App::new();

        'main: loop {
            let current_list: Vec<_> = connection.query().iter().map(|&i| i.clone()).collect();

            let (show_tabs, tab) = (app.show_tabs, app.tab);
            let (mut activity_frame, mut daily_report_frame) = match tab {
                Mode::EnterActivity => (
                    FrameOrState::Frame(app.activity.into_frame(&current_list)),
                    FrameOrState::State(app.daily_report),
                ),
                Mode::DailyReport => (
                    FrameOrState::State(app.activity),
                    FrameOrState::Frame(app.daily_report.into_frame(&current_list)),
                ),
            };
            terminal.draw(|f| {
                let content_area = if show_tabs {
                    let mut chunks = Layout::default()
                        .constraints([Constraint::Length(1), Constraint::Min(0)])
                        .split(f.size())
                        .into_iter();
                    let tab =
                        Tabs::new(vec![Spans::from("Activities"), Spans::from("Daily Report")])
                            .highlight_style(Style::default().add_modifier(Modifier::BOLD))
                            .select(match tab {
                                Mode::EnterActivity => 0,
                                Mode::DailyReport => 1,
                            });
                    f.render_widget(tab, chunks.next().unwrap());
                    chunks.next().unwrap()
                } else {
                    f.size()
                };
                if let FrameOrState::Frame(ref mut frame) = activity_frame {
                    f.render_stateful_widget(ActivityTab, content_area, frame);
                    frame.set_focus(f);
                }
                if let FrameOrState::Frame(ref mut frame) = daily_report_frame {
                    f.render_stateful_widget(DailyReport, content_area, frame);
                    frame.set_focus(f);
                }
            })?;
            if poll(Duration::from_millis(500))? {
                loop {
                    match read()? {
                        Event::Key(event) => {
                            let consumed = if let FrameOrState::Frame(ref mut frame) =
                                activity_frame
                            {
                                frame.handle_event(event)
                            } else if let FrameOrState::Frame(ref mut frame) = daily_report_frame {
                                frame.handle_event(event)
                            } else {
                                panic!("Invalid tab")
                            };
                            let consumed = match consumed {
                                Consumed::Command(cmd) => {
                                    match cmd {
                                        Command::StartActivity(time, activity) => {
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
                                        Command::Fin(time) => {
                                            let item = connection.query_latest();
                                            if let Some(item) = item {
                                                let mut item = item.clone();
                                                connection.delete_item(&item).unwrap();
                                                item.end = Ending::At(time.to_date_time().into());
                                                connection.insert_item(item);
                                            }
                                        }
                                        _ => (),
                                    };
                                    database.flush();
                                    connection = database.open_connection();
                                    true
                                }
                                Consumed::Consumed => true,
                                Consumed::NotConsumed => false,
                            };
                            if !consumed {
                                match event.code {
                                    KeyCode::Tab => {
                                        app.show_tabs = true;
                                        app.tab = match app.tab {
                                            Mode::EnterActivity => Mode::DailyReport,
                                            Mode::DailyReport => Mode::EnterActivity,
                                        }
                                    }
                                    KeyCode::Esc if !app.show_tabs => app.show_tabs = true,
                                    KeyCode::Char('q') => break 'main,
                                    _ => (),
                                }
                            } else {
                                app.show_tabs = false;
                            }
                        }
                        Event::Mouse(_event) => (),
                        Event::Resize(_width, _height) => (),
                    }
                    if !poll(Duration::from_secs(0))? {
                        break;
                    }
                }
            }
            app.activity = activity_frame.into_state();
            app.daily_report = daily_report_frame.into_state()
        }
        execute!(
            terminal.backend_mut(),
            LeaveAlternateScreen,
            DisableBlinking
        )?;
        disable_raw_mode()?;
        Ok(())
    };
    result()
}

pub fn duration_to_string(duration: &chrono::Duration) -> String {
    format!(
        "{}:{:02}:{:02}",
        duration.num_hours(),
        duration.num_minutes() % 60,
        duration.num_seconds() % 60
    )
}
