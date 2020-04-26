use chrono::prelude::*;
use chrono::Duration;
use nom::branch::alt;
use nom::bytes::complete::tag;
use nom::character::complete::{anychar, char, digit1, multispace0, multispace1};
use nom::combinator::all_consuming;
use nom::combinator::opt;
use nom::multi::many_till;
use nom::sequence::tuple;
use nom::IResult;
use std::fmt::Display;
use std::iter::FromIterator;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Command {
    Fin,
    StartActivity(TimeSpec, String),
}

#[derive(Debug, Clone, PartialEq, Eq, Copy)]
pub enum TimeSpec {
    Relative(Duration),
    Absolute(DateTime<Local>),
    Now,
}

fn fin(i: &str) -> Result<&str, Command> {
    tag("fin")(i)?;
    Ok((i, Command::Fin))
}

fn hours(i: &str) -> Result<&str, Duration> {
    let (i, (delta, _, _)) = tuple((
        digit1,
        multispace0,
        alt((tag("hours"), tag("hour"), tag("hrs"), tag("hr"), tag("h"))),
    ))(i)?;
    Ok((i, Duration::hours(delta.parse::<i64>().unwrap())))
}

fn minutes(i: &str) -> Result<&str, Duration> {
    let (i, (delta, _, _)) = tuple((
        digit1,
        multispace0,
        alt((tag("minutes"), tag("mins"), tag("min"))),
    ))(i)?;
    Ok((i, Duration::minutes(delta.parse::<i64>().unwrap())))
}

fn seconds(i: &str) -> Result<&str, Duration> {
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

fn relative_time(i: &str) -> Result<&str, Duration> {
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

type Result<I, T> = IResult<I, T, ErrorKind<I>>;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ErrorKind<I> {
    Nom(I, nom::error::ErrorKind),
    InvalidDateTimeFormat(String),
}

impl<I> nom::error::ParseError<I> for ErrorKind<I> {
    fn from_error_kind(input: I, kind: nom::error::ErrorKind) -> Self {
        ErrorKind::Nom(input, kind)
    }

    fn append(_: I, _: nom::error::ErrorKind, other: Self) -> Self {
        other
    }
}

fn to_nom_err<F: Display, I>(err: F) -> nom::Err<ErrorKind<I>> {
    nom::Err::Error(ErrorKind::InvalidDateTimeFormat(err.to_string()))
}

fn date(i: &str) -> Result<&str, Date<Local>> {
    let (i, (y, _, m, _, d)) = tuple((digit1, char('.'), digit1, char('.'), digit1))(i)?;
    let y = y.parse::<i32>().map_err(to_nom_err)?;
    let m = m.parse::<u32>().map_err(to_nom_err)?;
    let d = d.parse::<u32>().map_err(to_nom_err)?;
    Ok((i, Local.ymd(y, m, d)))
}

fn time(i: &str) -> Result<&str, NaiveTime> {
    let (i, (h, _, m)) = tuple((digit1, char(':'), digit1))(i)?;
    let (i, s) = opt(tuple((char(':'), digit1)))(i)?;
    let s = if let Some((_, s)) = s {
        s.parse::<u32>().map_err(to_nom_err)?
    } else {
        0
    };
    let h = h.parse::<u32>().map_err(to_nom_err)?;
    let m = m.parse::<u32>().map_err(to_nom_err)?;
    Ok((i, NaiveTime::from_hms(h, m, s)))
}

fn absolute_timespec(i: &str) -> Result<&str, TimeSpec> {
    let (i, date) = opt(tuple((date, multispace1)))(i)?;
    let (i, time) = time(i)?;
    let date = if let Some((date, _)) = date {
        date
    } else {
        Local::today()
    };
    let date_time = date.and_time(time).unwrap();
    Ok((i, TimeSpec::Absolute(date_time)))
}

fn negative_relative_timespec(i: &str) -> Result<&str, TimeSpec> {
    let (i, d) = relative_time(i)?;
    Ok((i, TimeSpec::Relative(-d)))
}

fn since_or_from(i: &str) -> Result<&str, TimeSpec> {
    let (i, (_, ts)) = tuple((
        opt(tuple((alt((tag("since"), tag("from"))), multispace1))),
        alt((negative_relative_timespec, absolute_timespec)),
    ))(i)?;
    Ok((i, ts))
}

fn since_or_end(i: &str) -> Result<&str, Option<TimeSpec>> {
    if i.is_empty() {
        Ok((i, None))
    } else {
        let (i, ts) = all_consuming(since_or_from)(i)?;
        Ok((i, Some(ts)))
    }
}

fn activity(i: &str) -> Result<&str, Command> {
    let (i, (chars, ts)) = many_till(anychar, since_or_end)(i)?;
    let ts = if let Some(ts) = ts { ts } else { TimeSpec::Now };
    let mut activity = String::from_iter(chars);
    activity.pop();
    Ok((i, Command::StartActivity(ts, activity)))
}

pub fn command(i: &str) -> Result<&str, Command> {
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
        let m = since_or_from("since 2min 17secs");

        // THEN
        assert_eq!(Ok(("", TimeSpec::Relative(Duration::seconds(-137)))), m)
    }

    #[test]
    fn should_parse_from_absolute() {
        // GIVEN

        // WHEN
        let m = since_or_from("from 2010.10.12 12:02:01");

        // THEN
        assert_eq!(
            Ok((
                "",
                TimeSpec::Absolute(Local.ymd(2010, 10, 12).and_hms(12, 02, 01))
            )),
            m
        )
    }

    #[test]
    fn should_parse_since_absolute_ymd_hms() {
        // GIVEN

        // WHEN
        let m = since_or_from("since 2010.10.12 12:02:00");

        // THEN
        assert_eq!(
            Ok((
                "",
                TimeSpec::Absolute(Local.ymd(2010, 10, 12).and_hms(12, 02, 00))
            )),
            m
        )
    }

    #[test]
    fn should_parse_from_absolute_hms() {
        // GIVEN

        // WHEN
        let m = since_or_from("from 12:02:03");

        // THEN
        let time = match m {
            Ok(("", TimeSpec::Absolute(date))) => date.time(),
            _ => panic!("Unexpected result"),
        };
        assert_eq!(time, NaiveTime::from_hms(12, 02, 03));
    }

    #[test]
    fn should_parse_sinc_absolute_yms_hm() {
        // GIVEN

        // WHEN
        let m = since_or_from("since 2010.10.12 12:02");

        // THEN
        assert_eq!(
            Ok((
                "",
                TimeSpec::Absolute(Local.ymd(2010, 10, 12).and_hms(12, 02, 00))
            )),
            m
        )
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
