package bms.player.beatoraja.external;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.BufferUtils;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.config.KeyConfiguration;
import bms.player.beatoraja.decide.MusicDecide;
import bms.player.beatoraja.play.BMSPlayer;
import bms.player.beatoraja.result.CourseResult;
import bms.player.beatoraja.result.MusicResult;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.skin.property.IntegerPropertyFactory;
import bms.player.beatoraja.skin.property.StringPropertyFactory;

public class ScreenShotFileExporter implements ScreenShotExporter {

	@Override
	public boolean send(MainState currentState, byte[] pixels) {
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String stateName = "";
		if (currentState instanceof MusicSelector) {
			stateName = "_Music_Select";
		} else if (currentState instanceof MusicDecide) {
			stateName = "_Decide";
		}
		if (currentState instanceof BMSPlayer) {
			final String tablelevel = StringPropertyFactory.getStringProperty(STRING_TABLE_LEVEL).get(currentState);
			if (tablelevel.length() > 0) {
				stateName = "_Play_" + tablelevel;
			} else {
				stateName = "_Play_LEVEL"
						+ IntegerPropertyFactory.getIntegerProperty(NUMBER_PLAYLEVEL).get(currentState);
			}
			final String fulltitle = StringPropertyFactory.getStringProperty(STRING_FULLTITLE).get(currentState);
			if (fulltitle.length() > 0) {
				stateName += " " + fulltitle;
			}
		} else if (currentState instanceof MusicResult || currentState instanceof CourseResult) {
			if (currentState instanceof MusicResult) {
				final String tablelevel = StringPropertyFactory.getStringProperty(STRING_TABLE_LEVEL).get(currentState);
				if (tablelevel.length() > 0) {
					stateName += "_" + tablelevel + " ";
				} else {
					stateName += "_LEVEL"
							+ IntegerPropertyFactory.getIntegerProperty(NUMBER_PLAYLEVEL).get(currentState) + " ";
				}
			} else {
				stateName += "_";
			}
			final String fulltitle = StringPropertyFactory.getStringProperty(STRING_FULLTITLE).get(currentState);
			if (fulltitle.length() > 0)
				stateName += fulltitle;
			stateName += " " + ScreenShotExporter.getClearTypeName(currentState);
			stateName += " " + ScreenShotExporter.getRankTypeName(currentState);
		} else if (currentState instanceof KeyConfiguration) {
			stateName = "_Config";
		}
		stateName = stateName.replace("\\", "￥").replace("/", "／").replace(":", "：").replace("*", "＊").replace("?", "？")
				.replace("\"", "”").replace("<", "＜").replace(">", "＞").replace("|", "｜").replace("\t", " ");
		stateName = "_LR2oraja" + stateName;

		int width = Gdx.graphics.getBackBufferWidth();
		int height = Gdx.graphics.getBackBufferHeight();
		Config.ScreenShotFormat format = currentState.resource.getConfig().getScreenshotFormat();
		try {
			String ext = (format == Config.ScreenShotFormat.JPG) ? ".jpg" : ".png";
			String path = "screenshot/" + sdf.format(Calendar.getInstance().getTime()) + stateName + ext;

			BufferedImage image = null;
			if (format == Config.ScreenShotFormat.JPG) {
				image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
				int idx = 0;
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						int r = pixels[idx++] & 0xFF;
						int g = pixels[idx++] & 0xFF;
						int b = pixels[idx++] & 0xFF;
						idx++;
						int rgb = (r << 16) | (g << 8) | b;
						image.setRGB(x, y, rgb);
					}
				}

				File file = new File(path);
				if (file.getParentFile() != null) {
					file.getParentFile().mkdirs();
				}
				ImageIO.write(image, "jpg", file);
			} else {
				Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
				BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
				PixmapIO.writePNG(new FileHandle(path), pixmap);
				pixmap.dispose();
			}

			Logger.getGlobal().info("スクリーンショット保存:" + path);
			currentState.main.getMessageRenderer().addMessage("Screen shot saved : " + path, 2000, Color.GOLD, 0);

			if (image == null) {
				// PNGの場合でもクリップボード用にBufferedImageが必要
				image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
				int idx = 0;
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						int r = pixels[idx++] & 0xFF;
						int g = pixels[idx++] & 0xFF;
						int b = pixels[idx++] & 0xFF;
						idx++;
						int rgb = (r << 16) | (g << 8) | b;
						image.setRGB(x, y, rgb);
					}
				}
			}
			this.sendClipboard(currentState, image);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private void sendClipboard(MainState currentState, BufferedImage image) {
		if (!currentState.resource.getConfig().isSetClipboardWhenScreenshot()) {
			// スクショのクリップボードコピーが有効でないなら終わる
			return;
		}
		try {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			ImageTransferable imageTransferable = new ImageTransferable(image);
			clipboard.setContents(imageTransferable, null);
			Logger.getGlobal().info("スクリーンショット保存: Clipboard");
			currentState.main.getMessageRenderer().addMessage("Screen shot saved : Clipboard", 2000, Color.GOLD, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class ImageTransferable implements Transferable {
	private Image image;

	public ImageTransferable(Image image) {
		this.image = image;
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		for (DataFlavor f : getTransferDataFlavors()) {
			if (f.equals(flavor)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException, IOException {
		if (flavor.equals(DataFlavor.imageFlavor)) {
			return image;
		}
		throw new UnsupportedFlavorException(flavor);
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { DataFlavor.imageFlavor };
	}
}
