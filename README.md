# PIVOT. Bots

PIVOT. Bots is a framework to create chatbot-based automatic assessments.
![alt assessment](http://i.imgur.com/a7JWbwd.png)
The bots learn from domain-specific text you provide by extracting keywords and questions. 
In the dialogue later, they evaluate users based on measuring associated sentiments in the answers given.

# Features
  - Create, import or export separate profiles and bots.
  - Teach bots with domain-specific corpora.
  - Generate and/or retrieve questions with extracted data.
  - Assess during interaction by sentiment analysis.
  - In-depth graphical result presentation.

# Run
You need [Java](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (Win/Unix)  installed.
Run the webapp (port 9000) with the standalone jar-file

    java -Dninja.port=9000 -Dninja.mode=test -jar pivot-bot-1.0.jar

And call [http://localhost:9000](http://localhost:9000).
To use a pre-trained bot, import the included **FroBa**.db file. It assesses your affinity to either backend or frontend dev.
# Build from source
You need [Apache Maven](https://maven.apache.org/download.cgi) (Win/Unix) installed.
In the project directory, use
    
    mvn clean package
    
After that, you find the created jar file in */target*    
To run the project in dev mode (hot reload), use

    mvn ninja:run -Dninja.port=9000

For futher configuration (e.g. ssl for prod mode) and deployment options (e.g. as servlet), see
http://www.ninjaframework.org/documentation/deployment/ninja_standalone.html

# Usage
#### /settings
![user](http://i.imgur.com/qgYE6No.png)
First, create a new user and a new bot to identify your progress.
You may export or import a *.db file to other systems.

#### /learning
![learning](http://i.imgur.com/cAuQ4t8.png)
Let your bot learn domain-specific texts for question generation and sentiment analysis.

#### /dialogue
![dialogue](http://i.imgur.com/RufOhrA.png)
Speak to your bot. If you don't understand a question, it will be skipped and doesn't influence the assessment further.
The more the bot learns, the more the right side resembles a brain. Teach him/her thoroughly!

# Now fly, you fools!