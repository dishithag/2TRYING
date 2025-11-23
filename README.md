# 1 Introduction

In this assignment, we will extend the functionality of the calendar 
application with additional features. Hopefully your current design
of the calendar application is flexible and scalable in a way that will
allow you to incorporate the additional requirements with minimal changes to the existing implementation.

# 2 Support multiple calendars

The previous iteration of the calendar application allowed users
to create, modify, and query events for a single calendar. You must now extend the capability of your application to support multiple calendars. 

The application should support the ability to create and maintain several calendars, each with its own set of events and a unique name. The calendar name will be used to identify the calendar. It should be possible to edit the name of a calendar. Two calendars cannot have the same name.

# 3 Support Timezones

Currently the times specified for events are assumed to be in reference to a default timezone (presumably EST). You must now support the ability to manage different timezones. 

Specifically, each calendar is associated with a specific timezone. All events in that calendar are assumed to be in its timezone (and thus, timezone does not have to be specified when dealing with events). However it should be possible to change the timezone of a calendar.


# 4 Copy events

Several users suggested that given the application can support multiple calendars, they should be able to copy events to another timeline, even across calendars. For example, the order of lecture topics in CS 5010 stay the same from one semester to another, so these events could simply be copied *en-masse* to a time line that starts with a specified date (for the next semester). For example, *copy all events for lecture topics from September 5 2024 to December 18 2024 (Fall 2024 semester) to a place in the calendar that starts at January 8 2025 (start of the Spring 2025 semester)*.

Based on this need you must add support to copy a single or recurring event  from a specified (source) calendar to a specified (starting) date and time in a specified (target) calendar. You must also support copy all events within a specified interval to a specified (starting) date and time. In both cases, the source and target calendars may or may not be the same.

Copying an event to a target calendar must be considered a new event in the target calendar, and therefore conflicts must be managed consistent with the above.

# 5 Support for exporting in iCal format

The application should also be able to export the calendar to a iCal file (in addition to CSV). The expected format of the iCal file must conform
to the file format specs listed [here](https://support.google.com/calendar/answer/37118?hl=en&co=GENIE.Platform%3DDesktop&oco=1#zippy=%2Ccreate-or-edit-an-icalendar-file).

You can verify the correctness of the iCal file by uploading it to Google Calendar. If the upload is successful, you should be able to see the events in Google calendar.

When exporting the calendar the program should automatically detect the export format by detecting the extension of the filename. For example:

`export cal fileName.csv` should save in CSV format
`export cal fileName.ical` should save in iCal format

In both cases the absolute path of the exported file must be printed so a user knows where to find this file.

# 6 Support for additional commands

The following commands must be supported in the text-based
interface:

`create calendar --name <calName> --timezone area/location`

This command will create a new calendar with a unique name 
and timezone as specified by the user. The expected timezone
format is the [IANA Time Zone Database format](https://en.wikipedia.org/wiki/List_of_tz_database_time_zones). In this format the timezone is specified as "area/location". Few examples include "America/New_York", "Europe/Paris", "Asia/Kolkata", "Australia/Sydney", "Africa/Cairo", etc. The command is invalid if the user provides a non-unique calendar name or an unsupported timezone. 

`edit calendar --name <name-of-calendar> --property <property-name> <new-property-value>`

This command is used to change/modify an existing property (`name` or `timezone`) of the 
calendar. The command is invalid if the property being changed is absent
or the value is invalid in the context of the property.

`use calendar --name <name-of-calendar>`

A user can create/edit/print/export events in the context of a
calendar. They can use this command to set the calendar context.
Note this means that the commands in the previous iteration only
make sense when a calendar is in use (i.e. some calendar must be in use for them to work). Otherwise, they are invalid.

`copy event <eventName> on <dateStringTtimeString> --target <calendarName> to <dateStringTtimeString>`

The command is used to copy a specific event with the given name and
start date/time from the current calendar to the target calendar to start at the specified date/time. The "to" date/time is assumed to be specified in the timezone of the target calendar. 

`copy events on <dateString> --target <calendarName> to <dateString>  `

This command has the same behavior as the `copy event` above, except it 
copies all events scheduled on that day. The times physically remain the same, except they are converted to the timezone of the target calendar (e.g. an event that starts at 2pm in the source calendar which is in EST would start at 11am in the destination calendar which is in PST).

`copy events between <dateString> and <dateString> --target <calendarName> to <dateString>  `

The command has the same behavior as the other copy commands, except it copies all events scheduled in the specified date interval (i.e. overlaps with the specified interval). The date string in the target calendar corresponds to the start of the interval. The endpoint dates of the interval are inclusive.

In both the `copy events` commands, if an event series partly overlaps with the specified range, only those events in the series that overlap with the specified range should be copied, and their status as part of a series should be retained in the destination calendar.

# 7 Miscellaneous Requirements and Recommendations

## 7.1 Design Considerations

As you add these features, pay attention to your design. 

   * What changes in your existing design do you make, and why?

   * What is the best way to incorporate new features into an existing application? Do you support all features, or do you release incremental versions with different features (e.g. Starter Edition, Pro Edition, etc.)?

   * As you make changes, are you still adhering to the MVC architecture? Are you putting each class and operation where it belongs?

   * Have your design enhancements gone beyond the specific operations to be implemented? How easy would it be to add similar extensions in the future?

   * Whatever design choices you make, what are its advantages and limitations?
 
While you are allowed to change your design, you should be able to justify it. The bigger the change, the better we expect your justification to be. Please document and justify each change in a README file (maintain this file as you complete this assignment, rather than summarize after you are done and risk missing something). 

## 7.2 Testing

You are expected to test each part of the program that you have written. Accordingly, you are expected to test your model, controller and if applicable, your view. Use of mocks to test the controller is highly recommended, but not required. 

You are not allowed to use external tools (i.e. classes not within the JDK), with the exception of the JUnit testing framework.

## 7.3 Prepare JAR

A user should be able to run your application using a JAR file. To create a JAR file run the command `./gradlew jar`. This will create a JAR file in the build/libs directory. You can run the jar using the command `java -jar build/libs/JARNAME.jar`. You can provide arguments after the jar file path.

You should assume that the user will run your program from this project's root. You must ensure that file paths that your program relies on are platform independent.

# 8 What to Submit

This is a group assignment. The submission should be made to the group repo created on Pawtograder for this assignment. 

You must submit the following to be considered for grading:

- A directory called **res/** with the following:
  - A screenshot of the Google Calendar with your events
  - A txt file, **commands.txt**, with the list of valid commands.
  - A txt file, **invalid.txt** with a list of commands where at least one command is invalid.
  - A class diagram representing the structure of the application's design. The class diagram should show names of classes and interfaces, signature of methods and relationships (inheritance and composition). Do not show field names they may clutter the diagram. We expect these to be not hand-scribbled, you stand to lose points for submitting a picture of a hand-drawn diagram.
- A **USEME.md file** with instructions to run the JAR file with arguments. Make sure to provide example commands. 
- The main method must be in the class 'src/main/java/CalendarRunner.java'.
- Complete the anonymous [peer evaluation survey](https://forms.gle/V12tTqsjmPdZ5xv69). You do not need to take this survey if you are working alone.
	
# Grading standards
 
For this assignment, you will be graded on 

- completeness of your submission, including all the expected examples

- adherence to an MVC design

- the design of your interface(s), in terms of clarity, flexibility, and how plausibly it will support needed functionality;

- the appropriateness of your representation choices for the data, and the adequacy of any documented class invariants (please comment on why you chose the representation you did in the code);

-  the forward thinking in your design, in terms of its flexibility, use of abstraction, etc.

-  the correctness and style of your implementation, and

-  the comprehensiveness and correctness of your test coverage.

**Make sure there are no binary files in your `src` folder! Pawtograder only expects text files (source code) in this folder, so binary files will result in a rejection of your submission. Remember that most image formats are binary.**

# Grading standards
 
For this assignment, you will be graded on 

- completeness of your submission, including all the expected examples

- adherence to an MVC design

- the design of your interface(s), in terms of clarity, flexibility, and how plausibly it will support needed functionality;

- the appropriateness of your representation choices for the data, and the adequacy of any documented class invariants (please comment on why you chose the representation you did in the code);

-  the forward thinking in your design, in terms of its flexibility, use of abstraction, etc.

-  the correctness and style of your implementation, and

-  the comprehensiveness and correctness of your test coverage.
