```
 __     __     ______     ______     _____     __         ______    
/\ \  _ \ \   /\  __ \   /\  == \   /\  __-.  /\ \       /\  ___\   
\ \ \/ ".\ \  \ \ \/\ \  \ \  __<   \ \ \/\ \ \ \ \____  \ \  __\   
 \ \__/".~\_\  \ \_____\  \ \_\ \_\  \ \____-  \ \_____\  \ \_____\ 
  \/_/   \/_/   \/_____/   \/_/ /_/   \/____/   \/_____/   \/_____/ 
                                                                    
 ______     ______     __         __   __   ______     ______       
/\  ___\   /\  __ \   /\ \       /\ \ / /  /\  ___\   /\  == \      
\ \___  \  \ \ \/\ \  \ \ \____  \ \ \'/   \ \  __\   \ \  __<      
 \/\_____\  \ \_____\  \ \_____\  \ \__|    \ \_____\  \ \_\ \_\    
  \/_____/   \/_____/   \/_____/   \/_/      \/_____/   \/_/ /_/    
                                                                                                                         
```

## Local Dev How to 

#### Run Psql and create database
```
# In the root-level directory of the project
psql -f db/schema.sql
```

#### Run the Wikipedia scraper

The Wikipedia scraper crawls random articles from Wikipedia and stores.
The longer this is run, the more informed the database will be.
```
# In the 'analysis' directory of the project
cd analysis
python3.9 analyzer.py
```

#### Run the Wordle solver program

When you have a Wordle puzzle ready to solve, run the Worlde solver application.
This requires the path to a dictionary text file. 
```
# In the root-level directory of the project
./gradlew run --args='./examples/dictionary.txt'
```
