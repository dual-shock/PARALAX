Use case 1#: Check launch viability

System: Android application collecting weather data from MET APIs
Primary actor: A engineer looking to launch a rocket
Secondary actor: Meteorological Institute, weather data APIs
Goals: Quickly get information on whether it is safe to launch a rocket in the next 4 hours
Stakeholders: Portal Space, MET, UIO
Preconditions: user downloads the app
Trigger: User opens application

Scenarios:

Basic flow - User opens system and the app connects to associated weather APIs to collect data.
From there the user chooses the location for their launch pad using coordinates. On choice, the app loads the data associated with the location
and compares the weather conditions to the set limits to provide a viability score for the user. The app presents scores for all the upcoming hours.
User checks scores and chooses the one that suits their needs or decides none of them work.

Alternate flow 1:
User opens system and the app tries to connect to weather APIs to fetch data. Connection fails and the app fails to retrieve data.
User selects coordinates but no data shows. User left wondering whether it is safe to launch.