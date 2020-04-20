use chrono::format::{Item, ParseResult, Parsed};
use chrono::prelude::*;
use chrono::Duration;
use lazy_static::lazy_static;
use nom::branch::alt;
use nom::bytes::complete::tag;
use nom::character::complete::anychar;
use nom::character::complete::digit1;
use nom::character::complete::{multispace0, multispace1};
use nom::combinator::all_consuming;
use nom::combinator::opt;
use nom::eof;
use nom::multi::many_till;
use nom::sequence::tuple;
use nom::IResult;
use std::iter::FromIterator;

lazy_static! {
    static ref FORMAT: Vec<Item<'static>> =
        chrono::format::strftime::StrftimeItems::new("%Y.%m.%d %H:%M:%S").collect();
}

#[derive(Debug, Clone, PartialEq, Eq)]
enum Command {
    Fin,
    StartActivity(TimeSpec, String),
}

#[derive(Debug, Clone, PartialEq, Eq, Copy)]
enum TimeSpec {
    Relative(Duration),
    Absolute(DateTime<Local>),
    Now,
}

fn fin(i: &str) -> IResult<&str, Command> {
    tag("fin")(i)?;
    Ok((i, Command::Fin))
}

fn hours(i: &str) -> IResult<&str, Duration> {
    let (i, (delta, _, _)) = tuple((
        digit1,
        multispace0,
        alt((tag("hours"), tag("hour"), tag("hrs"), tag("hr"), tag("h"))),
    ))(i)?;
    Ok((i, Duration::hours(delta.parse::<i64>().unwrap())))
}

fn minutes(i: &str) -> IResult<&str, Duration> {
    let (i, (delta, _, _)) = tuple((
        digit1,
        multispace0,
        alt((tag("minutes"), tag("mins"), tag("min"))),
    ))(i)?;
    Ok((i, Duration::minutes(delta.parse::<i64>().unwrap())))
}

fn seconds(i: &str) -> IResult<&str, Duration> {
    let (i, (delta, _, _)) = tuple((
        digit1,
        multispace0,
        alt((
            tag("seconds"),
            tag("second"),
            tag("secs"),
            tag("sec"),
            tag("s"),
        )),
    ))(i)?;
    Ok((i, Duration::seconds(delta.parse::<i64>().unwrap())))
}

fn relative_time(i: &str) -> IResult<&str, Duration> {
    let mut result = Duration::zero();
    let mut hit = false;
    let (i, h) = opt(hours)(i)?;
    let i = if let Some(h) = h {
        result = result + h;
        hit = true;
        multispace0(i)?.0
    } else {
        i
    };

    let (i, m) = opt(minutes)(i)?;
    let i = if let Some(m) = m {
        result = result + m;
        hit = true;
        multispace0(i)?.0
    } else {
        i
    };
    let i = if hit {
        let (i, s) = opt(seconds)(i)?;
        if let Some(s) = s {
            result = result + s;
        }
        i
    } else {
        let (i, s) = seconds(i)?;
        result = result + s;
        i
    };
    Ok((i, result))
}

fn absolute_timespec(i: &str) -> IResult<&str, TimeSpec> {
    Ok((i, TimeSpec::Absolute(Local::now())))
}

fn negative_relative_timespec(i: &str) -> IResult<&str, TimeSpec> {
    let (i, d) = relative_time(i)?;
    Ok((i, TimeSpec::Relative(-d)))
}

fn since(i: &str) -> IResult<&str, TimeSpec> {
    let (i, (_, ts)) = tuple((
        opt(tuple((tag("since"), multispace1))),
        alt((negative_relative_timespec, absolute_timespec)),
    ))(i)?;
    Ok((i, ts))
}

fn since_or_end(i: &str) -> IResult<&str, Option<TimeSpec>> {
    if i.is_empty() {
        Ok((i, None))
    } else {
        let (i, ts) = all_consuming(since)(i)?;
        Ok((i, Some(ts)))
    }
}

fn activity(i: &str) -> IResult<&str, Command> {
    let (i, (chars, ts)) = many_till(anychar, since_or_end)(i)?;
    println!("{:?} {:?}", ts, chars);
    let ts = if let Some(ts) = ts { ts } else { TimeSpec::Now };
    let mut activity = String::from_iter(chars);
    activity.pop();
    Ok((i, Command::StartActivity(ts, activity)))
}

fn command(i: &str) -> IResult<&str, Command> {
    alt((fin, activity))(i)
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn should_parse_hours() {
        // GIVEN

        // WHEN
        let h = hours("5 h");

        // THEN
        assert_eq!(Ok(("", Duration::hours(5))), h)
    }

    #[test]
    fn should_parse_minutes() {
        // GIVEN

        // WHEN
        let m = minutes("15 mins");

        // THEN
        assert_eq!(Ok(("", Duration::minutes(15))), m)
    }

    #[test]
    fn should_parse_seconds() {
        // GIVEN

        // WHEN
        let m = seconds("23   s");

        // THEN
        assert_eq!(Ok(("", Duration::seconds(23))), m)
    }

    #[test]
    fn should_parse_hms_relative_time() {
        // GIVEN

        // WHEN
        let m = relative_time("1hours 2minutes 17secs");

        // THEN
        assert_eq!(Ok(("", Duration::seconds(3737))), m)
    }

    #[test]
    fn should_parse_hs_relative_time() {
        // GIVEN

        // WHEN
        let m = relative_time("1hours 17sec");

        // THEN
        assert_eq!(Ok(("", Duration::seconds(3617))), m)
    }

    #[test]
    fn should_parse_ms_relative_time() {
        // GIVEN

        // WHEN
        let m = relative_time("2min 17secs");

        // THEN
        assert_eq!(Ok(("", Duration::seconds(137))), m)
    }

    #[test]
    fn should_parse_hm_relative_time() {
        // GIVEN

        // WHEN
        let m = relative_time("2hour 17min");

        // THEN
        assert_eq!(Ok(("", Duration::seconds(8220))), m)
    }

    #[test]
    fn should_parse_m_relative_time() {
        // GIVEN

        // WHEN
        let m = relative_time("11min");

        // THEN
        assert_eq!(Ok(("", Duration::seconds(660))), m)
    }

    #[test]
    fn should_parse_since() {
        // GIVEN

        // WHEN
        let m = since("since 2min 17secs");

        // THEN
        assert_eq!(Ok(("", TimeSpec::Relative(Duration::seconds(-137)))), m)
    }

    #[test]
    fn should_parse_activity_since() {
        // GIVEN

        // WHEN
        let res = activity("argl since 5min");

        // THEN
        assert_eq!(
            res,
            Ok((
                "",
                Command::StartActivity(
                    TimeSpec::Relative(Duration::minutes(-5)),
                    "argl".to_string()
                )
            ))
        );
    }

    #[test]
    fn should_parse_activity_without_timespec() {
        // GIVEN

        // WHEN
        let res = activity("argl since 5min but actually not");

        // THEN
        assert_eq!(
            res,
            Ok((
                "",
                Command::StartActivity(
                    TimeSpec::Now,
                    "argl since 5min but actually no".to_string()
                )
            ))
        );
    }
}
