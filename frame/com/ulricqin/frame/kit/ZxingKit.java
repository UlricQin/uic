package com.ulricqin.frame.kit;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class ZxingKit {

	public static void encode(String contents, int width, int height,
			File imgFile) {
		Hashtable<Object, Object> hints = new Hashtable<Object, Object>();
		// 指定纠错等级
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
		// 指定编码格式
		hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
		try {
			BitMatrix bitMatrix = new MultiFormatWriter().encode(contents,
					BarcodeFormat.QR_CODE, width, height, hints);

			MatrixToImageWriter.writeToFile(bitMatrix, "png", imgFile);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void main(String[] args) throws IOException {
		int width = 300, height = 300;
		File imgFile = File.createTempFile("uic_qr", ".png");
		System.out.println(imgFile.getAbsolutePath());
		encode("秦晓辉\n18612185520", width, height, imgFile);
		System.out.println("done");
	}
}
