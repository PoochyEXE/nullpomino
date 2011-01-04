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

import mu.nu.nullpo.game.component.BGMStatus;
import mu.nu.nullpo.game.component.Controller;
import mu.nu.nullpo.game.component.Piece;
import mu.nu.nullpo.game.event.EventReceiver;
import mu.nu.nullpo.game.net.NetUtil;
import mu.nu.nullpo.game.play.GameEngine;
import mu.nu.nullpo.util.CustomProperties;
import mu.nu.nullpo.util.GeneralUtil;

/**
 * TECHNICIAN Mode
 */
public class TechnicianMode extends NetDummyMode {
	/** Current version */
	private static final int CURRENT_VERSION = 2;

	/** Fall velocity table (numerators) */
	private static final int tableGravity[]     = { 1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1, 465, 731, 1280, 1707,  -1,  -1,  -1};

	/** Fall velocity table (denominators) */
	private static final int tableDenominator[] = {63, 50, 39, 30, 22, 16, 12,  8,  6,  4,  3,  2,  1, 256, 256,  256,  256, 256, 256, 256};

	/** BGM change levels */
	private static final int tableBGMChange[]   = {9, 15, 19, 23, 27, -1};

	/** Combo goal table */
	private static final int COMBO_GOAL_TABLE[] = {0,0,1,1,2,2,3,3,4,4,4,5};

	/** Number of entries in rankings */
	private static final int RANKING_MAX = 10;

	/** Number of ranking types */
	private static final int RANKING_TYPE = 5;

	/** Most recent scoring event type constants */
	private static final int EVENT_NONE = 0,
							 EVENT_SINGLE = 1,
							 EVENT_DOUBLE = 2,
							 EVENT_TRIPLE = 3,
							 EVENT_FOUR = 4,
							 EVENT_TSPIN_ZERO_MINI = 5,
							 EVENT_TSPIN_ZERO = 6,
							 EVENT_TSPIN_SINGLE_MINI = 7,
							 EVENT_TSPIN_SINGLE = 8,
							 EVENT_TSPIN_DOUBLE_MINI = 9,
							 EVENT_TSPIN_DOUBLE = 10,
							 EVENT_TSPIN_TRIPLE = 11,
							 EVENT_TSPIN_EZ = 12;

	/** Game type constants */
	private static final int GAMETYPE_LV15_EASY = 0,
							 GAMETYPE_LV15_HARD = 1,
							 GAMETYPE_10MIN_EASY = 2,
							 GAMETYPE_10MIN_HARD = 3,
							 GAMETYPE_SPECIAL = 4;

	/** Game type names */
	private static final String[] GAMETYPE_NAME = {"LV15-EASY", "LV15-HARD", "10MIN-EASY", "10MIN-HARD", "SPECIAL"};

	/** Game type max */
	private static final int GAMETYPE_MAX = 5;

	/** Time limit for each level */
	private static final int TIMELIMIT_LEVEL = 3600*2;

	/** Time limit of 10min games */
	private static final int TIMELIMIT_10MIN = 3600*10;

	/** Default time limit of Special game */
	private static final int TIMELIMIT_SPECIAL = 3600*2;

	/** Extra time of Special game */
	private static final int TIMELIMIT_SPECIAL_BONUS = 60*30;

	/** Ending time */
	private static final int TIMELIMIT_ROLL = 3600;

	/** Drawing and event handling EventReceiver */
	private EventReceiver receiver;

	/** Number of Goal-points remaining */
	private int goal;

	/** Level timer */
	private int levelTimer;

	/** true if level timer runs out */
	private boolean levelTimeOut;

	/** Master time limit */
	private int totalTimer;

	/** Ending time */
	private int rolltime;

	/** Most recent increase in goal-points */
	private int lastgoal;

	/** Most recent increase in score */
	private int lastscore;

	/** Most recent increase in time limit */
	private int lasttimebonus;

	/** Time to display the most recent increase in score */
	private int scgettime;

	/** REGRET display time frame count */
	private int regretdispframe;

	/** Most recent scoring event type */
	private int lastevent;

	/** Most recent scoring event b2b */
	private boolean lastb2b;

	/** Most recent scoring event combo count */
	private int lastcombo;

	/** Most recent scoring event piece ID */
	private int lastpiece;

	/** Current BGM */
	private int bgmlv;

	/** Game type */
	private int goaltype;

	/** Level at start time */
	private int startlevel;

	/** Flag for types of T-Spins allowed (0=none, 1=normal, 2=all spin) */
	private int tspinEnableType;

	/** Old flag for allowing T-Spins */
	private boolean enableTSpin;

	/** Flag for enabling wallkick T-Spins */
	private boolean enableTSpinKick;

	/** Spin check type (4Point or Immobile) */
	private int spinCheckType;

	/** Immobile EZ spin */
	private boolean tspinEnableEZ;

	/** Flag for enabling B2B */
	private boolean enableB2B;

	/** Flag for enabling combos */
	private boolean enableCombo;

	/** Big */
	private boolean big;

	/** Version */
	private int version;

	/** Current round's ranking rank */
	private int rankingRank;

	/** Rankings' scores */
	private int[][] rankingScore;

	/** Rankings' line counts */
	private int[][] rankingLines;

	/** Rankings' times */
	private int[][] rankingTime;

	/*
	 * Mode name
	 */
	@Override
	public String getName() {
		return "TECHNICIAN";
	}

	/*
	 * Initialization
	 */
	@Override
	public void playerInit(GameEngine engine, int playerID) {
		owner = engine.owner;
		receiver = engine.owner.receiver;
		goal = 0;
		levelTimer = 0;
		levelTimeOut = false;
		totalTimer = 0;
		rolltime = 0;
		lastgoal = 0;
		lastscore = 0;
		lasttimebonus = 0;
		scgettime = 0;
		regretdispframe = 0;
		lastevent = EVENT_NONE;
		lastb2b = false;
		lastcombo = 0;
		lastpiece = 0;
		bgmlv = 0;

		rankingRank = -1;
		rankingScore = new int[RANKING_TYPE][RANKING_MAX];
		rankingLines = new int[RANKING_TYPE][RANKING_MAX];
		rankingTime = new int[RANKING_TYPE][RANKING_MAX];

		netPlayerInit(engine, playerID);

		if(owner.replayMode == false) {
			loadSetting(owner.modeConfig);
			loadRanking(owner.modeConfig, engine.ruleopt.strRuleName);
			version = CURRENT_VERSION;
		} else {
			loadSetting(owner.replayProp);
			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty(playerID + ".net.netPlayerName", "");
		}

		engine.owner.backgroundStatus.bg = startlevel;
		if(engine.owner.backgroundStatus.bg > 19) engine.owner.backgroundStatus.bg = 19;
		engine.framecolor = GameEngine.FRAME_COLOR_GRAY;
	}

	/**
	 * Set the gravity rate
	 * @param engine GameEngine
	 */
	private void setSpeed(GameEngine engine) {
		int lv = engine.statistics.level;

		if(lv < 0) lv = 0;
		if(lv >= tableGravity.length) lv = tableGravity.length - 1;

		engine.speed.gravity = tableGravity[lv];
		engine.speed.denominator = tableDenominator[lv];
	}

	/**
	 * Set BGM at start of game
	 * @param engine GameEngine
	 */
	private void setStartBgmlv(GameEngine engine) {
		bgmlv = 0;
		while((tableBGMChange[bgmlv] != -1) && (engine.statistics.level >= tableBGMChange[bgmlv])) bgmlv++;
	}

	/*
	 * Called at settings screen
	 */
	@Override
	public boolean onSetting(GameEngine engine, int playerID) {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode) {
			netOnUpdateNetPlayRanking(engine, goaltype);
		}
		// Menu
		else if(engine.owner.replayMode == false) {
			// Configuration changes
			int change = updateCursor(engine, 8);

			if(change != 0) {
				engine.playSE("change");

				switch(engine.statc[2]) {
				case 0:
					goaltype += change;
					if(goaltype < 0) goaltype = GAMETYPE_MAX - 1;
					if(goaltype > GAMETYPE_MAX - 1) goaltype = 0;
					break;
				case 1:
					startlevel += change;
					if(startlevel < 0) startlevel = 29;
					if(startlevel > 29) startlevel = 0;
					engine.owner.backgroundStatus.bg = startlevel;
					if(engine.owner.backgroundStatus.bg > 19) engine.owner.backgroundStatus.bg = 19;
					break;
				case 2:
					//enableTSpin = !enableTSpin;
					tspinEnableType += change;
					if(tspinEnableType < 0) tspinEnableType = 2;
					if(tspinEnableType > 2) tspinEnableType = 0;
					break;
				case 3:
					enableTSpinKick = !enableTSpinKick;
					break;
				case 4:
					spinCheckType += change;
					if(spinCheckType < 0) spinCheckType = 1;
					if(spinCheckType > 1) spinCheckType = 0;
					break;
				case 5:
					tspinEnableEZ = !tspinEnableEZ;
					break;
				case 6:
					enableB2B = !enableB2B;
					break;
				case 7:
					enableCombo = !enableCombo;
					break;
				case 8:
					big = !big;
					break;
				}

				// NET: Signal options change
				if(netIsNetPlay && (netNumSpectators > 0)) netSendOptions(engine);
			}

			// Confirm
			if(engine.ctrl.isPush(Controller.BUTTON_A) && (engine.statc[3] >= 5)) {
				engine.playSE("decide");
				saveSetting(owner.modeConfig);
				receiver.saveModeConfig(owner.modeConfig);

				// NET: Signal start of the game
				if(netIsNetPlay) netLobby.netPlayerClient.send("start1p\n");

				return false;
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B) && !netIsNetPlay) {
				engine.quitflag = true;
			}

			// NET: Netplay Ranking
			if(engine.ctrl.isPush(Controller.BUTTON_D) && netIsNetPlay && netIsNetRankingViewOK(engine)) {
				netEnterNetPlayRankingScreen(engine, playerID, goaltype);
			}

			engine.statc[3]++;
		}
		// Replay
		else {
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
		if(netIsNetRankingDisplayMode) {
			// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, playerID, receiver);
		} else {
			String strTSpinEnable = "";
			if(version >= 1) {
				if(tspinEnableType == 0) strTSpinEnable = "OFF";
				if(tspinEnableType == 1) strTSpinEnable = "T-ONLY";
				if(tspinEnableType == 2) strTSpinEnable = "ALL";
			} else {
				strTSpinEnable = GeneralUtil.getONorOFF(enableTSpin);
			}
			drawMenu(engine, playerID, receiver, 0, EventReceiver.COLOR_BLUE, 0,
					"GAME TYPE", GAMETYPE_NAME[goaltype],
					"LEVEL", String.valueOf(startlevel + 1),
					"SPIN BONUS", strTSpinEnable,
					"EZ SPIN", GeneralUtil.getONorOFF(enableTSpinKick),
					"SPIN TYPE", (spinCheckType == 0) ? "4POINT" : "IMMOBILE",
					"EZIMMOBILE", GeneralUtil.getONorOFF(tspinEnableEZ),
					"B2B", GeneralUtil.getONorOFF(enableB2B),
					"COMBO",  GeneralUtil.getONorOFF(enableCombo),
					"BIG", GeneralUtil.getONorOFF(big));
		}
	}

	/*
	 * Called for initialization during "Ready" screen
	 */
	@Override
	public void startGame(GameEngine engine, int playerID) {
		engine.statistics.level = startlevel;
		engine.statistics.levelDispAdd = 1;
		engine.b2bEnable = enableB2B;
		if(enableCombo == true) {
			engine.comboType = GameEngine.COMBO_TYPE_NORMAL;
		} else {
			engine.comboType = GameEngine.COMBO_TYPE_DISABLE;
		}
		engine.big = big;

		if(version >= 2) {
			engine.tspinAllowKick = enableTSpinKick;
			if(tspinEnableType == 0) {
				engine.tspinEnable = false;
			} else if(tspinEnableType == 1) {
				engine.tspinEnable = true;
			} else {
				engine.tspinEnable = true;
				engine.useAllSpinBonus = true;
			}
		} else {
			engine.tspinEnable = enableTSpin;
		}

		engine.spinCheckType = spinCheckType;
		engine.tspinEnableEZ = tspinEnableEZ;

		engine.speed.lineDelay = 8;

		goal = (engine.statistics.level + 1) * 5;

		setSpeed(engine);

		if(netIsWatch) {
			owner.bgmStatus.bgm = BGMStatus.BGM_NOTHING;
		} else {
			setStartBgmlv(engine);
			owner.bgmStatus.bgm = bgmlv;
		}

		if((goaltype == GAMETYPE_10MIN_EASY) || (goaltype == GAMETYPE_10MIN_HARD))
			totalTimer = TIMELIMIT_10MIN;
		if(goaltype == GAMETYPE_SPECIAL)
			totalTimer = TIMELIMIT_SPECIAL;

		if(goaltype == GAMETYPE_SPECIAL) {
			engine.staffrollEnable = true;
			engine.staffrollEnableStatistics = true;
			engine.staffrollNoDeath = true;
		}
	}

	/*
	 * Render score
	 */
	@Override
	public void renderLast(GameEngine engine, int playerID) {
		if(owner.menuOnly) return;

		receiver.drawScoreFont(engine, playerID, 0, 0, "TECHNICIAN\n(" + GAMETYPE_NAME[goaltype] + ")", EventReceiver.COLOR_WHITE);

		if( (engine.stat == GameEngine.STAT_SETTING) || ((engine.stat == GameEngine.STAT_RESULT) && (owner.replayMode == false)) ) {
			if((owner.replayMode == false) && (big == false) && (startlevel == 0) && (engine.ai == null)) {
				float scale = (receiver.getNextDisplayType() == 2) ? 0.5f : 1.0f;
				int topY = (receiver.getNextDisplayType() == 2) ? 6 : 4;
				receiver.drawScoreFont(engine, playerID, 3, topY-1, "SCORE   LINE TIME", EventReceiver.COLOR_BLUE, scale);

				for(int i = 0; i < RANKING_MAX; i++) {
					receiver.drawScoreFont(engine, playerID, 0, topY+i, String.format("%2d", i + 1), EventReceiver.COLOR_YELLOW, scale);
					receiver.drawScoreFont(engine, playerID, 3, topY+i, String.valueOf(rankingScore[goaltype][i]), (i == rankingRank), scale);
					receiver.drawScoreFont(engine, playerID, 11, topY+i, String.valueOf(rankingLines[goaltype][i]), (i == rankingRank), scale);
					receiver.drawScoreFont(engine, playerID, 16, topY+i, GeneralUtil.getTime(rankingTime[goaltype][i]), (i == rankingRank), scale);
				}
			}
		} else {
			// SCORE
			receiver.drawScoreFont(engine, playerID, 0, 3, "SCORE", EventReceiver.COLOR_BLUE);
			String strScore = String.valueOf(engine.statistics.score);
			if((lasttimebonus > 0) && (scgettime < 120) && (goaltype != GAMETYPE_SPECIAL))
				strScore += "(+" + lastscore + "+" + lasttimebonus + ")";
			else if((lastscore > 0) && (scgettime < 120))
				strScore += "(+" + lastscore + ")";
			receiver.drawScoreFont(engine, playerID, 0, 4, strScore);

			// GOAL
			receiver.drawScoreFont(engine, playerID, 0, 6, "GOAL", EventReceiver.COLOR_BLUE);
			String strGoal = String.valueOf(goal);
			if((lastgoal != 0) && (scgettime < 120) && (engine.ending == 0))
				strGoal += "(-" + String.valueOf(lastgoal) + ")";
			receiver.drawScoreFont(engine, playerID, 0, 7, strGoal);

			// LEVEL
			receiver.drawScoreFont(engine, playerID, 0, 9, "LEVEL", EventReceiver.COLOR_BLUE);
			receiver.drawScoreFont(engine, playerID, 0, 10, String.valueOf(engine.statistics.level + 1));

			// LEVEL TIME
			if(goaltype != GAMETYPE_SPECIAL) {
				receiver.drawScoreFont(engine, playerID, 0, 12, "LEVEL TIME", EventReceiver.COLOR_BLUE);
				int remainLevelTime = TIMELIMIT_LEVEL - levelTimer;
				if(remainLevelTime < 0) remainLevelTime = 0;
				int fontcolorLevelTime = EventReceiver.COLOR_WHITE;
				if((remainLevelTime < 60 * 60) && (remainLevelTime > 0)) fontcolorLevelTime = EventReceiver.COLOR_YELLOW;
				if((remainLevelTime < 30 * 60) && (remainLevelTime > 0)) fontcolorLevelTime = EventReceiver.COLOR_ORANGE;
				if((remainLevelTime < 10 * 60) && (remainLevelTime > 0)) fontcolorLevelTime = EventReceiver.COLOR_RED;
				receiver.drawScoreFont(engine, playerID, 0, 13, GeneralUtil.getTime(TIMELIMIT_LEVEL - levelTimer), fontcolorLevelTime);
			}

			// TOTAL TIME
			receiver.drawScoreFont(engine, playerID, 0, 15, "TOTAL TIME", EventReceiver.COLOR_BLUE);
			int totaltime = engine.statistics.time;
			if((goaltype == GAMETYPE_10MIN_EASY) || (goaltype == GAMETYPE_10MIN_HARD)) totaltime = TIMELIMIT_10MIN - engine.statistics.time;
			if(goaltype == GAMETYPE_SPECIAL) totaltime = totalTimer;
			int fontcolorTotalTime = EventReceiver.COLOR_WHITE;
			if((goaltype != GAMETYPE_LV15_EASY) && (goaltype != GAMETYPE_LV15_HARD)) {
				if((totaltime < 60 * 60) && (totaltime > 0)) fontcolorTotalTime = EventReceiver.COLOR_YELLOW;
				if((totaltime < 30 * 60) && (totaltime > 0)) fontcolorTotalTime = EventReceiver.COLOR_ORANGE;
				if((totaltime < 10 * 60) && (totaltime > 0)) fontcolorTotalTime = EventReceiver.COLOR_RED;
			}
			receiver.drawScoreFont(engine, playerID, 0, 16, GeneralUtil.getTime(totaltime), fontcolorTotalTime);

			// +30sec
			if((goaltype == GAMETYPE_SPECIAL) && (lasttimebonus > 0) && (scgettime < 120) && (engine.ending == 0)) {
				receiver.drawScoreFont(engine, playerID, 0, 17, "+" + (lasttimebonus / 60) + "SEC.", EventReceiver.COLOR_YELLOW);
			}

			// Ending time
			if( (engine.gameActive) && ((engine.ending == 2) || (rolltime > 0)) ) {
				int remainRollTime = TIMELIMIT_ROLL - rolltime;
				if(remainRollTime < 0) remainRollTime = 0;

				receiver.drawScoreFont(engine, playerID, 0, 12, "ROLL TIME", EventReceiver.COLOR_BLUE);
				receiver.drawScoreFont(engine, playerID, 0, 13, GeneralUtil.getTime(remainRollTime), ((remainRollTime > 0)&&(remainRollTime < 10*60)));
			}

			if(regretdispframe > 0) {
				// REGRET
				receiver.drawMenuFont(engine,playerID,2,21,"REGRET",(regretdispframe % 4 == 0),EventReceiver.COLOR_WHITE,EventReceiver.COLOR_ORANGE);
			}
			else if((lastevent != EVENT_NONE) && (scgettime < 120)) {
				// Most recent event
				String strPieceName = Piece.getPieceName(lastpiece);

				switch(lastevent) {
				case EVENT_SINGLE:
					receiver.drawMenuFont(engine, playerID, 2, 21, "SINGLE", EventReceiver.COLOR_DARKBLUE);
					break;
				case EVENT_DOUBLE:
					receiver.drawMenuFont(engine, playerID, 2, 21, "DOUBLE", EventReceiver.COLOR_BLUE);
					break;
				case EVENT_TRIPLE:
					receiver.drawMenuFont(engine, playerID, 2, 21, "TRIPLE", EventReceiver.COLOR_GREEN);
					break;
				case EVENT_FOUR:
					if(lastb2b) receiver.drawMenuFont(engine, playerID, 3, 21, "FOUR", EventReceiver.COLOR_RED);
					else receiver.drawMenuFont(engine, playerID, 3, 21, "FOUR", EventReceiver.COLOR_ORANGE);
					break;
				case EVENT_TSPIN_ZERO_MINI:
					receiver.drawMenuFont(engine, playerID, 2, 21, strPieceName + "-SPIN", EventReceiver.COLOR_PURPLE);
					break;
				case EVENT_TSPIN_ZERO:
					receiver.drawMenuFont(engine, playerID, 2, 21, strPieceName + "-SPIN", EventReceiver.COLOR_PINK);
					break;
				case EVENT_TSPIN_SINGLE_MINI:
					if(lastb2b) receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-MINI-S", EventReceiver.COLOR_RED);
					else receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-MINI-S", EventReceiver.COLOR_ORANGE);
					break;
				case EVENT_TSPIN_SINGLE:
					if(lastb2b) receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-SINGLE", EventReceiver.COLOR_RED);
					else receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-SINGLE", EventReceiver.COLOR_ORANGE);
					break;
				case EVENT_TSPIN_DOUBLE_MINI:
					if(lastb2b) receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-MINI-D", EventReceiver.COLOR_RED);
					else receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-MINI-D", EventReceiver.COLOR_ORANGE);
					break;
				case EVENT_TSPIN_DOUBLE:
					if(lastb2b) receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-DOUBLE", EventReceiver.COLOR_RED);
					else receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-DOUBLE", EventReceiver.COLOR_ORANGE);
					break;
				case EVENT_TSPIN_TRIPLE:
					if(lastb2b) receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-TRIPLE", EventReceiver.COLOR_RED);
					else receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-TRIPLE", EventReceiver.COLOR_ORANGE);
					break;
				case EVENT_TSPIN_EZ:
					if(lastb2b) receiver.drawMenuFont(engine, playerID, 3, 21, "EZ-" + strPieceName, EventReceiver.COLOR_RED);
					else receiver.drawMenuFont(engine, playerID, 3, 21, "EZ-" + strPieceName, EventReceiver.COLOR_ORANGE);
					break;
				}

				if((lastcombo >= 2) && (lastevent != EVENT_TSPIN_ZERO_MINI) && (lastevent != EVENT_TSPIN_ZERO))
					receiver.drawMenuFont(engine, playerID, 2, 22, (lastcombo - 1) + "COMBO", EventReceiver.COLOR_CYAN);
			}
		}

		// NET: Number of spectators
		netDrawSpectatorsCount(engine, 0, 18);
		// NET: All number of players
		if(playerID == getPlayers() - 1) {
			netDrawAllPlayersCount(engine);
			netDrawGameRate(engine);
		}
		// NET: Player name (It may also appear in offline replay)
		netDrawPlayerName(engine);
	}

	/*
	 * Called after every frame
	 */
	@Override
	public void onLast(GameEngine engine, int playerID) {
		scgettime++;
		if(regretdispframe > 0) regretdispframe--;

		// Level Time
		if(engine.gameActive && engine.timerActive && (goaltype != GAMETYPE_SPECIAL)) {
			levelTimer++;
			int remainTime = TIMELIMIT_LEVEL - levelTimer;

			// Time meter
			engine.meterValue = (remainTime * receiver.getMeterMax(engine)) / TIMELIMIT_LEVEL;
			engine.meterColor = GameEngine.METER_COLOR_GREEN;
			if(remainTime <= 60*60) engine.meterColor = GameEngine.METER_COLOR_YELLOW;
			if(remainTime <= 30*60) engine.meterColor = GameEngine.METER_COLOR_ORANGE;
			if(remainTime <= 10*60) engine.meterColor = GameEngine.METER_COLOR_RED;

			if(!netIsWatch) {
				if(levelTimer >= TIMELIMIT_LEVEL) {
					// Out of time
					levelTimeOut = true;

					if((goaltype == GAMETYPE_LV15_HARD) || (goaltype == GAMETYPE_10MIN_HARD)) {
						engine.gameEnded();
						engine.resetStatc();
						engine.stat = GameEngine.STAT_GAMEOVER;
					} else if(goaltype == GAMETYPE_10MIN_EASY) {
						regretdispframe = 180;
						engine.playSE("regret");
						goal = (engine.statistics.level + 1) * 5;
						levelTimer = 0;
					}
				} else if((remainTime <= 10 * 60) && (remainTime % 60 == 0)) {
					// Countdown
					engine.playSE("countdown");
				}
			}
		}

		// Total Time
		if(engine.gameActive && engine.timerActive && (goaltype != GAMETYPE_LV15_EASY) && (goaltype != GAMETYPE_LV15_HARD)) {
			totalTimer--;

			// Time meter
			if(goaltype == GAMETYPE_SPECIAL) {
				engine.meterValue = (totalTimer * receiver.getMeterMax(engine)) / (5 * 3600);
				engine.meterColor = GameEngine.METER_COLOR_GREEN;
				if(totalTimer <= 60*60) engine.meterColor = GameEngine.METER_COLOR_YELLOW;
				if(totalTimer <= 30*60) engine.meterColor = GameEngine.METER_COLOR_ORANGE;
				if(totalTimer <= 10*60) engine.meterColor = GameEngine.METER_COLOR_RED;
			}

			if(!netIsWatch) {
				if(totalTimer < 0) {
					// Out of time
					engine.gameEnded();
					engine.resetStatc();

					if((goaltype == GAMETYPE_10MIN_EASY) || (goaltype == GAMETYPE_10MIN_HARD)) {
						engine.stat = GameEngine.STAT_ENDINGSTART;
					} else {
						engine.stat = GameEngine.STAT_GAMEOVER;
					}

					totalTimer = 0;
				} else if((totalTimer <= 10 * 60) && (totalTimer % 60 == 0)) {
					// Countdown
					engine.playSE("countdown");
				}
			}
		}

		// Ending
		if((engine.gameActive) && (engine.ending == 2)) {
			rolltime++;

			// Time meter
			int remainRollTime = TIMELIMIT_ROLL - rolltime;
			engine.meterValue = (remainRollTime * receiver.getMeterMax(engine)) / TIMELIMIT_ROLL;
			engine.meterColor = GameEngine.METER_COLOR_GREEN;
			if(remainRollTime <= 30*60) engine.meterColor = GameEngine.METER_COLOR_YELLOW;
			if(remainRollTime <= 20*60) engine.meterColor = GameEngine.METER_COLOR_ORANGE;
			if(remainRollTime <= 10*60) engine.meterColor = GameEngine.METER_COLOR_RED;

			// Finished
			if((rolltime >= TIMELIMIT_ROLL) && (!netIsWatch)) {
				scgettime = 0;
				lastscore = totalTimer * 2;
				engine.statistics.score += lastscore;
				engine.statistics.scoreFromOtherBonus += lastscore;
				lastevent = EVENT_NONE;

				engine.gameEnded();
				engine.resetStatc();
				engine.stat = GameEngine.STAT_EXCELLENT;
			}
		}
	}

	/*
	 * Calculate score
	 */
	@Override
	public void calcScore(GameEngine engine, int playerID, int lines) {
		// Line clear bonus
		int pts = 0;
		int cmb = 0;

		if(engine.tspin) {
			// T-Spin 0 lines
			if((lines == 0) && (!engine.tspinez)) {
				if(engine.tspinmini) {
					pts += 100 * (engine.statistics.level + 1);
					lastevent = EVENT_TSPIN_ZERO_MINI;
				} else {
					pts += 400 * (engine.statistics.level + 1);
					lastevent = EVENT_TSPIN_ZERO;
				}
			}
			// Immobile EZ Spin
			else if(engine.tspinez && (lines > 0)) {
				if(engine.b2b) {
					pts += 180 * (engine.statistics.level + 1);
				} else {
					pts += 120 * (engine.statistics.level + 1);
				}
				lastevent = EVENT_TSPIN_EZ;
			}
			// T-Spin 1 line
			else if(lines == 1) {
				if(engine.tspinmini) {
					if(engine.b2b) {
						pts += 300 * (engine.statistics.level + 1);
					} else {
						pts += 200 * (engine.statistics.level + 1);
					}
					lastevent = EVENT_TSPIN_SINGLE_MINI;
				} else {
					if(engine.b2b) {
						pts += 1200 * (engine.statistics.level + 1);
					} else {
						pts += 800 * (engine.statistics.level + 1);
					}
					lastevent = EVENT_TSPIN_SINGLE;
				}
			}
			// T-Spin 2 lines
			else if(lines == 2) {
				if(engine.tspinmini && engine.useAllSpinBonus) {
					if(engine.b2b) {
						pts += 600 * (engine.statistics.level + 1);
					} else {
						pts += 400 * (engine.statistics.level + 1);
					}
					lastevent = EVENT_TSPIN_DOUBLE_MINI;
				} else {
					if(engine.b2b) {
						pts += 1800 * (engine.statistics.level + 1);
					} else {
						pts += 1200 * (engine.statistics.level + 1);
					}
					lastevent = EVENT_TSPIN_DOUBLE;
				}
			}
			// T-Spin 3 lines
			else if(lines >= 3) {
				if(engine.b2b) {
					pts += 2400 * (engine.statistics.level + 1);
				} else {
					pts += 1600 * (engine.statistics.level + 1);
				}
				lastevent = EVENT_TSPIN_TRIPLE;
			}
		} else {
			if(lines == 1) {
				pts += 100 * (engine.statistics.level + 1); // 1列
				lastevent = EVENT_SINGLE;
			} else if(lines == 2) {
				pts += 300 * (engine.statistics.level + 1); // 2列
				lastevent = EVENT_DOUBLE;
			} else if(lines == 3) {
				pts += 500 * (engine.statistics.level + 1); // 3列
				lastevent = EVENT_TRIPLE;
			} else if(lines >= 4) {
				// 4 lines
				if(engine.b2b) {
					pts += 1200 * (engine.statistics.level + 1);
				} else {
					pts += 800 * (engine.statistics.level + 1);
				}
				lastevent = EVENT_FOUR;
			}
		}

		lastb2b = engine.b2b;

		// Combo
		if((enableCombo) && (engine.combo >= 1) && (lines >= 1)) {
			cmb += ((engine.combo - 1) * 50) * (engine.statistics.level + 1);
			lastcombo = engine.combo;
		}

		// All clear
		if((lines >= 1) && (engine.field.isEmpty())) {
			engine.playSE("bravo");
			pts += 1800 * (engine.statistics.level + 1);
		}

		// Add to score
		if((pts > 0) || (cmb > 0)) {
			lastpiece = engine.nowPieceObject.id;
			lastscore = pts + cmb;
			scgettime = 0;
			if(lines >= 1) engine.statistics.scoreFromLineClear += pts;
			else engine.statistics.scoreFromOtherBonus += pts;
			engine.statistics.score += pts;

			int cmbindex = engine.combo - 1;
			if(cmbindex < 0) cmbindex = 0;
			if(cmbindex >= COMBO_GOAL_TABLE.length) cmbindex = COMBO_GOAL_TABLE.length - 1;
			lastgoal = ((pts / 100) / (engine.statistics.level + 1)) + COMBO_GOAL_TABLE[cmbindex];
			goal -= lastgoal;
			if(goal <= 0) goal = 0;
		}

		if(engine.ending == 0) {
			// Time bonus
			if((goal <= 0) && (levelTimeOut == false) && (goaltype != GAMETYPE_SPECIAL)) {
				lasttimebonus = (TIMELIMIT_LEVEL - levelTimer) * (engine.statistics.level + 1);
				if(lasttimebonus < 0) lasttimebonus = 0;
				scgettime = 0;
				engine.statistics.scoreFromOtherBonus += lasttimebonus;
				engine.statistics.score += lasttimebonus;
			} else if((goal <= 0) && (goaltype == GAMETYPE_SPECIAL)) {
				lasttimebonus = TIMELIMIT_SPECIAL_BONUS;
				totalTimer += lasttimebonus;
			} else if(pts > 0) {
				lasttimebonus = 0;
			}

			// BGM fade-out effects and BGM changes
			if((tableBGMChange[bgmlv] != -1) && (engine.statistics.level == tableBGMChange[bgmlv] - 1)) {
				if((goal > 0) && (goal <= 10)) {
					owner.bgmStatus.fadesw = true;
				} else if(goal <= 0) {
					bgmlv++;
					owner.bgmStatus.bgm = bgmlv;
					owner.bgmStatus.fadesw = false;
				}
			}

			if(goal <= 0) {
				if((engine.statistics.level >= 14) && ((goaltype == GAMETYPE_LV15_EASY) || (goaltype == GAMETYPE_LV15_HARD))) {
					// Ending (LV15-EASY/HARD）
					engine.ending = 1;
					engine.gameEnded();
				} else if((engine.statistics.level >= 29) && (goaltype == GAMETYPE_SPECIAL)) {
					// Ending (SPECIAL）
					engine.ending = 2;
					engine.timerActive = false;
					owner.bgmStatus.bgm = BGMStatus.BGM_ENDING1;
					owner.bgmStatus.fadesw = false;
					engine.playSE("endingstart");
				} else {
					// Level up
					engine.statistics.level++;
					if(engine.statistics.level > 29) engine.statistics.level = 29;

					if(owner.backgroundStatus.bg < 19) {
						owner.backgroundStatus.fadesw = true;
						owner.backgroundStatus.fadecount = 0;
						owner.backgroundStatus.fadebg = engine.statistics.level;
					}

					goal = (engine.statistics.level + 1) * 5;

					levelTimer = 0;
					if(version >= 1) engine.holdUsedCount = 0;

					setSpeed(engine);
					engine.playSE("levelup");
				}
			}
		}
	}

	/*
	 * Soft drop
	 */
	@Override
	public void afterSoftDropFall(GameEngine engine, int playerID, int fall) {
		engine.statistics.scoreFromSoftDrop += fall;
		engine.statistics.score += fall;
	}

	/*
	 * Hard drop
	 */
	@Override
	public void afterHardDropFall(GameEngine engine, int playerID, int fall) {
		engine.statistics.scoreFromHardDrop += fall * 2;
		engine.statistics.score += fall * 2;
	}

	/*
	 * Render results screen
	 */
	@Override
	public void renderResult(GameEngine engine, int playerID) {
		drawResultStats(engine, playerID, receiver, 0, EventReceiver.COLOR_BLUE,
				STAT_SCORE, STAT_LINES, STAT_LEVEL, STAT_TIME, STAT_SPL, STAT_LPM);
		drawResultRank(engine, playerID, receiver, 12, EventReceiver.COLOR_BLUE, rankingRank);
		drawResultNetRank(engine, playerID, receiver, 14, EventReceiver.COLOR_BLUE, netRankingRank[0]);
		drawResultNetRankDaily(engine, playerID, receiver, 16, EventReceiver.COLOR_BLUE, netRankingRank[1]);

		if(netIsPB) {
			receiver.drawMenuFont(engine, playerID, 2, 18, "NEW PB", EventReceiver.COLOR_ORANGE);
		}

		if(netIsNetPlay && (netReplaySendStatus == 1)) {
			receiver.drawMenuFont(engine, playerID, 0, 19, "SENDING...", EventReceiver.COLOR_PINK);
		} else if(netIsNetPlay && !netIsWatch && (netReplaySendStatus == 2)) {
			receiver.drawMenuFont(engine, playerID, 1, 19, "A: RETRY", EventReceiver.COLOR_RED);
		}
	}

	/*
	 * Called when saving replay
	 */
	@Override
	public void saveReplay(GameEngine engine, int playerID, CustomProperties prop) {
		saveSetting(prop);

		// NET: Save name
		if((netPlayerName != null) && (netPlayerName.length() > 0)) {
			prop.setProperty(playerID + ".net.netPlayerName", netPlayerName);
		}

		// Update rankings
		if((owner.replayMode == false) && (big == false) && (engine.ai == null) && (startlevel == 0)) {
			updateRanking(engine.statistics.score, engine.statistics.lines, engine.statistics.time, goaltype);

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
		goaltype = prop.getProperty("technician.gametype", 0);
		startlevel = prop.getProperty("technician.startlevel", 0);
		tspinEnableType = prop.getProperty("technician.tspinEnableType", 1);
		enableTSpin = prop.getProperty("technician.enableTSpin", true);
		enableTSpinKick = prop.getProperty("technician.enableTSpinKick", true);
		spinCheckType = prop.getProperty("technician.spinCheckType", 0);
		tspinEnableEZ = prop.getProperty("technician.tspinEnableEZ", false);
		enableB2B = prop.getProperty("technician.enableB2B", true);
		enableCombo = prop.getProperty("technician.enableCombo", true);
		big = prop.getProperty("technician.big", false);
		version = prop.getProperty("technician.version", 0);
	}

	/**
	 * Save settings to property file
	 * @param prop Property file
	 */
	private void saveSetting(CustomProperties prop) {
		prop.setProperty("technician.gametype", goaltype);
		prop.setProperty("technician.startlevel", startlevel);
		prop.setProperty("technician.tspinEnableType", tspinEnableType);
		prop.setProperty("technician.enableTSpin", enableTSpin);
		prop.setProperty("technician.enableTSpinKick", enableTSpinKick);
		prop.setProperty("technician.spinCheckType", spinCheckType);
		prop.setProperty("technician.tspinEnableEZ", tspinEnableEZ);
		prop.setProperty("technician.enableB2B", enableB2B);
		prop.setProperty("technician.enableCombo", enableCombo);
		prop.setProperty("technician.big", big);
		prop.setProperty("technician.version", version);
	}

	/**
	 * Read rankings from property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	@Override
	protected void loadRanking(CustomProperties prop, String ruleName) {
		for(int i = 0; i < RANKING_MAX; i++) {
			for(int gametypeIndex = 0; gametypeIndex < RANKING_TYPE; gametypeIndex++) {
				rankingScore[gametypeIndex][i] = prop.getProperty("technician.ranking." + ruleName + "." + gametypeIndex + ".score." + i, 0);
				rankingLines[gametypeIndex][i] = prop.getProperty("technician.ranking." + ruleName + "." + gametypeIndex + ".lines." + i, 0);
				rankingTime[gametypeIndex][i] = prop.getProperty("technician.ranking." + ruleName + "." + gametypeIndex + ".time." + i, 0);
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
			for(int gametypeIndex = 0; gametypeIndex < RANKING_TYPE; gametypeIndex++) {
				prop.setProperty("technician.ranking." + ruleName + "." + gametypeIndex + ".score." + i, rankingScore[gametypeIndex][i]);
				prop.setProperty("technician.ranking." + ruleName + "." + gametypeIndex + ".lines." + i, rankingLines[gametypeIndex][i]);
				prop.setProperty("technician.ranking." + ruleName + "." + gametypeIndex + ".time." + i, rankingTime[gametypeIndex][i]);
			}
		}
	}

	/**
	 * Update rankings
	 * @param sc Score
	 * @param li Lines
	 * @param time Time
	 * @param type Game type
	 */
	private void updateRanking(int sc, int li, int time, int type) {
		rankingRank = checkRanking(sc, li, time, type);

		if(rankingRank != -1) {
			// Shift down ranking entries
			for(int i = RANKING_MAX - 1; i > rankingRank; i--) {
				rankingScore[type][i] = rankingScore[type][i - 1];
				rankingLines[type][i] = rankingLines[type][i - 1];
				rankingTime[type][i] = rankingTime[type][i - 1];
			}

			// Add new data
			rankingScore[type][rankingRank] = sc;
			rankingLines[type][rankingRank] = li;
			rankingTime[type][rankingRank] = time;
		}
	}

	/**
	 * Calculate ranking position
	 * @param sc Score
	 * @param li Lines
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private int checkRanking(int sc, int li, int time, int type) {
		for(int i = 0; i < RANKING_MAX; i++) {
			if(sc > rankingScore[type][i]) {
				return i;
			} else if((sc == rankingScore[type][i]) && (li > rankingLines[type][i])) {
				return i;
			} else if((sc == rankingScore[type][i]) && (li == rankingLines[type][i]) && (time < rankingTime[type][i])) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * NET: Send various in-game stats (as well as goaltype)
	 * @param engine GameEngine
	 */
	@Override
	protected void netSendStats(GameEngine engine) {
		int bg = owner.backgroundStatus.fadesw ? owner.backgroundStatus.fadebg : owner.backgroundStatus.bg;
		String msg = "game\tstats\t";
		msg += engine.statistics.score + "\t" + engine.statistics.lines + "\t" + engine.statistics.totalPieceLocked + "\t";
		msg += engine.statistics.time + "\t" + engine.statistics.lpm + "\t" + engine.statistics.spl + "\t";
		msg += goaltype + "\t" + engine.gameActive + "\t" + engine.timerActive + "\t";
		msg += lastscore + "\t" + scgettime + "\t" + lastevent + "\t" + lastb2b + "\t" + lastcombo + "\t" + lastpiece + "\t";
		msg += lastgoal + "\t" + lasttimebonus + "\t" + regretdispframe + "\t";
		msg += bg + "\t" + engine.meterValue + "\t" + engine.meterColor + "\t";
		msg += engine.statistics.level + "\t" + levelTimer + "\t" + totalTimer + "\t" + rolltime + "\t" + goal + "\n";
		netLobby.netPlayerClient.send(msg);
	}

	/**
	 * NET: Receive various in-game stats (as well as goaltype)
	 */
	@Override
	protected void netRecvStats(GameEngine engine, String[] message) {
		engine.statistics.score = Integer.parseInt(message[4]);
		engine.statistics.lines = Integer.parseInt(message[5]);
		engine.statistics.totalPieceLocked = Integer.parseInt(message[6]);
		engine.statistics.time = Integer.parseInt(message[7]);
		engine.statistics.lpm = Float.parseFloat(message[8]);
		engine.statistics.spl = Double.parseDouble(message[9]);
		goaltype = Integer.parseInt(message[10]);
		engine.gameActive = Boolean.parseBoolean(message[11]);
		engine.timerActive = Boolean.parseBoolean(message[12]);
		lastscore = Integer.parseInt(message[13]);
		scgettime = Integer.parseInt(message[14]);
		lastevent = Integer.parseInt(message[15]);
		lastb2b = Boolean.parseBoolean(message[16]);
		lastcombo = Integer.parseInt(message[17]);
		lastpiece = Integer.parseInt(message[18]);
		lastgoal = Integer.parseInt(message[19]);
		lasttimebonus = Integer.parseInt(message[20]);
		regretdispframe = Integer.parseInt(message[21]);
		owner.backgroundStatus.bg = Integer.parseInt(message[22]);
		engine.meterValue = Integer.parseInt(message[23]);
		engine.meterColor = Integer.parseInt(message[24]);
		engine.statistics.level = Integer.parseInt(message[25]);
		levelTimer = Integer.parseInt(message[26]);
		totalTimer = Integer.parseInt(message[27]);
		rolltime = Integer.parseInt(message[28]);
		goal = Integer.parseInt(message[29]);
	}

	/**
	 * NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	@Override
	protected void netSendEndGameStats(GameEngine engine) {
		String subMsg = "";
		subMsg += "SCORE;" + engine.statistics.score + "\t";
		subMsg += "LINE;" + engine.statistics.lines + "\t";
		subMsg += "LEVEL;" + (engine.statistics.level + engine.statistics.levelDispAdd) + "\t";
		subMsg += "TIME;" + GeneralUtil.getTime(engine.statistics.time) + "\t";
		subMsg += "SCORE/LINE;" + engine.statistics.spl + "\t";
		subMsg += "LINE/MIN;" + engine.statistics.lpm + "\t";

		String msg = "gstat1p\t" + NetUtil.urlEncode(subMsg) + "\n";
		netLobby.netPlayerClient.send(msg);
	}

	/**
	 * NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	@Override
	protected void netSendOptions(GameEngine engine) {
		String msg = "game\toption\t";
		msg += goaltype + "\t" + startlevel + "\t" + tspinEnableType + "\t";
		msg += enableTSpinKick + "\t" + enableB2B + "\t" + enableCombo + "\t" + big + "\t";
		msg += spinCheckType + "\t" + tspinEnableEZ + "\n";
		netLobby.netPlayerClient.send(msg);
	}

	/**
	 * NET: Receive game options
	 */
	@Override
	protected void netRecvOptions(GameEngine engine, String[] message) {
		goaltype = Integer.parseInt(message[4]);
		startlevel = Integer.parseInt(message[5]);
		tspinEnableType = Integer.parseInt(message[6]);
		enableTSpinKick = Boolean.parseBoolean(message[7]);
		enableB2B = Boolean.parseBoolean(message[8]);
		enableCombo = Boolean.parseBoolean(message[9]);
		big = Boolean.parseBoolean(message[10]);
		spinCheckType = Integer.parseInt(message[11]);
		tspinEnableEZ = Boolean.parseBoolean(message[12]);
	}

	/**
	 * NET: Get goal type
	 */
	@Override
	protected int netGetGoalType() {
		return goaltype;
	}

	/**
	 * NET: It returns true when the current settings doesn't prevent leaderboard screen from showing.
	 */
	@Override
	protected boolean netIsNetRankingViewOK(GameEngine engine) {
		return (!big) && (engine.ai == null) && (startlevel == 0);
	}
}
