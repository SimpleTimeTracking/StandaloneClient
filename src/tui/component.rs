use crate::tui::Command;
use crossterm::event::KeyEvent;


#[allow(clippy::enum_variant_names)]
pub enum Consumed {
    Consumed,
    NotConsumed,
    Command(Command),
}

impl Consumed {
    pub fn is_consumed(&self) -> bool {
        !matches!(self, Consumed::NotConsumed)
    }
}

pub trait EventHandler {
    fn handle_event(&mut self, event: KeyEvent) -> Consumed;
    fn set_focus(&self, _frame: &mut tui::Frame) {}
}

pub trait Frame<S> {
    fn into_state(self) -> S;
}
