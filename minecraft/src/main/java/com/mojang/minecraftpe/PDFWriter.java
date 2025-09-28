package com.mojang.minecraftpe;

import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import androidx.core.view.ViewCompat;
import com.mojang.minecraftpe.utils.FileHelper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class PDFWriter {
    private final Rect mImageRect;
    private final Rect mPageRect = new Rect(0, 0, 612, 792);
    private final TextPaint mPageTextPaint;
    private final Rect mTextRect;
    private final Rect mTitleRect = new Rect(0, 0, mPageRect.width(), (int) (((float) mPageRect.height()) * (1.0f - 0.3f)));
    private final TextPaint mTitleTextPaint;
    private PdfDocument mOpenDocument;

    public PDFWriter() {
        mTitleRect.offset(0, (int) (((float) mPageRect.height()) * 0.3f));
        mTextRect = new Rect(mPageRect);
        mTextRect.inset(20, 20);
        mImageRect = new Rect(0, 0, 500, 500);
        mImageRect.offset(mPageRect.centerX() - mImageRect.centerX(), mPageRect.centerY() - mImageRect.centerY());
        Typeface titleFont = Typeface.DEFAULT_BOLD;
        try {
            titleFont = Typeface.createFromAsset(MainActivity.mInstance.getAssets(), "fonts/Mojangles.ttf");
        } catch (Exception e) {
            System.out.println("Failed to load mojangles font: " + e.getMessage());
        }
        mTitleTextPaint = new TextPaint();
        mTitleTextPaint.setAntiAlias(true);
        mTitleTextPaint.setTextSize(64.0f);
        mTitleTextPaint.setColor(-16777216);
        mTitleTextPaint.setTypeface(titleFont);
        mPageTextPaint = new TextPaint();
        mPageTextPaint.setAntiAlias(true);
        mPageTextPaint.setTextSize(32.0f);
        mPageTextPaint.setColor(-16777216);
    }

    public boolean createDocument(String[] orderedFilenames, String title) {
        if (mOpenDocument != null) {
            mOpenDocument.close();
        }
        mOpenDocument = new PdfDocument();
        PdfDocument.Page titlePage = mOpenDocument.startPage(_getPageInfo(1));
        _drawTextInRect(title, titlePage, mTitleTextPaint, mTitleRect, Layout.Alignment.ALIGN_CENTER);
        mOpenDocument.finishPage(titlePage);
        int i = 0;
        while (i < orderedFilenames.length) {
            String filename = orderedFilenames[i];
            PdfDocument.Page page = mOpenDocument.startPage(_getPageInfo(i + 2));
            try {
                String fileExtension = _getExtension(filename);
                if (fileExtension.equals("txt")) {
                    _drawTextInRect(_readFileToString(filename), page, mPageTextPaint, mTextRect, Layout.Alignment.ALIGN_NORMAL);
                } else if (fileExtension.equals("jpeg")) {
                    page.getCanvas().drawBitmap(BitmapFactory.decodeFile(filename), null, mImageRect, null);
                } else {
                    throw new UnsupportedOperationException("Unsupported extension from file: " + filename);
                }
                mOpenDocument.finishPage(page);
                i++;
            } catch (Exception e) {
                System.out.println("Failed to write page: " + e.getMessage());
                closeDocument();
                return false;
            }
        }
        return true;
    }

    public boolean writeDocumentToFile(String destinationFilename) {
        try {
            mOpenDocument.writeTo(new FileOutputStream(destinationFilename));
            return true;
        } catch (Exception e) {
            System.out.println("Failed to write pdf file: " + e.getMessage());
            return false;
        }
    }

    public void closeDocument() {
        if (mOpenDocument != null) {
            mOpenDocument.close();
            mOpenDocument = null;
        }
    }

    public String getPicturesDirectory() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
    }

    private PdfDocument.PageInfo _getPageInfo(int pageNumber) {
        return new PdfDocument.PageInfo.Builder(this.mPageRect.width(), mPageRect.height(), pageNumber).create();
    }

    private void _drawTextInRect(String text, @NotNull PdfDocument.Page page, TextPaint textPaint, @NotNull Rect rect, Layout.Alignment alignment) {
        StaticLayout textLayout = new StaticLayout(text, textPaint, rect.width(), alignment, 1.0f, 0.0f, false);
        Canvas pageCanvas = page.getCanvas();
        pageCanvas.translate((float) rect.left, (float) rect.top);
        textLayout.draw(pageCanvas);
    }

    @NotNull
    @Contract("_ -> new")
    private String _readFileToString(String filename) throws IOException {
        return FileHelper.readFile(filename);
    }

    @NotNull
    private String _getExtension(@NotNull String filename) {
        int index = filename.lastIndexOf(46);
        if (index < 0 || index + 1 >= filename.length()) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase();
    }
}
