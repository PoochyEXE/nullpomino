/*
    Copyright (c) 2010, NullNoname
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright
          notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
          notice, this list of conditions and the following disclaimer in the
          documentation and/or other materials provided with the distribution.
 * Neither the name of NullNoname nor the names of its
          contributors may be used to endorse or promote products derived from
          this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
    POSSIBILITY OF SUCH DAMAGE.
 */
package mu.nu.nullpo.game.subsystem.ai;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;

import mu.nu.nullpo.game.component.Controller;
import mu.nu.nullpo.game.component.Field;
import mu.nu.nullpo.game.component.Piece;
import mu.nu.nullpo.game.play.GameEngine;
import mu.nu.nullpo.game.play.GameManager;
import mu.nu.nullpo.tool.airanksgenerator.AIRanksGenerator;
import mu.nu.nullpo.tool.airanksgenerator.Ranks;
import mu.nu.nullpo.util.CustomProperties;

import org.apache.log4j.Logger;

public class RanksAI extends DummyAI implements Runnable {

	static Logger log = Logger.getLogger(RanksAI.class);


	//public boolean bestHold;
	

	//public int bestX;

	//public int bestY;

	//public int bestRt;

	public int bestXSub;

	public int bestYSub;

	public int bestRtSub;

	public int bestPts;


	//public boolean forceHold;

	public int delay;

	public GameEngine gEngine;

	public GameManager gManager;

	public boolean thinkRequest;

	public boolean thinking;

	public int thinkDelay;

	//public int thinkCurrentPieceNo;

	//public int thinkLastPieceNo;

	public volatile boolean threadRunning;

	public Thread thread;

	private Ranks ranks;
	private boolean skipNextFrame;

	private int currentHeightMin;
	private int currentHeightMax;

	private static int THRESHOLD_FORCE_4LINES=8;
	private  int MAX_PREVIEWS=2;

	private int[] heights;
	private Score bestScore;
	private boolean gameOver;
	private boolean plannedToUseIPiece;
	private String currentRanksFile="";

	public class Score{

		public float rankStacking;
		public int distanceToSet;
		public boolean iPieceUsedInTheStack;
		public Score(){

			rankStacking=0;
			distanceToSet=ranks.getStackWidth()*20;
		}
		public String toString(){
			return " Rank Stacking : "+rankStacking+ " distance to set :"+distanceToSet;

		}
		public void computeScore(int heights[]){
			this.distanceToSet=0;
			int [] surface= new int[ranks.getStackWidth()-1];
			int maxJump=ranks.getMaxJump();
			for (int i=0;i<ranks.getStackWidth()-1;i++){
				int diff=heights[i+1]-heights[i];
				if (diff>maxJump){

					this.distanceToSet+=(diff-maxJump);
					diff=maxJump;
				}
				if (diff<-maxJump){
					this.distanceToSet-=(diff+maxJump);

					diff=-maxJump;
				}
				surface[i]=diff;
			}
			this.rankStacking=ranks.getRankValue(ranks.encode(ranks.heightsToSurface(heights)));

		}
		public int compareTo(Object o) {
			Score otherScore=(Score) o;

			if (this.distanceToSet!= otherScore.distanceToSet){
				return this.distanceToSet<otherScore.distanceToSet?1:-1;
			}

			if (this.rankStacking != otherScore.rankStacking){
				return this.rankStacking>otherScore.rankStacking?1:-1;
			}

			return 0;
		}
	}

	@Override
	public String getName() {
		return "RANKSAI";
	}
	public void initRanks(){
		delay = 0;

		thinkRequest = false;
		thinking = false;
		threadRunning = false;
		CustomProperties propRanksAI = new CustomProperties();
		try {
			FileInputStream in = new FileInputStream(AIRanksGenerator.RANKSAI_CONFIG_FILE);
			propRanksAI.load(in);
			in.close();
		} catch (IOException e) {}
		String file=propRanksAI.getProperty("ranksai.file");
		MAX_PREVIEWS=propRanksAI.getProperty("ranksai.numpreviews", 2);
		// If no ranks file has been loaded yet, try to load it
		if (ranks==null || !(currentRanksFile.equals(file))){
			currentRanksFile=file;
			String inputFile="";
			if (file!=null && file.trim().length()>0){
			 inputFile=AIRanksGenerator.RANKSAI_DIR+currentRanksFile;
			}
			FileInputStream fis = null;
			ObjectInputStream in = null;

			if (inputFile.trim().length() == 0)
				ranks=new Ranks(4,9);
			else {
				try {
					fis = new FileInputStream(inputFile);
					in = new ObjectInputStream(fis);
					ranks = (Ranks)in.readObject();
					in.close();

				} catch (FileNotFoundException e) {
					ranks=new Ranks(4,9);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		heights=new int [ranks.getStackWidth()];
		gameOver=false;
	}

	@Override
	public void init(GameEngine engine, int playerID) {
		gEngine = engine;
		gManager = engine.owner;

		// Inits the ranks
		initRanks();

		//Starts the thread
		if( ((thread == null) || !thread.isAlive()) && (engine.aiUseThread) ) {
			thread = new Thread(this, "AI_" + playerID);
			thread.setDaemon(true);
			thread.start();
			thinkDelay = engine.aiThinkDelay;
			thinkCurrentPieceNo = 0;
			thinkLastPieceNo = 0;
		}

	}

	@Override
	public void shutdown(GameEngine engine, int playerID) {
		ranks=null;
		if((thread != null) && (thread.isAlive())) {
			thread.interrupt();
			threadRunning = false;
			thread = null;
		}
	}

	@Override
	public void newPiece(GameEngine engine, int playerID) {
		if(!engine.aiUseThread) {
			thinkBestPosition(engine, playerID);
		} else {
			thinkRequest = true;
			thinkCurrentPieceNo++;
		}
	}

	@Override
	public void onFirst(GameEngine engine, int playerID) {
	}

	@Override
	public void onLast(GameEngine engine, int playerID) {
	}

	@Override
	public void setControl(GameEngine engine, int playerID, Controller ctrl) {
		if( (engine.nowPieceObject != null) && (engine.stat == GameEngine.STAT_MOVE) && (delay >= engine.aiMoveDelay) && (engine.statc[0] > 0) &&
				(!engine.aiUseThread || (threadRunning && !thinking && (thinkCurrentPieceNo <= thinkLastPieceNo))) )
		{
			int input = 0;
			Piece pieceNow = engine.nowPieceObject;
			int nowX = engine.nowPieceX;
			int nowY = engine.nowPieceY;
			int rt = pieceNow.direction;
			Field fld = engine.field;
			boolean pieceTouchGround = pieceNow.checkCollision(nowX, nowY + 1, fld);

			if((bestHold || forceHold) ) {
				if  (engine.isHoldOK())
				input |= Controller.BUTTON_BIT_D;
			} else {

				if(rt != bestRt) {
					int lrot = engine.getRotateDirection(-1);
					int rrot = engine.getRotateDirection(1);

					if((Math.abs(rt - bestRt) == 2) && (engine.ruleopt.rotateButtonAllowDouble) && !ctrl.isPress(Controller.BUTTON_E)) {
						input |= Controller.BUTTON_BIT_E;
					} else if(!ctrl.isPress(Controller.BUTTON_B) && engine.ruleopt.rotateButtonAllowReverse &&
							!engine.isRotateButtonDefaultRight() && (bestRt == rrot)) {
						input |= Controller.BUTTON_BIT_B;
					} else if(!ctrl.isPress(Controller.BUTTON_B) && engine.ruleopt.rotateButtonAllowReverse &&
							engine.isRotateButtonDefaultRight() && (bestRt == lrot)) {
						input |= Controller.BUTTON_BIT_B;
					} else if(!ctrl.isPress(Controller.BUTTON_A)) {
						input |= Controller.BUTTON_BIT_A;
					}
				}

				int minX = pieceNow.getMostMovableLeft(nowX, nowY, rt, fld);
				int maxX = pieceNow.getMostMovableRight(nowX, nowY, rt, fld);
				if (!skipNextFrame){
					skipNextFrame=true;

					if( ((bestX < minX - 1) || (bestX > maxX + 1) || (bestY < nowY)) && (rt == bestRt)  ){

						thinkRequest = true;

					} else {

						if((nowX == bestX) && (pieceTouchGround) && (rt == bestRt)) {

							if(bestRtSub != -1) {
								bestRt = bestRtSub;
								bestRtSub = -1;
							}

							if(bestX != bestXSub) {
								bestX = bestXSub;
								bestY = bestYSub;
							}
						}

						if(nowX > bestX) {

							if(!ctrl.isPress(Controller.BUTTON_LEFT) || (engine.aiMoveDelay >= 0))
								input |= Controller.BUTTON_BIT_LEFT;
						} else if(nowX < bestX) {

							if(!ctrl.isPress(Controller.BUTTON_RIGHT) || (engine.aiMoveDelay >= 0))
								input |= Controller.BUTTON_BIT_RIGHT;
						} else if((nowX == bestX) && (rt == bestRt)) {

							if((bestRtSub == -1) && (bestX == bestXSub)) {
								if(engine.ruleopt.harddropEnable && !ctrl.isPress(Controller.BUTTON_UP))
									input |= Controller.BUTTON_BIT_UP;
								else if(engine.ruleopt.softdropEnable || engine.ruleopt.softdropLock)
									input |= Controller.BUTTON_BIT_DOWN;
							} else {
								if(engine.ruleopt.harddropEnable && !engine.ruleopt.harddropLock && !ctrl.isPress(Controller.BUTTON_UP))
									input |= Controller.BUTTON_BIT_UP;
								else if(engine.ruleopt.softdropEnable && !engine.ruleopt.softdropLock)
									input |= Controller.BUTTON_BIT_DOWN;
							}
						}
					}
				}
				else {
					skipNextFrame=false;
				}
			}

			delay = 0;
			ctrl.setButtonBit(input);
		} else {
			delay++;
			ctrl.setButtonBit(0);
		}
	}

	/**
	 * Plays a fictitious move (ie not rendering it on the screen) for use in AIRanksTester
	 * It searches for the best possible move and plays it(ie updates the heights array)
	 * @param heights Heights of the columns
	 * @param pieces Current Piece and Next Pieces
	 */
	public void playFictitiousMove (int [] heights,int [] pieces){
		currentHeightMin=25;
		currentHeightMax=0;
		for (int i=0;i<ranks.getStackWidth();i++){

			if (heights[i]<currentHeightMin){
				currentHeightMin=heights[i];
			}
			if (heights[i]>currentHeightMax){
				currentHeightMax=heights[i];
			}
		}

		thinkBestPosition(heights,pieces);
		if (bestScore.rankStacking==0){
			gameOver=true;
		} else{
			if (bestX==9){
				for (int i=0;i<heights.length;i++){
					heights[i]-=4;
				}
			}
			else {
				ranks.addToHeights(heights, pieces[0], bestRt, bestX);
				for (int i=0;i<heights.length;i++){
					if (heights[i]>20){
						gameOver=true;
						break;
					}

				}
			}

		}

	}

	/**
	 * Tells other classes if the fictitious game is over
	 * @return true if fictitious game is over
	 */
	public boolean isGameOver() {
		return gameOver;
	}

	/**
	 * Think the best position, for a given engine. It will do the necessary conversion from engine representation of the field to ranks representation
	 * of it, and run the main method thinkBestPosition
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	 public void thinkBestPosition(GameEngine engine, int playerID) {

		 // Current line of the current piece
		 int nowY = engine.nowPieceY;

		 // Currently considered piece
		 Piece pieceNow=engine.nowPieceObject;

		 // Initialization of the heights array
		 for (int i=0;i<ranks.getStackWidth();i++){
			 heights[i]=engine.field.getHeight()-engine.field.getHighestBlockY(i);

		 }

		 // Initialization of the pieces array (contains the current piece and the next pieces)
		 int [] pieces= new int [engine.nextPieceArraySize];
		 pieces[0]=pieceNow.id;
		 for (int i=1;i<pieces.length;i++){
			 pieces[i]=engine.getNextObject(engine.nextPieceCount+i-1).id;
		 }

		 // Call the main method (that actually does the work, on the heights and pieces
		 thinkBestPosition(heights,pieces);

		 // Convert the best chosen move to the engine representation
		 bestX-=(pieceNow.dataOffsetX[bestRt]+Ranks.PIECES_LEFTMOSTS[pieceNow.id][bestRt]);
		 bestXSub=bestX;

		 bestY = pieceNow.getBottom(bestX, nowY, bestRt, engine.field);
		 bestYSub=bestY;
		 bestYSub=bestY;
		 
		 // If we cant fit the pieces anymore without creating holes, try hold
		 bestHold=false;
		 if (bestScore.rankStacking==0)
			// threadRunning=false;
		 	 bestHold=true;
		 thinkLastPieceNo++;
		 log.debug("nowX : "+engine.nowPieceX+" X:" + bestX + " Y:" + bestY + " R:" + bestRt + " H:" + bestHold + " Pts:" + bestScore);

	 }

	 /**
	  * Main method that will return the best move for the current piece, by calling the recursive method thinkmain on each possible move
	  * @param heights Array containing the heights of the columns in the field
	  * @param pieces Array containing the current piece and the next pieces.
	 */

	 public void thinkBestPosition(int heights[],int [] pieces) {

		 // The best* variables contain the chosen best position for the current piece.
		 // The best*Sub variables are used in case you want to do a twist or a spin or a slide. they give the final position for the best move
		 // We are not using slides or twists so the best*Sub basically equal the best* variables.
		 // bestRtSub=-1 means there is no twist.
		 bestX = 0;
		 bestY = 0;
		 bestRt = 0;
		 bestXSub = 0;
		 bestYSub = 0;
		 bestRtSub = -1;
		 bestPts = 0;

		 // We keep track of the best score too to be able to know when there is no possibility to place a piece( when bestScore.rankstacking=0)
		 bestScore=new Score();

		 // Variable to temporarily store the score
		 Score score;

		 // Current piece
		 int pieceNow = pieces[0];

		 // Number of previews to consider
		 int numPreviews=MAX_PREVIEWS;

		 // Initialization maximum height and minimum height
		 currentHeightMin=99;
		 currentHeightMax=0;

		 //Compute the maximum/minimum heights
		 for (int i=0;i<ranks.getStackWidth();i++){

			 if (heights[i]<currentHeightMin){
				 currentHeightMin=heights[i];
			 }
			 if (heights[i]>currentHeightMax){
				 currentHeightMax=heights[i];
			 }
		 }

		 // If we are able to score a 4-Line and if the maximum height is dangerously high, then force doing a tetris
		 if (pieceNow==Piece.PIECE_I &&currentHeightMax>=THRESHOLD_FORCE_4LINES && currentHeightMin>=4 &&!plannedToUseIPiece){

			 //Rightmost column
			 bestX = ranks.getStackWidth();

			 // Vertical rotation
			 bestRt = 1;

			 bestXSub = bestX;
			 bestRtSub = -1;

			 // Dummy score so that the AI doesnt think the game is over (can't fit a piece anymore)
			 bestScore.rankStacking=Integer.MAX_VALUE;

		 }

		 //If we don't have to score a 4-Line, consider other moves.
		 else {
			 // try all possible rotations
			 for(int rt = 0; rt < Ranks.PIECES_NUM_ROTATIONS[pieceNow]; rt++) {

				 // Columns go from 0 to 9 in Ranks representation
				 int minX=0;
				 int maxX=ranks.getStackWidth()-Ranks.PIECES_WIDTHS[pieceNow][rt];
				 for(int x = minX; x <= maxX; x++) {

					 // Run thinkmain on that move to get its score
					 score = thinkMain( x,  rt,  heights, pieces, numPreviews);
					 log.debug("MAIN  id="+pieceNow+" posX="+x+" rt="+rt+" score:"+score);

					 //If the score is better than the previos best score, change it, and record the chosen move for further application by setControl
					 if(score.compareTo(bestScore)>0) {
						 log.debug("MAIN new best piece !");
						 if (pieceNow==Piece.PIECE_I){
							 bestScore.iPieceUsedInTheStack=true;
						 }
						 bestHold = false;
						 bestX = x;
						 bestRt = rt;
						 bestXSub = x;
						 bestRtSub = -1;
						 bestScore=score;
					 }

				 }

				 // If we can score a 4-Line, try it
				 if (pieceNow==Piece.PIECE_I && ((rt==1)||(rt==3)) && currentHeightMin>=4){

					 // What are the consequences of scoring a 4-Line ?
					 score = thinkMain( maxX+1,  rt,  heights, pieces, numPreviews);
					 log.debug("MAIN (4 Lines) id="+pieceNow+" posX="+(maxX+1)+" rt="+rt+" score:"+score);

					//If the score is better than the previos best score, change it, and record the chosen move for further application by setControl
					  if(score.compareTo(bestScore)>0) {
						 log.debug("MAIN (4 Lines) new best piece !");

						 bestHold = false;
						 bestX = maxX+1;
						 bestRt = rt;
						 bestXSub = maxX+1;
						 bestRtSub = -1;
						 bestScore=score;
					 }
				 }

			 }
		 }
		 if (numPreviews>0)
			 plannedToUseIPiece=bestScore.iPieceUsedInTheStack;

	 }

/**
 * Recursive method that returns the score of a given move for a given field and given next pieces
 * @param x Column where the piece has to be put.
 * @param rt Rotation of the piece.
 * @param heights Array containing the heights of the field
 * @param pieces Array containing the Current Piece and Next Pieces
 * @param numPreviews Number of previews to consider in the thinking process
 * @return The score for this move (placing the piece in this column, with this rotation)
 */
	 public Score thinkMain( int x,  int rt,  int[] heights, int pieces[], int numPreviews) {

		 // Initialize the score with zero
		 Score score=new Score();

		 // Initialize maximum height and minimum height of the stack with dummy values
		 int heightMin=99;
		 int heightMax=0;

		 //Convert the heights to a surface to be able to check if the piece fits the surface
		 int []surface=ranks.heightsToSurface(heights);

		 log.debug("piece id : " +pieces[0]+" rot : "+rt+" x :"+x+" surface :"+Arrays.toString(surface));

		 //Boolean value representing the fact that the current piece is the I piece, vertical, and in the rightmost column.
		 boolean isVerticalIRightMost=(pieces[0]==Piece.PIECE_I && ((rt==1)||(rt==3))&&(x==9));

		 // Either we are going to score a 4-Line or we have to check that the piece fits the surface
		 if (isVerticalIRightMost || ranks.surfaceFitsPiece(surface, pieces[0], rt, x)){

			 // Cloning the heights in order to not alter the heights array, that was passed in parameters (and possibly used elsewhere)
			 int [] heightsWork=heights.clone();

			 // If we are not going to score a 4-Line, add the piece to the heightsWork array and update the minimum and maximum height.
			 if (!isVerticalIRightMost){

				 ranks.addToHeights(heightsWork, pieces[0], rt, x);
				 for (int i=0;i<ranks.getStackWidth();i++){
					 if (heightsWork[i]>heightMax){
						 heightMax=heights[i];
					 }
					 if (heightsWork[i]<heightMin){
						 heightMin=heights[i];
					 }
				 }

			 }
			 // If we are going to score a 4-Line, substract 4 to the heights and to the minimum and maximum heights. They will remain positive because
			 // the necessary condition to score a tetris is that the minimal height be greater than 4.
			 else {
				 for (int i=0;i<ranks.getStackWidth();i++)
					 heightsWork[i]-=4;

				 heightMin-=4;
				 heightMax-=4;

			 }

			 // If there are still previews left, then we go on recursively to explore the lower branches of the decision tree.
			 if (numPreviews>0){
				 // We are going to examine all possible moves for the next piece and return the best score.

				 // Initialize the best score to zero.
				 Score bestScore=new Score();

				 // We will need a score variable to temporary store the score of the currently considered move.
				 Score scoreCurrent;

				 // The piece considered now is the next piece
				 int pieceNow = pieces[1];

				 // The pieces array that we will pass to the recursive call to thinkMain has to be shifted to the left
				 // 2nd piece becomes 1st piece, 3d piece becomes 2d, etc...
				 int [] pieces2=new int[pieces.length];
				 System.arraycopy(pieces, 1, pieces2, 0, pieces.length-1);

				 // If current piece is I Piece,  and minimum height is greater 4 and maximum height is greater than threshold, force the 4-Line
				 if (pieceNow==Piece.PIECE_I && heightMax>=THRESHOLD_FORCE_4LINES && heightMin>=4 &&!plannedToUseIPiece){
					 int rt2=1;
					 int maxX2=ranks.getStackWidth()-1;
					// Recursive call to thinkMain to examine that move
					 scoreCurrent = thinkMain( maxX2+1,  rt2,  heightsWork, pieces2, numPreviews-1);
					 log.debug("SUB (4 Lines)"+ numPreviews+" id="+pieceNow+" posX="+(maxX2+1)+" rt="+rt2+" score:"+scoreCurrent);

					 // if the score is better than the previous best score, replace it.
					 if(scoreCurrent.compareTo(bestScore)>0) {
						 log.debug("SUB new best piece !");

						 bestScore=scoreCurrent;
					 }
				 }

				 // If we cannot score a 4-Line because we don't have a vertical I or the height is <4 or if we don't want to
				 // force the 4-Line because the maximum height is under the threshold, then we test all possible columns
				 // Let's try all the possible rotations for the currently considered piece.
				 else {
					 for(int rt2 = 0; rt2 < Ranks.PIECES_NUM_ROTATIONS[pieceNow]; rt2++) {

						 // is the piece a vertical I ?
						 boolean isVerticalI2=(pieceNow==Piece.PIECE_I && ((rt2==1)||(rt2==3)));

						 // the columns go from 0 to 9 in the representation that Ranks use
						 int minX2=0;
						 int maxX2=minX2+ranks.getStackWidth()-Ranks.PIECES_WIDTHS[pieceNow][rt2];

						 for(int x2 = minX2; x2 <= maxX2; x2++) {

							 // Recursive call to thinkMain to examine that move
							 scoreCurrent = thinkMain( x2,  rt2, heightsWork,pieces2,numPreviews-1);
							 log.debug("SUB "+numPreviews +" id="+pieceNow+" posX="+x2+" rt="+rt2+" score "+scoreCurrent);

							 // if the score is better than the previous best score, replace it.
							 if(scoreCurrent.compareTo(bestScore)>0) {
								 log.debug("SUB new best piece !");
								 bestScore=scoreCurrent;
							 }

						 }

						 // If the piece considered is vertical I and if the minimum height is greater than 4, try scoring a 4-Line
						 if (isVerticalI2 && heightMin>=4){

							 // Recursive call to thinkMain to examine that move
							 scoreCurrent = thinkMain( maxX2+1,  rt2,  heightsWork, pieces2, numPreviews-1);
							 log.debug("SUB (4 Lines)"+ numPreviews+" id="+pieceNow+" posX="+(maxX2+1)+" rt="+rt2+" score:"+scoreCurrent);
							 // if the score is better than the previous best score, replace it.
							 if(scoreCurrent.compareTo(bestScore)>0) {
								 log.debug("SUB new best piece !");

								 bestScore=scoreCurrent;
							 }
						 }
					 }
				 }

				 // Uncomment to compare all the nodes of the tree between themselves, and not only the end nodes

				  /*scoreCurrent=new Score();
					scoreCurrent.computeScore(heightsWork);
					if(scoreCurrent.compareTo(bestScore)>0) {
						bestScore=scoreCurrent;
					}*/

				 // Returns the best score
				 return bestScore;
			 }

			 // If numPreviews==0, that is, if there are no previews left to condier, just return the score of the surface resulting from the move.
			 else {

				 score.computeScore(heightsWork);
				 if ((pieces[0]==Piece.PIECE_I)&&(x<ranks.getStackWidth()) && numPreviews<MAX_PREVIEWS){
					 score.iPieceUsedInTheStack=true;
				 }
				 return score;
			 }
		 }

		 // If the piece doesnt fit, return 0,ditch the whole branch of the tree
		 else
		 {
			 return score;
		 }

	 }

	 /**
	  * Get max think level
	  * @return Max think level (1 in this AI)
	 */
	 public int getMaxThinkDepth() {
		 return 1;
	 }

	 /*
	  * Thread routine for this AI
	 */
	 public void run() {
		 log.info("RanksAI: Thread start");
		 threadRunning = true;

		 while(threadRunning) {
			 if(thinkRequest) {
				 thinkRequest = false;
				 thinking = true;
				 try {
					 thinkBestPosition(gEngine, gEngine.playerID);

				 } catch (Throwable e) {
					 log.debug("RanksAI: thinkBestPosition Failed", e);
				 }
				 thinking = false;
				 skipNextFrame=false;
			 }

			 if(thinkDelay > 0) {
				 try {
					 Thread.sleep(thinkDelay);
				 } catch (InterruptedException e) {
					 break;
				 }
			 }
		 }

		 threadRunning = false;
		 ranks=null;
		 log.info("RanksAI: Thread end");
	 }
}
