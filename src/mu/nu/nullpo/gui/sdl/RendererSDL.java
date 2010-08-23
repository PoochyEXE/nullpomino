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
package mu.nu.nullpo.gui.sdl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import mu.nu.nullpo.game.component.Block;
import mu.nu.nullpo.game.component.Field;
import mu.nu.nullpo.game.component.Piece;
import mu.nu.nullpo.game.event.EventReceiver;
import mu.nu.nullpo.game.play.GameEngine;
import mu.nu.nullpo.game.play.GameManager;
import mu.nu.nullpo.util.CustomProperties;
import mu.nu.nullpo.util.GeneralUtil;

import org.apache.log4j.Logger;

import sdljava.SDLException;
import sdljava.video.SDLRect;
import sdljava.video.SDLSurface;
import sdljava.video.SDLVideo;

/**
 * ゲームのイベント処理と描画処理（SDL版）
 */
public class RendererSDL extends EventReceiver {
	/** Log */
	static Logger log = Logger.getLogger(RendererSDL.class);

	/** フィールドの表示位置(1人・2人のとき) */
	public static final int[] FIELD_OFFSET_X = {32, 432, 432, 432, 432, 432},
							  FIELD_OFFSET_Y = {32, 32, 32, 32, 32, 32};

	/** フィールドの表示位置(3人以上のとき) */
	public static final int[] FIELD_OFFSET_X_MULTI = {119, 247, 375, 503, 247, 375},
							  FIELD_OFFSET_Y_MULTI = {80, 80, 80, 80, 286, 286};

	/** 描画先サーフェイス */
	protected SDLSurface graphics;

	/** 演出オブジェクト */
	protected ArrayList<EffectObjectSDL> effectlist;

	/** 背景表示 */
	protected boolean showbg;

	/** Line clearエフェクト表示 */
	protected boolean showlineeffect;

	/** 重い演出を使う */
	protected boolean heavyeffect;

	/** フィールド背景の明るさ */
	protected int fieldbgbright;

	/** フィールド右側にMeterを表示 */
	protected boolean showmeter;

	/** NEXT欄を暗くする */
	protected boolean darknextarea;

	/** ゴーストピースの上にNEXT表示 */
	protected boolean nextshadow;

	/** 枠線型ゴーストピース */
	protected boolean outlineghost;

	/** Piece previews on sides */
	protected boolean sidenext;

	/**
	 * Constructor
	 */
	public RendererSDL() {
		graphics = null;
		effectlist = new ArrayList<EffectObjectSDL>(10*4);

		showbg = NullpoMinoSDL.propConfig.getProperty("option.showbg", true);
		showlineeffect = NullpoMinoSDL.propConfig.getProperty("option.showlineeffect", true);
		heavyeffect = NullpoMinoSDL.propConfig.getProperty("option.heavyeffect", false);
		fieldbgbright = NullpoMinoSDL.propConfig.getProperty("option.fieldbgbright", 128);
		showmeter = NullpoMinoSDL.propConfig.getProperty("option.showmeter", true);
		darknextarea = NullpoMinoSDL.propConfig.getProperty("option.darknextarea", true);
		nextshadow = NullpoMinoSDL.propConfig.getProperty("option.nextshadow", false);
		outlineghost = NullpoMinoSDL.propConfig.getProperty("option.outlineghost", false);
		sidenext = NullpoMinoSDL.propConfig.getProperty("option.sidenext", false);
	}

	/**
	 * SDL用カラー値を取得
	 * @param r 赤
	 * @param g 緑
	 * @param b 青
	 * @return SDL用カラー値
	 */
	public long getColorValue(int r, int g, int b) {
		try {
			return SDLVideo.mapRGB(graphics.getFormat(), r, g, b);
		} catch (SDLException e) {
			log.debug("SDLException throwed", e);
		}
		return 0;
	}

	/**
	 * Blockの色IDに応じてSDL用カラー値を取得
	 * @param colorID Blockの色ID
	 * @return SDL用カラー値
	 */
	public long getColorByID(int colorID) {
		switch(colorID) {
		case Block.BLOCK_COLOR_GRAY:   return getColorValue( 64, 64, 64);
		case Block.BLOCK_COLOR_RED:    return getColorValue(128,  0,  0);
		case Block.BLOCK_COLOR_ORANGE: return getColorValue(128, 64,  0);
		case Block.BLOCK_COLOR_YELLOW: return getColorValue(128,128,  0);
		case Block.BLOCK_COLOR_GREEN:  return getColorValue(  0,128,  0);
		case Block.BLOCK_COLOR_CYAN:   return getColorValue(  0,128,128);
		case Block.BLOCK_COLOR_BLUE:   return getColorValue(  0,  0,128);
		case Block.BLOCK_COLOR_PURPLE: return getColorValue(128,  0,128);
		}
		return getColorValue(0,0,0);
	}

	/*
	 * メニュー用の文字列を描画
	 */
	@Override
	public void drawMenuFont(GameEngine engine, int playerID, int x, int y, String str, int color, float scale) {
		try {
			int size = (int)(16 * scale);
			int x2 = x * size;
			int y2 = y * size;
			if(!engine.owner.menuOnly) {
				x2 += getFieldDisplayPositionX(engine, playerID) + 4;
				y2 += getFieldDisplayPositionY(engine, playerID) + 52;
			}
			NormalFontSDL.printFont(x2, y2, str, color, scale);
		} catch (SDLException e) {
			log.debug("SDLException throwed", e);
		}
	}

	/*
	 * メニュー用の文字列をTTFフォントで描画
	 */
	@Override
	public void drawTTFMenuFont(GameEngine engine, int playerID, int x, int y, String str, int color) {
		try {
			int x2 = x * 16;
			int y2 = y * 16;
			if(!engine.owner.menuOnly) {
				x2 += getFieldDisplayPositionX(engine, playerID) + 4;
				y2 += getFieldDisplayPositionY(engine, playerID) + 52;
			}
			NormalFontSDL.printTTFFont(x2, y2, str, color);
		} catch (SDLException e) {
			log.debug("SDLException throwed", e);
		}
	}

	/*
	 * Render score用のフォントを描画
	 */
	@Override
	public void drawScoreFont(GameEngine engine, int playerID, int x, int y, String str, int color, float scale) {
		if(engine.owner.menuOnly) return;

		try {
			int size = (int)(16 * scale);
			NormalFontSDL.printFont(getFieldDisplayPositionX(engine, playerID) + 216 + (x * size),
									getFieldDisplayPositionY(engine, playerID) + 48 + (y * size),
									str, color, scale);
		} catch (SDLException e) {
			log.debug("SDLException throwed", e);
		}
	}

	/*
	 * Render score用のフォントをTTFフォントで描画
	 */
	@Override
	public void drawTTFScoreFont(GameEngine engine, int playerID, int x, int y, String str, int color) {
		if(engine.owner.menuOnly) return;

		try {
			NormalFontSDL.printTTFFont(getFieldDisplayPositionX(engine, playerID) + 216 + (x * 16),
									   getFieldDisplayPositionY(engine, playerID) + 48 + (y * 16),
									   str, color);
		} catch (SDLException e) {
			log.debug("SDLException throwed", e);
		}
	}

	/*
	 * 直接指定した座標へ文字列を描画
	 */
	@Override
	public void drawDirectFont(GameEngine engine, int playerID, int x, int y, String str, int color, float scale) {
		try {
			NormalFontSDL.printFont(x, y, str, color, scale);
		} catch (SDLException e) {
			log.debug("SDLException throwed", e);
		}
	}

	/*
	 * 直接指定した座標へ描画できるTTFフォントを描画
	 */
	@Override
	public void drawTTFDirectFont(GameEngine engine, int playerID, int x, int y, String str, int color) {
		try {
			NormalFontSDL.printTTFFont(x, y, str, color);
		} catch (SDLException e) {
			log.debug("SDLException throwed", e);
		}
	}

	/*
	 * スピードMeterを描画
	 */
	@Override
	public void drawSpeedMeter(GameEngine engine, int playerID, int x, int y, int s) {
		if(graphics == null) return;
		if(engine.owner.menuOnly) return;

		int dx1 = getFieldDisplayPositionX(engine, playerID) + 222 + (x * 16);
		int dy1 = getFieldDisplayPositionY(engine, playerID) + 48 + 6 + (y * 16);

		SDLRect rectSrc = new SDLRect(0, 0, 42, 4);
		SDLRect rectDst = new SDLRect(dx1, dy1, 42, 4);

		try {
			ResourceHolderSDL.imgSprite.blitSurface(rectSrc, graphics, rectDst);
		} catch (SDLException e) {
			log.debug("SDLException throwed", e);
		}

		int tempSpeedMeter = s;
		if((tempSpeedMeter < 0) || (tempSpeedMeter > 40)) tempSpeedMeter = 40;

		if(tempSpeedMeter > 0) {
			SDLRect rectSrc2 = new SDLRect(0, 4, tempSpeedMeter, 2);
			SDLRect rectDst2 = new SDLRect(dx1 + 1, dy1 + 1, tempSpeedMeter, 2);

			try {
				ResourceHolderSDL.imgSprite.blitSurface(rectSrc2, graphics, rectDst2);
			} catch (SDLException e) {
				log.debug("SDLException throwed", e);
			}
		}
	}

	/*
	 * TTF使用可能
	 */
	@Override
	public boolean isTTFSupport() {
		return (ResourceHolderSDL.ttfFont != null);
	}

	/*
	 * フィールド右のMeterの最大量
	 */
	@Override
	public int getMeterMax(GameEngine engine) {
		if(!showmeter) return 0;
		int blksize = engine.minidisplay ? 8 : 16;
		return engine.fieldHeight * blksize;
	}

	/*
	 * Blockの画像の幅
	 */
	@Override
	public int getBlockGraphicsWidth(GameEngine engine, int playerID) {
		return engine.minidisplay ? 8 : 16;
	}

	/*
	 * Blockの画像の高さ
	 */
	@Override
	public int getBlockGraphicsHeight(GameEngine engine, int playerID) {
		return engine.minidisplay ? 8 : 16;
	}

	/*
	 * フィールドの表示位置の左端の座標を取得
	 */
	@Override
	public int getFieldDisplayPositionX(GameEngine engine, int playerID) {
		return engine.minidisplay ? FIELD_OFFSET_X_MULTI[playerID] : FIELD_OFFSET_X[playerID];
	}

	/*
	 * フィールドの表示位置の上端の座標を取得
	 */
	@Override
	public int getFieldDisplayPositionY(GameEngine engine, int playerID) {
		return engine.minidisplay ? FIELD_OFFSET_Y_MULTI[playerID] : FIELD_OFFSET_Y[playerID];
	}

	/*
	 * 効果音再生
	 */
	@Override
	public void playSE(String name) {
		ResourceHolderSDL.soundManager.play(name);
	}

	/*
	 * 描画先のサーフェイスを設定
	 */
	@Override
	public void setGraphics(Object g) {
		if(g instanceof SDLSurface) {
			graphics = (SDLSurface)g;
		}
	}

	/*
	 * Mode の設定ファイルを読み込み
	 */
	@Override
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

	/*
	 * Mode の設定ファイルを保存
	 */
	@Override
	public void saveModeConfig(CustomProperties modeConfig) {
		try {
			FileOutputStream out = new FileOutputStream("config/setting/mode.cfg");
			modeConfig.store(out, "NullpoMino Mode Config");
			out.close();
		} catch(IOException e) {
			log.error("Failed to save mode config", e);
		}
	}

	/*
	 * 任意のプロパティセットを読み込み
	 */
	@Override
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

	/*
	 * 任意のプロパティセットを任意のファイル名で保存
	 */
	@Override
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

	/*
	 * リプレイを保存
	 */
	@Override
	public void saveReplay(GameManager owner, CustomProperties prop) {
		if(owner.mode.isNetplayMode()) return;

		String foldername = NullpoMinoSDL.propGlobal.getProperty("custom.replay.directory", "replay");
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

	/*
	 * 1マスBlockを描画
	 */
	@Override
	public void drawSingleBlock(GameEngine engine, int playerID, int x, int y, int color, int skin, boolean bone, float darkness, float alpha, float scale) {
		try {
			drawBlock(x, y, color, skin, bone, darkness, alpha, scale);
		} catch (SDLException e) {
			log.debug("SDLException throwed", e);
		}
	}

	/**
	 * Blockを描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param color 色
	 * @param skin 模様
	 * @param bone 骨Block
	 * @param darkness 暗さもしくは明るさ
	 * @param alpha 透明度
	 * @param scale 拡大率
	 * @throws SDLException 描画に失敗した場合
	 */
	protected void drawBlock(int x, int y, int color, int skin, boolean bone, float darkness, float alpha, float scale) throws SDLException {
		if(graphics == null) return;

		if(color <= Block.BLOCK_COLOR_INVALID) return;
		boolean isSpecialBlocks = (color >= Block.BLOCK_COLOR_COUNT);

		int size = 16;
		SDLSurface img = isSpecialBlocks ? ResourceHolderSDL.imgSpBlock : ResourceHolderSDL.imgBlock;
		if(scale == 0.5f) {
			size = 8;
			img = isSpecialBlocks ? ResourceHolderSDL.imgSpBlockSmall : ResourceHolderSDL.imgBlockSmall;
		}
		if(scale == 2.0f) {
			size = 32;
			img = isSpecialBlocks ? ResourceHolderSDL.imgSpBlockBig : ResourceHolderSDL.imgBlockBig;
		}

		int sx = color * size;
		if(bone) sx += 9 * size;
		int sy = skin * size;

		if(isSpecialBlocks) {
			sx = (color - Block.BLOCK_COLOR_COUNT) * size;
			sy = 0;
		}

		int imageWidth = img.getWidth();
		if((sx >= imageWidth) && (imageWidth != -1)) sx = 0;
		int imageHeight = img.getHeight();
		if((sy >= imageHeight) && (imageHeight != -1)) sy = 0;

		SDLRect rectSrc = new SDLRect(sx, sy, size, size);
		SDLRect rectDst = new SDLRect(x, y, size, size);

		NullpoMinoSDL.fixRect(rectSrc, rectDst);

		if(alpha < 1.0f) {
			int alphalv = (int)(255 * alpha);
			img.setAlpha(SDLVideo.SDL_SRCALPHA | SDLVideo.SDL_RLEACCEL, alphalv);
		} else {
			img.setAlpha(0, 255);
		}

		img.blitSurface(rectSrc, graphics, rectDst);

		if(darkness > 0) {
			int alphalv = (int)(255 * darkness);
			ResourceHolderSDL.imgBlankBlack.setAlpha(SDLVideo.SDL_SRCALPHA | SDLVideo.SDL_RLEACCEL, alphalv);
			ResourceHolderSDL.imgBlankBlack.blitSurface(new SDLRect(0, 0, size, size), graphics, rectDst);
		} else if(darkness < 0) {
			int alphalv = (int)(255 * -darkness);
			ResourceHolderSDL.imgBlankWhite.setAlpha(SDLVideo.SDL_SRCALPHA | SDLVideo.SDL_RLEACCEL, alphalv);
			ResourceHolderSDL.imgBlankWhite.blitSurface(new SDLRect(0, 0, size, size), graphics, rectDst);
		}
	}

	/**
	 * Blockクラスのインスタンスを使用してBlockを描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param blk Blockクラスのインスタンス
	 * @throws SDLException 描画に失敗した場合
	 */
	protected void drawBlock(int x, int y, Block blk) throws SDLException {
		drawBlock(x, y, blk.getDrawColor(), blk.skin, blk.getAttribute(Block.BLOCK_ATTRIBUTE_BONE), blk.darkness, blk.alpha, 1.0f);
	}

	/**
	 * Blockクラスのインスタンスを使用してBlockを描画（拡大率指定可能）
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param blk Blockクラスのインスタンス
	 * @param scale 拡大率
	 * @throws SDLException 描画に失敗した場合
	 */
	protected void drawBlock(int x, int y, Block blk, float scale) throws SDLException {
		drawBlock(x, y, blk.getDrawColor(), blk.skin, blk.getAttribute(Block.BLOCK_ATTRIBUTE_BONE), blk.darkness, blk.alpha, scale);
	}

	/**
	 * Blockクラスのインスタンスを使用してBlockを描画（拡大率と暗さ指定可能）
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param blk Blockクラスのインスタンス
	 * @param scale 拡大率
	 * @param darkness 暗さもしくは明るさ
	 * @throws SDLException 描画に失敗した場合
	 */
	protected void drawBlock(int x, int y, Block blk, float scale, float darkness) throws SDLException {
		drawBlock(x, y, blk.getDrawColor(), blk.skin, blk.getAttribute(Block.BLOCK_ATTRIBUTE_BONE), darkness, blk.alpha, scale);
	}
	
	protected void drawBlockForceVisible(int x, int y, Block blk, float scale) throws SDLException {
		drawBlock(x, y, blk.getDrawColor(), blk.skin, blk.getAttribute(Block.BLOCK_ATTRIBUTE_BONE), blk.darkness,
				(0.5f*blk.alpha)+0.5f, scale);
	}

	/**
	 * Blockピースを描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param piece 描画するピース
	 * @throws SDLException 描画に失敗した場合
	 */
	protected void drawPiece(int x, int y, Piece piece) throws SDLException {
		drawPiece(x, y, piece, 1.0f);
	}

	/**
	 * Blockピースを描画（拡大率指定可能）
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param piece 描画するピース
	 * @param scale 拡大率
	 * @throws SDLException 描画に失敗した場合
	 */
	protected void drawPiece(int x, int y, Piece piece, float scale) throws SDLException {
		drawPiece(x, y, piece, scale, 0f);
	}

	/**
	 * Blockピースを描画（暗さもしくは明るさの指定可能）
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param piece 描画するピース
	 * @param scale 拡大率
	 * @param darkness 暗さもしくは明るさ
	 * @throws SDLException 描画に失敗した場合
	 */
	protected void drawPiece(int x, int y, Piece piece, float scale, float darkness) throws SDLException {
		for(int i = 0; i < piece.getMaxBlock(); i++) {
			int x2 = x + (int)(piece.dataX[piece.direction][i] * 16 * scale);
			int y2 = y + (int)(piece.dataY[piece.direction][i] * 16 * scale);

			Block blkTemp = new Block(piece.block[i]);
			blkTemp.darkness = darkness;

			drawBlock(x2, y2, blkTemp, scale);
		}
	}

	/**
	 * 現在操作中のBlockピースを描画（Y-coordinateが0以上のBlockだけ表示）
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 * @param scale 表示倍率
	 * @throws SDLException 描画に失敗した場合
	 */
	protected void drawCurrentPiece(int x, int y, GameEngine engine, float scale) throws SDLException {
		Piece piece = engine.nowPieceObject;
		int blksize = (int)(16 * scale);

		if(piece != null) {
			for(int i = 0; i < piece.getMaxBlock(); i++) {
				if(piece.big == false) {
					int x2 = engine.nowPieceX + piece.dataX[piece.direction][i];
					int y2 = engine.nowPieceY + piece.dataY[piece.direction][i];

					if(y2 >= 0) {
						Block blkTemp = piece.block[i];
						if(engine.nowPieceColorOverride >= 0) {
							blkTemp = new Block(piece.block[i]);
							blkTemp.color = engine.nowPieceColorOverride;
						}
						drawBlock(x + (x2 * blksize), y + (y2 * blksize), blkTemp, scale);
					}
				} else {
					int x2 = engine.nowPieceX + (piece.dataX[piece.direction][i] * 2);
					int y2 = engine.nowPieceY + (piece.dataY[piece.direction][i] * 2);

					Block blkTemp = piece.block[i];
					if(engine.nowPieceColorOverride >= 0) {
						blkTemp = new Block(piece.block[i]);
						blkTemp.color = engine.nowPieceColorOverride;
					}
					drawBlock(x + (x2 * blksize), y + (y2 * blksize), blkTemp, scale * 2.0f);
				}
			}
		}
	}

	/**
	 * 現在操作中のBlockピースのゴーストを描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 * @param scale 表示倍率
	 * @throws SDLException 描画に失敗した場合
	 */
	protected void drawGhostPiece(int x, int y, GameEngine engine, float scale) throws SDLException {
		Piece piece = engine.nowPieceObject;
		int blksize = (int)(16 * scale);

		if(piece != null) {
			for(int i = 0; i < piece.getMaxBlock(); i++) {
				if(piece.big == false) {
					int x2 = engine.nowPieceX + piece.dataX[piece.direction][i];
					int y2 = engine.nowPieceBottomY + piece.dataY[piece.direction][i];

					if(y2 >= 0) {
						if(outlineghost) {
							Block blkTemp = piece.block[i];
							int x3 = x + (x2 * blksize);
							int y3 = y + (y2 * blksize);

							int colorID = blkTemp.getDrawColor();
							if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_BONE)) colorID = -1;
							long color = getColorByID(colorID);
							graphics.fillRect(new SDLRect(x3, y3, blksize, blksize), color);

							if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP)) {
								ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(43,0,blksize,1), graphics, new SDLRect(x3,y3,blksize,1));
								ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(43,0,blksize,1), graphics, new SDLRect(x3,y3+1,blksize,1));
							}
							if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN)) {
								ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(43,0,blksize,1), graphics, new SDLRect(x3,y3 + blksize-1,blksize,1));
								ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(43,0,blksize,1), graphics, new SDLRect(x3,y3 + blksize-2,blksize,1));
							}
							if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT)) {
								ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(42,0,1,blksize), graphics, new SDLRect(x3,y3,1,blksize));
								ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(42,0,1,blksize), graphics, new SDLRect(x3+1,y3,1,blksize));
							}
							if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT)) {
								ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(42,0,1,blksize), graphics, new SDLRect(x3 + blksize-1,y3,1,blksize));
								ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(42,0,1,blksize), graphics, new SDLRect(x3 + blksize-2,y3,1,blksize));
							}

							color = getColorValue(255, 255, 255);
							if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT | Block.BLOCK_ATTRIBUTE_CONNECT_UP)) {
								graphics.fillRect(new SDLRect(x3, y3, 2, 2), color);
							}
							if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT | Block.BLOCK_ATTRIBUTE_CONNECT_DOWN)) {
								graphics.fillRect(new SDLRect(x3, y3 + (blksize-2), 2, 2), color);
							}
							if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT | Block.BLOCK_ATTRIBUTE_CONNECT_UP)) {
								graphics.fillRect(new SDLRect(x3 + (blksize-2), y3, 2, 2), color);
							}
							if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT | Block.BLOCK_ATTRIBUTE_CONNECT_DOWN)) {
								graphics.fillRect(new SDLRect(x3 + (blksize-2), y3 + (blksize-2), 2, 2), color);
							}
						} else {
							Block blkTemp = new Block(piece.block[i]);
							blkTemp.darkness = 0.3f;
							if(engine.nowPieceColorOverride >= 0) {
								blkTemp.color = engine.nowPieceColorOverride;
							}
							drawBlock(x + (x2 * blksize), y + (y2 * blksize), blkTemp, scale);
						}
					}
				} else {
					int x2 = engine.nowPieceX + (piece.dataX[piece.direction][i] * 2);
					int y2 = engine.nowPieceBottomY + (piece.dataY[piece.direction][i] * 2);

					if(outlineghost) {
						Block blkTemp = piece.block[i];
						int x3 = x + (x2 * blksize);
						int y3 = y + (y2 * blksize);

						int colorID = blkTemp.getDrawColor();
						if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_BONE)) colorID = -1;
						long color = getColorByID(colorID);
						graphics.fillRect(new SDLRect(x3, y3, blksize * 2, blksize * 2), color);

						if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP)) {
							ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(1,16,blksize*2,1), graphics, new SDLRect(x3,y3,blksize*2,1));
							ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(1,16,blksize*2,1), graphics, new SDLRect(x3,y3+1,blksize*2,1));
						}
						if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN)) {
							ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(1,16,blksize*2,1), graphics, new SDLRect(x3,y3 + blksize*2-1,blksize*2,1));
							ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(1,16,blksize*2,1), graphics, new SDLRect(x3,y3 + blksize*2-2,blksize*2,1));
						}
						if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT)) {
							ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(0,16,1,blksize*2), graphics, new SDLRect(x3,y3,1,blksize*2));
							ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(0,16,1,blksize*2), graphics, new SDLRect(x3+1,y3,1,blksize*2));
						}
						if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT)) {
							ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(0,16,1,blksize*2), graphics, new SDLRect(x3 + blksize*2-1,y3,1,blksize*2));
							ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(0,16,1,blksize*2), graphics, new SDLRect(x3 + blksize*2-2,y3,1,blksize*2));
						}

						color = getColorValue(255, 255, 255);
						if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT | Block.BLOCK_ATTRIBUTE_CONNECT_UP)) {
							graphics.fillRect(new SDLRect(x3, y3, 2, 2), color);
						}
						if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT | Block.BLOCK_ATTRIBUTE_CONNECT_DOWN)) {
							graphics.fillRect(new SDLRect(x3, y3 + (blksize*2-2), 2, 2), color);
						}
						if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT | Block.BLOCK_ATTRIBUTE_CONNECT_UP)) {
							graphics.fillRect(new SDLRect(x3 + (blksize*2-2), y3, 2, 2), color);
						}
						if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT | Block.BLOCK_ATTRIBUTE_CONNECT_DOWN)) {
							graphics.fillRect(new SDLRect(x3 + (blksize*2-2), y3 + (blksize*2-2), 2, 2), color);
						}
					} else {
						Block blkTemp = new Block(piece.block[i]);
						blkTemp.darkness = 0.3f;
						if(engine.nowPieceColorOverride >= 0) {
							blkTemp.color = engine.nowPieceColorOverride;
						}
						drawBlock(x + (x2 * blksize), y + (y2 * blksize), blkTemp, scale * 2.0f);
					}
				}
			}
		}
	}

	/**
	 * フィールドのBlockを描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 * @param small 半分サイズ
	 * @throws SDLException 描画に失敗した場合
	 */
	protected void drawField(int x, int y, GameEngine engine, boolean small) throws SDLException {
		if(graphics == null) return;

		int blksize = small ? 8 : 16;

		Field field = engine.field;
		int width = 10;
		int height = 20;
		int viewHeight = 20;

		if(field != null) {
			width = field.getWidth();
			viewHeight = height = field.getHeight();
		}
		if((engine.heboHiddenEnable) && (engine.gameActive) && (field != null)) {
			viewHeight -= engine.heboHiddenYNow;
		}

		for(int i = 0; i < viewHeight; i++) {
			for(int j = 0; j < width; j++) {
				int x2 = x + (j * blksize);
				int y2 = y + (i * blksize);

				Block blk = null;
				if(field != null) blk = field.getBlock(j, i);

				if((field != null) && (blk != null) && (blk.color > Block.BLOCK_COLOR_NONE)) {
					if(blk.getAttribute(Block.BLOCK_ATTRIBUTE_WALL)) {
						drawBlock(x2, y2, Block.BLOCK_COLOR_NONE, blk.skin, blk.getAttribute(Block.BLOCK_ATTRIBUTE_BONE),
								  blk.darkness, blk.alpha, small ? 0.5f : 1.0f);
					} else if (engine.owner.replayMode && engine.owner.replayShowInvisible) {
						drawBlockForceVisible(x2, y2, blk, small ? 0.5f : 1.0f);
					} else if(blk.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE)) {
						drawBlock(x2, y2, blk, small ? 0.5f : 1.0f);
					}

					if( (!blk.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE) || (blk.alpha < 1.0f)) && (fieldbgbright > 0) ) {
						if((showbg) && (engine.owner.getPlayers() < 2))
							ResourceHolderSDL.imgFieldbg.setAlpha(SDLVideo.SDL_SRCALPHA | SDLVideo.SDL_RLEACCEL, fieldbgbright);
						else
							ResourceHolderSDL.imgFieldbg.setAlpha(0, 255);

						int sx = (((i % 2 == 0) && (j % 2 == 0)) || ((i % 2 != 0) && (j % 2 != 0))) ? 0 : 16;
						ResourceHolderSDL.imgFieldbg.blitSurface(new SDLRect(sx,0,blksize,blksize), graphics, new SDLRect(x2,y2,blksize,blksize));
					}

					if(blk.getAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE) && !blk.getAttribute(Block.BLOCK_ATTRIBUTE_BONE)) {
						ResourceHolderSDL.imgSprite.setAlpha(SDLVideo.SDL_SRCALPHA | SDLVideo.SDL_RLEACCEL, (int)(255 * blk.alpha));

						if(engine.blockOutlineType == GameEngine.BLOCK_OUTLINE_NORMAL) {
							if(field.getBlockColor(j, i - 1) == Block.BLOCK_COLOR_NONE)
								ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(43,0,blksize,1), graphics, new SDLRect(x2,y2,blksize,1));
							if(field.getBlockColor(j, i + 1) == Block.BLOCK_COLOR_NONE)
								ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(43,0,blksize,1), graphics, new SDLRect(x2,y2 + blksize-1,blksize,1));
							if(field.getBlockColor(j - 1, i) == Block.BLOCK_COLOR_NONE)
								ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(42,0,1,blksize), graphics, new SDLRect(x2,y2,1,blksize));
							if(field.getBlockColor(j + 1, i) == Block.BLOCK_COLOR_NONE)
								ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(42,0,1,blksize), graphics, new SDLRect(x2 + blksize-1,y2,1,blksize));
						} else if(engine.blockOutlineType == GameEngine.BLOCK_OUTLINE_CONNECT) {
							if(!blk.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP))
								ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(43,0,blksize,1), graphics, new SDLRect(x2,y2,blksize,1));
							if(!blk.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN))
								ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(43,0,blksize,1), graphics, new SDLRect(x2,y2 + blksize-1,blksize,1));
							if(!blk.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT))
								ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(42,0,1,blksize), graphics, new SDLRect(x2,y2,1,blksize));
							if(!blk.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT))
								ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(42,0,1,blksize), graphics, new SDLRect(x2 + blksize-1,y2,1,blksize));
						} else if(engine.blockOutlineType == GameEngine.BLOCK_OUTLINE_SAMECOLOR) {
							if(field.getBlockColor(j, i - 1) != blk.color)
								ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(43,0,blksize,1), graphics, new SDLRect(x2,y2,blksize,1));
							if(field.getBlockColor(j, i + 1) != blk.color)
								ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(43,0,blksize,1), graphics, new SDLRect(x2,y2 + blksize-1,blksize,1));
							if(field.getBlockColor(j - 1, i) != blk.color)
								ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(42,0,1,blksize), graphics, new SDLRect(x2,y2,1,blksize));
							if(field.getBlockColor(j + 1, i) != blk.color)
								ResourceHolderSDL.imgSprite.blitSurface(new SDLRect(42,0,1,blksize), graphics, new SDLRect(x2 + blksize-1,y2,1,blksize));
						}

						ResourceHolderSDL.imgSprite.setAlpha(0, 255);
					}
				} else if(fieldbgbright > 0) {
					if((showbg) && (engine.owner.getPlayers() < 2))
						ResourceHolderSDL.imgFieldbg.setAlpha(SDLVideo.SDL_SRCALPHA | SDLVideo.SDL_RLEACCEL, fieldbgbright);
					else
						ResourceHolderSDL.imgFieldbg.setAlpha(0, 255);

					int dx = (((i % 2 == 0) && (j % 2 == 0)) || ((i % 2 != 0) && (j % 2 != 0))) ? 0 : 16;
					ResourceHolderSDL.imgFieldbg.blitSurface(new SDLRect(dx,0,blksize,blksize), graphics, new SDLRect(x2,y2,blksize,blksize));
				}
			}
		}

		// ヘボHIDDEN
		if((engine.heboHiddenEnable) && (engine.gameActive) && (field != null)) {
			int maxY = engine.heboHiddenYNow;
			if(maxY > height) maxY = height;
			for(int i = 0; i < maxY; i++) {
				for(int j = 0; j < width; j++) {
					drawBlock(x + (j * blksize), y + ((height - 1 - i) * blksize), Block.BLOCK_COLOR_GRAY, 0, false, 0.0f, 1.0f, small ? 0.5f : 1.0f);
				}
			}
		}
	}

	/**
	 * フィールドの枠を描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 * @param small 半分サイズ
	 * @throws SDLException 描画に失敗した場合
	 */
	protected void drawFrame(int x, int y, GameEngine engine, boolean small) throws SDLException {
		if(graphics == null) return;

		int size = small ? 2 : 4;
		int width = 10;
		int height = 20;
		int offsetX = 0;

		if(engine.field != null) {
			width = engine.field.getWidth();
			height = engine.field.getHeight();
		}
		if(engine != null) {
			offsetX = engine.framecolor * 16;
		}

		SDLRect rectSrc = null;
		SDLRect rectDst = null;

		// Upと下
		int maxWidth = (width * size);
		if(showmeter) maxWidth = (width * size) + 2;

		for(int i = 0; i < maxWidth; i++) {
			rectSrc = new SDLRect(offsetX + 4, 0, 4, 4);
			rectDst = new SDLRect(x + ((i + 1) * 4), y, 4, 4);
			ResourceHolderSDL.imgFrame.blitSurface(rectSrc, graphics, rectDst);

			rectSrc = new SDLRect(offsetX + 4, 8, 4, 4);
			rectDst = new SDLRect(x + ((i + 1) * 4), y + (height * size * 4) + 4, 4, 4);
			ResourceHolderSDL.imgFrame.blitSurface(rectSrc, graphics, rectDst);
		}

		// 左と右
		for(int i = 0; i < height * size; i++) {
			rectSrc = new SDLRect(offsetX + 0, 4, 4, 4);
			rectDst = new SDLRect(x, y + ((i + 1) * 4), 4, 4);
			ResourceHolderSDL.imgFrame.blitSurface(rectSrc, graphics, rectDst);

			rectSrc = new SDLRect(offsetX + 8, 4, 4, 4);
			if(showmeter) rectDst = new SDLRect(x + (width * size * 4) + 12, y + ((i + 1) * 4), 4, 4);
			else rectDst = new SDLRect(x + (width * size * 4) + 4, y + ((i + 1) * 4), 4, 4);
			ResourceHolderSDL.imgFrame.blitSurface(rectSrc, graphics, rectDst);
		}

		// 左上
		rectSrc = new SDLRect(offsetX + 0, 0, 4, 4);
		rectDst = new SDLRect(x, y, 4, 4);
		ResourceHolderSDL.imgFrame.blitSurface(rectSrc, graphics, rectDst);

		// 左下
		rectSrc = new SDLRect(offsetX + 0, 8, 4, 4);
		rectDst = new SDLRect(x, y + (height * size * 4) + 4, 4, 4);
		ResourceHolderSDL.imgFrame.blitSurface(rectSrc, graphics, rectDst);

		if(showmeter) {
			// MeterONのときの右上
			rectSrc = new SDLRect(offsetX + 8, 0, 4, 4);
			rectDst = new SDLRect(x + (width * size * 4) + 12, y, 4, 4);
			ResourceHolderSDL.imgFrame.blitSurface(rectSrc, graphics, rectDst);

			// MeterONのときの右下
			rectSrc = new SDLRect(offsetX + 8, 8, 4, 4);
			rectDst = new SDLRect(x + (width * size * 4) + 12, y + (height * size * 4) + 4, 4, 4);
			ResourceHolderSDL.imgFrame.blitSurface(rectSrc, graphics, rectDst);

			// 右Meterの枠
			for(int i = 0; i < height * size; i++) {
				rectSrc = new SDLRect(offsetX + 12, 4, 4, 4);
				rectDst = new SDLRect(x + (width * size * 4) + 4, y + ((i + 1) * 4), 4, 4);
				ResourceHolderSDL.imgFrame.blitSurface(rectSrc, graphics, rectDst);
			}

			rectSrc = new SDLRect(offsetX + 12, 0, 4, 4);
			rectDst = new SDLRect(x + (width * size * 4) + 4, y, 4, 4);
			ResourceHolderSDL.imgFrame.blitSurface(rectSrc, graphics, rectDst);

			rectSrc = new SDLRect(offsetX + 12, 8, 4, 4);
			rectDst = new SDLRect(x + (width * size * 4) + 4, y + (height * size * 4) + 4, 4, 4);
			ResourceHolderSDL.imgFrame.blitSurface(rectSrc, graphics, rectDst);

			// 右Meter
			int maxHeight = height * size * 4;
			if((engine != null) && (engine.meterValue > 0)) maxHeight = (height * size * 4) - engine.meterValue;

			for(int i = 0; i < maxHeight; i++) {
				rectSrc = new SDLRect(59, 0, 4, 1);
				rectDst = new SDLRect(x + (width * size * 4) + 8, y + 4 + i, 4, 1);
				ResourceHolderSDL.imgSprite.blitSurface(rectSrc, graphics, rectDst);
			}

			if((engine != null) && (engine.meterValue > 0)) {
				int value = engine.meterValue;
				if(value > height * size * 4) value = height * size * 4;

				for(int i = 0; i < value; i++) {
					rectSrc = new SDLRect(63 + (engine.meterColor * 4), 0, 4, 1);
					rectDst = new SDLRect(x + (width * size * 4) + 8, y + (height * size * 4) + 3 - i, 4, 1);
					ResourceHolderSDL.imgSprite.blitSurface(rectSrc, graphics, rectDst);
				}
			}
		} else {
			// MeterOFFのときの右上
			rectSrc = new SDLRect(offsetX + 8, 0, 4, 4);
			rectDst = new SDLRect(x + (width * size * 4) + 4, y, 4, 4);
			ResourceHolderSDL.imgFrame.blitSurface(rectSrc, graphics, rectDst);

			// MeterOFFのときの右下
			rectSrc = new SDLRect(offsetX + 8, 8, 4, 4);
			rectDst = new SDLRect(x + (width * size * 4) + 4, y + (height * size * 4) + 4, 4, 4);
			ResourceHolderSDL.imgFrame.blitSurface(rectSrc, graphics, rectDst);
		}
	}

	/**
	 * NEXTを描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 * @throws SDLException 描画に失敗した場合
	 */
	protected void drawNext(int x, int y, GameEngine engine) throws SDLException {
		if(graphics == null) return;

		// NEXT欄背景
		if(showbg && darknextarea) {
			if(sidenext) {
				int x2 = showmeter ? (x + 176) : (x + 168);
				int maxNext = engine.isNextVisible ? engine.ruleopt.nextDisplay : 0;

				// HOLD area
				if(engine.ruleopt.holdEnable && engine.isHoldVisible) {
					ResourceHolderSDL.imgBlankBlack.setAlpha(0, 255);
					ResourceHolderSDL.imgBlankBlack.blitSurface(new SDLRect(0,0,32,32 - 16), graphics, new SDLRect(x - 32, y + 48 + 8, 32, 32 - 16));

					for(int i = 0; i <= 8; i++) {
						int alpha = (int)(((float)i / (float)8) * 255);
						ResourceHolderSDL.imgBlankBlack.setAlpha(SDLVideo.SDL_SRCALPHA | SDLVideo.SDL_RLEACCEL, alpha);
						ResourceHolderSDL.imgBlankBlack.blitSurface(new SDLRect(0,0,32,1), graphics, new SDLRect(x - 32, y + 47 + i, 32, 1));
					}
					for(int i = 0; i <= 8; i++) {
						int alpha = (int)(((float)i / (float)8) * 255);
						ResourceHolderSDL.imgBlankBlack.setAlpha(SDLVideo.SDL_SRCALPHA | SDLVideo.SDL_RLEACCEL, alpha);
						ResourceHolderSDL.imgBlankBlack.blitSurface(new SDLRect(0,0,32,1), graphics, new SDLRect(x - 32, y + 80 - i, 32, 1));
					}
				}

				// NEXT area
				if(maxNext > 0) {
					ResourceHolderSDL.imgBlankBlack.setAlpha(0, 255);
					ResourceHolderSDL.imgBlankBlack.blitSurface(new SDLRect(0,0,32,(32 * maxNext)-16),graphics,
							new SDLRect(x2,y + 48 + 8,32,(32 * maxNext) - 16));

					for(int i = 0; i <= 8; i++) {
						int alpha = (int)(((float)i / (float)8) * 255);
						ResourceHolderSDL.imgBlankBlack.setAlpha(SDLVideo.SDL_SRCALPHA | SDLVideo.SDL_RLEACCEL, alpha);
						ResourceHolderSDL.imgBlankBlack.blitSurface(new SDLRect(0,0,32,1), graphics, new SDLRect(x2, y + 47 + i, 32, 1));
					}
					for(int i = 0; i <= 8; i++) {
						int alpha = (int)(((float)i / (float)8) * 255);
						ResourceHolderSDL.imgBlankBlack.setAlpha(SDLVideo.SDL_SRCALPHA | SDLVideo.SDL_RLEACCEL, alpha);
						ResourceHolderSDL.imgBlankBlack.blitSurface(new SDLRect(0,0,32,1), graphics, new SDLRect(x2, y + 48+(32*maxNext)-i, 32, 1));
					}
				}
			} else {
				ResourceHolderSDL.imgBlankBlack.setAlpha(0, 255);
				ResourceHolderSDL.imgBlankBlack.blitSurface(new SDLRect(0,0,135,48), graphics, new SDLRect(x + 20, y, 135, 48));

				for(int i = 0; i <= 20; i++) {
					int alpha = (int)(((float)i / (float)20) * 255);
					ResourceHolderSDL.imgBlankBlack.setAlpha(SDLVideo.SDL_SRCALPHA | SDLVideo.SDL_RLEACCEL, alpha);
					ResourceHolderSDL.imgBlankBlack.blitSurface(new SDLRect(0,0,1,48), graphics, new SDLRect(x + i - 1, y, 1, 48));
				}
				for(int i = 0; i <= 20; i++) {
					int alpha = (int)(((float)(20 - i) / (float)20) * 255);
					ResourceHolderSDL.imgBlankBlack.setAlpha(SDLVideo.SDL_SRCALPHA | SDLVideo.SDL_RLEACCEL, alpha);
					ResourceHolderSDL.imgBlankBlack.blitSurface(new SDLRect(0,0,1,48), graphics, new SDLRect(x + i + 155, y, 1, 48));
				}
			}

			ResourceHolderSDL.imgBlankBlack.setAlpha(0, 255);
		}

		if(engine.isNextVisible) {
			if(sidenext) {
				if(engine.ruleopt.nextDisplay >= 1) {
					int x2 = showmeter ? (x + 176) : (x + 168);
					NormalFontSDL.printFont(x2, y + 40, NullpoMinoSDL.getUIText("InGame_Next"), COLOR_ORANGE, 0.5f);

					for(int i = 0; i < engine.ruleopt.nextDisplay; i++) {
						Piece piece = engine.getNextObject(engine.nextPieceCount + i);

						if(piece != null) {
							int centerX = ( (32 - ((piece.getWidth() + 1) * 8)) / 2 ) - (piece.getMinimumBlockX() * 8);
							int centerY = ( (32 - ((piece.getHeight() + 1) * 8)) / 2 ) - (piece.getMinimumBlockY() * 8);
							drawPiece(x2 + centerX, y + 48 + (i * 32) + centerY, piece, 0.5f);
						}
					}
				}
			} else {
				// NEXT1
				if(engine.ruleopt.nextDisplay >= 1) {
					Piece piece = engine.getNextObject(engine.nextPieceCount);
					NormalFontSDL.printFont(x + 60, y, NullpoMinoSDL.getUIText("InGame_Next"), COLOR_ORANGE, 0.5f);

					if(piece != null) {
						//int x2 = x + 4 + ((-1 + (engine.field.getWidth() - piece.getWidth() + 1) / 2) * 16);
					   int x2 = x + 4 + engine.getSpawnPosX(engine.field, piece) * 16; //Rules with spawn x modified were misaligned.
						int y2 = y + 48 - ((piece.getMaximumBlockY() + 1) * 16);
						drawPiece(x2, y2, piece);
					}
				}

				// NEXT2・3
				for(int i = 0; i < engine.ruleopt.nextDisplay - 1; i++) {
					if(i >= 2) break;

					Piece piece = engine.getNextObject(engine.nextPieceCount + i + 1);

					if(piece != null) {
						drawPiece(x + 124 + (i * 40), y + 48 - ((piece.getMaximumBlockY() + 1) * 8), piece, 0.5f);
					}
				}

				// NEXT4～
				for(int i = 0; i < engine.ruleopt.nextDisplay - 3; i++) {
					Piece piece = engine.getNextObject(engine.nextPieceCount + i + 3);

					if(piece != null) {
						if(showmeter)
							drawPiece(x + 176, y + (i * 40) + 88 - ((piece.getMaximumBlockY() + 1) * 8), piece, 0.5f);
						else
							drawPiece(x + 168, y + (i * 40) + 88 - ((piece.getMaximumBlockY() + 1) * 8), piece, 0.5f);
					}
				}
			}
		}

		if(engine.isHoldVisible) {
			// HOLD
			int holdRemain = engine.ruleopt.holdLimit - engine.holdUsedCount;
			int x2 = sidenext ? (x - 32) : x;
			int y2 = sidenext ? (y + 40) : y;

			if( (engine.ruleopt.holdEnable == true) && ((engine.ruleopt.holdLimit < 0) || (holdRemain > 0)) ) {
				int tempColor = COLOR_GREEN;
				if(engine.holdDisable == true) tempColor = COLOR_WHITE;

				if(engine.ruleopt.holdLimit < 0) {
					NormalFontSDL.printFont(x2, y2, NullpoMinoSDL.getUIText("InGame_Hold"), tempColor, 0.5f);
				} else {
					if(!engine.holdDisable) {
						if((holdRemain > 0) && (holdRemain <= 10)) tempColor = COLOR_YELLOW;
						if((holdRemain > 0) && (holdRemain <= 5)) tempColor = COLOR_RED;
					}

					NormalFontSDL.printFont(x2, y2, NullpoMinoSDL.getUIText("InGame_Hold") + "\ne " + holdRemain, tempColor, 0.5f);
				}

				if(engine.holdPieceObject != null) {
					float dark = 0f;
					if(engine.holdDisable == true) dark = 0.3f;
					Piece piece = new Piece(engine.holdPieceObject);
					piece.resetOffsetArray();

					if(sidenext) {
						int centerX = ( (32 - ((piece.getWidth() + 1) * 8)) / 2 ) - (piece.getMinimumBlockX() * 8);
						int centerY = ( (32 - ((piece.getHeight() + 1) * 8)) / 2 ) - (piece.getMinimumBlockY() * 8);
						drawPiece(x2 + centerX, y + 48 + centerY, piece, 0.5f, dark);
					} else {
						drawPiece(x2, y + 48 - ((piece.getMaximumBlockY() + 1) * 8), piece, 0.5f, dark);
					}
				}
			}
		}
	}

	/**
	 * Draw shadow nexts
	 * @param x X coord
	 * @param y Y coord
	 * @param engine GameEngine
	 * @param scale Display size of piece
	 * @author Wojtek
	 * @throws SDLException
	 */
	protected void drawShadowNexts(int x, int y, GameEngine engine, float scale) throws SDLException {
		Piece piece = engine.nowPieceObject;
		int blksize = (int) (16 * scale);

		if (piece != null) {
			int shadowX = engine.nowPieceX;
			int shadowY = engine.nowPieceBottomY + piece.getMinimumBlockY();

			for (int i = 0; i < engine.ruleopt.nextDisplay - 1; i++) {
				if (i >= 3)
					break;

				Piece next = engine.getNextObject(engine.nextPieceCount + i);

				if (next != null) {
					int size = (piece.big ? 2 : 1);
					int shadowCenter = blksize * piece.getMinimumBlockX() + blksize
							* (piece.getWidth() + size) / 2;
					int nextCenter = blksize / 2 * next.getMinimumBlockX() + blksize / 2
							* (next.getWidth() + 1) / 2;
					int vPos = blksize * shadowY - (i + 1) * 24 - 8;

					if (vPos >= -blksize / 2)
						drawPiece(x + blksize * shadowX + shadowCenter - nextCenter, y
								+ vPos, next, 0.5f * scale, 0.1f);
				}
			}
		}
	}

	/*
	 * 各フレーム最初の描画処理
	 */
	@Override
	public void renderFirst(GameEngine engine, int playerID) {
		try {
			// 背景
			if(playerID == 0) {
				if(engine.owner.menuOnly) {
					ResourceHolderSDL.imgMenu.blitSurface(graphics);
				} else {
					int bg = engine.owner.backgroundStatus.bg;
					if(engine.owner.backgroundStatus.fadesw && !heavyeffect) {
						bg = engine.owner.backgroundStatus.fadebg;
					}

					if((ResourceHolderSDL.imgPlayBG != null) && (bg >= 0) && (bg < ResourceHolderSDL.imgPlayBG.length) && (showbg == true)) {
						ResourceHolderSDL.imgPlayBG[bg].blitSurface(graphics);

						if(engine.owner.backgroundStatus.fadesw && heavyeffect) {
							int alphalv = engine.owner.backgroundStatus.fadestat ? (100 - engine.owner.backgroundStatus.fadecount) : engine.owner.backgroundStatus.fadecount;
							ResourceHolderSDL.imgBlankBlack.setAlpha(SDLVideo.SDL_SRCALPHA | SDLVideo.SDL_RLEACCEL, alphalv * 2);
							ResourceHolderSDL.imgBlankBlack.blitSurface(graphics);
						}
					} else {
						graphics.fillRect(SDLVideo.mapRGB(graphics.getFormat(), 0, 0, 0));
					}
				}
			}

			// NEXTなど
			if(!engine.owner.menuOnly && engine.isVisible) {
				int offsetX = getFieldDisplayPositionX(engine, playerID);
				int offsetY = getFieldDisplayPositionY(engine, playerID);

				if(!engine.minidisplay) {
					drawNext(offsetX, offsetY, engine);
					drawFrame(offsetX, offsetY + 48, engine, false);
					drawField(offsetX + 4, offsetY + 52, engine, false);
				} else {
					drawFrame(offsetX, offsetY, engine, true);
					drawField(offsetX + 4, offsetY + 4, engine, true);
				}
			}
		} catch (SDLException e) {
			log.debug("SDLException throwed", e);
		}
	}

	/*
	 * Ready画面の描画処理
	 */
	@Override
	public void renderReady(GameEngine engine, int playerID) {
		if(graphics == null) return;
		if(engine.allowTextRenderByReceiver == false) return;
		//if(!engine.isVisible) return;

		try {
			int offsetX = getFieldDisplayPositionX(engine, playerID);
			int offsetY = getFieldDisplayPositionY(engine, playerID);

			if(engine.statc[0] > 0) {
				if(!engine.minidisplay) {
					if((engine.statc[0] >= engine.readyStart) && (engine.statc[0] < engine.readyEnd))
						NormalFontSDL.printFont(offsetX + 44, offsetY + 204, "READY", COLOR_WHITE, 1.0f);
					else if((engine.statc[0] >= engine.goStart) && (engine.statc[0] < engine.goEnd))
						NormalFontSDL.printFont(offsetX + 62, offsetY + 204, "GO!", COLOR_WHITE, 1.0f);
				} else {
					if((engine.statc[0] >= engine.readyStart) && (engine.statc[0] < engine.readyEnd))
						NormalFontSDL.printFont(offsetX + 24, offsetY + 80, "READY", COLOR_WHITE, 0.5f);
					else if((engine.statc[0] >= engine.goStart) && (engine.statc[0] < engine.goEnd))
						NormalFontSDL.printFont(offsetX + 32, offsetY + 80, "GO!", COLOR_WHITE, 0.5f);
				}
			}
		} catch (SDLException e) {
			log.debug("SDLException throwed", e);
		}
	}

	/*
	 * Blockピース移動時の処理
	 */
	@Override
	public void renderMove(GameEngine engine, int playerID) {
		try {
			if(!engine.isVisible) return;

			int offsetX = getFieldDisplayPositionX(engine, playerID);
			int offsetY = getFieldDisplayPositionY(engine, playerID);

			if((engine.statc[0] > 1) || (engine.ruleopt.moveFirstFrame)) {
				if(!engine.minidisplay) {
					if(nextshadow) drawShadowNexts(offsetX + 4, offsetY + 52, engine, 1.0f);
					if(engine.ghost && engine.ruleopt.ghost) drawGhostPiece(offsetX + 4, offsetY + 52, engine, 1.0f);
					drawCurrentPiece(offsetX + 4, offsetY + 52, engine, 1.0f);
				} else {
					if(engine.ghost && engine.ruleopt.ghost) drawGhostPiece(offsetX + 4, offsetY + 4, engine, 0.5f);
					drawCurrentPiece(offsetX + 4, offsetY + 4, engine, 0.5f);
				}
			}
		} catch (SDLException e) {
			log.debug("SDLException throwed", e);
		}
	}

	/*
	 * Blockを消す演出を出すときの処理
	 */
	@Override
	public void blockBreak(GameEngine engine, int playerID, int x, int y, Block blk) {
		if(showlineeffect && !engine.minidisplay) {
			int color = blk.getDrawColor();
			// 通常Block
			if((color >= Block.BLOCK_COLOR_GRAY) && (color <= Block.BLOCK_COLOR_PURPLE) && !blk.getAttribute(Block.BLOCK_ATTRIBUTE_BONE)) {
				EffectObjectSDL obj =
					new EffectObjectSDL(1,
										getFieldDisplayPositionX(engine, playerID) + 4 + (x * 16),
										getFieldDisplayPositionY(engine, playerID) + 52 + (y * 16),
										color);
				effectlist.add(obj);
			}
			// 宝石Block
			else if(blk.isGemBlock()) {
				EffectObjectSDL obj =
					new EffectObjectSDL(2,
										getFieldDisplayPositionX(engine, playerID) + 4 + (x * 16),
										getFieldDisplayPositionY(engine, playerID) + 52 + (y * 16),
										color);
				effectlist.add(obj);
			}
		}
	}

	/*
	 * EXCELLENT画面の描画処理
	 */
	@Override
	public void renderExcellent(GameEngine engine, int playerID) {
		if(graphics == null) return;
		if(engine.allowTextRenderByReceiver == false) return;
		if(!engine.isVisible) return;

		try {
			int offsetX = getFieldDisplayPositionX(engine, playerID);
			int offsetY = getFieldDisplayPositionY(engine, playerID);

			if(!engine.minidisplay) {
				if(engine.statc[1] == 0)
					NormalFontSDL.printFont(offsetX + 4, offsetY + 204, "EXCELLENT!", COLOR_ORANGE, 1.0f);
				else if(engine.owner.getPlayers() < 3)
					NormalFontSDL.printFont(offsetX + 52, offsetY + 204, "WIN!", COLOR_ORANGE, 1.0f);
				else
					NormalFontSDL.printFont(offsetX + 4, offsetY + 204, "1ST PLACE!", COLOR_ORANGE, 1.0f);
			} else {
				if(engine.statc[1] == 0)
					NormalFontSDL.printFont(offsetX + 4, offsetY + 80, "EXCELLENT!", COLOR_ORANGE, 0.5f);
				else if(engine.owner.getPlayers() < 3)
					NormalFontSDL.printFont(offsetX + 33, offsetY + 80, "WIN!", COLOR_ORANGE, 0.5f);
				else
					NormalFontSDL.printFont(offsetX + 4, offsetY + 80, "1ST PLACE!", COLOR_ORANGE, 0.5f);
			}
		} catch (SDLException e) {
			log.debug("SDLException throwed", e);
		}
	}

	/*
	 * ゲームオーバー画面の描画処理
	 */
	@Override
	public void renderGameOver(GameEngine engine, int playerID) {
		if(graphics == null) return;
		if(engine.allowTextRenderByReceiver == false) return;
		if(!engine.isVisible) return;

		if((engine.statc[0] >= engine.field.getHeight() + 1) && (engine.statc[0] < engine.field.getHeight() + 1 + 180))
			try {
				int offsetX = getFieldDisplayPositionX(engine, playerID);
				int offsetY = getFieldDisplayPositionY(engine, playerID);

				if(!engine.minidisplay) {
					if(engine.owner.getPlayers() < 2)
						NormalFontSDL.printFont(offsetX + 12, offsetY + 204, "GAME OVER", COLOR_WHITE, 1.0f);
					else if(engine.owner.getWinner() == -2)
						NormalFontSDL.printFont(offsetX + 52, offsetY + 204, "DRAW", COLOR_GREEN, 1.0f);
					else if(engine.owner.getPlayers() < 3)
						NormalFontSDL.printFont(offsetX + 52, offsetY + 204, "LOSE", COLOR_WHITE, 1.0f);
				} else {
					if(engine.owner.getPlayers() < 2)
						NormalFontSDL.printFont(offsetX + 4, offsetY + 80, "GAME OVER", COLOR_WHITE, 0.5f);
					else if(engine.owner.getWinner() == -2)
						NormalFontSDL.printFont(offsetX + 28, offsetY + 80, "DRAW", COLOR_GREEN, 0.5f);
					else if(engine.owner.getPlayers() < 3)
						NormalFontSDL.printFont(offsetX + 28, offsetY + 80, "LOSE", COLOR_WHITE, 0.5f);
				}
			} catch (SDLException e) {
				log.debug("SDLException throwed", e);
			}
	}

	/*
	 * Render results screen処理
	 */
	@Override
	public void renderResult(GameEngine engine, int playerID) {
		if(graphics == null) return;
		if(engine.allowTextRenderByReceiver == false) return;
		if(!engine.isVisible) return;

		try {
			int offsetX = getFieldDisplayPositionX(engine, playerID);
			int offsetY = getFieldDisplayPositionY(engine, playerID);

			int tempColor;

			if(engine.statc[0] == 0)
				tempColor = COLOR_RED;
			else
				tempColor = COLOR_WHITE;
			NormalFontSDL.printFont(offsetX + 12, offsetY + 340, "RETRY", tempColor, 1.0f);

			if(engine.statc[0] == 1)
				tempColor = COLOR_RED;
			else
				tempColor = COLOR_WHITE;
			NormalFontSDL.printFont(offsetX + 108, offsetY + 340, "END", tempColor, 1.0f);
		} catch (SDLException e) {
			log.debug("SDLException throwed", e);
		}
	}

	/*
	 * フィールドエディット画面の描画処理
	 */
	@Override
	public void renderFieldEdit(GameEngine engine, int playerID) {
		if(graphics == null) return;

		try {
			int x = getFieldDisplayPositionX(engine, playerID) + 4 + (engine.fldeditX * 16);
			int y = getFieldDisplayPositionY(engine, playerID) + 52 + (engine.fldeditY * 16);
			float bright = (engine.fldeditFrames % 60 >= 30) ? -0.5f : -0.2f;
			drawBlock(x, y, engine.fldeditColor, engine.getSkin(), false, bright, 1.0f, 1.0f);
		} catch (SDLException e) {
			log.debug("SDLException throwed", e);
		}
	}

	/*
	 * 各フレームの最後に行われる処理
	 */
	@Override
	public void onLast(GameEngine engine, int playerID) {
		if(playerID == engine.owner.getPlayers() - 1) effectUpdate();
	}

	/*
	 * 各フレームの最後に行われる描画処理
	 */
	@Override
	public void renderLast(GameEngine engine, int playerID) {
		if(playerID == engine.owner.getPlayers() - 1) effectRender();
	}

	/**
	 * 画面演出の更新
	 */
	protected void effectUpdate() {
		boolean emptyflag = true;

		for(int i = 0; i < effectlist.size(); i++) {
			EffectObjectSDL obj = effectlist.get(i);

			if(obj.effect != 0) emptyflag = false;

			// 通常Block
			if(obj.effect == 1) {
				obj.anim++;
				if(obj.anim >= 36) obj.effect = 0;
			}
			// 宝石Block
			if(obj.effect == 2) {
				obj.anim++;
				if(obj.anim >= 60) obj.effect = 0;
			}
		}

		if(emptyflag) effectlist.clear();
	}

	/**
	 * 画面演出を描画
	 */
	protected void effectRender() {
		for(int i = 0; i < effectlist.size(); i++) {
			EffectObjectSDL obj = effectlist.get(i);

			// 通常Block
			if(obj.effect == 1) {
				int x = obj.x - 40;
				int y = obj.y - 15;
				int color = obj.param - Block.BLOCK_COLOR_GRAY;

				int srcx = ((obj.anim-1) % 6) * 96;
				int srcy = ((obj.anim-1) / 6) * 96;
				if(obj.anim >= 30) {
					srcx = ((obj.anim-30) % 6) * 96;
					srcy = ((obj.anim-30) / 6) * 96;
				}

				SDLRect rectSrc = new SDLRect(srcx, srcy, 96, 96);
				SDLRect rectDst = new SDLRect(x, y, 96, 96);
				NullpoMinoSDL.fixRect(rectSrc, rectDst);

				try {
					if(ResourceHolderSDL.imgBreak != null) {
						if(obj.anim < 30) {
							ResourceHolderSDL.imgBreak[color][0].blitSurface(rectSrc, graphics, rectDst);
						} else {
							ResourceHolderSDL.imgBreak[color][1].blitSurface(rectSrc, graphics, rectDst);
						}
					}
				} catch (SDLException e) {
					log.debug("SDLException throwed", e);
				}
			}
			// 宝石Block
			if(obj.effect == 2) {
				int x = obj.x - 8;
				int y = obj.y - 8;
				int srcx = ((obj.anim-1) % 10) * 32;
				int srcy = ((obj.anim-1) / 10) * 32;
				int color = obj.param - Block.BLOCK_COLOR_GEM_RED;

				SDLRect rectSrc = new SDLRect(srcx, srcy, 32, 32);
				SDLRect rectDst = new SDLRect(x, y, 32, 32);
				NullpoMinoSDL.fixRect(rectSrc, rectDst);

				try {
					if(ResourceHolderSDL.imgPErase != null) {
						ResourceHolderSDL.imgPErase[color].blitSurface(rectSrc, graphics, rectDst);
					}
				} catch (SDLException e) {
					log.debug("SDLException throwed", e);
				}
			}
		}
	}
}
