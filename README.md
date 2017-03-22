[![Build Status](https://travis-ci.org/SimpleTimeTracking/StandaloneClient.svg)](https://travis-ci.org/SimpleTimeTracking/StandaloneClient) 

![Main window](https://raw.githubusercontent.com/SimpleTimeTracking/StandaloneClient/master/doc/MainApp.png)

# Mission statement 

The goal of STT is to make time tracking as simple and quick as possible.

# Background

Many people struggle with time tracking software which is targeted not at the person tracking his/her time efficiently but on management to provide statistics etc.
To remedy this, there already are some great projects (like http://ti.sharats.me/ ) but they lack some features like reporting/aggregation of tasks or a quick way of copy & pasting recorded times to other places. 

The creators of STT are not very excited about the time tracking software they have to use in their company, so they try to implement the least annoying time tracking software from which they can copy & paste tracked time into other systems (like the "not exciting one" ;-) ).

# Features of STT

- modular design to allow easy extension
- simple setup: no complicated installation, just download and start tracking times
- information privacy: the data is located on your machine
- simple data storage format: it is just a text file with one time tracking record per line
- powerful and customizable parser for commands using [ANTLR](http://www.antlr.org/)
- Graphical and command line interface. Use what suits you more

# Usage

## Graphical UI

To start STT UI, double click STT.jar or (if this does not work) start it by command line 
```bash
java -jar path/to/STT.jar
```

Also see https://github.com/SimpleTimeTracking/StandaloneClient/wiki/Intro for other options for launching.

When starting work on a task, just enter a activity about what you are working on in the text field. Comments can span multiple lines. Then either press CTRL+ENTER or click on "Done". The window closes and the task is stored with the current time as start.

## CLI

To avoid typing a long command every time you want to use STT, create a little script and put it in your $PATH
```bash
#!/bin/bash
java -cp path/to/STT.jar org.stt.cli.Main $*
```

See https://github.com/SimpleTimeTracking/StandaloneClient/wiki/CLI for additional information about the CLI and available commands

# Configuration

When STT is started the first time, a configuration file will be created in your home directory automatically. 
The file is $HOME/.stt/stt.yaml (Linux) resp. %HOME%\.stt\stt.yaml (Windows)

# Start hacking

to start hacking on STT:
- make sure you have an Oracle JDK >= 1.8
- clone this repository 
- build it
```bash
git clone https://github.com/SimpleTimeTracking/StandaloneClient.git
cd StandaloneClient
gradlew build
```
The created fat jar can be found in build/libs

# License

STT is licensed under the GPLv3. See LICENSE.txt for the license itself.
