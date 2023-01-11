mod commands;
mod database;
mod tti;
mod tui;

use database::{Connection, Database};
use tti::{Ending, TimeTrackingItem};

fn main() -> Result<(), Box<dyn std::error::Error>> {
    tui::run()
}
