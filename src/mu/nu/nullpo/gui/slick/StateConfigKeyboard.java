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
package mu.nu.nullpo.gui.slick;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;

/**
 * キーボード設定画面のステート
 */
public class StateConfigKeyboard extends BasicGameState {
	/** This state's ID */
	public static final int ID = 9;

	/** Key input を受付可能になるまでの frame count */
	public static final int KEYACCEPTFRAME = 30;

	/** Player number */
	public int player = 0;

	/** 初期設定Mode */
	protected boolean firstSetupMode;

	/** StateBasedGame */
	protected StateBasedGame gameObj;

	/** Number of button currently being configured */
	protected int keynum;

	/** 経過 frame count */
	protected int frame;

	/** Button settings */
	protected int keymap[];

	/*
	 * Fetch this state's ID
	 */
	@Override
	public int getID() {
		return ID;
	}

	/**
	 * Button settings initialization
	 */
	protected void reset() {
		firstSetupMode = NullpoMinoSlick.propConfig.getProperty("option.firstSetupMode", true);

		keynum = 0;
		frame = 0;

		keymap = new int[GameKey.MAX_BUTTON];

		for(int i = 0; i < GameKey.MAX_BUTTON; i++) {
			keymap[i] = GameKey.gamekey[player].keymap[i];
		}
	}

	/*
	 * State initialization
	 */
	public void init(GameContainer container, StateBasedGame game) throws SlickException {
		gameObj = game;
	}

	/**
	 * キーのNameを取得
	 * @param key キー
	 * @return キーのName
	 */
	protected String getKeyName(int key) {
		String str = org.lwjgl.input.Keyboard.getKeyName(key);
		return (str == null) ? String.valueOf(key) : str.toUpperCase();
	}

	/*
	 * Draw the screen
	 */
	public void render(GameContainer container, StateBasedGame game, Graphics g) throws SlickException {
		g.drawImage(ResourceHolder.imgMenu, 0, 0);

		NormalFont.printFontGrid(1, 1, "KEYBOARD SETTING (" + (player + 1) + "P)", NormalFont.COLOR_ORANGE);

		NormalFont.printFontGrid(2, 3, "UP                : " + getKeyName(keymap[GameKey.BUTTON_UP]), (keynum == 0));
		NormalFont.printFontGrid(2, 4, "DOWN              : " + getKeyName(keymap[GameKey.BUTTON_DOWN]), (keynum == 1));
		NormalFont.printFontGrid(2, 5, "LEFT              : " + getKeyName(keymap[GameKey.BUTTON_LEFT]), (keynum == 2));
		NormalFont.printFontGrid(2, 6, "RIGHT             : " + getKeyName(keymap[GameKey.BUTTON_RIGHT]), (keynum == 3));
		NormalFont.printFontGrid(2, 7, "A (L/R-ROT)       : " + getKeyName(keymap[GameKey.BUTTON_A]), (keynum == 4));
		NormalFont.printFontGrid(2, 8, "B (R/L-ROT)       : " + getKeyName(keymap[GameKey.BUTTON_B]), (keynum == 5));
		NormalFont.printFontGrid(2, 9, "C (L/R-ROT)       : " + getKeyName(keymap[GameKey.BUTTON_C]), (keynum == 6));
		NormalFont.printFontGrid(2, 10, "D (HOLD)          : " + getKeyName(keymap[GameKey.BUTTON_D]), (keynum == 7));
		NormalFont.printFontGrid(2, 11, "E (180-ROT)       : " + getKeyName(keymap[GameKey.BUTTON_E]), (keynum == 8));
		NormalFont.printFontGrid(2, 12, "F                 : " + getKeyName(keymap[GameKey.BUTTON_F]), (keynum == 9));
		NormalFont.printFontGrid(2, 13, "QUIT              : " + getKeyName(keymap[GameKey.BUTTON_QUIT]), (keynum == 10));
		NormalFont.printFontGrid(2, 14, "PAUSE             : " + getKeyName(keymap[GameKey.BUTTON_PAUSE]), (keynum == 11));
		NormalFont.printFontGrid(2, 15, "GIVEUP            : " + getKeyName(keymap[GameKey.BUTTON_GIVEUP]), (keynum == 12));
		NormalFont.printFontGrid(2, 16, "RETRY             : " + getKeyName(keymap[GameKey.BUTTON_RETRY]), (keynum == 13));
		NormalFont.printFontGrid(2, 17, "FRAME STEP        : " + getKeyName(keymap[GameKey.BUTTON_FRAMESTEP]), (keynum == 14));
		NormalFont.printFontGrid(2, 18, "SCREEN SHOT       : " + getKeyName(keymap[GameKey.BUTTON_SCREENSHOT]), (keynum == 15));
		NormalFont.printFontGrid(2, 19, "NAVIGATION UP     : " + getKeyName(keymap[GameKey.BUTTON_NAV_UP]), (keynum == 16));
		NormalFont.printFontGrid(2, 20, "NAVIGATION DOWN   : " + getKeyName(keymap[GameKey.BUTTON_NAV_DOWN]), (keynum == 17));
		NormalFont.printFontGrid(2, 21, "NAVIGATION LEFT   : " + getKeyName(keymap[GameKey.BUTTON_NAV_LEFT]), (keynum == 18));
		NormalFont.printFontGrid(2, 22, "NAVIGATION RIGHT  : " + getKeyName(keymap[GameKey.BUTTON_NAV_RIGHT]), (keynum == 19));
		NormalFont.printFontGrid(2, 23, "NAVIGATION SELECT : " + getKeyName(keymap[GameKey.BUTTON_NAV_SELECT]), (keynum == 20));
		NormalFont.printFontGrid(2, 24, "NAVIGATION CANCEL : " + getKeyName(keymap[GameKey.BUTTON_NAV_CANCEL]), (keynum == 21));

		if(frame >= KEYACCEPTFRAME) {
			if(keynum < GameKey.MAX_BUTTON) {
				NormalFont.printFontGrid(1, 3 + keynum, "b", NormalFont.COLOR_RED);

				NormalFont.printFontGrid(1, 25, "DELETE:    NO SET", NormalFont.COLOR_GREEN);
				if(!firstSetupMode) NormalFont.printFontGrid(1, 26, "BACKSPACE: CANCEL", NormalFont.COLOR_GREEN);
			} else {
				NormalFont.printFontGrid(1, 25, "ENTER:     OK", NormalFont.COLOR_GREEN);
				NormalFont.printFontGrid(1, 26, "DELETE:    AGAIN", NormalFont.COLOR_GREEN);
				if(!firstSetupMode) NormalFont.printFontGrid(1, 27, "BACKSPACE: CANCEL", NormalFont.COLOR_GREEN);
			}
		}

		if(!NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep();
	}

	/*
	 * Update game state
	 */
	public void update(GameContainer container, StateBasedGame game, int delta) throws SlickException {
		if(!container.hasFocus()) {
			if(NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep();
			return;
		}

		frame++;
		if(NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep();
	}

	/*
	 * キーを押して離したときの処理
	 */
	@Override
	public void keyReleased(int key, char c) {
		if(frame >= KEYACCEPTFRAME) {
			if(keynum < GameKey.MAX_BUTTON) {
				if(key == Input.KEY_DELETE) {
					ResourceHolder.soundManager.play("move");
					keymap[keynum] = 0;
				} else if(key == Input.KEY_BACK) {
					if(!firstSetupMode) {
						gameObj.enterState(StateConfigMainMenu.ID);
						return;
					}
				} else {
					ResourceHolder.soundManager.play("move");
					keymap[keynum] = key;
				}

				keynum++;
			} else {
				if(key == Input.KEY_ENTER) {
					ResourceHolder.soundManager.play("decide");

					//NullpoMinoSlick.propConfig.setProperty("option.firstSetupMode", false);
					for(int i = 0; i < GameKey.MAX_BUTTON; i++) {
						GameKey.gamekey[player].keymap[i] = keymap[i];
					}
					GameKey.gamekey[player].saveConfig(NullpoMinoSlick.propConfig);
					NullpoMinoSlick.saveConfig();

					if(!firstSetupMode)
						gameObj.enterState(StateConfigMainMenu.ID);
					else
						gameObj.enterState(StateConfigRuleSelect.ID);
				} else if(key == Input.KEY_DELETE) {
					ResourceHolder.soundManager.play("move");
					reset();
				} else if(key == Input.KEY_BACK) {
					if(!firstSetupMode) {
						gameObj.enterState(StateConfigMainMenu.ID);
						return;
					}
				}
			}
		}
	}

	/**
	 * Called when entering this state
	 */
	@Override
	public void enter(GameContainer container, StateBasedGame game) throws SlickException {
		reset();
	}

	/**
	 * Called when leaving this state
	 */
	@Override
	public void leave(GameContainer container, StateBasedGame game) throws SlickException {
		reset();
	}
}
