# Code Smell and SOLID Assessment

## Overview
This note captures current design risks related to common code smells and SOLID alignment. It focuses on the GUI view/controller layer and the calendar model implementation, with references to representative code locations.

## Findings

### Large Class and Long Method Risks (GUI)
* `CalendarFrame` is 800+ lines and bundles layout, event rendering, dialog construction, validation, and helper utilities in one class. The event-creation dialog alone spans dozens of lines of input wiring and validation, indicating a need to extract focused dialog/view components and reduce method size.【F:src/main/java/calendar/view/gui/CalendarFrame.java†L200-L447】
* The recurrence handling and time parsing logic in the same class mixes UI concerns with parsing/validation. Extracting a dedicated dialog or form object would help follow SRP and reduce long-method smell.【F:src/main/java/calendar/view/gui/CalendarFrame.java†L450-L612】

### Large Class and Duplicate Logic (Model)
* `CalendarImpl` centralizes calendar state, event creation, series generation, duplicate checks, timezone conversion, and edit workflows in a ~700-line class. Responsibilities overlap across methods and could be decomposed into collaborators (e.g., event factory/normalizer, series generator, duplicate guard) to better align with SRP/Open–Closed.【F:src/main/java/calendar/CalendarImpl.java†L95-L166】
* Normalizing missing end times to working hours is repeated in multiple creation methods. Consolidating this into a shared helper/factory reduces duplication and risk of divergence.【F:src/main/java/calendar/CalendarImpl.java†L95-L166】

## Recommendations
* **Extract GUI subviews/dialogs**: Move event creation/editing forms, recurrence controls, and tooltip/detail rendering into dedicated components to shrink `CalendarFrame` and separate concerns.
* **Isolate parsing/validation**: Create small utility classes for time/recurrence parsing so view code stays declarative, improving testability and adherence to SRP.
* **Decompose `CalendarImpl`**: Introduce collaborators for event normalization, duplication checks, and series construction to reduce class size and ease future changes; consider a parameter object for repeated subject/start/end data to address data clumps.
* **Refine SOLID alignment**: The above decompositions improve Single Responsibility, make extension easier without modifying large classes (Open–Closed), and reduce coupling for Interface Segregation.
