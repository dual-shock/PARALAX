Use case #2: Trajectory simulation

System: Android application collecting weather data from MET APIs
Primary actor: A engineer looking to launch a rocket
Secondary actor: Meteorological Institute, weather data APIs
Goals: See a visual representation of a rockets predicted flight. Get an understanding of where it will land and which areas are affected
Stakeholders: Portal Space, MET, UIO
Preconditions: User has created and saved a launch site and selected a time to view.
Trigger: User taps "Calculate trajectory"

Scenarios:

Basic flow - User is viewing weather details for the upcoming hour at their designated launch site. While viewing the details, th user wishes to see
the predicted trajectory their rocket would take. So they tap to calculate trajectory. The app responds by navigating user to a new screen. Here the user is asked
to provide the details of the rocket they plan to launch, such as weight, thrust, fuel capacity etc. Once the user has provided information about the rocket, the app starts
calculating the trajectory. When finished it is presented to the user as curve above a 3-D rendered map.

Alternate flow 1 - User has already saved a rocket as default, and the app skips the configuration step.

Alternate flow 2 - The app fails to load weather data and is unable to calculate trajectory