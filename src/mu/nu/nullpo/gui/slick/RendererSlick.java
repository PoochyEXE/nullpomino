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
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;

/**
 * ゲームのイベント処理と描画処理（Slick版）
 */
public class RendererSlick extends EventReceiver {
	/** Log */
	static Logger log = Logger.getLogger(RendererSlick.class);

	/** フィールドの表示位置(1人・2人のとき) */
	public static final int[] FIELD_OFFSET_X = {32, 432, 432, 432, 432, 432},
							  FIELD_OFFSET_Y = {32, 32, 32, 32, 32, 32};

	/** フィールドの表示位置(3人以上のとき) */
	public static final int[] FIELD_OFFSET_X_MULTI = {119, 247, 375, 503, 247, 375},
							  FIELD_OFFSET_Y_MULTI = {80, 80, 80, 80, 286, 286};

	/** 描画先サーフェイス */
	protected Graphics graphics;

	/** 演出オブジェクト */
	protected ArrayList<EffectObject> effectlist;

	/** 背景表示 */
	protected boolean showbg;

	/** Line clearエフェクト表示 */
	protected boolean showlineeffect;

	/** 重い演出を使う */
	protected boolean heavyeffect;

	/** フィールド背景の明るさ */
	protected float fieldbgbright;

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
	 * Blockの色IDに応じてSlick用Colorオブジェクトを作成・取得
	 * @param colorID Blockの色ID
	 * @return Slick用Colorオブジェクト
	 */
	public static Color getColorByID(int colorID) {
		switch(colorID) {
		case Block.BLOCK_COLOR_GRAY:   return new Color(Color.gray);
		case Block.BLOCK_COLOR_RED:    return new Color(Color.red);
		case Block.BLOCK_COLOR_ORANGE: return new Color(255,128,0);
		case Block.BLOCK_COLOR_YELLOW: return new Color(Color.yellow);
		case Block.BLOCK_COLOR_GREEN:  return new Color(Color.green);
		case Block.BLOCK_COLOR_CYAN:   return new Color(Color.cyan);
		case Block.BLOCK_COLOR_BLUE:   return new Color(Color.blue);
		case Block.BLOCK_COLOR_PURPLE: return new Color(Color.magenta);
		}
		return new Color(Color.black);
	}

	/**
	 * Constructor
	 */
	public RendererSlick() {
		graphics = null;
		effectlist = new ArrayList<EffectObject>(10*4);

		showbg = NullpoMinoSlick.propConfig.getProperty("option.showbg", true);
		showlineeffect = NullpoMinoSlick.propConfig.getProperty("option.showlineeffect", true);
		heavyeffect = NullpoMinoSlick.propConfig.getProperty("option.heavyeffect", false);
		int bright = NullpoMinoSlick.propConfig.getProperty("option.fieldbgbright", 64);
		fieldbgbright = bright / (float)128;
		showmeter = NullpoMinoSlick.propConfig.getProperty("option.showmeter", true);
		darknextarea = NullpoMinoSlick.propConfig.getProperty("option.darknextarea", true);
		nextshadow = NullpoMinoSlick.propConfig.getProperty("option.nextshadow", false);
		outlineghost = NullpoMinoSlick.propConfig.getProperty("option.outlineghost", false);
		sidenext = NullpoMinoSlick.propConfig.getProperty("option.sidenext", false);
	}

	/*
	 * メニュー用の文字列を描画
	 */
	@Override
	public void drawMenuFont(GameEngine engine, int playerID, int x, int y, String str, int color, float scale) {
		int size = (int)(16 * scale);
		int x2 = x * size;
		int y2 = y * size;
		if(!engine.owner.menuOnly) {
			x2 += getFieldDisplayPositionX(engine, playerID) + 4;
			y2 += getFieldDisplayPositionY(engine, playerID) + 52;
		}
		NormalFont.printFont(x2, y2, str, color, scale);
	}

	/*
	 * メニュー用の文字列をTTFフォントで描画
	 */
	@Override
	public void drawTTFMenuFont(GameEngine engine, int playerID, int x, int y, String str, int color) {
		int x2 = x * 16;
		int y2 = y * 16;
		if(!engine.owner.menuOnly) {
			x2 += getFieldDisplayPositionX(engine, playerID) + 4;
			y2 += getFieldDisplayPositionY(engine, playerID) + 52;
		}
		NormalFont.printTTFFont(x2, y2, str, color);
	}

	/*
	 * Render score用のフォントを描画
	 */
	@Override
	public void drawScoreFont(GameEngine engine, int playerID, int x, int y, String str, int color, float scale) {
		if(engine.owner.menuOnly) return;
		int size = (int)(16 * scale);
		NormalFont.printFont(getFieldDisplayPositionX(engine, playerID) + 216 + (x * size),
							 getFieldDisplayPositionY(engine, playerID) + 48 + (y * size),
							 str, color, scale);
	}

	/*
	 * Render score用のフォントをTTFフォントで描画
	 */
	@Override
	public void drawTTFScoreFont(GameEngine engine, int playerID, int x, int y, String str, int color) {
		if(engine.owner.menuOnly) return;
		NormalFont.printTTFFont(getFieldDisplayPositionX(engine, playerID) + 216 + (x * 16),
								getFieldDisplayPositionY(engine, playerID) + 48 + (y * 16),
								str, color);
	}

	/*
	 * 直接指定した座標へ文字列を描画
	 */
	@Override
	public void drawDirectFont(GameEngine engine, int playerID, int x, int y, String str, int color, float scale) {
		NormalFont.printFont(x, y, str, color, scale);
	}

	/*
	 * 直接指定した座標へ描画できるTTFフォントを描画
	 */
	@Override
	public void drawTTFDirectFont(GameEngine engine, int playerID, int x, int y, String str, int color) {
		NormalFont.printTTFFont(x, y, str, color);
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

		graphics.setColor(Color.black);
		graphics.drawRect(dx1, dy1, 41, 3);
		graphics.setColor(Color.green);
		graphics.fillRect(dx1 + 1, dy1 + 1, 40, 2);

		int tempSpeedMeter = s;
		if((tempSpeedMeter < 0) || (tempSpeedMeter > 40)) tempSpeedMeter = 40;

		if(tempSpeedMeter > 0) {
			graphics.setColor(Color.red);
			graphics.fillRect(dx1 + 1, dy1 + 1, tempSpeedMeter, 2);
		}

		graphics.setColor(Color.white);
	}

	/*
	 * TTF使用可能
	 */
	@Override
	public boolean isTTFSupport() {
		return (ResourceHolder.ttfFont != null);
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
		ResourceHolder.soundManager.play(name);
	}

	/*
	 * 描画先のサーフェイスを設定
	 */
	@Override
	public void setGraphics(Object g) {
		if(g instanceof Graphics) {
			graphics = (Graphics)g;
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
			log.error("Couldn't save mode config", e);
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

		String foldername = NullpoMinoSlick.propGlobal.getProperty("custom.replay.directory", "replay");
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
		drawBlock(x, y, color, skin, bone, darkness, alpha, scale);
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
	 */
	protected void drawBlock(int x, int y, int color, int skin, boolean bone, float darkness, float alpha, float scale) {
		if(graphics == null) return;

		if((color <= Block.BLOCK_COLOR_INVALID)) return;
		boolean isSpecialBlocks = (color >= Block.BLOCK_COLOR_COUNT);

		int size = 16;
		Image img = isSpecialBlocks ? ResourceHolder.imgSpBlock : ResourceHolder.imgBlock;
		if(scale == 0.5f) {
			size = 8;
			img = isSpecialBlocks ? ResourceHolder.imgSpBlockSmall : ResourceHolder.imgBlockSmall;
		}
		if(scale == 2.0f) {
			size = 32;
			img = isSpecialBlocks ? ResourceHolder.imgSpBlockBig : ResourceHolder.imgBlockBig;
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

		Color filter = new Color(Color.white);
		filter.a = alpha;
		if(darkness > 0) {
			filter = filter.darker(darkness);
		}

		graphics.drawImage(img, x, y, x + size, y + size, sx, sy, sx + size, sy + size, filter);

		if(darkness < 0) {
			Color brightfilter = new Color(Color.white);
			brightfilter.a = -darkness;
			graphics.setColor(brightfilter);
			graphics.fillRect(x, y, size, size);
		}
	}

	/**
	 * Blockクラスのインスタンスを使用してBlockを描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param blk Blockクラスのインスタンス
	 */
	protected void drawBlock(int x, int y, Block blk) {
		drawBlock(x, y, blk.getDrawColor(), blk.skin, blk.getAttribute(Block.BLOCK_ATTRIBUTE_BONE), blk.darkness, blk.alpha, 1.0f);
	}

	/**
	 * Blockクラスのインスタンスを使用してBlockを描画（拡大率指定可能）
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param blk Blockクラスのインスタンス
	 * @param scale 拡大率
	 */
	protected void drawBlock(int x, int y, Block blk, float scale) {
		drawBlock(x, y, blk.getDrawColor(), blk.skin, blk.getAttribute(Block.BLOCK_ATTRIBUTE_BONE), blk.darkness, blk.alpha, scale);
	}

	/**
	 * Blockクラスのインスタンスを使用してBlockを描画（拡大率と暗さ指定可能）
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param blk Blockクラスのインスタンス
	 * @param scale 拡大率
	 * @param darkness 暗さもしくは明るさ
	 */
	protected void drawBlock(int x, int y, Block blk, float scale, float darkness) {
		drawBlock(x, y, blk.getDrawColor(), blk.skin, blk.getAttribute(Block.BLOCK_ATTRIBUTE_BONE), darkness, blk.alpha, scale);
	}

	/**
	 * Blockピースを描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param piece 描画するピース
	 */
	protected void drawPiece(int x, int y, Piece piece) {
		drawPiece(x, y, piece, 1.0f);
	}

	/**
	 * Blockピースを描画（拡大率指定可能）
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param piece 描画するピース
	 * @param scale 拡大率
	 */
	protected void drawPiece(int x, int y, Piece piece, float scale) {
		drawPiece(x, y, piece, scale, 0f);
	}

	/**
	 * Blockピースを描画（暗さもしくは明るさの指定可能）
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param piece 描画するピース
	 * @param scale 拡大率
	 * @param darkness 暗さもしくは明るさ
	 */
	protected void drawPiece(int x, int y, Piece piece, float scale, float darkness) {
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
	 */
	protected void drawCurrentPiece(int x, int y, GameEngine engine, float scale) {
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
	 */
	protected void drawGhostPiece(int x, int y, GameEngine engine, float scale) {
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
							int ls = (blksize-1);

							int colorID = blkTemp.getDrawColor();
							if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_BONE)) colorID = -1;
							Color color = getColorByID(colorID);
							if(showbg) {
								color.a = 0.5f;
							} else {
								color = color.darker(0.5f);
							}
							graphics.setColor(color);
							graphics.fillRect(x3, y3, blksize, blksize);
							graphics.setColor(Color.white);

							if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP)) {
								graphics.drawLine(x3, y3, x3 + ls, y3);
								graphics.drawLine(x3, y3 + 1, x3 + ls, y3 + 1);
							}
							if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN)) {
								graphics.drawLine(x3, y3 + ls, x3 + ls, y3 + ls);
								graphics.drawLine(x3, y3 - 1 + ls, x3 + ls, y3 - 1 + ls);
							}
							if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT)) {
								graphics.drawLine(x3, y3, x3, y3 + ls);
								graphics.drawLine(x3 + 1, y3, x3 + 1, y3 + ls);
							}
							if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT)) {
								graphics.drawLine(x3 + ls, y3, x3 + ls, y3 + ls);
								graphics.drawLine(x3 - 1 + ls, y3, x3 - 1 + ls, y3 + ls);
							}
							if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT | Block.BLOCK_ATTRIBUTE_CONNECT_UP)) {
								graphics.fillRect(x3, y3, 2, 2);
							}
							if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT | Block.BLOCK_ATTRIBUTE_CONNECT_DOWN)) {
								graphics.fillRect(x3, y3 + (blksize-2), 2, 2);
							}
							if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT | Block.BLOCK_ATTRIBUTE_CONNECT_UP)) {
								graphics.fillRect(x3 + (blksize-2), y3, 2, 2);
							}
							if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT | Block.BLOCK_ATTRIBUTE_CONNECT_DOWN)) {
								graphics.fillRect(x3 + (blksize-2), y3 + (blksize-2), 2, 2);
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
						int ls = (blksize * 2 -1);

						int colorID = blkTemp.getDrawColor();
						if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_BONE)) colorID = -1;
						Color color = getColorByID(colorID);
						if(showbg) {
							color.a = 0.5f;
						} else {
							color = color.darker(0.5f);
						}
						graphics.setColor(color);
						graphics.fillRect(x3, y3, blksize * 2, blksize * 2);
						graphics.setColor(Color.white);

						if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP)) {
							graphics.drawLine(x3, y3, x3 + ls, y3);
							graphics.drawLine(x3, y3 + 1, x3 + ls, y3 + 1);
						}
						if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN)) {
							graphics.drawLine(x3, y3 + ls, x3 + ls, y3 + ls);
							graphics.drawLine(x3, y3 - 1 + ls, x3 + ls, y3 - 1 + ls);
						}
						if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT)) {
							graphics.drawLine(x3, y3, x3, y3 + ls);
							graphics.drawLine(x3 + 1, y3, x3 + 1, y3 + ls);
						}
						if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT)) {
							graphics.drawLine(x3 + ls, y3, x3 + ls, y3 + ls);
							graphics.drawLine(x3 - 1 + ls, y3, x3 - 1 + ls, y3 + ls);
						}
						if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT | Block.BLOCK_ATTRIBUTE_CONNECT_UP)) {
							graphics.fillRect(x3, y3, 2, 2);
						}
						if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT | Block.BLOCK_ATTRIBUTE_CONNECT_DOWN)) {
							graphics.fillRect(x3, y3 + (blksize*2-2), 2, 2);
						}
						if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT | Block.BLOCK_ATTRIBUTE_CONNECT_UP)) {
							graphics.fillRect(x3 + (blksize*2-2), y3, 2, 2);
						}
						if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT | Block.BLOCK_ATTRIBUTE_CONNECT_DOWN)) {
							graphics.fillRect(x3 + (blksize*2-2), y3 + (blksize*2-2), 2, 2);
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
	 */
	protected void drawField(int x, int y, GameEngine engine, boolean small) {
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
					} else if(blk.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE)) {
						drawBlock(x2, y2, blk, small ? 0.5f : 1.0f);
					}

					if(blk.getAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE) && !blk.getAttribute(Block.BLOCK_ATTRIBUTE_BONE)) {
						Color filter = new Color(Color.white);
						filter.a = blk.alpha;
						graphics.setColor(filter);
						int ls = (blksize-1);
						if(engine.blockOutlineType == GameEngine.BLOCK_OUTLINE_NORMAL) {
							if(field.getBlockColor(j, i - 1) == Block.BLOCK_COLOR_NONE) graphics.drawLine(x2, y2, x2 + ls, y2);
							if(field.getBlockColor(j, i + 1) == Block.BLOCK_COLOR_NONE) graphics.drawLine(x2, y2 + ls, x2 + ls, y2 + ls);
							if(field.getBlockColor(j - 1, i) == Block.BLOCK_COLOR_NONE) graphics.drawLine(x2, y2, x2, y2 + ls);
							if(field.getBlockColor(j + 1, i) == Block.BLOCK_COLOR_NONE) graphics.drawLine(x2 + ls, y2, x2 + ls, y2 + ls);
						} else if(engine.blockOutlineType == GameEngine.BLOCK_OUTLINE_CONNECT) {
							if(!blk.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP))     graphics.drawLine(x2, y2, x2 + ls, y2);
							if(!blk.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN))   graphics.drawLine(x2, y2 + ls, x2 + ls, y2 + ls);
							if(!blk.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT))   graphics.drawLine(x2, y2, x2, y2 + ls);
							if(!blk.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT))  graphics.drawLine(x2 + ls, y2, x2 + ls, y2 + ls);
						} else if(engine.blockOutlineType == GameEngine.BLOCK_OUTLINE_SAMECOLOR) {
							if(field.getBlockColor(j, i - 1) != blk.color) graphics.drawLine(x2, y2, x2 + ls, y2);
							if(field.getBlockColor(j, i + 1) != blk.color) graphics.drawLine(x2, y2 + ls, x2 + ls, y2 + ls);
							if(field.getBlockColor(j - 1, i) != blk.color) graphics.drawLine(x2, y2, x2, y2 + ls);
							if(field.getBlockColor(j + 1, i) != blk.color) graphics.drawLine(x2 + ls, y2, x2 + ls, y2 + ls);
						}
					}

					graphics.setColor(Color.white);
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
	 */
	protected void drawFrame(int x, int y, GameEngine engine, boolean small) {
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

		// フィールド背景
		if((fieldbgbright > 0) && (showbg)) {
			Color filter = new Color(Color.black);
			filter.a = fieldbgbright;
			graphics.setColor(filter);
			graphics.fillRect(x + 4, y + 4, width * size*4, height * size*4);
			graphics.setColor(Color.white);
		}

		// Upと下
		int maxWidth = (width * size * 4);
		if(showmeter) maxWidth = (width * size * 4) + (2 * 4);

		int tmpX = 0;
		int tmpY = 0;

		tmpX = x + 4;
		tmpY = y;
		graphics.drawImage(ResourceHolder.imgFrame, tmpX, tmpY, tmpX + maxWidth, tmpY + 4, offsetX + 4, 0, (offsetX + 4) + 4, 4);
		tmpY = y + (height * size * 4) + 4;
		graphics.drawImage(ResourceHolder.imgFrame, tmpX, tmpY, tmpX + maxWidth, tmpY + 4, offsetX + 4, 8, (offsetX + 4) + 4, 8 + 4);

		// 左と右
		tmpX = x;
		tmpY = y + 4;
		graphics.drawImage(ResourceHolder.imgFrame, tmpX, tmpY, tmpX + 4, tmpY + (height * size*4), offsetX, 4, offsetX + 4, 4 + 4);

		if(showmeter) {
			tmpX = x + (width * size * 4) + 12;
		} else {
			tmpX = x + (width * size * 4) + 4;
		}
		graphics.drawImage(ResourceHolder.imgFrame, tmpX, tmpY, tmpX + 4, tmpY + (height * size*4), offsetX + 8, 4, offsetX + 8 + 4, 4 + 4);

		// 左上
		tmpX = x;
		tmpY = y;
		graphics.drawImage(ResourceHolder.imgFrame, tmpX, tmpY, tmpX + 4, tmpY + 4, offsetX, 0, offsetX + 4, 4);

		// 左下
		tmpX = x;
		tmpY = y + (height * size * 4) + 4;
		graphics.drawImage(ResourceHolder.imgFrame, tmpX, tmpY, tmpX + 4, tmpY + 4, offsetX, 8, offsetX + 4, 8 + 4);

		if(showmeter) {
			// MeterONのときの右上
			tmpX = x + (width * size * 4) + 12;
			tmpY = y;
			graphics.drawImage(ResourceHolder.imgFrame, tmpX, tmpY, tmpX + 4, tmpY + 4, offsetX + 8, 0, (offsetX + 8) + 4, 4);

			// MeterONのときの右下
			tmpX = x + (width * size * 4) + 12;
			tmpY = y + (height * size * 4) + 4;
			graphics.drawImage(ResourceHolder.imgFrame, tmpX, tmpY, tmpX + 4, tmpY + 4, offsetX + 8, 8, (offsetX + 8) + 4, 8 + 4);

			// 右Meterの枠
			tmpX = x + (width * size * 4) + 4;
			tmpY = y + 4;
			graphics.drawImage(ResourceHolder.imgFrame, tmpX, tmpY, tmpX + 4, tmpY + (height * size * 4), offsetX + 12, 4, (offsetX + 12) + 4, 4 + 4);

			tmpX = x + (width * size * 4) + 4;
			tmpY = y;
			graphics.drawImage(ResourceHolder.imgFrame, tmpX, tmpY, tmpX + 4, tmpY + 4, offsetX + 12, 0, (offsetX + 12) + 4, 4);

			tmpX = x + (width * size * 4) + 4;
			tmpY = y + (height * size * 4) + 4;
			graphics.drawImage(ResourceHolder.imgFrame, tmpX, tmpY, tmpX + 4, tmpY + 4, offsetX + 12, 8, (offsetX + 12) + 4, 8 + 4);

			// 右Meter
			int maxHeight = height * size * 4;
			if((engine != null) && (engine.meterValue > 0)) maxHeight = (height * size * 4) - engine.meterValue;

			tmpX = x + (width * size * 4) + 8;
			tmpY = y + 4;

			if(maxHeight > 0) {
				graphics.setColor(Color.black);
				graphics.fillRect(tmpX, tmpY, 4, maxHeight);
				graphics.setColor(Color.white);
			}

			if((engine != null) && (engine.meterValue > 0)) {
				int value = engine.meterValue;
				if(value > height * size * 4) value = height * size * 4;

				if(value > 0) {
					tmpX = x + (width * size * 4) + 8;
					tmpY = y + (height * size * 4) + 3 - (value - 1);

					Color color = Color.white;
					switch(engine.meterColor) {
					case GameEngine.METER_COLOR_GREEN:
						color = Color.green;
						break;
					case GameEngine.METER_COLOR_YELLOW:
						color = Color.yellow;
						break;
					case GameEngine.METER_COLOR_ORANGE:
						color = Color.orange;
						break;
					case GameEngine.METER_COLOR_RED:
						color = Color.red;
						break;
					}
					graphics.setColor(color);
					graphics.fillRect(tmpX, tmpY, 4, value);
					graphics.setColor(Color.white);
				}
			}
		} else {
			// MeterOFFのときの右上
			tmpX = x + (width * size * 4) + 4;
			tmpY = y;
			graphics.drawImage(ResourceHolder.imgFrame, tmpX, tmpY, tmpX + 4, tmpY + 4, offsetX + 8, 0, (offsetX + 8) + 4, 4);

			// MeterOFFのときの右下
			tmpX = x + (width * size * 4) + 4;
			tmpY = y + (height * size * 4) + 4;
			graphics.drawImage(ResourceHolder.imgFrame, tmpX, tmpY, tmpX + 4, tmpY + 4, offsetX + 8, 8, (offsetX + 8) + 4, 8 + 4);
		}
	}

	/**
	 * NEXTを描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 */
	protected void drawNext(int x, int y, GameEngine engine) {
		if(graphics == null) return;

		// NEXT欄背景
		if(showbg && darknextarea) {
			Color filter = new Color(Color.black);
			graphics.setColor(filter);

			if(sidenext) {
				int x2 = showmeter ? (x + 176) : (x + 168);
				int maxNext = engine.isNextVisible ? engine.ruleopt.nextDisplay : 0;

				// HOLD area
				if(engine.ruleopt.holdEnable && engine.isHoldVisible) {
					graphics.fillRect(x - 32, y + 48 + 8, 32, 32 - 16);
					for(int i = 0; i <= 8; i++) {
						Color filter2 = new Color(Color.black);
						filter2.a = ((float)i / (float)8);
						graphics.setColor(filter2);
						graphics.fillRect(x - 32, y + 47 + i, 32, 1);
					}
					for(int i = 0; i <= 8; i++) {
						Color filter2 = new Color(Color.black);
						filter2.a = ((float)i / (float)8);
						graphics.setColor(filter2);
						graphics.fillRect(x - 32, y + 80 - i, 32, 1);
					}
				}

				// NEXT area
				if(maxNext > 0) {
					graphics.fillRect(x2, y + 48 + 8, 32, (32 * maxNext) - 16);
					for(int i = 0; i <= 8; i++) {
						Color filter2 = new Color(Color.black);
						filter2.a = ((float)i / (float)8);
						graphics.setColor(filter2);
						graphics.fillRect(x2, y + 47 + i, 32, 1);
					}
					for(int i = 0; i <= 8; i++) {
						Color filter2 = new Color(Color.black);
						filter2.a = ((float)i / (float)8);
						graphics.setColor(filter2);
						graphics.fillRect(x2, y + 48 + (32 * maxNext) - i, 32, 1);
					}
				}
			} else {
				graphics.fillRect(x + 20, y, 135, 48);

				for(int i = 0; i <= 20; i++) {
					Color filter2 = new Color(Color.black);
					filter2.a = ((float)i / (float)20);
					graphics.setColor(filter2);
					graphics.fillRect(x + i - 1, y, 1, 48);
				}
				for(int i = 0; i <= 20; i++) {
					Color filter2 = new Color(Color.black);
					filter2.a = ((float)(20 - i) / (float)20);
					graphics.setColor(filter2);
					graphics.fillRect(x + i + 155, y, 1, 48);
				}
			}

			graphics.setColor(Color.white);
		}

		if(engine.isNextVisible) {
			if(sidenext) {
				if(engine.ruleopt.nextDisplay >= 1) {
					int x2 = showmeter ? (x + 176) : (x + 168);
					NormalFont.printFont(x2, y + 40, NullpoMinoSlick.getUIText("InGame_Next"), COLOR_ORANGE, 0.5f);

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
					NormalFont.printFont(x + 60, y, NullpoMinoSlick.getUIText("InGame_Next"), COLOR_ORANGE, 0.5f);

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
					NormalFont.printFont(x2, y2, NullpoMinoSlick.getUIText("InGame_Hold"), tempColor, 0.5f);
				} else {
					if(!engine.holdDisable) {
						if((holdRemain > 0) && (holdRemain <= 10)) tempColor = COLOR_YELLOW;
						if((holdRemain > 0) && (holdRemain <= 5)) tempColor = COLOR_RED;
					}

					NormalFont.printFont(x2, y2, NullpoMinoSlick.getUIText("InGame_Hold") + "\ne " + holdRemain, tempColor, 0.5f);
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
	 */
	protected void drawShadowNexts(int x, int y, GameEngine engine, float scale) {
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

	/**
	 * 各フレーム最初の描画処理
	 * @param engine GameEngine
	 * @param playerID プレイヤーID
	 */
	@Override
	public void renderFirst(GameEngine engine, int playerID) {
		if(graphics == null) return;

		if(engine.playerID == 0) {
			// 背景
			if(engine.owner.menuOnly) {
				graphics.setColor(Color.white);
				graphics.drawImage(ResourceHolder.imgMenu, 0, 0);
			} else {
				int bg = engine.owner.backgroundStatus.bg;
				if(engine.owner.backgroundStatus.fadesw && !heavyeffect) {
					bg = engine.owner.backgroundStatus.fadebg;
				}

				if((ResourceHolder.imgPlayBG != null) && (bg >= 0) && (bg < ResourceHolder.imgPlayBG.length) && (showbg == true)) {
					graphics.setColor(Color.white);
					graphics.drawImage(ResourceHolder.imgPlayBG[bg], 0, 0);

					if(engine.owner.backgroundStatus.fadesw && heavyeffect) {
						Color filter = new Color(Color.black);
						if(engine.owner.backgroundStatus.fadestat == false) {
							filter.a = (float) engine.owner.backgroundStatus.fadecount / 100;
						} else {
							filter.a = (float) (100 - engine.owner.backgroundStatus.fadecount) / 100;
						}
						graphics.setColor(filter);
						graphics.fillRect(0, 0, 640, 480);
					}
				} else {
					graphics.setColor(Color.black);
					graphics.fillRect(0, 0, 640, 480);
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
	}

	/*
	 * Ready画面の描画処理
	 */
	@Override
	public void renderReady(GameEngine engine, int playerID) {
		if(graphics == null) return;
		if(engine.allowTextRenderByReceiver == false) return;
		//if(engine.isVisible == false) return;

		if(engine.statc[0] > 0) {
			int offsetX = getFieldDisplayPositionX(engine, playerID);
			int offsetY = getFieldDisplayPositionY(engine, playerID);

			if(engine.statc[0] > 0) {
				if(!engine.minidisplay) {
					if((engine.statc[0] >= engine.readyStart) && (engine.statc[0] < engine.readyEnd))
						NormalFont.printFont(offsetX + 44, offsetY + 204, "READY", COLOR_WHITE, 1.0f);
					else if((engine.statc[0] >= engine.goStart) && (engine.statc[0] < engine.goEnd))
						NormalFont.printFont(offsetX + 62, offsetY + 204, "GO!", COLOR_WHITE, 1.0f);
				} else {
					if((engine.statc[0] >= engine.readyStart) && (engine.statc[0] < engine.readyEnd))
						NormalFont.printFont(offsetX + 24, offsetY + 80, "READY", COLOR_WHITE, 0.5f);
					else if((engine.statc[0] >= engine.goStart) && (engine.statc[0] < engine.goEnd))
						NormalFont.printFont(offsetX + 32, offsetY + 80, "GO!", COLOR_WHITE, 0.5f);
				}
			}
		}
	}

	/*
	 * Blockピース移動時の処理
	 */
	@Override
	public void renderMove(GameEngine engine, int playerID) {
		if(engine.isVisible == false) return;

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
				EffectObject obj = new EffectObject(1,
													getFieldDisplayPositionX(engine, playerID) + 4 + (x * 16),
													getFieldDisplayPositionY(engine, playerID) + 52 + (y * 16),
													color);
				effectlist.add(obj);
			}
			// 宝石Block
			else if(blk.isGemBlock()) {
				EffectObject obj = new EffectObject(2,
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
		if(engine.isVisible == false) return;

		int offsetX = getFieldDisplayPositionX(engine, playerID);
		int offsetY = getFieldDisplayPositionY(engine, playerID);

		if(!engine.minidisplay) {
			if(engine.statc[1] == 0)
				NormalFont.printFont(offsetX + 4, offsetY + 204, "EXCELLENT!", COLOR_ORANGE, 1.0f);
			else if(engine.owner.getPlayers() < 3)
				NormalFont.printFont(offsetX + 52, offsetY + 204, "WIN!", COLOR_ORANGE, 1.0f);
			else
				NormalFont.printFont(offsetX + 4, offsetY + 204, "1ST PLACE!", COLOR_ORANGE, 1.0f);
		} else {
			if(engine.statc[1] == 0)
				NormalFont.printFont(offsetX + 4, offsetY + 80, "EXCELLENT!", COLOR_ORANGE, 0.5f);
			else if(engine.owner.getPlayers() < 3)
				NormalFont.printFont(offsetX + 33, offsetY + 80, "WIN!", COLOR_ORANGE, 0.5f);
			else
				NormalFont.printFont(offsetX + 4, offsetY + 80, "1ST PLACE!", COLOR_ORANGE, 0.5f);
		}
	}

	/*
	 * ゲームオーバー画面の描画処理
	 */
	@Override
	public void renderGameOver(GameEngine engine, int playerID) {
		if(graphics == null) return;
		if(engine.allowTextRenderByReceiver == false) return;
		if(engine.isVisible == false) return;

		if((engine.statc[0] >= engine.field.getHeight() + 1) && (engine.statc[0] < engine.field.getHeight() + 1 + 180)) {
			int offsetX = getFieldDisplayPositionX(engine, playerID);
			int offsetY = getFieldDisplayPositionY(engine, playerID);

			if(!engine.minidisplay) {
				if(engine.owner.getPlayers() < 2)
					NormalFont.printFont(offsetX + 12, offsetY + 204, "GAME OVER", COLOR_WHITE, 1.0f);
				else if(engine.owner.getWinner() == -2)
					NormalFont.printFont(offsetX + 52, offsetY + 204, "DRAW", COLOR_GREEN, 1.0f);
				else if(engine.owner.getPlayers() < 3)
					NormalFont.printFont(offsetX + 52, offsetY + 204, "LOSE", COLOR_WHITE, 1.0f);
			} else {
				if(engine.owner.getPlayers() < 2)
					NormalFont.printFont(offsetX + 4, offsetY + 80, "GAME OVER", COLOR_WHITE, 0.5f);
				else if(engine.owner.getWinner() == -2)
					NormalFont.printFont(offsetX + 28, offsetY + 80, "DRAW", COLOR_GREEN, 0.5f);
				else if(engine.owner.getPlayers() < 3)
					NormalFont.printFont(offsetX + 28, offsetY + 80, "LOSE", COLOR_WHITE, 0.5f);
			}
		}
	}

	/*
	 * Render results screen処理
	 */
	@Override
	public void renderResult(GameEngine engine, int playerID) {
		if(graphics == null) return;
		if(engine.allowTextRenderByReceiver == false) return;
		if(engine.isVisible == false) return;

		int tempColor;

		if(engine.statc[0] == 0)
			tempColor = COLOR_RED;
		else
			tempColor = COLOR_WHITE;
		NormalFont.printFont(getFieldDisplayPositionX(engine, playerID) + 12, getFieldDisplayPositionY(engine, playerID) + 340, "RETRY", tempColor, 1.0f);

		if(engine.statc[0] == 1)
			tempColor = COLOR_RED;
		else
			tempColor = COLOR_WHITE;
		NormalFont.printFont(getFieldDisplayPositionX(engine, playerID) + 108, getFieldDisplayPositionY(engine, playerID) + 340, "END", tempColor, 1.0f);
	}

	/*
	 * フィールドエディット画面の描画処理
	 */
	@Override
	public void renderFieldEdit(GameEngine engine, int playerID) {
		if(graphics == null) return;
		int x = getFieldDisplayPositionX(engine, playerID) + 4 + (engine.fldeditX * 16);
		int y = getFieldDisplayPositionY(engine, playerID) + 52 + (engine.fldeditY * 16);
		float bright = (engine.fldeditFrames % 60 >= 30) ? -0.5f : -0.2f;
		drawBlock(x, y, engine.fldeditColor, engine.getSkin(), false, bright, 1.0f, 1.0f);
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
			EffectObject obj = effectlist.get(i);

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
			EffectObject obj = effectlist.get(i);

			// 通常Block
			if(obj.effect == 1) {
				int x = obj.x - 40;
				int y = obj.y - 15;
				int color = obj.param - Block.BLOCK_COLOR_GRAY;

				if(obj.anim < 30) {
					int srcx = ((obj.anim-1) % 6) * 96;
					int srcy = ((obj.anim-1) / 6) * 96;
					try {
						graphics.drawImage(ResourceHolder.imgBreak[color][0], x, y, x + 96, y + 96, srcx, srcy, srcx + 96, srcy + 96);
					} catch (Exception e) {}
				} else {
					int srcx = ((obj.anim-30) % 6) * 96;
					int srcy = ((obj.anim-30) / 6) * 96;
					try {
						graphics.drawImage(ResourceHolder.imgBreak[color][1], x, y, x + 96, y + 96, srcx, srcy, srcx + 96, srcy + 96);
					} catch (Exception e) {}
				}
			}
			// 宝石Block
			if(obj.effect == 2) {
				int x = obj.x - 8;
				int y = obj.y - 8;
				int srcx = ((obj.anim-1) % 10) * 32;
				int srcy = ((obj.anim-1) / 10) * 32;
				int color = obj.param - Block.BLOCK_COLOR_GEM_RED;

				try {
					graphics.drawImage(ResourceHolder.imgPErase[color], x, y, x + 32, y + 32, srcx, srcy, srcx + 32, srcy + 32);
				} catch (Exception e) {}
			}
		}
	}
}
