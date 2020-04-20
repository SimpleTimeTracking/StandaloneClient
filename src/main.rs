mod commands;
mod tti;

use serde::Deserialize;
use std::fs::File;
use std::io::{BufRead, BufReader};
use std::str::FromStr;
use tti::TimeTrackingItem;
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
        .size(600, 600)
        .resizable(false)
        .debug(true)
        .user_data(())
        .invoke_handler(|webview, arg| {
            use Cmd::*;

            match serde_json::from_str(arg).unwrap() {
                Init => {
                    let mut stt_file = dirs::home_dir().unwrap();
                    stt_file.push(".stt");
                    stt_file.push("activities");
                    let file = File::open(stt_file).unwrap();
                    let reader = BufReader::new(file);
                    let items: Vec<TimeTrackingItem> = reader
                        .lines()
                        .filter_map(|l| TimeTrackingItem::from_str(&l.unwrap()).ok())
                        .collect();
                    let blub = format!(
                        "app.updateActivities({})",
                        serde_json::to_string(&items).unwrap()
                    );
                    webview.eval(&blub).unwrap();
                }
                AddActivity { activity } => {
                    if !activity.is_empty() {
                        println!("Added activity {}", activity);
                    }
                }
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
