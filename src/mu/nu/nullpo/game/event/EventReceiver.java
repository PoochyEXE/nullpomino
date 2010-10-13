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
package mu.nu.nullpo.game.event;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

import mu.nu.nullpo.game.component.Block;
import mu.nu.nullpo.game.play.GameEngine;
import mu.nu.nullpo.game.play.GameManager;
import mu.nu.nullpo.util.CustomProperties;
import mu.nu.nullpo.util.GeneralUtil;

/**
 * Drawing and event handling EventReceiver
 */
public class EventReceiver {
	/** Log */
	static Logger log = Logger.getLogger(EventReceiver.class);

	/** Field X position */
	public static final int[][] NEW_FIELD_OFFSET_X = {
		{119, 247, 375, 503, 247, 375}, // Small
		{ 32, 432, 432, 432, 432, 432}, // Normal
		{ 16, 416, 416, 416, 416, 416}, // Big
	};
	/** Field Y position */
	public static final int[][] NEW_FIELD_OFFSET_Y = {
		{ 80,  80,  80,  80, 286, 286}, // Small
		{ 32,  32,  32,  32,  32,  32}, // Normal
		{  8,   8,   8,   8,   8,   8}, // Big
	};

	/** Field X position (Big side preview) */
	public static final int[][] NEW_FIELD_OFFSET_X_BSP = {
		{119, 320, 432, 544, 320, 432}, // Small
		{ 64, 400, 400, 400, 400, 400}, // Normal
		{ 16, 352, 352, 352, 352, 352}, // Big
	};
	/** Field Y position (Big side preview) */
	public static final int[][] NEW_FIELD_OFFSET_Y_BSP = {
		{ 80,  80,  80,  80, 286, 286}, // Small
		{ 32,  32,  32,  32,  32,  32}, // Normal
		{  8,   8,   8,   8,   8,   8}, // Big
	};

	/** Background表示 */
	protected boolean showbg;

	/** field右側にMeterを表示 */
	protected boolean showmeter;

	/** 枠線型ghost ピース */
	protected boolean outlineghost;

	/** Piece previews on sides */
	protected boolean sidenext;

	/** Use bigger side previews */
	protected boolean bigsidenext;

	/**
	 * 文字色の定count
	 */
	public static final int COLOR_WHITE = 0, COLOR_BLUE = 1, COLOR_RED = 2, COLOR_PINK = 3, COLOR_GREEN = 4, COLOR_YELLOW = 5, COLOR_CYAN = 6,
			COLOR_ORANGE = 7, COLOR_PURPLE = 8, COLOR_DARKBLUE = 9;

	/**
	 * Menu 用の文字列を描画
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param color 色
	 * @param scale 拡大率
	 */
	public void drawMenuFont(GameEngine engine, int playerID, int x, int y, String str, int color, float scale) {}

	/**
	 * ※オーバーライドする必要はありません
	 * Menu 用の文字列を描画 (文字色は白）
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 */
	public void drawMenuFont(GameEngine engine, int playerID, int x, int y, String str) {
		drawMenuFont(engine, playerID, x, y, str, COLOR_WHITE, 1.0f);
	}

	/**
	 * ※オーバーライドする必要はありません
	 * Menu 用の文字列を描画
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param color 色
	 */
	public void drawMenuFont(GameEngine engine, int playerID, int x, int y, String str, int color) {
		drawMenuFont(engine, playerID, x, y, str, color, 1.0f);
	}

	/**
	 * ※オーバーライドする必要はありません
	 * flagがfalseだったらcolorF color, trueだったらcolorT colorでMenu 用の文字列を描画
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param flag 条件式またはboolean変count
	 * @param colorF flagがfalseの場合の文字色
	 * @param colorT flagがtrueの場合の文字色
	 */
	public void drawMenuFont(GameEngine engine, int playerID, int x, int y, String str, boolean flag, int colorF, int colorT) {
		if(flag == false) {
			drawMenuFont(engine, playerID, x, y, str, colorF, 1.0f);
		} else {
			drawMenuFont(engine, playerID, x, y, str, colorT, 1.0f);
		}
	}

	/**
	 * ※オーバーライドする必要はありません
	 * flagがfalseだったら白, trueだったら赤でMenu 用の文字列を描画
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param flag 条件式またはboolean変count
	 */
	public void drawMenuFont(GameEngine engine, int playerID, int x, int y, String str, boolean flag) {
		if(flag == false) {
			drawMenuFont(engine, playerID, x, y, str, COLOR_WHITE, 1.0f);
		} else {
			int fontcolor = COLOR_RED;
			if(playerID == 1) fontcolor = COLOR_BLUE;
			drawMenuFont(engine, playerID, x, y, str, fontcolor, 1.0f);
		}
	}

	/**
	 * Menu 用の文字列をTTF font で描画 (必ずしも全てのVersionで使えるわけではありません）
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param color 色
	 */
	public void drawTTFMenuFont(GameEngine engine, int playerID, int x, int y, String str, int color) {}

	/**
	 * ※オーバーライドする必要はありません
	 * Menu 用の文字列をTTF font で描画 (文字色は白）
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 */
	public void drawTTFMenuFont(GameEngine engine, int playerID, int x, int y, String str) {
		drawTTFMenuFont(engine, playerID, x, y, str, COLOR_WHITE);
	}

	/**
	 * ※オーバーライドする必要はありません
	 * flagがfalseだったらcolorF color, trueだったらcolorT colorでMenu 用の文字列をTTF font で描画
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param flag 条件式またはboolean変count
	 * @param colorF flagがfalseの場合の文字色
	 * @param colorT flagがtrueの場合の文字色
	 */
	public void drawTTFMenuFont(GameEngine engine, int playerID, int x, int y, String str, boolean flag, int colorF, int colorT) {
		if(flag == false) {
			drawTTFMenuFont(engine, playerID, x, y, str, colorF);
		} else {
			drawTTFMenuFont(engine, playerID, x, y, str, colorT);
		}
	}

	/**
	 * ※オーバーライドする必要はありません
	 * flagがfalseだったら白, trueだったら赤でMenu 用の文字列をTTF font で描画
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param flag 条件式またはboolean変count
	 */
	public void drawTTFMenuFont(GameEngine engine, int playerID, int x, int y, String str, boolean flag) {
		if(flag == false) {
			drawTTFMenuFont(engine, playerID, x, y, str, COLOR_WHITE);
		} else {
			int fontcolor = COLOR_RED;
			if(playerID == 1) fontcolor = COLOR_BLUE;
			drawTTFMenuFont(engine, playerID, x, y, str, fontcolor);
		}
	}

	/**
	 * Render score用の文字列を描画
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param color 色
	 * @param scale 拡大率
	 */
	public void drawScoreFont(GameEngine engine, int playerID, int x, int y, String str, int color, float scale) {}

	/**
	 * ※オーバーライドする必要はありません
	 * Render score用の文字列を描画 (文字色は白）
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 */
	public void drawScoreFont(GameEngine engine, int playerID, int x, int y, String str) {
		drawScoreFont(engine, playerID, x, y, str, COLOR_WHITE, 1.0f);
	}

	/**
	 * ※オーバーライドする必要はありません
	 * Render score用の文字列を描画
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param color 色
	 */
	public void drawScoreFont(GameEngine engine, int playerID, int x, int y, String str, int color) {
		drawScoreFont(engine, playerID, x, y, str, color, 1.0f);
	}

	/**
	 * ※オーバーライドする必要はありません
	 * flagがfalseだったらcolorF color, trueだったらcolorT colorでRender score用の文字列を描画
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param flag 条件式またはboolean変count
	 * @param colorF flagがfalseの場合の文字色
	 * @param colorT flagがtrueの場合の文字色
	 */
	public void drawScoreFont(GameEngine engine, int playerID, int x, int y, String str, boolean flag, int colorF, int colorT) {
		if(flag == false) {
			drawScoreFont(engine, playerID, x, y, str, colorF, 1.0f);
		} else {
			drawScoreFont(engine, playerID, x, y, str, colorT, 1.0f);
		}
	}

	/**
	 * ※オーバーライドする必要はありません
	 * flagがfalseだったら白, trueだったら赤でRender score用の文字列を描画
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param flag 条件式またはboolean変count
	 */
	public void drawScoreFont(GameEngine engine, int playerID, int x, int y, String str, boolean flag) {
		if(flag == false) {
			drawScoreFont(engine, playerID, x, y, str, COLOR_WHITE, 1.0f);
		} else {
			int fontcolor = COLOR_RED;
			if(playerID == 1) fontcolor = COLOR_BLUE;
			drawScoreFont(engine, playerID, x, y, str, fontcolor, 1.0f);
		}
	}

	/**
	 * Render score用の文字列をTTF font で描画 (必ずしも全てのVersionで使えるわけではありません）
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param color 色
	 */
	public void drawTTFScoreFont(GameEngine engine, int playerID, int x, int y, String str, int color) {}

	/**
	 * ※オーバーライドする必要はありません
	 * Render score用の文字列をTTF font で描画 (文字色は白）
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 */
	public void drawTTFScoreFont(GameEngine engine, int playerID, int x, int y, String str) {
		drawTTFScoreFont(engine, playerID, x, y, str, COLOR_WHITE);
	}

	/**
	 * ※オーバーライドする必要はありません
	 * flagがfalseだったらcolorF color, trueだったらcolorT colorでRender score用の文字列をTTF font で描画
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param flag 条件式またはboolean変count
	 * @param colorF flagがfalseの場合の文字色
	 * @param colorT flagがtrueの場合の文字色
	 */
	public void drawTTFScoreFont(GameEngine engine, int playerID, int x, int y, String str, boolean flag, int colorF, int colorT) {
		if(flag == false) {
			drawTTFScoreFont(engine, playerID, x, y, str, colorF);
		} else {
			drawTTFScoreFont(engine, playerID, x, y, str, colorT);
		}
	}

	/**
	 * ※オーバーライドする必要はありません
	 * flagがfalseだったら白, trueだったら赤でRender score用の文字列をTTF font で描画
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param flag 条件式またはboolean変count
	 */
	public void drawTTFScoreFont(GameEngine engine, int playerID, int x, int y, String str, boolean flag) {
		if(flag == false) {
			drawTTFScoreFont(engine, playerID, x, y, str, COLOR_WHITE);
		} else {
			int fontcolor = COLOR_RED;
			if(playerID == 1) fontcolor = COLOR_BLUE;
			drawTTFScoreFont(engine, playerID, x, y, str, fontcolor);
		}
	}

	/**
	 * 直接指定した座標へ文字列を描画
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param color 色
	 * @param scale 拡大率
	 */
	public void drawDirectFont(GameEngine engine, int playerID, int x, int y, String str, int color, float scale) {}

	/**
	 * ※オーバーライドする必要はありません
	 * 直接指定した座標へ文字列を描画 (文字色は白）
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 */
	public void drawDirectFont(GameEngine engine, int playerID, int x, int y, String str) {
		drawDirectFont(engine, playerID, x, y, str, COLOR_WHITE, 1.0f);
	}

	/**
	 * ※オーバーライドする必要はありません
	 * 直接指定した座標へ文字列を描画
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param color 色
	 */
	public void drawDirectFont(GameEngine engine, int playerID, int x, int y, String str, int color) {
		drawDirectFont(engine, playerID, x, y, str, color, 1.0f);
	}

	/**
	 * ※オーバーライドする必要はありません
	 * flagがfalseだったらcolorF color, trueだったらcolorT colorで直接指定した座標へ文字列を描画
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param flag 条件式またはboolean変count
	 * @param colorF flagがfalseの場合の文字色
	 * @param colorT flagがtrueの場合の文字色
	 */
	public void drawDirectFont(GameEngine engine, int playerID, int x, int y, String str, boolean flag, int colorF, int colorT) {
		if(flag == false) {
			drawDirectFont(engine, playerID, x, y, str, colorF, 1.0f);
		} else {
			drawDirectFont(engine, playerID, x, y, str, colorT, 1.0f);
		}
	}

	/**
	 * ※オーバーライドする必要はありません
	 * flagがfalseだったら白, trueだったら赤で直接指定した座標へ文字列を描画
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param flag 条件式またはboolean変count
	 */
	public void drawDirectFont(GameEngine engine, int playerID, int x, int y, String str, boolean flag) {
		if(flag == false) {
			drawDirectFont(engine, playerID, x, y, str, COLOR_WHITE, 1.0f);
		} else {
			int fontcolor = COLOR_RED;
			if(playerID == 1) fontcolor = COLOR_BLUE;
			drawDirectFont(engine, playerID, x, y, str, fontcolor, 1.0f);
		}
	}

	/**
	 * 直接指定した座標へ描画できる文字列をTTF font で描画 (必ずしも全てのVersionで使えるわけではありません）
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param color 色
	 */
	public void drawTTFDirectFont(GameEngine engine, int playerID, int x, int y, String str, int color) {}

	/**
	 * ※オーバーライドする必要はありません
	 * 直接指定した座標へ描画できる文字列をTTF font で描画 (文字色は白）
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 */
	public void drawTTFDirectFont(GameEngine engine, int playerID, int x, int y, String str) {
		drawTTFDirectFont(engine, playerID, x, y, str, COLOR_WHITE);
	}

	/**
	 * ※オーバーライドする必要はありません
	 * flagがfalseだったらcolorF color, trueだったらcolorT colorで直接指定した座標へ描画できる文字列をTTF font で描画
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param flag 条件式またはboolean変count
	 * @param colorF flagがfalseの場合の文字色
	 * @param colorT flagがtrueの場合の文字色
	 */
	public void drawTTFDirectFont(GameEngine engine, int playerID, int x, int y, String str, boolean flag, int colorF, int colorT) {
		if(flag == false) {
			drawTTFDirectFont(engine, playerID, x, y, str, colorF);
		} else {
			drawTTFDirectFont(engine, playerID, x, y, str, colorT);
		}
	}

	/**
	 * ※オーバーライドする必要はありません
	 * flagがfalseだったら白, trueだったら赤で直接指定した座標へ描画できる文字列をTTF font で描画
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param flag 条件式またはboolean変count
	 */
	public void drawTTFDirectFont(GameEngine engine, int playerID, int x, int y, String str, boolean flag) {
		if(flag == false) {
			drawTTFDirectFont(engine, playerID, x, y, str, COLOR_WHITE);
		} else {
			int fontcolor = COLOR_RED;
			if(playerID == 1) fontcolor = COLOR_BLUE;
			drawTTFDirectFont(engine, playerID, x, y, str, fontcolor);
		}
	}

	/**
	 * スピードMeterを描画
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param s スピード
	 */
	public void drawSpeedMeter(GameEngine engine, int playerID, int x, int y, int s) {}

	/**
	 * 1マスBlockを描画
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param color 色
	 * @param skin 絵柄
	 * @param bone When true,骨Block
	 * @param darkness 暗さもしくは明るさ
	 * @param alpha 透明度
	 * @param scale 拡大率
	 */
	public void drawSingleBlock(GameEngine engine, int playerID, int x, int y, int color, int skin, boolean bone, float darkness, float alpha, float scale) {}

	/**
	 * TTF font を使用できるか判定
	 * @return TTF font を使用できるならtrue
	 */
	public boolean isTTFSupport() {
		return false;
	}

	/**
	 * field右のMeterのMaximum量を取得
	 * @param engine GameEngineのインスタンス
	 * @return field右のMeterのMaximum量
	 */
	public int getMeterMax(GameEngine engine) {
		if(!showmeter) return 0;
		int blksize = 16;
		if (engine.displaysize == -1)
			blksize =  8;
		else if (engine.displaysize == 1)
			blksize = 32;
		return engine.fieldHeight * blksize;
	}

	/**
	 * Block imagesの幅を取得
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @return Block imagesの幅
	 */
	public int getBlockGraphicsWidth(GameEngine engine, int playerID) {
		if (engine.displaysize == -1)
			return 8;
		else if (engine.displaysize == 1)
			return 32;
		else
			return 16;
	}

	/**
	 * Block imagesの高さを取得
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @return Block imagesの高さ
	 */
	public int getBlockGraphicsHeight(GameEngine engine, int playerID) {
		if (engine.displaysize == -1)
			return 8;
		else if (engine.displaysize == 1)
			return 32;
		else
			return 16;
	}

	/**
	 * Get X position of field
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return X position of field
	 */
	public int getFieldDisplayPositionX(GameEngine engine, int playerID) {
		if(getNextDisplayType() == 2) return NEW_FIELD_OFFSET_X_BSP[engine.displaysize + 1][playerID];
		return NEW_FIELD_OFFSET_X[engine.displaysize + 1][playerID];
	}

	/**
	 * Get Y position of field
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return Y position of field
	 */
	public int getFieldDisplayPositionY(GameEngine engine, int playerID) {
		if(getNextDisplayType() == 2) return NEW_FIELD_OFFSET_Y_BSP[engine.displaysize + 1][playerID];
		return NEW_FIELD_OFFSET_Y[engine.displaysize + 1][playerID];
	}

	/**
	 * Get X position of score display area
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return X position of score display area
	 */
	public int getScoreDisplayPositionX(GameEngine engine, int playerID) {
		int xOffset = (getNextDisplayType() == 2) ? 256 : 216;
		return getFieldDisplayPositionX(engine, playerID) + xOffset;
	}

	/**
	 * Get Y position of score display area
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return Y position of score display area
	 */
	public int getScoreDisplayPositionY(GameEngine engine, int playerID) {
		return getFieldDisplayPositionY(engine, playerID) + 48;
	}

	/**
	 * Get type of piece preview
	 * @return 0=Above 1=Side Small 2=Side Big
	 */
	public int getNextDisplayType() {
		if(sidenext && bigsidenext) return 2;
		if(sidenext && !bigsidenext) return 1;
		return 0;
	}

	/**
	 * Sound effects再生
	 * @param name Sound effectsのName
	 */
	public void playSE(String name) {}

	/**
	 * 描画先のGraphicsを設定
	 * @param g 描画先のGraphics
	 */
	public void setGraphics(Object g) {}

	/**
	 * Mode の設定を読み込み
	 * @return Modeの設定 data (nullならなし）
	 */
	public CustomProperties loadModeConfig() {
		CustomProperties propModeConfig = new CustomProperties();

		try {
			FileInputStream in = new FileInputStream("config/setting/mode.cfg");
			propModeConfig.load(in);
			in.close();
		} catch(IOException e) {
			return null;
		}

		return propModeConfig;
	}

	/**
	 * Mode の設定を保存
	 * @param modeConfig Modeの設定 data
	 */
	public void saveModeConfig(CustomProperties modeConfig) {
		try {
			FileOutputStream out = new FileOutputStream("config/setting/mode.cfg");
			modeConfig.store(out, "NullpoMino Mode Config");
			out.close();
		} catch(IOException e) {
			log.error("Failed to save mode config", e);
		}
	}

	/**
	 * 任意のプロパティセットを読み込み
	 * @param filename Filename
	 * @return プロパティセット (失敗したらnull）
	 */
	public CustomProperties loadProperties(String filename) {
		CustomProperties prop = new CustomProperties();

		try {
			FileInputStream in = new FileInputStream(filename);
			prop.load(in);
			in.close();
		} catch(IOException e) {
			log.debug("Failed to load custom property file from " + filename, e);
			return null;
		}

		return prop;
	}

	/**
	 * 任意のプロパティセットを任意のFilenameで保存
	 * @param filename Filename
	 * @param prop 保存するプロパティセット
	 * @return 成功するとtrue
	 */
	public boolean saveProperties(String filename, CustomProperties prop) {
		try {
			FileOutputStream out = new FileOutputStream(filename);
			prop.store(out, "NullpoMino Custom Property File");
			out.close();
		} catch(IOException e) {
			log.debug("Failed to save custom property file to " + filename, e);
			return false;
		}

		return true;
	}

	/**
	 * ゲーム画面表示直前に呼び出される処理
	 * @param manager GameManager that owns this mode
	 */
	public void modeInit(GameManager manager) {}

	/**
	 * Initialization for each playerが終わるときに呼び出される処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void playerInit(GameEngine engine, int playerID) {}

	/**
	 * Ready→Go直後, 最初のピースが現れる直前の処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void startGame(GameEngine engine, int playerID) {}

	/**
	 * Each player's 最初の処理の時に呼び出される
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void onFirst(GameEngine engine, int playerID) {}

	/**
	 * Each player's 最後の処理の時に呼び出される
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void onLast(GameEngine engine, int playerID) {}

	/**
	 * 開始前の設定画面のときの処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void onSetting(GameEngine engine, int playerID) {}

	/**
	 * Ready→Goのときの処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void onReady(GameEngine engine, int playerID) {}

	/**
	 * Blockピースの移動処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void onMove(GameEngine engine, int playerID) {}

	/**
	 * Block固定直後の光っているときの処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void onLockFlash(GameEngine engine, int playerID) {}

	/**
	 * Line clear処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void onLineClear(GameEngine engine, int playerID) {}

	/**
	 * ARE中の処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void onARE(GameEngine engine, int playerID) {}

	/**
	 * Ending突入時の処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void onEndingStart(GameEngine engine, int playerID) {}

	/**
	 * 各ゲームMode が自由に使えるステータスの処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void onCustom(GameEngine engine, int playerID) {}

	/**
	 * Ending画面の処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void onExcellent(GameEngine engine, int playerID) {}

	/**
	 * game over画面の処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void onGameOver(GameEngine engine, int playerID) {}

	/**
	 * 結果画面の処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void onResult(GameEngine engine, int playerID) {}

	/**
	 * fieldエディット画面の処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void onFieldEdit(GameEngine engine, int playerID) {}

	/**
	 * Each player's 最初の描画処理の時に呼び出される
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderFirst(GameEngine engine, int playerID) {}

	/**
	 * Each player's 最後の描画処理の時に呼び出される
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderLast(GameEngine engine, int playerID) {}

	/**
	 * 開始前の設定画面のときの描画処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderSetting(GameEngine engine, int playerID) {}

	/**
	 * Ready→Goのときの描画処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderReady(GameEngine engine, int playerID) {}

	/**
	 * Blockピースの移動描画処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderMove(GameEngine engine, int playerID) {}

	/**
	 * Block固定直後の光っているときの処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderLockFlash(GameEngine engine, int playerID) {}

	/**
	 * Line clear描画処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderLineClear(GameEngine engine, int playerID) {}

	/**
	 * ARE中の描画処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderARE(GameEngine engine, int playerID) {}

	/**
	 * Ending突入時の描画処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderEndingStart(GameEngine engine, int playerID) {}

	/**
	 * 各ゲームMode が自由に使えるステータスの描画処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderCustom(GameEngine engine, int playerID) {}

	/**
	 * Ending画面の描画処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderExcellent(GameEngine engine, int playerID) {}

	/**
	 * game over画面の描画処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderGameOver(GameEngine engine, int playerID) {}

	/**
	 * Render results screen処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderResult(GameEngine engine, int playerID) {}

	/**
	 * fieldエディット画面の描画処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderFieldEdit(GameEngine engine, int playerID) {}

	/**
	 * Blockを消す演出を出すときの処理
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param blk Block
	 */
	public void blockBreak(GameEngine engine, int playerID, int x, int y, Block blk) {}

	/**
	 * Calculate score
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param lines 消えるLinescount (消えなかった場合は0）
	 */
	public void calcScore(GameEngine engine, int playerID, int lines) {}

	/**
	 * Soft drop使用後の処理
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param fall 今落下した段count
	 */
	public void afterSoftDropFall(GameEngine engine, int playerID, int fall) {}

	/**
	 * Hard drop使用後の処理
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param fall 今落下した段count
	 */
	public void afterHardDropFall(GameEngine engine, int playerID, int fall) {}

	/**
	 * fieldエディット画面から出たときの処理
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 */
	public void fieldEditExit(GameEngine engine, int playerID) {}

	/**
	 * Blockピースが固定されたときの処理(calcScoreの直後)
	 * @param engine GameEngineのインスタンス
	 * @param playerID Player ID
	 * @param lines 消えるLinescount (消えなかった場合は0）
	 */
	public void pieceLocked(GameEngine engine, int playerID, int lines) {}

	/**
	 * Line clearが終わるときに呼び出される処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void lineClearEnd(GameEngine engine, int playerID) {}

	/**
	 * Called when saving replay
	 * @param owner GameManagerのインスタンス
	 * @param prop リプレイ保存先のプロパティセット
	 */
	public void saveReplay(GameManager owner, CustomProperties prop) {}

	public void saveReplay(GameManager owner, CustomProperties prop, String foldername) {
		if(owner.mode.isNetplayMode()) return;

		String filename = foldername + "/" + GeneralUtil.getReplayFilename();
		try {
			File repfolder = new File(foldername);
			if (!repfolder.exists()) {
				if (repfolder.mkdir()) {
					log.info("Created replay folder: " + foldername);
				} else {
					log.info("Couldn't create replay folder at "+ foldername);
				}
			}

			FileOutputStream out = new FileOutputStream(filename);
			prop.store(new FileOutputStream(filename), "NullpoMino Replay");
			out.close();
			log.info("Saved replay file: " + filename);
		} catch(IOException e) {
			log.error("Couldn't save replay file to " + filename, e);
		}
	}
}
