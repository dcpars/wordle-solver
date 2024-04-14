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

#### Run the analyzer, which maintains the dictionary. 
```
# In the root-level directory of the project
docker compose up -d
```

#### Run the Wordle solver program

When you have a Wordle puzzle ready to solve, run the Worlde solver application.
This requires the path to a dictionary text file.

The easiest way to run this is in IntelliJ IDEA, by running the `WordleSolver` class.
```
# In the root-level directory of the project
./gradlew run --args='./dictionary.txt'
```
