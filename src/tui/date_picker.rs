use chrono::{Datelike, Duration, Month, NaiveDate, Weekday};
use num_traits::FromPrimitive;
use tui::style::{Modifier, Style};
use tui::{buffer::Buffer, layout::Rect, widgets::StatefulWidget};

pub struct DatePickerState {
    month: u32,
    year: i32,
    selected: Option<NaiveDate>,
}

#[derive(Default)]
pub struct DatePicker {
}

impl DatePickerState {
    pub fn new(year: i32, month: u32) -> Self {
        Self {
            year,
            month,
            selected: None,
        }
    }

    pub fn set_selected(&mut self, selected: Option<NaiveDate>) {
        self.selected = selected;
    }
}

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
                Month::from_u32(state.month).unwrap().name(),
                state.year
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
        let mut day = NaiveDate::from_ymd(state.year, state.month, 1);
        let mut dow = Weekday::Mon;
        while day.month() == state.month {
            y += 1;
            let mut x = 0;
            for _ in 0..7 {
                if dow == day.weekday() && day.month() == state.month {
                    let style = match state.selected {
                        Some(selected) if selected == day => {
                            Style::default().add_modifier(Modifier::BOLD)
                        }
                        _ => Style::default(),
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
