package net.tetrisconcept.poochy.nullpomino.ai;

import mu.nu.nullpo.game.component.Controller;
import mu.nu.nullpo.game.component.Field;
import mu.nu.nullpo.game.component.Piece;
import mu.nu.nullpo.game.component.WallkickResult;
import mu.nu.nullpo.game.play.GameEngine;
import mu.nu.nullpo.game.play.GameManager;
import mu.nu.nullpo.game.subsystem.ai.DummyAI;

import org.apache.log4j.Logger;

/**
 * PoochyBot Combo Race AI
 * @author Poochy.EXE
 *         Poochy.Spambucket@gmail.com
 */
public class ComboRaceBot extends DummyAI implements Runnable {
	/** Log */
	static Logger log = Logger.getLogger(ComboRaceBot.class);
	
	/** List of field state codes which are possible to sustain a stable combo */
	private static final int[] FIELDS = {
		0x7, 0xB, 0xD, 0xE,
		0x13, 0x15, 0x16, 0x19, 0x1A, 0x1C,
		0x23, 0x29,
		0x31, 0x32,
		0x49, 0x4C,
		0x61, 0x68,
		0x83, 0x85, 0x86, 0x89, 0x8A, 0x8C,
		0xC4, 0xC8,
		0x111, 0x888
	};
	
	protected int[] scores = {6, 7, 7, 6, 8, 3, 2, 9, 3, 4, 3, 1, 8, 4, 1, 3, 1, 1, 4, 3, 9, 2, 3, 8, 4, 8, 3, 3};
	
	protected Transition[][] moves;
	
	protected int[] nextQueueIDs;
	
	protected boolean createTablesRequest;

	/** 接地したあとのDirection(0: None) */
	public int bestRtSub;

	/** 最善手のEvaluation score */
	public int bestPts;

	/** Movement state. 0 = initial, 1 = twist, 2 = post-twist */
	public int movestate;

	/** 移動を遅らせる用の変count */
	public int delay;

	/** The GameEngine that owns this AI */
	public GameEngine gEngine;

	/** The GameManager that owns this AI */
	public GameManager gManager;

	/** When true,スレッドにThink routineの実行を指示 */
	public boolean thinkRequest;

	/** true when thread is executing the think routine. */
	public boolean thinking;

	/** スレッドを停止させる time */
	public int thinkDelay;

	/** When true,スレッド動作中 */
	public volatile boolean threadRunning;

	/** Thread for executing the think routine */
	public Thread thread;

	/** Last input if done in ARE */
	protected int inputARE;
	/** Number of pieces to think ahead */
	protected static final int MAX_THINK_DEPTH = 5;
	/** Set to true to print debug information */
	protected static final boolean DEBUG_ALL = false;
	/** Did the thinking thread finish successfully? */
	protected boolean thinkComplete;
	/** Did the thinking thread find a possible position? */
	protected boolean thinkSuccess;
	/** Was the game in ARE as of the last frame? */
	protected boolean inARE;

	/*
	 * AI's name
	 */
	public String getName() {
		return "Combo Race AI V1.00";
	}

	/*
	 * Called at initialization
	 */
	public void init(GameEngine engine, int playerID) {
		delay = 0;
		gEngine = engine;
		gManager = engine.owner;
		thinkRequest = false;
		thinking = false;
		threadRunning = false;
		createTablesRequest = false;

		inputARE = 0;
		thinkComplete = false;
		thinkSuccess = false;
		inARE = false;

		if( ((thread == null) || !thread.isAlive()) && (engine.aiUseThread) ) {
			thread = new Thread(this, "AI_" + playerID);
			thread.setDaemon(true);
			thread.start();
			thinkDelay = engine.aiThinkDelay;
			thinkCurrentPieceNo = 0;
			thinkLastPieceNo = 0;
		}
	}

	/*
	 * 終了処理
	 */
	public void shutdown(GameEngine engine, int playerID) {
		if((thread != null) && (thread.isAlive())) {
			thread.interrupt();
			threadRunning = false;
			thread = null;
		}
	}

	/*
	 * Called whenever a new piece is spawned
	 */
	public void newPiece(GameEngine engine, int playerID) {
		if(!engine.aiUseThread) {
			thinkBestPosition(engine, playerID);
		} else if (!thinking && !thinkComplete) {
			thinkRequest = true;
			thinkCurrentPieceNo++;
		}
		movestate = 0;
	}

	/*
	 * Called at the start of each frame
	 */
	public void onFirst(GameEngine engine, int playerID) {
		inputARE = 0;
		boolean newInARE = engine.stat == GameEngine.STAT_ARE ||
			engine.stat == GameEngine.STAT_READY;
		if ((newInARE && !inARE) || (!thinking && !thinkSuccess))
		{
			if (DEBUG_ALL) log.debug("Begin pre-think of next piece.");
			inARE = newInARE;
			thinkComplete = false;
			thinkRequest = true;
		}
		else if (inARE && !newInARE)
			inARE = false;
		if(inARE && delay >= engine.aiMoveDelay) {
			int input = 0;
			Piece nextPiece = engine.getNextObject(engine.nextPieceCount);
			if (bestHold && thinkComplete)
			{
				input |= Controller.BUTTON_BIT_D;
				if (engine.holdPieceObject == null)
					nextPiece = engine.getNextObject(engine.nextPieceCount+1);
				else
					nextPiece = engine.holdPieceObject;
			}
			if (nextPiece == null)
				return;
			nextPiece = checkOffset(nextPiece, engine);
			//input |= calcIRS(nextPiece, engine);
			if (threadRunning && !thinking && (thinkCurrentPieceNo <= thinkLastPieceNo))
			{
				int spawnX = engine.getSpawnPosX(engine.field, nextPiece);
				if(bestX - spawnX > 1) {
					// left
					input |= Controller.BUTTON_BIT_LEFT;
				} else if(spawnX - bestX > 1) {
					// right
					input |= Controller.BUTTON_BIT_RIGHT;
				}
				delay = 0;
			}
			if (DEBUG_ALL) log.debug("Currently in ARE. Next piece type = " + nextPiece.id + ", IRS = " + input);
			//engine.ctrl.setButtonBit(input);
			inputARE = input;
		}
	}

	/*
	 * Called after every frame
	 */
	public void onLast(GameEngine engine, int playerID) {
		if (engine.stat == GameEngine.STAT_READY && engine.statc[0] == 0)
			createTablesRequest = true;
	}

	/*
	 * Set button input states
	 */
	public void setControl(GameEngine engine, int playerID, Controller ctrl) {
		if( (engine.nowPieceObject != null) && (engine.stat == GameEngine.STAT_MOVE) &&
			(delay >= engine.aiMoveDelay) && (engine.statc[0] > 0) &&
		    (!engine.aiUseThread || (threadRunning && !thinking && (thinkCurrentPieceNo <= thinkLastPieceNo))) )
		{
			inputARE = 0;
			int input = 0;	// Button input data
			Piece pieceNow = checkOffset(engine.nowPieceObject, engine);
			int nowX = engine.nowPieceX;
			int nowY = engine.nowPieceY;
			int rt = pieceNow.direction;
			Field fld = engine.field;
			boolean pieceTouchGround = pieceNow.checkCollision(nowX, nowY + 1, fld);
			int nowType = pieceNow.id;
			int width = fld.getWidth();

			int moveDir = 0; //-1 = left,  1 = right
			int rotateDir = 0; //-1 = left,  1 = right
			int drop = 0; //1 = up, -1 = down
			
			if((bestHold == true) && thinkComplete && engine.isHoldOK()) {
				// Hold
				input |= Controller.BUTTON_BIT_D;
				/*
				Piece holdPiece = engine.holdPieceObject;
				if (holdPiece != null)
					input |= calcIRS(holdPiece, engine);
				*/
			} else {
				if (DEBUG_ALL) log.debug("bestX = " + bestX + ", nowX = " + nowX +
						", bestY = " + bestY + ", nowY = " + nowY +
						", bestRt = " + bestRt + ", rt = " + rt +
						", bestRtSub = " + bestRtSub);
				printPieceAndDirection(nowType, rt);
				// Rotation
				//Rotate iff near destination or stuck
				int xDiff = Math.abs(nowX - bestX);
				if (bestX < nowX && nowType == Piece.PIECE_I &&
						rt == Piece.DIRECTION_DOWN && bestRt != rt)
					xDiff--;
				boolean best180 = Math.abs(rt - bestRt) == 2;
				if((rt != bestRt && ((xDiff <= 1) ||
						(bestX == 0 && nowX == 2 && nowType == Piece.PIECE_I) ||
						(((nowX < bestX && pieceNow.checkCollision(nowX+1, nowY, rt, fld)) ||
						(nowX > bestX && pieceNow.checkCollision(nowX-1, nowY, rt, fld))) &&
						!(pieceNow.getMaximumBlockX()+nowX == width-2 && (rt&1) == 1) &&
						!(pieceNow.getMinimumBlockY()+nowY == 2 && pieceTouchGround && (rt&1) == 0 && nowType != Piece.PIECE_I)))))
				{
					//if (DEBUG_ALL) log.debug("Case 1 rotation");

					int lrot = engine.getRotateDirection(-1);
					int rrot = engine.getRotateDirection(1);
					if (DEBUG_ALL) log.debug("lrot = " + lrot + ", rrot = " + rrot);

					if(best180 && (engine.ruleopt.rotateButtonAllowDouble) && !ctrl.isPress(Controller.BUTTON_E))
						input |= Controller.BUTTON_BIT_E;
					else if (bestRt == rrot)
						rotateDir = 1;
					else if(bestRt == lrot)
						rotateDir = -1;
					else if (engine.ruleopt.rotateButtonAllowReverse && best180 && (rt&1) == 1)
					{
						if(rrot == Piece.DIRECTION_UP)
							rotateDir = 1;
						else
							rotateDir = -1;
					}
					else
						rotateDir = 1;
				}

				// 到達可能な位置かどうか
				int minX = pieceNow.getMostMovableLeft(nowX, nowY, rt, fld);
				int maxX = pieceNow.getMostMovableRight(nowX, nowY, rt, fld);

				if(movestate == 0 && (rt == bestRt)
						 && ((bestX < minX - 1) || (bestX > maxX + 1) || (bestY < nowY))) {
					// 到達不能なので再度思考する
					//thinkBestPosition(engine, playerID);
					thinkRequest = true;
					thinkComplete = false;
					//thinkCurrentPieceNo++;
					//System.out.println("rethink c:" + thinkCurrentPieceNo + " l:" + thinkLastPieceNo);
					if (DEBUG_ALL) log.debug("Needs rethink - cannot reach desired position");
				} else {
					// 到達できる場合
					if((nowX == bestX) && (pieceTouchGround)) {
						if (rt == bestRt) {
							// 接地rotation
							if(bestRtSub != 0 && movestate == 0) {
								/*
								int bestRtNew = pieceNow.getRotateDirection(bestRtSub, bestRt);
								if(pieceNow.checkCollision(bestX, bestY, bestRtNew, engine.field) &&
										(engine.wallkick != null) && (engine.ruleopt.rotateWallkick)) {
									WallkickResult kick = engine.wallkick.executeWallkick(bestX, bestY, bestRtSub,
											bestRt, bestRtNew, (engine.ruleopt.rotateMaxUpwardWallkick != 0),
											pieceNow, engine.field, null);

									if(kick != null) {
										bestX += kick.offsetX;
										bestY = pieceNow.getBottom(bestX, bestY + kick.offsetY, engine.field);
									}
								}
								*/
								bestRt = pieceNow.getRotateDirection(bestRtSub, bestRt);
								bestRtSub = 0;
								movestate = 1;
							}
						}
					}
					/*
					//Move left if need to move left, or if at rightmost position and can move left.
					if (pieceTouchGround && pieceNow.id != Piece.PIECE_I &&
							nowX+pieceNow.getMaximumBlockX() == width-1 &&
							!pieceNow.checkCollision(nowX-1, nowY, fld))
					{
						if(!ctrl.isPress(Controller.BUTTON_LEFT) && (engine.aiMoveDelay >= 0))
							input |= Controller.BUTTON_BIT_LEFT;
						bestX = nowX - 1;
					}
					*/
					if((nowX == bestX || movestate > 0) && (rt == bestRt)) {
						moveDir = 0;
						// 目標到達
						if(bestRtSub == 0) {
							if (pieceTouchGround && engine.ruleopt.softdropLock)
								drop = -1;
							else if(engine.ruleopt.harddropEnable)
								drop = 1;
							else if(engine.ruleopt.softdropEnable || engine.ruleopt.softdropLock)
								drop = -1;
						} else {
							if(engine.ruleopt.harddropEnable && !engine.ruleopt.harddropLock)
								drop = 1;
							else if(engine.ruleopt.softdropEnable && !engine.ruleopt.softdropLock)
								drop = -1;
						}
					}
					else if (nowX > bestX)
						moveDir = -1;
					else if(nowX < bestX)
						moveDir = 1;
				}
			}
			//Convert parameters to input
			if(moveDir == -1 && !ctrl.isPress(Controller.BUTTON_LEFT))
				input |= Controller.BUTTON_BIT_LEFT;
			else if(moveDir == 1 && !ctrl.isPress(Controller.BUTTON_RIGHT))
				input |= Controller.BUTTON_BIT_RIGHT;
			if(drop == 1 && !ctrl.isPress(Controller.BUTTON_UP))
				input |= Controller.BUTTON_BIT_UP;
			else if(drop == -1)
				input |= Controller.BUTTON_BIT_DOWN;

			if (rotateDir != 0)
			{
				boolean defaultRotateRight = (engine.owRotateButtonDefaultRight == 1 ||
						(engine.owRotateButtonDefaultRight == -1 &&
								engine.ruleopt.rotateButtonDefaultRight));
				
				if(engine.ruleopt.rotateButtonAllowDouble &&
						rotateDir == 2 && !ctrl.isPress(Controller.BUTTON_E))
					input |= Controller.BUTTON_BIT_E;
				else if(engine.ruleopt.rotateButtonAllowReverse &&
						  !defaultRotateRight && (rotateDir == 1))
				{
					if(!ctrl.isPress(Controller.BUTTON_B))
						input |= Controller.BUTTON_BIT_B;
				}
				else if(engine.ruleopt.rotateButtonAllowReverse &&
						defaultRotateRight && (rotateDir == -1))
				{
					if(!ctrl.isPress(Controller.BUTTON_B))
						input |= Controller.BUTTON_BIT_B;
				}
				else if(!ctrl.isPress(Controller.BUTTON_A))
					input |= Controller.BUTTON_BIT_A;
			}

			if (DEBUG_ALL) log.debug ("Input = " + input + ", moveDir = " + moveDir  + ", rotateDir = " + rotateDir +
					 ", drop = " + drop);

			delay = 0;
			ctrl.setButtonBit(input);
		}
		else {
			delay++;
			ctrl.setButtonBit(inputARE);
		}
	}

	protected void printPieceAndDirection(int pieceType, int rt)
	{
		String result = "Piece ";
		switch (pieceType)
		{
			case Piece.PIECE_I: result = result + "I"; break;
			case Piece.PIECE_L: result = result + "L"; break;
			case Piece.PIECE_O: result = result + "O"; break;
			case Piece.PIECE_Z: result = result + "Z"; break;
			case Piece.PIECE_T: result = result + "T"; break;
			case Piece.PIECE_J: result = result + "J"; break;
			case Piece.PIECE_S: result = result + "S"; break;
			case Piece.PIECE_I1: result = result + "I1"; break;
			case Piece.PIECE_I2: result = result + "I2"; break;
			case Piece.PIECE_I3: result = result + "I3"; break;
			case Piece.PIECE_L3: result = result + "L3"; break;
		}
		result = result + ", direction ";

		switch (rt)
		{
			case Piece.DIRECTION_LEFT:  result = result + "left";  break;
			case Piece.DIRECTION_DOWN:  result = result + "down";  break;
			case Piece.DIRECTION_UP:    result = result + "up";    break;
			case Piece.DIRECTION_RIGHT: result = result + "right"; break;
		}
		if (DEBUG_ALL) log.debug(result);
	}

	/*
	public int calcIRS(Piece piece, GameEngine engine)
	{
		piece = checkOffset(piece, engine);
		int nextType = piece.id;
		Field fld = engine.field;
		int spawnX = engine.getSpawnPosX(fld, piece);
		SpeedParam speed = engine.speed;
		boolean gravityHigh = speed.gravity > speed.denominator;
		int width = fld.getWidth();
		int midColumnX = (width/2)-1;
		if(Math.abs(spawnX - bestX) == 1)
		{
			if (bestRt == 1)
			{
				if (engine.ruleopt.rotateButtonDefaultRight)
					return Controller.BUTTON_BIT_A;
				else
					return Controller.BUTTON_BIT_B;
			}
			else if (bestRt == 3)
			{
				if (engine.ruleopt.rotateButtonDefaultRight)
					return Controller.BUTTON_BIT_B;
				else
					return Controller.BUTTON_BIT_A;
			}
		}
		else if (nextType == Piece.PIECE_L)
		{
			if (gravityHigh && fld.getHighestBlockY(midColumnX-1) <
					Math.min(fld.getHighestBlockY(midColumnX), fld.getHighestBlockY(midColumnX+1)))
				return 0;
			else if (engine.ruleopt.rotateButtonDefaultRight)
				return Controller.BUTTON_BIT_B;
			else
				return Controller.BUTTON_BIT_A;
		}
		else if (nextType == Piece.PIECE_J)
		{
			if (gravityHigh && fld.getHighestBlockY(midColumnX+1) <
					Math.min(fld.getHighestBlockY(midColumnX), fld.getHighestBlockY(midColumnX-1)))
				return 0;
			if (engine.ruleopt.rotateButtonDefaultRight)
				return Controller.BUTTON_BIT_A;
			else
				return Controller.BUTTON_BIT_B;
		}
		//else if (nextType == Piece.PIECE_I)
		//	return Controller.BUTTON_BIT_A;
		return 0;
	}
	*/

	/**
	 * Search for the best choice
	 * @param engine The GameEngine that owns this AI
	 * @param playerID Player ID
	 */
	public void thinkBestPosition(GameEngine engine, int playerID) {
		if (DEBUG_ALL) log.debug("thinkBestPosition called, inARE = " + inARE + ", piece: ");
		bestHold = false;
		bestX = 0;
		bestY = 0;
		bestRt = 0;
		bestRtSub = 0;
		bestPts = Integer.MIN_VALUE;
		thinkSuccess = false;

		Field fld;
		if (engine.field == null)
			fld = new Field(engine.fieldWidth, engine.fieldHeight,
					engine.fieldHiddenHeight, engine.ruleopt.fieldCeiling);
		else
			fld = new Field(engine.field);
		Piece pieceNow = engine.nowPieceObject;
		Piece pieceHold = engine.holdPieceObject;
		boolean holdBoxEmpty = (pieceHold == null);
		/*
		Piece pieceNow = null;
		if (engine.nowPieceObject != null)
			pieceNow = new Piece(engine.nowPieceObject);
		Piece pieceHold = null;
		if (engine.holdPieceObject != null)
			pieceHold = new Piece(engine.holdPieceObject);
		*/
		int nextIndex = engine.nextPieceCount;
		if (inARE || pieceNow == null)
		{
			pieceNow = engine.getNextObjectCopy(nextIndex);
			nextIndex++;
		}
		if(holdBoxEmpty)
			pieceHold = engine.getNextObjectCopy(nextIndex);
		else if (holdBoxEmpty)
			pieceHold = engine.getNextObjectCopy(engine.nextPieceCount);
		pieceNow = checkOffset(pieceNow, engine);
		pieceHold = checkOffset(pieceHold, engine);
		boolean holdOK = engine.isHoldOK();
		
		nextQueueIDs = new int[MAX_THINK_DEPTH-1];
		for (int i = 0; i < nextQueueIDs.length; i++)
			nextQueueIDs[i] = engine.getNextID(nextIndex+i);
		
		int state = fieldToIndex(fld);
		if (state < 0)
		{
			thinkLastPieceNo++;
			return;
		}
		Transition t = moves[state][pieceNow.id];
		
		while (t != null)
		{
			int holdID = -1;
			if (engine.holdPieceObject != null)
				holdID = engine.holdPieceObject.id;
			int pts = thinkMain(engine, t.newField, holdID, 0);
			
			if (pts > bestPts)
			{
				bestPts = pts;
				bestX = t.x + 3;
				bestRt = t.rt;
				bestY = pieceNow.getBottom(bestX, 0, bestRt, fld);
				bestRtSub = t.rtSub;
				bestHold = false;
				thinkSuccess = true;
			}
			
			t = t.next;
		}
		if (pieceHold.id != pieceNow.id && holdOK)
		{
			t = moves[state][pieceHold.id];
			
			while (t != null)
			{
				int pts = thinkMain(engine, t.newField, pieceNow.id, holdBoxEmpty ? 1 : 0);
				
				if (pts > bestPts)
				{
					bestPts = pts;
					bestX = t.x + 3;
					bestRt = t.rt;
					bestY = pieceNow.getBottom(bestX, 0, bestRt, fld);
					bestRtSub = t.rtSub;
					bestHold = true;
					thinkSuccess = true;
				}
				
				t = t.next;
			}
		}

		thinkLastPieceNo++;

		//System.out.println("X:" + bestX + " Y:" + bestY + " R:" + bestRt + " H:" + bestHold + " Pts:" + bestPts);
	}

	/**
	 * Think routine
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param rt Direction
	 * @param rtOld Direction before rotation (-1: None）
	 * @param fld Field (Can be modified without problems)
	 * @param piece Piece
	 * @param depth Compromise level (ranges from 0 through getMaxThinkDepth-1)
	 * @return Evaluation score
	 */
	public int thinkMain(GameEngine engine, int state, int holdID, int depth) {
		if (state == -1)
			return 0;
		if (depth == nextQueueIDs.length)
			return scores[state] + ((holdID == Piece.PIECE_I) ? 1000 : 0);
		
		int bestPts = 0;
		Transition t = moves[state][nextQueueIDs[depth]];
		
		while (t != null)
		{
			bestPts = Math.max(bestPts,
					thinkMain(engine, t.newField, holdID, depth+1) + 1000);
			t = t.next;
		}
		
		if (engine.ruleopt.holdEnable)
		{
			if (holdID == -1)
				bestPts = Math.max(bestPts, thinkMain(engine, state, nextQueueIDs[depth], depth+1));
			else
			{
				t = moves[state][holdID];
				while (t != null)
				{
					bestPts = Math.max(bestPts,
							thinkMain(engine, t.newField, nextQueueIDs[depth], depth+1) + 1000);
					t = t.next;
				}
			}
		}
		
		return bestPts;
	}

	public static Piece checkOffset(Piece p, GameEngine engine)
	{
		Piece result = new Piece(p);
		result.big = engine.big;
		if (!p.offsetApplied)
			result.applyOffsetArray(engine.ruleopt.pieceOffsetX[p.id], engine.ruleopt.pieceOffsetY[p.id]);
		return result;
	}

	protected void logBest(int caseNum)
	{
		log.debug("New best position found (Case " + caseNum +
				"): bestHold = " + bestHold +
				", bestX = " + bestX +
				", bestY = " + bestY +
				", bestRt = " + bestRt +
				", bestRtSub = " + bestRtSub +
				", bestPts = " + bestPts);
	}

	/*
	 * スレッドの処理
	 */
	public void run() {
		log.info("ComboRaceBot: Thread start");
		threadRunning = true;

		while(threadRunning) {
			if(thinkRequest) {
				thinkRequest = false;
				thinking = true;
				try {
					thinkBestPosition(gEngine, gEngine.playerID);
					thinkComplete = true;
					log.debug("ComboRaceBot: thinkBestPosition completed successfully");
				} catch (Throwable e) {
					log.debug("ComboRaceBot: thinkBestPosition Failed", e);
				}
				thinking = false;
			}
			else if (createTablesRequest)
				createTables(gEngine);

			if(thinkDelay > 0) {
				try {
					Thread.sleep(thinkDelay);
				} catch (InterruptedException e) {
					break;
				}
			}
		}

		threadRunning = false;
		log.info("ComboRaceBot: Thread end");
	}
	
	/**
	 * Constructs the moves table if necessary.
	 * Note: Horizontal I placement is NOT included!
	 */
	public void createTables (GameEngine engine)
	{
		if (moves != null)
			return;
		
		moves = new Transition[FIELDS.length][7];
		
		Field fldEmpty = new Field(4, Field.DEFAULT_HEIGHT, Field.DEFAULT_HIDDEN_HEIGHT);
		Field fldBackup = new Field(fldEmpty);
		Field fldTemp = new Field(fldEmpty);
		
		Piece[] pieces = new Piece[7];
		for (int p = 0; p < 7; p++)
		{
			pieces[p] = checkOffset(new Piece(p), engine);
			pieces[p].setColor(1);
		}
		
		int count = 0;
		
		for (int i = 0; i < FIELDS.length; i++)
		{
			fldBackup.copy(fldEmpty);
			int code = FIELDS[i];

			for (int y = Field.DEFAULT_HEIGHT-1; y > Field.DEFAULT_HEIGHT-4; y--)
				for (int x = 3; x >= 0; x--)
				{
					if ((code & 1) == 1)
						fldBackup.setBlockColor(x, y, 1);
					code >>= 1;
				}

			for (int p = 0; p < 7; p++)
			{
				int tempX = engine.getSpawnPosX(fldBackup, pieces[p]);
				for (int rt = 0; rt < Piece.DIRECTION_COUNT; rt++)
				{
					int minX = pieces[p].getMostMovableLeft(tempX, 0, rt, fldBackup);
					int maxX = pieces[p].getMostMovableRight(tempX, 0, rt, fldBackup);
					
					for (int x = minX; x <= maxX; x++)
					{
						int y = pieces[p].getBottom(x, 0, rt, fldBackup);
						if (p == Piece.PIECE_L || p == Piece.PIECE_T || p == Piece.PIECE_J || rt < 2)
						{
							fldTemp.copy(fldBackup);
							pieces[p].placeToField(x, y, rt, fldTemp);
							if (fldTemp.checkLine() == 1)
							{
								fldTemp.clearLine();
								fldTemp.downFloatingBlocks();
								int index = fieldToIndex(fldTemp, 0);
								if (index >= 0)
								{
									moves[i][p] = new Transition(x, rt, index, moves[i][p]);
									count++;
								}
							}
							if (p == Piece.PIECE_O)
								continue;
						}

						// Left rotation
						if(!engine.ruleopt.rotateButtonDefaultRight || engine.ruleopt.rotateButtonAllowReverse) {
							int rot = pieces[p].getRotateDirection(-1, rt);
							int newX = x;
							int newY = y;
							fldTemp.copy(fldBackup);

							if(pieces[p].checkCollision(x, y, rot, fldTemp) && (engine.wallkick != null) &&
									(engine.ruleopt.rotateWallkick)) {
								WallkickResult kick = engine.wallkick.executeWallkick(x, y, -1, rt, rot,
										(engine.ruleopt.rotateMaxUpwardWallkick != 0), pieces[p], fldTemp, null);

								if(kick != null) {
									newX = x + kick.offsetX;
									newY = pieces[p].getBottom(newX, y + kick.offsetY, fldTemp);
								}
							}
							
							if (!pieces[p].checkCollision(newX, newY, rot, fldTemp)
									&& newY > pieces[p].getBottom(newX, 0, rot, fldTemp))
							{
								pieces[p].placeToField(newX, newY, rot, fldTemp);
								if (fldTemp.checkLine() == 1)
								{
									fldTemp.clearLine();
									fldTemp.downFloatingBlocks();
									int index = fieldToIndex(fldTemp, 0);
									if (index >= 0)
									{
										moves[i][p] = new Transition(x, rt, -1, index, moves[i][p]);
										count++;
									}
								}
							}
						}

						// Right rotation
						if(engine.ruleopt.rotateButtonDefaultRight || engine.ruleopt.rotateButtonAllowReverse) {
							int rot = pieces[p].getRotateDirection(1, rt);
							int newX = x;
							int newY = y;
							fldTemp.copy(fldBackup);
							
							if(pieces[p].checkCollision(x, y, rot, fldTemp) && (engine.wallkick != null) &&
									(engine.ruleopt.rotateWallkick)) {
								WallkickResult kick = engine.wallkick.executeWallkick(x, y, 1, rt, rot,
										(engine.ruleopt.rotateMaxUpwardWallkick != 0), pieces[p], fldTemp, null);

								if(kick != null) {
									newX = x + kick.offsetX;
									newY = pieces[p].getBottom(newX, y + kick.offsetY, fldTemp);
								}
							}
							
							if (!pieces[p].checkCollision(newX, newY, rot, fldTemp)
									&& newY > pieces[p].getBottom(newX, 0, rot, fldTemp))
							{
								pieces[p].placeToField(newX, newY, rot, fldTemp);
								if (fldTemp.checkLine() == 1)
								{
									fldTemp.clearLine();
									fldTemp.downFloatingBlocks();
									int index = fieldToIndex(fldTemp, 0);
									if (index >= 0)
									{
										moves[i][p] = new Transition(x, rt, 1, index, moves[i][p]);
										count++;
									}
								}
							}
						}
					}
					
					if (pieces[p].id == Piece.PIECE_O)
						break;
				}
			}
		}
		log.debug("Transition table created. Total entries: " + count);
		//TODO: PageRank scores for each state
	}
	
	/**
	 * Converts field to field state int code
	 * @param field Field object
	 * @param valleyX Leftmost x-coordinate of 4-block-wide valley to combo in
	 * @return Field state int code.
	 */
	public static int fieldToCode(Field field, int valleyX)
	{
		int height = field.getHeight();
		int result = 0;
		for (int y = height-3; y < height; y++)
			for (int x = 0; x < 4; x++)
			{
				result <<= 1;
				if (!field.getBlockEmptyF(x+valleyX, y))
					result++;
			}
		return result;
	}
	public static int fieldToCode(Field field)
	{
		return fieldToCode(field, 3);
	}
	
	/**
	 * Converts field state int code to FIELDS array index
	 * @param field Field state int code
	 * @return State index if found; -1 if not found.
	 */
	public static int fieldToIndex(int field)
	{
		int min = 0;
		int max = FIELDS.length-1;
		int mid;
		while (min <= max)
		{
			mid = (min+max) >> 1;
			if (FIELDS[mid] > field)
				max = mid-1;
			else if (FIELDS[mid] < field)
				min = mid+1;
			else
				return mid;
		}
		return -1;
	}
	
	/**
	 * Converts field object to FIELDS array index
	 * @param field Field object
	 * @param valleyX Leftmost x-coordinate of 4-block-wide valley to combo in
	 * @return State index if found; -1 if not found.
	 */
	public static int fieldToIndex(Field field, int valleyX)
	{
		return fieldToIndex(fieldToCode(field, valleyX));
	}
	public static int fieldToIndex(Field field)
	{
		return fieldToIndex(fieldToCode(field));
	}

	protected static class Transition
	{
		public int x, rt, rtSub, newField;
		public Transition next;
		public Transition (int bestX, int bestRt, int bestRtSub, int newFld)
		{
			x = bestX;
			rt = bestRt;
			rtSub = bestRtSub;
			newField = newFld;
		}
		public Transition (int bestX, int bestRt, int newFld)
		{
			x = bestX;
			rt = bestRt;
			rtSub = 0;
			newField = newFld;
		}
		public Transition (int bestX, int bestRt, int bestRtSub, int newFld, Transition nxt)
		{
			x = bestX;
			rt = bestRt;
			rtSub = bestRtSub;
			newField = newFld;
			next = nxt;
		}
		public Transition (int bestX, int bestRt, int newFld, Transition nxt)
		{
			x = bestX;
			rt = bestRt;
			rtSub = 0;
			newField = newFld;
			next = nxt;
		}
	}
}
