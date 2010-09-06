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

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;

import mu.nu.nullpo.util.CustomProperties;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.StateBasedGame;

/**
 * ルール選択画面のステート
 */
public class StateConfigRuleSelect extends DummyMenuScrollState {
	/** This state's ID */
	public static final int ID = 7;

	/** 1画面に表示するMaximumファイルcount */
	public static final int PAGE_HEIGHT = 20;

	/** Player ID */
	public int player = 0;

	/** 初期設定Mode */
	protected boolean firstSetupMode;

	/** ファイルパス一覧 */
	protected String[] strFilePathList;

	/** Rule name一覧 */
	protected String[] strRuleNameList;

	/** Current ルールファイル */
	protected String strCurrentFileName;

	/** Current Rule name */
	protected String strCurrentRuleName;

	public StateConfigRuleSelect() {
		pageHeight = PAGE_HEIGHT;
		nullError = "RULE DIRECTORY NOT FOUND";
		emptyError = "NO RULE FILE";
	}

	/*
	 * Fetch this state's ID
	 */
	@Override
	public int getID() {
		return ID;
	}

	/**
	 * ルールファイル一覧を取得
	 * @return ルールファイルのFilenameの配列。ディレクトリがないならnull
	 */
	protected String[] getRuleFileList() {
		File dir = new File("config/rule");

		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir1, String name) {
				return name.endsWith(".rul");
			}
		};

		String[] list = dir.list(filter);

		return list;
	}

	/**
	 * 詳細情報を更新
	 */
	protected void updateDetails() {
		strFilePathList = new String[list.length];
		strRuleNameList = new String[list.length];

		for(int i = 0; i < list.length; i++) {
			File file = new File("config/rule/" + list[i]);
			strFilePathList[i] = file.getPath();

			CustomProperties prop = new CustomProperties();

			try {
				FileInputStream in = new FileInputStream("config/rule/" + list[i]);
				prop.load(in);
				in.close();
				strRuleNameList[i] = prop.getProperty("0.ruleopt.strRuleName", "");
			} catch (Exception e) {
				strRuleNameList[i] = "";
			}
		}
	}

	/*
	 * Called when entering this state
	 */
	@Override
	public void enter(GameContainer container, StateBasedGame game) throws SlickException {
		firstSetupMode = NullpoMinoSlick.propConfig.getProperty("option.firstSetupMode", true);

		list = getRuleFileList();
		maxCursor = list.length-1;
		updateDetails();

		strCurrentFileName = NullpoMinoSlick.propGlobal.getProperty(player + ".rulefile", "");
		strCurrentRuleName = NullpoMinoSlick.propGlobal.getProperty(player + ".rulename", "");
		for(int i = 0; i < list.length; i++) {
			if(strCurrentFileName.equals(list[i])) {
				cursor = i;
			}
		}
	}

	/*
	 * State initialization
	 */
	public void init(GameContainer container, StateBasedGame game) throws SlickException {
	}

	/*
	 * Draw the screen
	 */
	public void render(GameContainer container, StateBasedGame game, Graphics graphics) throws SlickException {
		if(firstSetupMode)
			NormalFont.printFontGrid(6, 28, "D:USE DEFAULT RULE", NormalFont.COLOR_GREEN);
		else
			NormalFont.printFontGrid(6, 28, "B:CANCEL D:USE DEFAULT RULE", NormalFont.COLOR_GREEN);

		super.render(container, game, graphics);
	}

	@Override
	protected void onRenderSuccess (GameContainer container, StateBasedGame game, Graphics graphics)  {
		String title = "SELECT " + (player + 1) + "P RULE (" + (cursor + 1) + "/" + (list.length) + ")";
		NormalFont.printFontGrid(1, 1, title, NormalFont.COLOR_ORANGE);

		NormalFont.printFontGrid(1, 26, "FILE:" + list[cursor].toUpperCase(), NormalFont.COLOR_CYAN);
		NormalFont.printFontGrid(1, 27, "CURRENT:" + strCurrentRuleName.toUpperCase(), NormalFont.COLOR_BLUE);

		NormalFont.printFontGrid(1, 28, "A:OK", NormalFont.COLOR_GREEN);
	}

	@Override
	protected boolean onDecide(GameContainer container, StateBasedGame game, int delta) {
		ResourceHolder.soundManager.play("decide");
		NullpoMinoSlick.propGlobal.setProperty(player + ".rule", strFilePathList[cursor]);
		NullpoMinoSlick.propGlobal.setProperty(player + ".rulefile", list[cursor]);
		NullpoMinoSlick.propGlobal.setProperty(player + ".rulename", strRuleNameList[cursor]);
		NullpoMinoSlick.propConfig.setProperty("option.firstSetupMode", false);
		NullpoMinoSlick.saveConfig();

		if(!firstSetupMode) game.enterState(StateConfigMainMenu.ID);
		else game.enterState(StateTitle.ID);

		return true;
	}

	@Override
	protected boolean onCancel(GameContainer container, StateBasedGame game, int delta) {
		if (!firstSetupMode)
		{
			game.enterState(StateConfigMainMenu.ID);
			return true;
		}
		return false;
	}

	@Override
	protected boolean onPushButtonD(GameContainer container, StateBasedGame game, int delta) {
		ResourceHolder.soundManager.play("decide");
		NullpoMinoSlick.propGlobal.setProperty(player + ".rule", "");
		NullpoMinoSlick.propGlobal.setProperty(player + ".rulefile", "");
		NullpoMinoSlick.propGlobal.setProperty(player + ".rulename", "");
		NullpoMinoSlick.propConfig.setProperty("option.firstSetupMode", false);
		NullpoMinoSlick.saveConfig();
		if(!firstSetupMode) game.enterState(StateConfigMainMenu.ID);
		else game.enterState(StateTitle.ID);
		return true;
	}
}
