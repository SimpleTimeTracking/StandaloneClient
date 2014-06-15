[![Build Status](https://travis-ci.org/Bytekeeper/SimpleTimeTrack.svg)](https://travis-ci.org/Bytekeeper/SimpleTimeTrack) [ ![Download](https://api.bintray.com/packages/bytekeeper/generic/SimpleTimeTrack/images/download.png) ](https://bintray.com/bytekeeper/generic/SimpleTimeTrack/_latestVersion)

# Mission statement 

The goal of SimpleTimeTrack is to make time tracking as simple and quick as possible.

# Background

Many people struggle with time tracking software which is targeted not at the person tracking his/her time efficiently but on management to provide statistics etc.
To remedy this, there already are some great projects (like http://ti.sharats.me/ ) but they lack some features like integration of other time tracking software.

# Features of SimpleTimeTrack

- modular design to allow easy extension
- simple setup: no complicated installation, just download and start tracking times
- information privacy: the data is located on your machine

# Usage

## Graphical UI

To start SimpleTimeTrack UI, double click SimpleTimeTrack.jar or (if this does not work) start it by command line 
```bash
java -jar path/to/SimpleTimeTrack.jar
```

When starting work on a task, just enter a comment about what you are working on in the text field. Comments can span multiple lines. Then either press CTRL+ENTER or click on "Done". The window closes and the task is stored with the current time as start.

## CLI

To avoid typing a long command every time you want to use SimpleTimeTrack, create a little script and put it in your $PATH
```bash
#!/bin/bash
java -cp path/to/SimpleTimeTrack.jar org.stt.cli.Main $*
```

Autocompletion for Bash (commands and comments) can be achieved by 
FIXME: describe auto completion here

Autocompletion for ZSH (commands and comments):
FIXME: describe auto completion here
```zsh
```

# Configuration

When SimpleTimeTrack is started the first time, a configuration file will be created in your home directory automatically. See the comments for information about what the options do.
The file is $HOME/.sttrc (Linux) resp. %HOME%\.sttrc (Windows)

# Start hacking

to start hacking on SimpleTimeTrack:
- install gradle from http://www.gradle.org/
- make sure you have an Oracle JDK >= 1.7
- clone this repository 
- build it
```bash
git clone https://github.com/Bytekeeper/SimpleTimeTrack.git
cd SimpleTimeTrack
gralde build
```
The created fat jar can be found in build/libs
