#![windows_subsystem = "windows"]

mod commands;
mod database;
mod tti;

use commands::{Command, TimeSpec};
use database::{Connection, Database};
use serde::Deserialize;
use tti::TimeTrackingItem;
use web_view::*;

#[derive(Deserialize)]
#[serde(tag = "cmd", rename_all = "camelCase")]
pub enum Cmd {
    Init,
    ExecuteCommand { activity: String },
    DeleteActivity { activity: TimeTrackingItem },
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
        .debug(true)
        .user_data(())
        .invoke_handler(|webview, arg| {
            use Cmd::*;
            println!("{}", arg);

            match serde_json::from_str(arg).unwrap() {
                Init => {
                    update_activities(webview, Database::open().unwrap().open_connection())?;
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
                                let mut database = Database::open().unwrap();
                                let connection = database.open_connection();
                                connection.insert_item(item);
                                update_activities(webview, connection)?;
                            }
                            Command::ResumeLast(time) => {
                                let mut database = Database::open().unwrap();
                                let connection = database.open_connection();
                                let latest_item = connection.query_latest();
                                if let Some(latest_item) = latest_item {
                                    let new_item = TimeTrackingItem::starting_at(
                                        time.to_date_time(),
                                        &latest_item.activity,
                                    );
                                    connection.insert_item(new_item);
                                    update_activities(webview, connection)?;
                                }
                            }
                            Command::Fin(time) => {
                                let mut database = Database::open().unwrap();
                                let connection = database.open_connection();
                                let item = connection.query_latest();
                                if let Some(item) = item {
                                    let mut item = item.clone();
                                    connection.delete_item(&item).unwrap();
                                    item.end = tti::Ending::At(time.to_date_time().into());
                                    connection.insert_item(item);
                                    update_activities(webview, connection)?;
                                }
                            }
                        }

                        webview.eval("app.commandAccepted();")?;
                    }
                    Err(msg) => {
                        println!("Invalid activity {}", msg);
                        webview.eval("app.commandError('error');")?;
                    }
                },
                DeleteActivity { activity } => {
                    let mut database = Database::open().unwrap();
                    let connection = database.open_connection();
                    connection.delete_item(&activity).unwrap();
                    update_activities(webview, connection)?;
                }
                Quit => webview.exit(),
            }
            Ok(())
        })
        .run()
        .unwrap();
}

fn update_activities<T>(webview: &mut WebView<T>, connection: &impl Connection) -> WVResult {
    let update_activities = format!(
        "app.updateActivities({})",
        serde_json::to_string(&connection.query_n(3000)).unwrap()
    );
    webview.eval(&update_activities)
}

fn inline_style(s: &str) -> String {
    format!(r#"<style type="text/css">{}</style>"#, s)
}

fn inline_script(s: &str) -> String {
    format!(r#"<script type="text/javascript">{}</script>"#, s)
}
