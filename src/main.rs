mod commands;
mod database;
mod tti;
#[cfg(not(feature = "webui"))]
mod tui;

#[cfg(feature = "webui")]
mod webui;

use commands::{Command, TimeSpec};
use database::{Connection, Database};
use serde::Deserialize;
use std::time::Instant;
use tti::{Ending, TimeTrackingItem};

fn main() {
    tui::run();
}
