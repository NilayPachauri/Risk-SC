package classes;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

public class Game {
	
	private Territory[] territoryMap;
	private User[] players;
	private int winAmount;
	
	public Game(Vector<User> players)	{
		
		init(players);
	}
	
	/**
	 * Initialize the game based on the number of players
	 * @param playerNum
	 */
	private void init(Vector<User> players)	{
		initPlayers(players);
		initMap(players.size());
		initWinAmount(players.size());
	}
	
	private void initPlayers(Vector <User> players)	{
		this.players = new User[players.size()];
		for (int i = 0; i < players.size(); i++)
			this.players[i] = players.elementAt(i);
	}
	
	/**
	 * Randomly initialize the map by uniformly distributing the players over the territories
	 * @param playerNum the number of players in the game
	 */
	private void initMap(int playerNum)	{
		
		this.territoryMap = new Territory[Adjacencies.getTerritories().length];
		
		int territoriesToAllocate[] = new int[playerNum];
		for (int i = 0; i < playerNum; i++)
			territoriesToAllocate[i] = this.territoryMap.length/playerNum;
		
		for (int i = 0; i < this.territoryMap.length; i++)	{
			int temp = (int) (Math.random()*playerNum);
			while (territoriesToAllocate[temp] == 0)
				temp = (int) (Math.random()*playerNum);
			
			int troops = (Math.random() < 0.5) ? 1 : 2;
			this.territoryMap[i] = new Territory(Adjacencies.getTerritory(i), i, temp, troops);
			this.players[temp].addTerritory();
			this.players[temp].addTroops(troops);
			territoriesToAllocate[temp]--;
		}
		
		for (int i = 1; i < this.territoryMap.length; i++)	{
			// Select a random value below the current value
			int randIndex = (int) (Math.random()*i);
			
			// Swap the randomly selected value with one below it
			int temp = this.territoryMap[i].getOccupier();
			this.territoryMap[i].setOccupier(this.territoryMap[randIndex].getOccupier());
			this.territoryMap[randIndex].setOccupier(temp);
		}
	}
	
	private void initWinAmount(int playerNum)	{
		this.winAmount = this.territoryMap.length * (5 - playerNum) / (6 - playerNum);
	}
	
	/**
	 * 
	 * @param territory the territory to place troops on
	 * @param troops the number of troops to place on the territory
	 * @return true if place was successful, false otherwise
	 */
	public boolean place(int territory, int troops)	{
		
		// Place the troops so long as the player places onto a self-controlled territory
		if (this.territoryMap[territory].getOccupier() != this.territoryMap[territory].getOccupier())	{
			return false;
		}
		
		// Updates the troops on the territory and belonging to the player
		this.territoryMap[territory].addTroops(troops);
		this.players[this.territoryMap[territory].getOccupier()].addTroops(troops);
		
		return true;
	}
	
	/**
	 * 
	 * @param attackTerritory the territory that troops are attacking from
	 * @param attackTroops the number of troops of the attackingTerritory that are attacking the defendingTerritory
	 * @param defendTerritory the territory that troops are defending
	 * @param defendTroops the number of troops in the defendingTerritory that are defending from the attackingTerritory
	 * @return true if the attack was successful, false otherwise
	 */
	public boolean attack(int attackTerritory, int attackTroops, int defendTerritory, int defendTroops)	{
		
		// Not possible to attack the same player with itself
		if (this.territoryMap[attackTerritory].getOccupier() == this.territoryMap[defendTerritory].getOccupier())	{
			return false;
		}
		
		// Attacker cannot attack with more troops than strictly less than his current amount
		if (this.territoryMap[attackTerritory].getTroops() <= attackTroops || this.territoryMap[attackTerritory].getTroops() == 1)	{
			return false;
		}
		
		// Defender cannot defend with more troops than less than or equal to his current amount
		if (this.territoryMap[defendTerritory].getTroops() < defendTroops)	{
			return false;
		}

		// Not possible to attack from one territory to another if they are not adjacent
		if (!areNeighbors(attackTerritory, defendTerritory))	{
			return false;
		}
		
		// Store the simulated dice rolls for each attacker and defender
		int attackValues[] = new int[attackTroops];
		int defendValues[] = new int[defendTroops];
		
		for (int i = 0; i < attackValues.length; i++)
			attackValues[i] = this.simulateDiceRoll();
		
		for (int i = 0; i < defendValues.length; i++)
			defendValues[i] = this.simulateDiceRoll();
		
		// Sort Arrays by increasing value
		Arrays.sort(attackValues); 
		Arrays.sort(defendValues);
		
		// Compare the last results of each array as many times as the length of the smaller array
		boolean result[] = new boolean[Math.min(attackValues.length, defendValues.length)];
		for (int i = 0; i < result.length; i++)	{
			
			if (attackValues[(attackValues.length - 1) - i] > defendValues[(defendValues.length - 1) - i])
				result[i] = true;
			else
				result[i] = false;
		}
		
		// If the attacker won a single skirmish, then remove a troop from the defense
		// Otherwise, the defender must have one, so remove a troop from the attacker
		
		int attackTroopsLost = 0, defendTroopsLost = 0;
		for (int i = 0; i < result.length; i++)	{
			if (result[i])	{
				this.territoryMap[defendTerritory].removeTroops();
				this.players[this.territoryMap[defendTerritory].getOccupier()].removeTroops();
				defendTroopsLost++;
			}
			else	{
				this.territoryMap[attackTerritory].removeTroops();
				this.players[this.territoryMap[attackTerritory].getOccupier()].removeTroops();
				attackTroopsLost++;
			}
		}
		
		// If the defender has no more troops in his territory, then the attacker has conquered
		if (this.territoryMap[defendTerritory].getTroops() <= 0)	{
			
			// Update the territories each player has
			this.players[this.territoryMap[attackTerritory].getOccupier()].addTerritory();
			this.players[this.territoryMap[defendTerritory].getOccupier()].removeTerritory();
			
			// Update the occupier of the previously defended territory
			this.territoryMap[defendTerritory].setOccupier(this.territoryMap[attackTerritory].getOccupier());
			
			// Move the remaining offensive troops into the defended territory
			int attackTroopsRemaining = attackTroops - attackTroopsLost;
			this.territoryMap[defendTerritory].setTroops(attackTroopsRemaining);
			this.territoryMap[attackTerritory].removeTroops(attackTroopsRemaining);
			
		}
		
		// Successfully attacked from the attackingTerritory to the defendingTerritory
		return true;		
	}
	
	/**
	 * @param moveFromTerritory the territory to move troops from
	 * @param moveToTerritory the territory to move troops to
	 * @param troops the number of troops to move from moveFromTerritory to moveToTerritory
	 * @return true if the move was successful, false otherwise
	 */
	public boolean move(int moveFromTerritory, int moveToTerritory, int troops)	{
		
		// You cannot move troops from the same territory
		if (moveFromTerritory == moveToTerritory)	{
			return false;
		}
		
		// You cannot move troops between territories if they are not owned by the same person
		if (this.territoryMap[moveFromTerritory].getOccupier() != this.territoryMap[moveToTerritory].getOccupier())	{
			return false;
		}
		
		// You cannot move more troops than strictly less than the amount in the territory
		if (this.territoryMap[moveFromTerritory].getTroops() <= troops)	{
			return false;
		}
		
		// Not possible to move from one territory to another if they are not adjacent
		if (!areNeighbors(moveFromTerritory, moveToTerritory))	{
			return false;
		}
		
		// Move the troops from one territory to another
		this.territoryMap[moveFromTerritory].removeTroops(troops);
		this.territoryMap[moveToTerritory].addTroops(troops);
		
		return true;
		
	}
	
	/**
	 * 
	 * @return true if there is any player among the list of players who has exceeded the win condition, false otherwise
	 */
	public boolean checkWin()	{
		for (User player: this.players)
			if (player.getTerritories() > this.winAmount)
				return true;
		
		return false;
	}
	
	/**
	 * 
	 * @param playerID the player to check if they have won
	 * @return true if the player with the playerID has won, false otherwise
	 */
	public boolean checkWin(int playerID)	{
		
		return this.players[playerID].getTerritories() > this.winAmount;
		
	}

	/**
	 * 
	 * @param territory
	 * @param neighbor
	 * @return true if territory and neighbor are adjacent, false otherwise
	 */
	private boolean areNeighbors(int territory, int neighbor)	{
		// Determine if the two territories are adjacent
		boolean adjacentTerritories = false;
		for (int t: Adjacencies.getAdjacencyList()[territory])
			if (t == neighbor)
				adjacentTerritories = true;
		
		return adjacentTerritories;
	}
	
	/**
	 * 
	 * @return a number between 1 to 6 uniformly randomly
	 */
	private int simulateDiceRoll()	{
		return (int) (Math.random()*6) + 1;
	}
	
	/**
	 * @return the territory map of the territories
	 */
	public Territory[] getTerritoryMap() {
		return this.territoryMap;
	}

	/**
	 * @return the players
	 */
	public User[] getPlayers() {
		return this.players;
	}

	/**
	 * @return the number of territories necessary to win
	 */
	public int getWinAmount() {
		return this.winAmount;
	}
	
	/**
	 * 
	 * @param playerID the id of the player whose controlled territories are requested
	 * @return the owned territories of the player with the associated playerID
	 */
	public Territory[] getOwnedTerritories(int playerID)	{
		Territory[] ownedTerritories = new Territory[this.players[playerID].getTerritories()];
		
		// Update the list of owned territories
		int index = 0;
		for (int i = 0; i < this.territoryMap.length; i++)
			if (this.territoryMap[i].getOccupier() == playerID)
				ownedTerritories[index++] = this.territoryMap[i];
	
		return ownedTerritories;
	}
	
	/**
	 * 
	 * @param territory the id of the territory to find non-owned adjacent territories
	 * @return an array of non-owned adjacent territories
	 */
	public Territory[] getAdjacentNonOwnedTerritories(int territory)	{
		
		List<Territory> adjacentNonOwnedTerritoriesList = new LinkedList <Territory>();
		
		// Find the Territory List of the Adjacencies with different occupiers
		int[] adjacentTerritories = Adjacencies.getAdjacencyList()[territory];
		for (int i = 0; i < adjacentTerritories.length; i++)
			if (this.territoryMap[territory].getOccupier() != this.territoryMap[adjacentTerritories[i]].getOccupier())
				adjacentNonOwnedTerritoriesList.add(this.territoryMap[adjacentTerritories[i]]);
		
		// Convert the Territory List of Non Owned Adjacencies into an array 
		Territory[] adjacentNonOwnedTerritories = new Territory[adjacentNonOwnedTerritoriesList.size()];
		adjacentNonOwnedTerritories = adjacentNonOwnedTerritoriesList.toArray(adjacentNonOwnedTerritories);
		
		return adjacentNonOwnedTerritories;
		
	}
	
	/**
	 * 
	 * @param territory the id of the territory to find owned adjacent territories
	 * @return an array of owned adjacent territories
	 */
	public Territory[] getAdjacentOwnedTerritories(int territory)	{
		
		List<Territory> adjacentOwnedTerritoriesList = new LinkedList <Territory>();
		
		// Find the Territory List of the Adjacencies with same occupiers
		int[] adjacentTerritories = Adjacencies.getAdjacencyList()[territory];
		for (int i = 0; i < adjacentTerritories.length; i++)
			if (this.territoryMap[territory].getOccupier() == this.territoryMap[adjacentTerritories[i]].getOccupier())
				adjacentOwnedTerritoriesList.add(this.territoryMap[adjacentTerritories[i]]);
		
		// Convert the Territory List of Owned Adjacencies into an array 
		Territory[] adjacentOwnedTerritories = new Territory[adjacentOwnedTerritoriesList.size()];
		adjacentOwnedTerritories = adjacentOwnedTerritoriesList.toArray(adjacentOwnedTerritories);
		
		return adjacentOwnedTerritories;
		
	}
	
}
