package com.appineering.android.drawingpad.ArtCore.ToolCore.Filters;

import com.appineering.android.drawingpad.ArtCore.WorkingImage;



/**
 * @author robert.hinds
 *
 * Class that applies a dither effect to an image
 * 
 * Based on the Dithering effect at http://marvinproject.sourceforge.net/en/plugins/dithering.html
 * originally authored by Gabriel Ambr?sio Archanjo
 *
 */
public class BigBrotherFilter implements IAndroidFilter {

	private static final int DOT_AREA = 10;
	private static final int arrDither[] = {	
		167,200,230,216,181,
		94,72,193,242,232,
		36,52,222,167,200,
		181,126,210,94,72,
		232,153,111,36,52,
		167,200,230,216,181,
		94,72,193,242,232,
		36,52,222,167,200,
		181,126,210,94,72,
		232,153,111,36,52,
		167,200,230,216,181,
		94,72,193,242,232,
		36,52,222,167,200,
		181,126,210,94,72,
		232,153,111,36,52,
		167,200,230,216,181,
		94,72,193,242,232,
		36,52,222,167,200,
		181,126,210,94,72,
		232,153,111,36,52
	};

	@Override
	public WorkingImage process(WorkingImage imageIn) {
		for (int x = 0; x < imageIn.getWidth(); x+=DOT_AREA) {
			for (int y = 0; y < imageIn.getHeight(); y+=DOT_AREA) {
				drawTone(x,y,imageIn);
			}
		}
		
		return imageIn;
	}



	private void drawTone(int a_x, int a_y, WorkingImage imageIn){
		int l_grayIntensity;
		int l_x;
		int l_y;

		for(int x=0; x<DOT_AREA*DOT_AREA; x++){
			l_x = x%DOT_AREA;
			l_y = x/DOT_AREA;

			if(a_x+l_x < imageIn.getWidth() && a_y+l_y < imageIn.getHeight()){

				l_grayIntensity = 255-(imageIn.getRComponent(a_x+l_x, a_y+l_y));

				if(l_grayIntensity > arrDither[x]){
					imageIn.setPixelColor(a_x+l_x, a_y+l_y, 0,0,0);
				}
				else{
					imageIn.setPixelColor(a_x+l_x, a_y+l_y, 255,255,255);
				}
			}
		}
	}




}
