use crate::tui::component::{Consumed, EventHandler};
use chrono::{Datelike, Duration, Local, Month, NaiveDate, Weekday};
use crossterm::event::*;
use num_traits::FromPrimitive;
use tui::{
    buffer::Buffer,
    layout::Rect,
    style::{Color, Modifier, Style},
    widgets::StatefulWidget,
};

pub struct DatePickerState {
    selected: NaiveDate,
}

impl Default for DatePickerState {
    fn default() -> Self {
        Self {
            selected: Local::today().naive_local(),
        }
    }
}

impl DatePickerState {
    pub fn set_selected(&mut self, selected: NaiveDate) {
        self.selected = selected;
    }

    pub fn get_selected(&self) -> NaiveDate {
        self.selected
    }
}

impl EventHandler for DatePickerState {
    fn handle_event(&mut self, event: KeyEvent) -> Consumed {
        match event.code {
            KeyCode::Up => self.selected -= Duration::days(7),
            KeyCode::Down => self.selected += Duration::days(7),
            KeyCode::Left => self.selected -= Duration::days(1),
            KeyCode::Right => self.selected += Duration::days(1),
            _ => return Consumed::NotConsumed,
        };
        Consumed::Consumed
    }
}

pub struct DatePicker;

impl StatefulWidget for DatePicker {
    type State = DatePickerState;

    fn render(self, area: Rect, buf: &mut Buffer, state: &mut Self::State) {
        if area.height < 7 {
            return;
        }
        let mut y = area.y;

        y += 1;
        buf.set_string(
            area.x,
            y,
            format!(
                "{:<15} {}",
                Month::from_u32(state.selected.month()).unwrap().name(),
                state.selected.year()
            ),
            Style::default(),
        );
        y += 1;
        buf.set_string(
            area.x,
            y,
            "Mo Tu We Th Fr Sa Su",
            Style::default().add_modifier(Modifier::UNDERLINED),
        );
        let mut day = state.selected.with_day(1).unwrap();
        let month = day.month();
        let mut dow = Weekday::Mon;
        while day.month() == month {
            y += 1;
            let mut x = 0;
            for _ in 0..7 {
                if dow == day.weekday() && day.month() == month {
                    let style = Style::default();
                    let style = if state.selected == day {
                        style.bg(Color::Green).fg(Color::Black)
                    } else {
                        style
                    };
                    buf.set_string(x, y, format!("{:>2}", day.day()), style);
                    day += Duration::days(1);
                }
                dow = dow.succ();
                x += 3;
            }
        }
    }
}
