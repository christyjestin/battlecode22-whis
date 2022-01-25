# Team Whis (Battlecode 2022)
Created by Christy Jestin and David Liu

## Battlecode 2022 Rules
At a high level, Battlecode 2022 is a 1v1 automated strategy game (like Dota :)) where both teams compete to eliminate the other team's archons (essentially home bases) while accumulating lead and gold. The four major robot types are archons, soldiers, miners, and builders:
* **Archons** repair and spawn other bots.
* **Soldiers** attack other bots.
* **Miners** collect lead and gold.
* **Builders** repair and build laboratories (they transmute lead to gold) and watchtowers (like soldiers but a lot less mobile).

Teams can win by either annihilating the other team's archons or winning on tiebreakers after round 2000. The tiebreakers are (in order) number of surviving archons, amount of gold, and amount of lead (the winner is chosen randomly if a winner is not determined by these 3 tiebreakers). Lead and gold are expended to create and mutate bots (mutation boosts certain stats). In Battlecode, the varying and aesthetically pleasing maps are riddled with rubble which increase cooldown (bots must wait a particular number of cooldown turns after each action). Lastly, different types of bots have differing action radii for repairing, attacking, or mining as well as differing vision radii for sensing lead, gold, rubble, or other bots.
<p align="center">
<img alt="Nyancat Map" src="https://user-images.githubusercontent.com/52580002/150923866-84e627a4-032b-4b21-9378-5764311a1f59.png" width="850"/>
</p>

## Engineering Limitations
All of the code we wrote can be found under /scr/whisplayer1. Note that the static keyword doesn't actually make properties/methods static in Battlecode (this was part of the rules and was setup by competition staff &mdash; not sure how). Instead all robots had to communicate using a shared array of 64 integers with each integer in the range \[0,2<sup>16</sup>). Robots were further restricted by \"bytecode limits\" i.e. how many atomic instructions each bot could execute per turn. We were also restricted from using thread related commands like Object.wait() since each bot is run by a thread.

## Our Strategy
Our strategy largely revolves around defense and strength in numbers.

#### Finding Enemy Archons
Our archons make use of the fact that the map is symmetric by either reflection or rotation to make 3 educated guesses about the location of the enemy's archons (one for reflecting our archon's location across the x-axis, one for reflecting the location across the y-axis, and one for rotating the location about the origin). Our soldiers check these guesses and quickly eliminate the incorrect options to locate the enemy's archons (miners help out here too but their main objective is always finding and mining lead and gold).

#### Attacking and Defending
Once an enemy archon has been found, the soldiers all rush a single enemy archon at a time until the enemy archons are annihilated. Soldiers will wait and/or regroup if they sense that they don't have enough teammates with them to launch an attack. This prevents the soldiers from just being picked off one by one as the enemy heals. In addition, our own archons use the shared array to indicate when they are under attack, and about half of the attacking soldiers will return to defend the archons that are under attack.
<p align="center">
<img alt="Red Team Simultaneously Attacking and Defending" src="https://user-images.githubusercontent.com/52580002/150924598-f7fc3b6f-6cf6-4255-9f28-270a8e62821e.png" width="850px"/>
</p>


#### Other
Each archon also spawns a pair of builders which repair the archon and produce a laboratory to create gold. Lastly, builders, miners, and soldiers use the shared array to indicate where lead/gold are or are not present, so that miners can explore and mine more effectively. There are far too many spots on every map to keep track of each individual location (the shared array is just too small &mdash; even with clever data representation schemes), so all robots use a grid system to represent blocks of locations on the map.

### Thanks for Reading!
