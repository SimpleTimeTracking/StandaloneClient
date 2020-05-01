use chrono::prelude::*;
use chrono::Duration;
use nom::branch::alt;
use nom::bytes::complete::tag_no_case;
use nom::character::complete::{anychar, char, digit1, multispace0, multispace1};
use nom::combinator::all_consuming;
use nom::combinator::opt;
use nom::multi::many0;
use nom::multi::many_till;
use nom::sequence::preceded;
use nom::sequence::tuple;
use nom::IResult;
use std::fmt::Display;
use std::iter::FromIterator;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Command {
    Fin(TimeSpec),
    StartActivity(TimeSpec, String),
    ResumeLast(TimeSpec),
}

#[derive(Debug, Clone, PartialEq, Eq, Copy)]
pub enum TimeSpec {
    Relative(Duration),
    Absolute(DateTime<Local>),
    Interval {
        from: DateTime<Local>,
        to: DateTime<Local>,
    },
    Now,
}

type Result<I, T> = IResult<I, T, ErrorKind<I>>;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ErrorKind<I> {
    Nom(I, nom::error::ErrorKind),
    InvalidDateTimeFormat(String),
}

impl TimeSpec {
    pub fn to_date_time(&self) -> DateTime<Local> {
        match self {
            TimeSpec::Now => Local::now(),
            TimeSpec::Absolute(date_time) => *date_time,
            TimeSpec::Relative(delta) => Local::now() + *delta,
            TimeSpec::Interval { from: _, to: _ } => panic!("Can't convert interval to date time"),
        }
    }
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

fn at_absolute_timespec(i: &str) -> Result<&str, TimeSpec> {
    let (i, (_, _, ts)) = tuple((tag_no_case("at"), multispace1, absolute_timespec))(i)?;
    Ok((i, ts))
}

fn fin(i: &str) -> Result<&str, Command> {
    let (i, _) = tag_no_case("fin")(i)?;
    let (i, ts) = opt(preceded(
        multispace1,
        alt((at_absolute_timespec, in_relative_timespec)),
    ))(i)?;
    let ts = if let Some(ts) = ts { ts } else { TimeSpec::Now };
    Ok((i, Command::Fin(ts)))
}

fn hours(i: &str) -> Result<&str, Duration> {
    let (i, (delta, _, _)) = tuple((
        digit1,
        multispace0,
        alt((
            tag_no_case("hours"),
            tag_no_case("hour"),
            tag_no_case("hrs"),
            tag_no_case("hr"),
            tag_no_case("h"),
        )),
    ))(i)?;
    Ok((i, Duration::hours(delta.parse::<i64>().unwrap())))
}

fn minutes(i: &str) -> Result<&str, Duration> {
    let (i, (delta, _, _)) = tuple((
        digit1,
        multispace0,
        alt((
            tag_no_case("minutes"),
            tag_no_case("mins"),
            tag_no_case("min"),
        )),
    ))(i)?;
    Ok((i, Duration::minutes(delta.parse::<i64>().unwrap())))
}

fn seconds(i: &str) -> Result<&str, Duration> {
    let (i, (delta, _, _)) = tuple((
        digit1,
        multispace0,
        alt((
            tag_no_case("seconds"),
            tag_no_case("second"),
            tag_no_case("secs"),
            tag_no_case("sec"),
            tag_no_case("s"),
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
            multispace0(i)?.0
        } else {
            i
        }
    } else {
        let (i, s) = seconds(i)?;
        result = result + s;
        multispace0(i)?.0
    };
    let (i, ago) = opt(tag_no_case("ago"))(i)?;
    let result = if ago.is_some() { -result } else { result };
    Ok((i, result))
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

fn absolute_date_time(i: &str) -> Result<&str, DateTime<Local>> {
    let (i, date) = opt(tuple((date, multispace1)))(i)?;
    let (i, time) = time(i)?;
    let date = if let Some((date, _)) = date {
        date
    } else {
        Local::today()
    };
    Ok((i, date.and_time(time).unwrap()))
}

fn absolute_timespec(i: &str) -> Result<&str, TimeSpec> {
    let (i, dt) = absolute_date_time(i)?;
    Ok((i, TimeSpec::Absolute(dt)))
}

fn negative_relative_timespec(i: &str) -> Result<&str, TimeSpec> {
    let (i, d) = relative_time(i)?;
    Ok((
        i,
        if d < Duration::zero() {
            TimeSpec::Relative(d)
        } else {
            TimeSpec::Relative(-d)
        },
    ))
}

fn from_to(i: &str) -> Result<&str, TimeSpec> {
    let (i, _) = alt((tag_no_case("since"), tag_no_case("from")))(i)?;
    let (i, from) = preceded(multispace1, absolute_date_time)(i)?;
    let (i, _) = preceded(multispace1, alt((tag_no_case("to"), tag_no_case("until"))))(i)?;
    let (i, to) = preceded(multispace1, absolute_date_time)(i)?;
    Ok((i, TimeSpec::Interval { from, to }))
}

fn since(i: &str) -> Result<&str, TimeSpec> {
    let (i, (_, ts)) = tuple((
        opt(tuple((
            alt((tag_no_case("since"), tag_no_case("from"))),
            multispace1,
        ))),
        alt((negative_relative_timespec, absolute_timespec)),
    ))(i)?;
    Ok((i, ts))
}

fn in_relative_time(i: &str) -> Result<&str, Duration> {
    let (i, (_, _, d)) = tuple((tag_no_case("in"), multispace1, relative_time))(i)?;
    if d < Duration::zero() {
        Err(nom::Err::Error(ErrorKind::InvalidDateTimeFormat(
            "Future time spec with relative time in the past".to_string(),
        )))
    } else {
        Ok((i, d))
    }
}
fn in_relative_timespec(i: &str) -> Result<&str, TimeSpec> {
    let (i, d) = in_relative_time(i)?;
    Ok((i, TimeSpec::Relative(d)))
}

fn timespec(i: &str) -> Result<&str, TimeSpec> {
    Ok(alt((in_relative_timespec, from_to, since))(i)?)
}

fn activity_now(i: &str) -> Result<&str, (Vec<char>, TimeSpec)> {
    let (i, activity) = many0(anychar)(i)?;
    Ok((i, (activity, TimeSpec::Now)))
}

fn start_activity(i: &str) -> Result<&str, Command> {
    let (i, (chars, ts)) = alt((
        many_till(anychar, preceded(multispace1, all_consuming(timespec))),
        activity_now,
    ))(i)?;
    Ok((i, Command::StartActivity(ts, String::from_iter(chars))))
}

fn resume_last(i: &str) -> Result<&str, Command> {
    let (i, (_, _, _, ts)) = tuple((
        tag_no_case("resume"),
        multispace1,
        tag_no_case("last"),
        opt(tuple((multispace1, timespec))),
    ))(i)?;
    let ts = if let Some((_, ts)) = ts {
        ts
    } else {
        TimeSpec::Now
    };
    Ok((i, Command::ResumeLast(ts)))
}

pub fn command(i: &str) -> Result<&str, Command> {
    all_consuming(alt((fin, resume_last, start_activity)))(i)
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
        let m = timespec("since 2min 17secs");

        // THEN
        assert_eq!(Ok(("", TimeSpec::Relative(Duration::seconds(-137)))), m)
    }

    #[test]
    fn should_parse_from_absolute() {
        // GIVEN

        // WHEN
        let m = timespec("from 2010.10.12 12:02:01");

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
        let m = timespec("since 2010.10.12 12:02:00");

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
        let m = timespec("from 12:02:03");

        // THEN
        let time = match m {
            Ok(("", TimeSpec::Absolute(date))) => date.time(),
            _ => panic!("Unexpected result"),
        };
        assert_eq!(time, NaiveTime::from_hms(12, 02, 03));
    }

    #[test]
    fn should_parse_since_absolute_yms_hm() {
        // GIVEN

        // WHEN
        let m = timespec("since 2010.10.12 12:02");

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
    fn should_parse_until() {
        // GIVEN

        // WHEN
        let m = timespec("since 2010.10.12 12:02 until 2011.10.13 13:03");

        // THEN
        assert_eq!(
            Ok((
                "",
                TimeSpec::Interval {
                    from: Local.ymd(2010, 10, 12).and_hms(12, 02, 00),
                    to: Local.ymd(2011, 10, 13).and_hms(13, 03, 00)
                }
            )),
            m
        )
    }

    #[test]
    fn should_parse_activity_since() {
        // GIVEN

        // WHEN
        let res = command("argl since 5min");

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
        let res = command("argl since 5min but actually not");

        // THEN
        assert_eq!(
            res,
            Ok((
                "",
                Command::StartActivity(
                    TimeSpec::Now,
                    "argl since 5min but actually not".to_string()
                )
            ))
        );
    }

    #[test]
    fn should_parse_resume_last() {
        // GIVEN

        // WHEN
        let res = command("resume last 5min ago");

        // THEN
        assert_eq!(
            res,
            Ok((
                "",
                Command::ResumeLast(TimeSpec::Relative(Duration::minutes(-5)))
            ))
        );
    }

    #[test]
    fn should_parse_just_fin() {
        // GIVEN

        // WHEN
        let res = command("fin");

        // THEN
        assert_eq!(res, Ok(("", Command::Fin(TimeSpec::Now))));
    }

    #[test]
    fn should_parse_fin_in_relative() {
        // GIVEN

        // WHEN
        let res = command("fin in 7min");

        // THEN
        assert_eq!(
            res,
            Ok(("", Command::Fin(TimeSpec::Relative(Duration::minutes(7)))))
        );
    }

    #[test]
    fn should_parse_fin_at_absolute() {
        // GIVEN

        // WHEN
        let res = command("fin at 12:12");

        // THEN
        match res {
            Ok((_, Command::Fin(TimeSpec::Absolute(dt)))) => {
                assert_eq!(dt.time(), NaiveTime::from_hms(12, 12, 0))
            }
            _ => panic!(format!("Invalid result: {:?}", res)),
        }
    }
}
