use crossterm::event::{KeyCode, KeyEvent};
use tui::{layout::Rect, text::Spans, widgets::Paragraph, Frame};
use unicode_segmentation::UnicodeSegmentation;

trait StringExt {
    fn remove_char(&mut self, idx: usize) -> char;
    fn insert_char(&mut self, idx: usize, ch: char);
    fn split_at_char(&mut self, mid: usize) -> (&str, &str);
}

impl StringExt for String {
    fn remove_char(&mut self, idx: usize) -> char {
        let byte_index = UnicodeSegmentation::grapheme_indices(self.as_str(), true)
            .nth(idx)
            .unwrap()
            .0;
        self.remove(byte_index)
    }

    fn insert_char(&mut self, idx: usize, ch: char) {
        let byte_index = UnicodeSegmentation::grapheme_indices(self.as_str(), true)
            .nth(idx)
            .map(|i| i.0)
            .unwrap_or(self.len());
        self.insert(byte_index, ch)
    }

    fn split_at_char(&mut self, mid: usize) -> (&str, &str) {
        let byte_index = UnicodeSegmentation::grapheme_indices(self.as_str(), true)
            .nth(mid)
            .map(|i| i.0)
            .unwrap_or(self.len());
        self.split_at(byte_index)
    }
}

pub struct TextField {
    cursor: (usize, usize),
    scroll: (u16, u16),
    lines: Vec<String>,
    pub focus: bool,
}

impl TextField {
    pub fn new() -> Self {
        TextField {
            cursor: (0, 0),
            scroll: (0, 0),
            lines: vec![],
            focus: false,
        }
    }

    pub fn clear(&mut self) {
        self.lines.clear();
        self.cursor = (0, 0);
        self.scroll = (0, 0);
    }

    pub fn first_line(&self) -> &str {
        self.lines.get(0).map(|s| s.as_str()).unwrap_or("")
    }

    pub fn get_text(&self) -> String {
        self.lines.join("\n")
    }

    pub fn set_text(&mut self, text: &str) {
        self.lines = text.lines().map(|s| s.to_owned()).collect();
    }

    pub fn end(&mut self) {
        if let Some(last) = self.lines.last() {
            self.cursor = (last.len(), self.lines.len() - 1);
        } else {
            self.cursor = (0, 0);
        }
    }

    fn current_line_len(&self) -> usize {
        self.lines.get(self.cursor.1).map(|l| l.len()).unwrap_or(0)
    }

    pub fn handle_event(&mut self, event: KeyEvent) {
        match event.code {
            KeyCode::Backspace => {
                if self.cursor.0 > 0 {
                    self.cursor.0 -= 1;
                    self.lines[self.cursor.1].remove_char(self.cursor.0);
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
                        line.remove_char(self.cursor.0);
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
                    let line = &mut self.lines[self.cursor.1];
                    let split = line.split_at_char(self.cursor.0);
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
                    self.lines[self.cursor.1].insert_char(self.cursor.0, c);
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

    pub fn render<B: tui::backend::Backend>(&mut self, f: &mut Frame<'_, B>, area: Rect) {
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
