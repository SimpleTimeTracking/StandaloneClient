#![windows_subsystem = "windows"]

mod commands;
mod database;
mod tti;

use commands::{Command, TimeSpec};
use database::{Connection, Database};
use serde::Deserialize;
use std::time::Instant;
use tti::{TimeTrackingItem, Ending};
use web_view::*;

#[derive(Deserialize)]
#[serde(tag = "cmd", rename_all = "camelCase")]
pub enum Cmd {
    Init,
    ExecuteCommand { activity: String },
    DeleteActivity { activity: TimeTrackingItem },
    StopActivity { activity: TimeTrackingItem },
    ContinueActivity { activity: TimeTrackingItem },
    Quit,
}

fn main() {
    let html = format!(
        r#"
		<!doctype html>
		<html>
                <head>
                {styles}
                </head>
    			<body>
				<!--[if lt IE 9]>
				<div class="ie-upgrade-container">
					<p class="ie-upgrade-message">Please, upgrade Internet Explorer to continue using this software.</p>
					<a class="ie-upgrade-link" target="_blank" href="https://www.microsoft.com/en-us/download/internet-explorer.aspx">Upgrade</a>
				</div>
				<![endif]-->
				<!--[if gte IE 9 | !IE ]> <!-->
                <div id="app"></div>
                {scripts}	
                <![endif]-->
			</body>
		</html>
        "#,
        styles = inline_style(include_str!("../frontend/build/app.css")),
        scripts = inline_script(include_str!("../frontend/build/app.js"))
    );
    web_view::builder()
        .title("STT")
        .content(Content::Html(html))
        .size(800, 600)
        .resizable(false)
        .user_data(())
        .invoke_handler(|webview, arg| {
            use Cmd::*;
            println!("{}", arg);

            let mut database = Database::open().unwrap();
            let connection = database.open_connection();
            match serde_json::from_str(arg).unwrap() {
                Init => {
                    // Just update activities below
                }
                ExecuteCommand { activity } => match commands::command(&activity) {
                    Ok((_, cmd)) => {
                        match cmd {
                            Command::StartActivity(time, activity) => {
                                let item = match time {
                                    TimeSpec::Interval { from, to } => {
                                        TimeTrackingItem::interval(from, to, &activity)
                                            .map_err(web_view::Error::custom)?
                                    }
                                    _ => TimeTrackingItem::starting_at(
                                        time.to_date_time(),
                                        &activity,
                                    ),
                                };
                                connection.insert_item(item);
                            }
                            Command::ResumeLast(time) => {
                                let latest_item = connection.query_latest();
                                if let Some(latest_item) = latest_item {
                                    let new_item = TimeTrackingItem::starting_at(
                                        time.to_date_time(),
                                        &latest_item.activity,
                                    );
                                    connection.insert_item(new_item);
                                }
                            }
                            Command::Fin(time) => {
                                let item = connection.query_latest();
                                if let Some(item) = item {
                                    let mut item = item.clone();
                                    connection.delete_item(&item).unwrap();
                                    item.end = tti::Ending::At(time.to_date_time().into());
                                    connection.insert_item(item);
                                }
                            }
                        }

                        webview.eval("app.commandAccepted();")?;
                    }
                    Err(msg) => {
                        println!("Invalid activity {}", msg);
                        webview.eval("app.commandError('error');")?;
                    }
                }
                DeleteActivity { activity } => {
                    connection.delete_item(&activity).unwrap();
                }
                StopActivity { activity } => {
                    connection.delete_item(&activity).unwrap();
                    connection.insert_item(activity.ending_now());
                }
                ContinueActivity { activity } => {
                    let item = connection.query_latest();
                    if let Some(item) = item {
                        let mut item = item.clone();
                        if item.end == Ending::Open {
                            connection.delete_item(&item).unwrap();
                            item.end = Ending::At(activity.start);
                            connection.insert_item(item);
                        }
                    }
                    connection.insert_item(activity);
                }
                Quit => webview.exit(),
            }
            update_activities(webview, connection)?;
            Ok(())
        })
        .run()
        .unwrap();
}

fn update_activities<T>(webview: &mut WebView<T>, connection: &impl Connection) -> WVResult {
    let i = Instant::now();
    let result = connection.query_n(500);
    let update_activities = format!(
        "app.updateActivities({})",
        serde_json::to_string(&result).unwrap()
    );
    println!("Preparing update : {} ms", i.elapsed().as_millis());
    webview.eval(&update_activities)
}

fn inline_style(s: &str) -> String {
    format!(r#"<style type="text/css">{}</style>"#, s)
}

fn inline_script(s: &str) -> String {
    format!(r#"<script type="text/javascript">{}</script>"#, s)
}
