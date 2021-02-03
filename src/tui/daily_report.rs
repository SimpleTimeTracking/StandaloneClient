use tui::{
    layout::{Rect, Constraint},
    text::Spans,
    widgets::{Paragraph, Table, Widget, Row, Cell},
    Frame,
};

pub struct CondensedActivityList {}

impl CondensedActivityList {
    pub fn new() -> Self {
        Self {}
    }
}

impl Widget for CondensedActivityList {
    fn render(self, area: Rect, buf: &mut tui::buffer::Buffer) {
        let table = Table::new(vec![
            Row::new(vec![Cell::from("aoeu"), Cell::from("ae")])
        ])
        .header(Row::new(vec!["Duration", "Activity"]))
        .widths(&[Constraint::Min(12), Constraint::Min(10)]);
        table.render(area, buf);
    }
}
