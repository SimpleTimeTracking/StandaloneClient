use crossterm::event::KeyEvent;

pub trait EventHandler {
    fn handle_event(&mut self, event: KeyEvent);
}