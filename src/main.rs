mod commands;
mod database;
mod tti;
#[cfg(not(feature = "webui"))]
mod tui;

#[cfg(feature = "webui")]
mod webui;

use database::{Connection, Database};
use tti::{Ending, TimeTrackingItem};

fn main() -> Result<(), Box<dyn std::error::Error>> {
    tui::run()
}
