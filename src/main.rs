#![windows_subsystem = "windows"]

mod commands;
mod tti;

use chrono::prelude::*;
use commands::{Command, TimeSpec};
use serde::Deserialize;
use tti::{Connection, Database, TimeTrackingItem};
use web_view::*;

#[derive(Deserialize)]
#[serde(tag = "cmd", rename_all = "camelCase")]
pub enum Cmd {
    Init,
    AddActivity { activity: String },
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

            match serde_json::from_str(arg).unwrap() {
                Init => {
                    let items = Database::open().unwrap().open_connection().query_n(3000);
                    let blub = format!(
                        "app.updateActivities({})",
                        serde_json::to_string(&items).unwrap()
                    );
                    webview.eval(&blub).unwrap();
                }
                AddActivity { activity } => match commands::command(&activity) {
                    Ok((_, cmd)) => {
                        match cmd {
                            Command::StartActivity(time, activity) => {
                                let date_time = match time {
                                    TimeSpec::Now => Local::now(),
                                    TimeSpec::Absolute(date_time) => date_time,
                                    TimeSpec::Relative(delta) => Local::now() + delta,
                                };
                                let item = TimeTrackingItem::starting_at(date_time, &activity);
                                println!("Added activity {:?}", item);
                                let mut database = Database::open().unwrap();
                                let connection = database.open_connection();
                                connection.insert_item(item);
                                let blub = format!(
                                    "app.updateActivities({})",
                                    serde_json::to_string(&connection.query_n(3000)).unwrap()
                                );
                                webview.eval(&blub).unwrap();
                            }
                            _ => (),
                        }

                        webview.eval("app.commandAccepted();")?;
                    }
                    Err(msg) => {
                        println!("Invalid activity {}", msg);
                        webview.eval("app.commandError('error');")?;
                    }
                },
                Quit => webview.exit(),
            }
            Ok(())
        })
        .run()
        .unwrap();
}

fn inline_style(s: &str) -> String {
    format!(r#"<style type="text/css">{}</style>"#, s)
}

fn inline_script(s: &str) -> String {
    format!(r#"<script type="text/javascript">{}</script>"#, s)
}
