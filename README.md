# GameAI

## KBAStar

### Notes

1. The files Agent.java, dSums.java, SingleMCTSPlayer.java, and SingleTreeNode.java come from package pavlos_97.
2. The main modifications are in the function 'evalKB' of the file SingleTreeNode.java.

3. The files AStar.java, Node.java come from the original GVGAI framework and without modification.

4. The file PathFinder.java partly comes from the original GVGAI framework and with modification. 

### Descriptions

This package uses the AStar code which is from the original GVGAI framework tool.pathfinder. 

#### Remark of the positions to calculating AStar distance here:

The original package tool.pathfinder uses Grid Position (GP) to calculate the AStar distance between two sprites, whereas SingleTreeNode.java of the original GVGAI tracks.singlePlayer.advanced.sampleMCTS uses Screen Position (SP) while passing the position of the sprites. Therefore the SP has been processed as GP in KBAStar. (Note that GP  = SP/block_size (Variable block_size can be found in core.game.Game.java, core.game.BasicGame.java, and core.game.StateObservation.java (where it is called in this KBAStar package). It is defined as the size of one block in the game window. It is actually the square_size defined in every game description file.) )

We can also use StateObservation.getObservationGrid() to get the Grid Position of all observations. And there is another way to calculate AStar distance, that is, using the Screen Position which means we need to write AStar ourselves and that is also what I am going to try out. 

Feel free to contact me if any question and blur. 

