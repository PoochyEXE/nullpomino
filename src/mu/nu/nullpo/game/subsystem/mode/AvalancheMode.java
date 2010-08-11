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
package mu.nu.nullpo.game.subsystem.mode;

import mu.nu.nullpo.game.component.Block;
import mu.nu.nullpo.game.component.Controller;
import mu.nu.nullpo.game.component.Piece;
import mu.nu.nullpo.game.event.EventReceiver;
import mu.nu.nullpo.game.play.GameEngine;
import mu.nu.nullpo.game.play.GameManager;
import mu.nu.nullpo.util.CustomProperties;
import mu.nu.nullpo.util.GeneralUtil;

/**
 * AVALANCHE mode (Release Candidate 1)
 */
public class AvalancheMode extends DummyMode {
	/** Current version */
	private static final int CURRENT_VERSION = 0;
	
	/** Enabled piece types */
	private static final int[] PIECE_ENABLE = {0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0};
	
	/** Block colors */
	private static final int[] BLOCK_COLORS =
	{
		Block.BLOCK_COLOR_RED,
		Block.BLOCK_COLOR_GREEN,
		Block.BLOCK_COLOR_BLUE,
		Block.BLOCK_COLOR_YELLOW,
		Block.BLOCK_COLOR_PURPLE
	};

	public int[] tableGravityChangeScore =
	{
		15000, 30000, 40000, 50000, 60000, 70000, 80000, 90000, 100000, 150000, 250000, 400000, Integer.MAX_VALUE
	};

	public int[] tableGravityValue =
	{
		1, 2, 3, 4, 6, 8, 10, 20, 30, 60, 120, 180, 300, -1
	};

	/** Number of ranking records */
	private static final int RANKING_MAX = 10;

	/** Number of ranking types */
	private static final int RANKING_TYPE = 3;

	/** Name of game types */
	private static final String[] GAMETYPE_NAME = {"MARATHON","ULTRA","SPRINT"};

	/** Number of game types */
	private static final int GAMETYPE_MAX = 3;

	/** Max time in Ultra */
	private static final int ULTRA_MAX_TIME = 10800;

	/** Max score in Sprint */
	private static final int SPRINT_MAX_SCORE = 15000;

	/** GameManager object (Manages entire game status) */
	private GameManager owner;

	/** EventReceiver object (This receives many game events, can also be used for drawing the fonts.) */
	private EventReceiver receiver;

	/** Current 落下速度の number（tableGravityChangeLevelの levelに到達するたびに1つ増える） */
	private int gravityindex;

	/** Amount of points earned from most recent clear */
	private int lastscore, lastmultiplier;

	/** Elapsed time from last line clear (lastscore is displayed to screen until this reaches to 120) */
	private int scgettime;

	/** Selected game type */
	private int gametype;

	/** Outline type */
	private int outlinetype;

	/** Version number */
	private int version;

	/** Current round's ranking rank */
	private int rankingRank;

	/** Rankings' line counts */
	private int[][] rankingScore;

	/** Rankings' times */
	private int[][] rankingTime;
	
	/** Flag for all clear */
	private boolean zenKeshi;
	
	/** Flag set to true if first group in the chain is larger minimum size */
	//private boolean firstExtra;
	
	/** Amount of garbage sent */
	private int garbageSent, garbageAdd;
	
	/** Number of colors to use */
	private int numColors;
	
	/** If true, both columns 3 and 4 are danger columns */
	private boolean dangerColumnDouble;
	
	/** If true, red X's appear at tops of danger columns */
	private boolean dangerColumnShowX;
	
	/** Last chain hit number */
	private int chain;
	
	/** Time to display last chain */
	private int chainDisplay;
	
	/** Chain display enable/disable */
	private boolean showChains;
	
	/** Number of all clears */
	private int zenKeshiCount;
	
	/** Max chain */
	private int maxChain;
	
	/** Score before adding zenkeshi bonus and max chain bonus */
	private int scoreBeforeBonus;
	
	/** Zenkeshi bonus and max chain bonus amounts */
	private int zenKeshiBonus, maxChainBonus;
	
	/** Blocks cleared */
	private int blocksCleared;
	
	/** Current level */
	private int level;
	
	/** Blocks blocksCleared needed to reach next level */
	private int toNextLevel;

	/*
	 * Mode  name
	 */
	@Override
	public String getName() {
		return "AVALANCHE 1P (RC1)";
	}

	/*
	 * Initialization
	 */
	@Override
	public void playerInit(GameEngine engine, int playerID) {
		owner = engine.owner;
		receiver = engine.owner.receiver;
		lastscore = 0;
		lastmultiplier = 0;
		scgettime = 0;

		outlinetype = 0;
		
		zenKeshi = false;
		garbageSent = 0;
		garbageAdd = 0;
		//firstExtra = false;
		
		zenKeshiCount = 0;
		maxChain = 0;
		scoreBeforeBonus = 0;
		zenKeshiBonus = 0;
		maxChainBonus = 0;
		blocksCleared = 0;
		
		chain = 0;
		chainDisplay = 0;
		showChains = true;
		level = 5;
		toNextLevel = 15;

		rankingRank = -1;
		rankingScore = new int[RANKING_TYPE][RANKING_MAX];
		rankingTime = new int[RANKING_TYPE][RANKING_MAX];

		if(owner.replayMode == false) {
			loadSetting(owner.modeConfig);
			loadRanking(owner.modeConfig, engine.ruleopt.strRuleName);
			version = CURRENT_VERSION;
		} else {
			loadSetting(owner.replayProp);
		}

		engine.framecolor = GameEngine.FRAME_COLOR_PURPLE;
		engine.clearMode = GameEngine.CLEAR_COLOR;
		engine.garbageColorClear = true;
		engine.colorClearSize = 4;
		engine.ignoreHidden = true;
		engine.lineGravityType = GameEngine.LINE_GRAVITY_CASCADE;
		for(int i = 0; i < Piece.PIECE_COUNT; i++)
			engine.nextPieceEnable[i] = (PIECE_ENABLE[i] == 1);
		engine.randomBlockColor = true;
		engine.blockColors = BLOCK_COLORS;
		engine.connectBlocks = false;
		engine.cascadeDelay = 1;
		engine.cascadeClearDelay = 10;
		/*
		engine.fieldWidth = 6;
		engine.fieldHeight = 12;
		engine.fieldHiddenHeight = 2;
		*/
	}

	/**
	 * Set the gravity rate
	 * @param engine GameEngine
	 */
	public void setSpeed(GameEngine engine) {
		if (gametype == 0) {
			int speedlv = engine.statistics.score;
			if (speedlv < 0) speedlv = 0;
			if (speedlv > 5000) speedlv = 5000;

			while(speedlv >= tableGravityChangeScore[gravityindex]) gravityindex++;
			engine.speed.gravity = tableGravityValue[gravityindex];
		} else {
			engine.speed.gravity = 1;
		}
		engine.speed.denominator = 256;
	}

	public boolean onReady(GameEngine engine, int playerID) {
		if(engine.statc[0] == 0)
		{
			engine.numColors = numColors;

			if(outlinetype == 0) engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL;
			else if(outlinetype == 1) engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_SAMECOLOR;
			else if(outlinetype == 2) engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NONE;

			if (numColors == 3) level = 1;
			else if (numColors == 4) level = 5;
			else if (numColors == 5) level = 10;
			toNextLevel = 15;
			
			zenKeshiCount = 0;
			maxChain = 0;
			scoreBeforeBonus = 0;
			zenKeshiBonus = 0;
			maxChainBonus = 0;
			blocksCleared = 0;
		}
		return false;
	}
	
	/*
	 * Called at settings screen
	 */
	@Override
	public boolean onSetting(GameEngine engine, int playerID) {
		// Menu
		if(engine.owner.replayMode == false) {
			// Up
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_UP)) {
				engine.statc[2]--;
				if(engine.statc[2] < 0) engine.statc[2] = 5;
				engine.playSE("cursor");
			}
			// Down
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
				engine.statc[2]++;
				if(engine.statc[2] > 5) engine.statc[2] = 0;
				engine.playSE("cursor");
			}

			// Configuration changes
			int change = 0;
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_LEFT)) change = -1;
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_RIGHT)) change = 1;

			if(change != 0) {
				engine.playSE("change");

				switch(engine.statc[2]) {

				case 0:
					gametype += change;
					if(gametype < 0) gametype = GAMETYPE_MAX - 1;
					if(gametype > GAMETYPE_MAX - 1) gametype = 0;
					break;
				case 1:
					outlinetype += change;
					if(outlinetype < 0) outlinetype = 2;
					if(outlinetype > 2) outlinetype = 0;
					break;
				case 2:
					numColors += change;
					if(numColors < 3) numColors = 5;
					if(numColors > 5) numColors = 3;
					break;
				case 3:
					dangerColumnDouble = !dangerColumnDouble;
					break;
				case 4:
					dangerColumnShowX = !dangerColumnShowX;
					break;
				case 5:
					showChains = !showChains;
				}
			}

			// 決定
			if(engine.ctrl.isPush(Controller.BUTTON_A) && (engine.statc[3] >= 5)) {
				engine.playSE("decide");
				saveSetting(owner.modeConfig);
				receiver.saveModeConfig(owner.modeConfig);
				return false;
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) {
				engine.quitflag = true;
			}

			engine.statc[3]++;
		} else {
			engine.statc[3]++;
			engine.statc[2] = -1;

			if(engine.statc[3] >= 60) {
				return false;
			}
		}

		return true;
	}

	/*
	 * Render the settings screen
	 */
	@Override
	public void renderSetting(GameEngine engine, int playerID) {
		if(engine.owner.replayMode == false) {
			receiver.drawMenuFont(engine, playerID, 0, (engine.statc[2] * 2) + 1, "b", EventReceiver.COLOR_RED);
		}

		receiver.drawMenuFont(engine, playerID, 0, 0, "GAME TYPE", EventReceiver.COLOR_BLUE);
		receiver.drawMenuFont(engine, playerID, 1, 1, GAMETYPE_NAME[gametype], (engine.statc[2] == 0));
		receiver.drawMenuFont(engine, playerID, 0, 2, "OUTLINE", EventReceiver.COLOR_BLUE);
		String strOutline = "";
		if(outlinetype == 0) strOutline = "NORMAL";
		if(outlinetype == 1) strOutline = "COLOR";
		if(outlinetype == 2) strOutline = "NONE";
		receiver.drawMenuFont(engine, playerID, 1, 3, strOutline, (engine.statc[2] == 1));
		receiver.drawMenuFont(engine, playerID, 0, 4, "COLORS", EventReceiver.COLOR_BLUE);
		receiver.drawMenuFont(engine, playerID, 1, 5, String.valueOf(numColors), (engine.statc[2] == 2));
		receiver.drawMenuFont(engine, playerID, 0, 6, "X COLUMN", EventReceiver.COLOR_BLUE);
		receiver.drawMenuFont(engine, playerID, 1, 7, dangerColumnDouble ? "3 AND 4" : "3 ONLY", (engine.statc[2] == 3));
		receiver.drawMenuFont(engine, playerID, 0, 8, "X SHOW", EventReceiver.COLOR_BLUE);
		receiver.drawMenuFont(engine, playerID, 1, 9, GeneralUtil.getONorOFF(dangerColumnShowX), (engine.statc[2] == 4));
		receiver.drawMenuFont(engine, playerID, 0, 10, "SHOW CHAIN", EventReceiver.COLOR_BLUE);
		receiver.drawMenuFont(engine, playerID, 1, 11, GeneralUtil.getONorOFF(showChains), (engine.statc[2] == 5));
	}

	/*
	 * Called for initialization during "Ready" screen
	 */
	@Override
	public void startGame(GameEngine engine, int playerID) {
		engine.comboType = GameEngine.COMBO_TYPE_DISABLE;

		engine.tspinEnable = false;
		engine.useAllSpinBonus = false;
		engine.tspinAllowKick = false;

		engine.speed.are = 30;
		engine.speed.areLine = 30;
		engine.speed.das = 10;
		engine.speed.lockDelay = 60;

		setSpeed(engine);
	}

	/*
	 * Render score
	 */
	@Override
	public void renderLast(GameEngine engine, int playerID) {
		receiver.drawScoreFont(engine, playerID, 0, 0, "AVALANCHE ("+GAMETYPE_NAME[gametype]+")", EventReceiver.COLOR_DARKBLUE);

		if( (engine.stat == GameEngine.STAT_SETTING) || ((engine.stat == GameEngine.STAT_RESULT) && (owner.replayMode == false)) ) {
			if((owner.replayMode == false) && (engine.ai == null)) {
				if (gametype == 0) {
					receiver.drawScoreFont(engine, playerID, 3, 3, "SCORE      TIME", EventReceiver.COLOR_BLUE);
				} else if (gametype == 1) {
					receiver.drawScoreFont(engine, playerID, 3, 3, "SCORE", EventReceiver.COLOR_BLUE);
				} else if (gametype == 2) {
					receiver.drawScoreFont(engine, playerID, 3, 3, "TIME", EventReceiver.COLOR_BLUE);
				}

				for(int i = 0; i < RANKING_MAX; i++) {
					receiver.drawScoreFont(engine, playerID, 0, 4 + i, String.format("%2d", i + 1), EventReceiver.COLOR_YELLOW);
					if (gametype == 0) {
						receiver.drawScoreFont(engine, playerID, 3, 4 + i, String.valueOf(rankingScore[gametype][i]), (i == rankingRank));
						receiver.drawScoreFont(engine, playerID, 14, 4 + i, GeneralUtil.getTime(rankingTime[gametype][i]), (i == rankingRank));
					} else if (gametype == 1) {
						receiver.drawScoreFont(engine, playerID, 3, 4 + i, String.valueOf(rankingScore[gametype][i]), (i == rankingRank));
					} else if (gametype == 2) {
						receiver.drawScoreFont(engine, playerID, 3, 4 + i, GeneralUtil.getTime(rankingTime[gametype][i]), (i == rankingRank));
					}
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "SCORE", EventReceiver.COLOR_BLUE);
			String strScore;
			if((lastscore == 0) || (lastmultiplier == 0) || (scgettime <= 0)) {
				strScore = String.valueOf(engine.statistics.score);
			} else {
				strScore = String.valueOf(engine.statistics.score) + "(+" + String.valueOf(lastscore) + "X" +
					String.valueOf(lastmultiplier) + ")";
			}
			receiver.drawScoreFont(engine, playerID, 0, 4, strScore);
			
			receiver.drawScoreFont(engine, playerID, 0, 6, "LEVEL", EventReceiver.COLOR_BLUE);
			receiver.drawScoreFont(engine, playerID, 0, 7, String.valueOf(level));

			receiver.drawScoreFont(engine, playerID, 0, 9, "OJAMA SENT", EventReceiver.COLOR_BLUE);
			String strSent = String.valueOf(garbageSent);
			if(garbageAdd > 0) {
				strSent = strSent + "(+" + String.valueOf(garbageAdd)+ ")";
			}
			receiver.drawScoreFont(engine, playerID, 0, 10, strSent);
			
			receiver.drawScoreFont(engine, playerID, 0, 12, "TIME", EventReceiver.COLOR_BLUE);
			receiver.drawScoreFont(engine, playerID, 0, 13, GeneralUtil.getTime(engine.statistics.time));
			
			receiver.drawScoreFont(engine, playerID, 14, 6, "CLEARED", EventReceiver.COLOR_BLUE);
			receiver.drawScoreFont(engine, playerID, 14, 7, String.valueOf(blocksCleared));
			
			receiver.drawScoreFont(engine, playerID, 14, 9, "ZENKESHI", EventReceiver.COLOR_BLUE);
			receiver.drawScoreFont(engine, playerID, 14, 10, String.valueOf(zenKeshiCount));
			
			receiver.drawScoreFont(engine, playerID, 14, 12, "MAX CHAIN", EventReceiver.COLOR_BLUE);
			receiver.drawScoreFont(engine, playerID, 14, 13, String.valueOf(maxChain));
			
			if (dangerColumnShowX)
				receiver.drawMenuFont(engine, playerID, 2, 0, dangerColumnDouble ? "XX" : "X", EventReceiver.COLOR_RED);

			int textHeight = 13;
			if (engine.field != null)
				textHeight = engine.field.getHeight()+1;
			if (chain > 0 && chainDisplay > 0 && showChains)
				receiver.drawMenuFont(engine, playerID, chain > 9 ? 0 : 1, textHeight, chain + " CHAIN!", EventReceiver.COLOR_YELLOW);
			if (zenKeshi)
				receiver.drawMenuFont(engine, playerID, 0, textHeight+1, "ZENKESHI!", EventReceiver.COLOR_YELLOW);
		}
	}

	/*
	 * Called after every frame
	 */
	@Override
	public void onLast(GameEngine engine, int playerID) {
		if (scgettime > 0)
			scgettime--;
		if (chainDisplay > 0)
			chainDisplay--;

		if (gametype == 1) {
			int remainTime = ULTRA_MAX_TIME - engine.statistics.time;
			//  timeMeter
			engine.meterValue = (remainTime * receiver.getMeterMax(engine)) / ULTRA_MAX_TIME;
			engine.meterColor = GameEngine.METER_COLOR_GREEN;
			if(remainTime <= 3600) engine.meterColor = GameEngine.METER_COLOR_YELLOW;
			if(remainTime <= 1800) engine.meterColor = GameEngine.METER_COLOR_ORANGE;
			if(remainTime <= 600) engine.meterColor = GameEngine.METER_COLOR_RED;

			//  time切れ
			if((engine.statistics.time >= ULTRA_MAX_TIME) && (engine.timerActive == true)) {
				engine.gameActive = false;
				engine.timerActive = false;
				engine.resetStatc();
				engine.stat = GameEngine.STAT_ENDINGSTART;
				return;
			}
		} else if (gametype == 2) {
			int remainScore = SPRINT_MAX_SCORE - engine.statistics.score;
			if(engine.timerActive == false) remainScore = 0;
			engine.meterValue = (remainScore * receiver.getMeterMax(engine)) / SPRINT_MAX_SCORE;
			engine.meterColor = GameEngine.METER_COLOR_GREEN;
			if(remainScore <= 50) engine.meterColor = GameEngine.METER_COLOR_YELLOW;
			if(remainScore <= 30) engine.meterColor = GameEngine.METER_COLOR_ORANGE;
			if(remainScore <= 10) engine.meterColor = GameEngine.METER_COLOR_RED;

			// ゴール
			if((engine.statistics.score >= SPRINT_MAX_SCORE) && (engine.timerActive == true)) {
				engine.gameActive = false;
				engine.timerActive = false;
				engine.resetStatc();
				engine.stat = GameEngine.STAT_ENDINGSTART;
			}
		}
	}

	/*
	 * ゲームオーバー
	 */
	@Override
	public boolean onGameOver(GameEngine engine, int playerID) {
		if(engine.statc[0] == 0)
		{
			scoreBeforeBonus = engine.statistics.score;
			zenKeshiBonus = zenKeshiCount*(zenKeshiCount+3)*250;
			maxChainBonus = maxChain*maxChain*2000;
			engine.statistics.score += zenKeshiBonus + maxChainBonus;
		}
		return false;
	}

	/*
	 * Hard dropしたときの処理
	 */
	@Override
	public void afterHardDropFall(GameEngine engine, int playerID, int fall) {
		engine.statistics.score += fall;
	}

	/*
	 * Hard dropしたときの処理
	 */
	@Override
	public void afterSoftDropFall(GameEngine engine, int playerID, int fall) {
		engine.statistics.score += fall;
	}

	/*
	 * Calculate score
	 */
	@Override
	public void calcScore(GameEngine engine, int playerID, int avalanche) {
		blocksCleared += avalanche;
		toNextLevel -= avalanche;
		if (toNextLevel <= 0)
		{
			toNextLevel = 15;
			level++;
		}
		
		// Line clear bonus
		int pts = avalanche*10;

		if (avalanche > 0) {
			if (zenKeshi)
				garbageAdd += 30;
			if (engine.field.isEmpty()) {
				engine.playSE("bravo");
				zenKeshi = true;
				//engine.statistics.score += 2100;
			}
			else
				zenKeshi = false;

			chain = engine.chain;
			chainDisplay = 60;
			engine.playSE("combo" + Math.min(chain, 20));
			int multiplier = engine.field.colorClearExtraCount;
			if (engine.field.colorsCleared > 1)
				multiplier += (engine.field.colorsCleared-1)*2;
			/*
			if (multiplier < 0)
				multiplier = 0;
			if (chain == 0)
				firstExtra = avalanche > engine.colorClearSize;
			*/
			if (chain == 2)
				multiplier += 8;
			else if (chain == 3)
				multiplier += 16;
			else if (chain >= 4)
				multiplier += 32*(chain-3);
			/*
			if (firstExtra)
				multiplier++;
			*/
			
			if (multiplier > 999)
				multiplier = 999;
			if (multiplier < 1)
				multiplier = 1;
			
			lastscore = pts;
			lastmultiplier = multiplier;
			scgettime = 120;
			int score = pts*multiplier;
			engine.statistics.scoreFromLineClear += score;
			engine.statistics.score += score;

			garbageAdd += (score+119)/120;
			
			setSpeed(engine);
		}
	}

	public boolean lineClearEnd(GameEngine engine, int playerID) {
		if (garbageAdd > 0)
		{
			garbageSent += garbageAdd;
			garbageAdd = 0;
		}
		if (engine.field != null)
		{
			if (!engine.field.getBlockEmpty(2, 0) || (dangerColumnDouble && !engine.field.getBlockEmpty(3, 0)))
			{
				engine.stat = GameEngine.STAT_GAMEOVER;
				engine.gameActive = false;
				engine.resetStatc();
				engine.statc[1] = 1;
			}
		}
		return false;
	}

	/*
	 * Render results screen
	 */
	@Override
	public void renderResult(GameEngine engine, int playerID) {
		receiver.drawMenuFont(engine, playerID,  0, 1, "PLAY DATA", EventReceiver.COLOR_ORANGE);

		receiver.drawMenuFont(engine, playerID,  0, 3, "SCORE", EventReceiver.COLOR_BLUE);
		String strScoreBefore = String.format("%10d", scoreBeforeBonus);
		receiver.drawMenuFont(engine, playerID,  0, 4, strScoreBefore, EventReceiver.COLOR_GREEN);

		receiver.drawMenuFont(engine, playerID,  0, 5, "ZENKESHI", EventReceiver.COLOR_BLUE);
		String strZenKeshi = String.format("%10d", zenKeshiCount);
		receiver.drawMenuFont(engine, playerID,  0, 6, strZenKeshi);
		String strZenKeshiBonus = "+" + zenKeshiBonus;
		receiver.drawMenuFont(engine, playerID, 10-strZenKeshiBonus.length(), 7, strZenKeshiBonus, EventReceiver.COLOR_GREEN);
		
		receiver.drawMenuFont(engine, playerID,  0, 8, "MAX CHAIN", EventReceiver.COLOR_BLUE);
		String strMaxChain = String.format("%10d", maxChain);
		receiver.drawMenuFont(engine, playerID,  0, 9, strMaxChain);
		String strMaxChainBonus = "+" + maxChainBonus;
		receiver.drawMenuFont(engine, playerID, 10-strMaxChainBonus.length(), 10, strMaxChainBonus, EventReceiver.COLOR_GREEN);

		receiver.drawMenuFont(engine, playerID,  0, 11, "TOTAL", EventReceiver.COLOR_BLUE);
		String strScore = String.format("%10d", engine.statistics.score);
		receiver.drawMenuFont(engine, playerID,  0, 12, strScore, EventReceiver.COLOR_RED);
		
		receiver.drawMenuFont(engine, playerID,  0, 13, "TIME", EventReceiver.COLOR_BLUE);
		String strTime = String.format("%10s", GeneralUtil.getTime(engine.statistics.time));
		receiver.drawMenuFont(engine, playerID,  0, 14, strTime);

		if(rankingRank != -1) {
			receiver.drawMenuFont(engine, playerID,  0, 15, "RANK", EventReceiver.COLOR_BLUE);
			String strRank = String.format("%10d", rankingRank + 1);
			receiver.drawMenuFont(engine, playerID,  0, 16, strRank);
		}
	}

	/*
	 * Called when saving replay
	 */
	@Override
	public void saveReplay(GameEngine engine, int playerID, CustomProperties prop) {
		saveSetting(prop);

		// Update rankings
		if((owner.replayMode == false) && (engine.ai == null)) {
			updateRanking(engine.statistics.score, engine.statistics.time, gametype);

			if(rankingRank != -1) {
				saveRanking(owner.modeConfig, engine.ruleopt.strRuleName);
				receiver.saveModeConfig(owner.modeConfig);
			}
		}
	}

	/**
	 * Load settings from property file
	 * @param prop Property file
	 */
	private void loadSetting(CustomProperties prop) {
		gametype = prop.getProperty("avalanche.gametype", 0);
		outlinetype = prop.getProperty("avalanche.outlinetype", 0);
		numColors = prop.getProperty("avalanche.numcolors", 5);
		version = prop.getProperty("avalanche.version", 0);
		dangerColumnDouble = prop.getProperty("avalanche.dangerColumnDouble", false);
		dangerColumnShowX = prop.getProperty("avalanche.dangerColumnShowX", false);
		showChains = prop.getProperty("avalanche.showChains", true);
	}

	/**
	 * Save settings to property file
	 * @param prop Property file
	 */
	private void saveSetting(CustomProperties prop) {
		prop.setProperty("avalanche.gametype", gametype);
		prop.setProperty("avalanche.outlinetype", outlinetype);
		prop.setProperty("avalanche.numcolors", numColors);
		prop.setProperty("avalanche.version", version);
		prop.setProperty("avalanche.dangerColumnDouble", dangerColumnDouble);
		prop.setProperty("avalanche.dangerColumnShowX", dangerColumnShowX);
		prop.setProperty("avalanche.showChains", showChains);
	}

	/**
	 * Read rankings from property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	private void loadRanking(CustomProperties prop, String ruleName) {
		for(int i = 0; i < RANKING_MAX; i++) {
			for(int j = 0; j < GAMETYPE_MAX; j++) {
				rankingScore[j][i] = prop.getProperty("avalanche.ranking." + ruleName + "." + numColors +
						"colors." + j + ".score." + i, 0);
				rankingTime[j][i] = prop.getProperty("avalanche.ranking." + ruleName + "." + numColors +
						"colors." + j + ".time." + i, -1);
			}
		}
	}

	/**
	 * Save rankings to property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	private void saveRanking(CustomProperties prop, String ruleName) {
		for(int i = 0; i < RANKING_MAX; i++) {
			for(int j = 0; j < GAMETYPE_MAX; j++) {
				prop.setProperty("avalanche.ranking." + ruleName + "." + numColors + "colors." +
						j + ".score." + i, rankingScore[j][i]);
				prop.setProperty("avalanche.ranking." + ruleName + "." + numColors + "colors." +
						j + ".time." + i, rankingTime[j][i]);
			}
		}
	}

	/**
	 * Update rankings
	 * @param sc Score
	 * @param li Lines
	 * @param time Time
	 */
	private void updateRanking(int sc, int time, int type) {
		rankingRank = checkRanking(sc, time, type);

		if(rankingRank != -1) {
			// Shift down ranking entries
			for(int i = RANKING_MAX - 1; i > rankingRank; i--) {
				rankingScore[type][i] = rankingScore[type][i - 1];
				rankingTime[type][i] = rankingTime[type][i - 1];
			}

			// Add new data
			rankingScore[type][rankingRank] = sc;
			rankingTime[type][rankingRank] = time;
		}
	}

	/**
	 * Calculate ranking position
	 * @param sc Score
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private int checkRanking(int sc, int time, int type) {
		if (gametype == 2 && sc < SPRINT_MAX_SCORE)
			return -1;
		for(int i = 0; i < RANKING_MAX; i++) {
			if (gametype == 0) {
				if(sc > rankingScore[type][i]) {
					return i;
				} else if((sc == rankingScore[type][i]) && (time < rankingTime[type][i])) {
					return i;
				}
			} else if (gametype == 1) {
				if(sc > rankingScore[type][i]) {
					return i;
				}
			} else if (gametype == 2) {
				if(time < rankingTime[type][i] || (rankingTime[type][i] < 0)) {
					return i;
				}
			}
		}

		return -1;
	}
}
