# ALPACA
ExAssist is an Android Studio plugin for recommending exception handling patterns.  
Copyright (C) 2017-2018 SIS Lab, Auburn University

This program is free software. 

Contact: tam@auburn.edu 
  
* [Poster at ICSE 2018 (pre-print)](https://bitbucket.org/tamnguyenthe/exassist/raw/b33885b9b5aa5cb64dd7ddbed5a73c15fe675412/resources/paper/ICSE_Poster_from_Research_Track_2018_32.pdf)
* [Video Demo](https://www.youtube.com/watch?v=J8JZtrWc3yE)

## Introduction
**ExAssist** is a code recommendation tool for exception handling and is released as a plugin of IntelliJ IDEA and Android Studio. 
**ExAssist** predicts what types of exception could occur in a given piece of code and recommends proper exception handling code for such an exception. 
When requested, it will add such code into the given piece of code.
After installation, it is incorporated with the IDE and users can invoke it directly via shortcut key Ctrl + Alt + {R, H} or via the menu bar.
## Installation
1. Download the plugin installation file ([ExAssist.zip](https://bitbucket.org/tamnguyenthe/exassist_repo/raw/87732c699dbb1c3f65232f9b69cfe77663f1f808/ExAssist.zip))
2. Press Ctrl+Alt+S or choose File | Settings (for Windows and Linux) or IntelliJ IDEA | Preferences (for macOS) from the main menu, and then go to Plugins.

![Step 2](https://bitbucket.org/tamnguyenthe/exassist_repo/raw/master/resources/figures/Step2.PNG)

3. Click the Install plugin from disk.

![Step 3](https://bitbucket.org/tamnguyenthe/exassist_repo/raw/master/resources/figures/Step3.PNG)

4. In the dialog that opens, navigate to the location of the downloaded installation file.
5. Confirm your intention to install the selected plugin.
6. Click Close.
7. Click OK in the Settings dialog and restart IntelliJ IDEA for the changes to take effect.

![Step 4](https://bitbucket.org/tamnguyenthe/exassist_repo/raw/master/resources/figures/Step4.PNG)

## Usage
### Recommending Exception Types
The figure below shows the usage of ExAssist in Recommending Exception Types. 

![Figure 1](https://bitbucket.org/tamnguyenthe/exassist_repo/raw/master/resources/figures/first_usage.png)

Assume a developer is writing
code to open and get data from a database. developer is aware that the code is dealing with database and Cursor
objects might throw unchecked exceptions at runtime, but she
might be unsure whether to catch exceptions on the code and which
type of exception to catch. The built-in exception checker in Android
Studio only supports adding checked exceptions, thus, does
not help her to make appropriate action in this case.
ExAssist aims to support the developer to make decisions whether
or not to add a try-catch block and what type of exception to caught.
The developer invokes ExAssist by first selecting the portion of
code that she wants to check for exception then pressing Ctrl + Alt + R. Figure 1 shows a screenshot of Android Studio with ExAssist
invoked for the portion of code that using the Cursor object for
reading data from database. 

As seen, ExAssist suggests that the code
is likely to throw an unchecked exception. It also displays a ranked
list of unchecked exceptions that could be thrown from the current
selecting code. Each unchecked exception in the ranked list has a
confident score represents how likely the exception will be thrown
from the code. The value for confident scores is between 0 and 1.
The higher the value of the confident score, the higher likelihood
the exception type is thrown. In this example, SQLiteException has
the highest score of 0.80. If the developer chooses that exception
type, the currently selected code will be wrapped in a try-catch
block with SQLiteException in the catch expression.

ExAssist uses the context of current selecting code to infer
whether or not adding exception handling code and the type of
the exception. For example, in the figure below, the context changes as the
developer selects the portion of code for opening and querying on
the SQLiteDatabase object. 

![Figure 2](https://bitbucket.org/tamnguyenthe/exassist_repo/raw/master/resources/figures/second_usage.png)

Thus, ExAssist updates the recommendation
list with SQLException has the highest confident of 0.81,
which is highest among all other exception types.
ExAssist could provide recommendations for a selected portion
of code includes one or multiple method calls. Additionally, ExAssist
could also recommend not to add try-catch block if it infers
that the selected code is very unlikely to throw an unchecked
exception. For example, if the developer selects the statement bookTitles.add(bookname);
and queries ExAssist, the tool will return an
empty list of exceptions as it is very unlikely the selected method
throws exceptions when it is executed.

### Recommending Exception Repairs
Handling exception situations and executing necessary recovery actions are important as 
it could help apps continue to run properly when an exception
occurs. For example, when an app reuses resources such as database
connections or files, the app should release the resources if
an exception is thrown. ExAssist is also designed to recommend
such repairing actions in the exception handling code based on the
context in the try block. The figure below demonstrates an usage of ExAssist in the task. 

![Figure 3](https://bitbucket.org/tamnguyenthe/exassist_repo/raw/master/resources/figures/third_usage.png)

After adding a try-catch block with SQLiteException for the code in the previous scenario, 
the developer wants to perform recovery actions.
To invoke ExAssist, she moves the cursor to the first line of the catch
and presses Ctrl + Alt + H. ExAssist then will analyze the context
of the code and provide repairing actions in the recommendation
windows. In the example, ExAssist detects that the Cursor object
should be closed to release all of its resources and making it invalid
for further usages. It also suggests to set bookTitles equals null to
indicate the error while collecting data from cursor. If the developer
chooses the recommended actions, ExAssist will generate the code
in the catch block as in the Figure above.

## Data
1. Our empirical data is stored under resources/empirical_data. The folder contains:
1.1. ExceptionBugFixes.xlsx: 380 exception bug fixes across 10 projects
1.2. 4000Apps.csv: List of 4000 android apps used in our empirical study and evaluation.
2. Our data for XRank model is stored under resourses/xrank
3. Our data for XHand model is stored under resourses/xhand
