/*
 * Copyright 2019 Dmitriy Ponomarenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dimowner.charttemplate;


public class ColorMap {

	private ColorMap() {}

	private static boolean isNight = false;
	private static final boolean isAvailable = true;

	private static int windowBackground = R.color.window_background;
	private static int viewBackground = R.color.view_background;
	private static int primaryColor = R.color.white;
	private static int gridColor = R.color.grid_color2;
	private static int gridBaseLineColor = R.color.grid_base_line;
	private static int gridTextColor = R.color.text_color;
	private static int selectionColor = R.color.selection_color;
	private static int overlayColor = R.color.overlay_color;
	private static int textCheckerColor = R.color.text_dark;
	private static int panelColor = R.color.panel_background;
	private static int panelTextColor = R.color.panel_text;
	private static int scrubblerColor = R.color.scrubbler_color;
	private static int shadowColor = R.color.shadow_color;
	private static int tittleColor = R.color.black ;
	private static int barOverlayColor = R.color.bar_overlay_color;
	private static int arrowColor = R.color.arrow_color;
	private static int zoomOutColor = R.color.zoom_out_color;

	private static int windowBackgroundNight = R.color.window_background_night;
	private static int viewBackgroundNight = R.color.view_background_night;
	private static int primaryColorNight = R.color.primary_night;
	private static int gridColorNight = R.color.grid_color2_night;
	private static int gridBaseLineColorNight = R.color.grid_base_line_night;
	private static int gridTextColorNight = R.color.text_color_night;
	private static int selectionColorNight = R.color.selection_color_night;
	private static int overlayColorNight = R.color.overlay_color_night;
	private static int textCheckerColorNight = R.color.text_light;
	private static int panelColorNight = R.color.panel_background_night;
	private static int panelTextColorNight = R.color.panel_text_night;
	private static int scrubblerColorNight = R.color.scrubbler_color_night;
	private static int shadowColorNight = R.color.shadow_color_night;
	private static int tittleColorNight = R.color.white ;
	private static int barOverlayColorNight = R.color.bar_overlay_color_night;
	private static int arrowColorNight = R.color.arrow_color_night;
	private static int zoomOutColorNight = R.color.zoom_out_color_night;

	private static int moonNight = R.drawable.moon_light7;
	private static int moon = R.drawable.moon7;

	private static int zoomOutIcon = R.drawable.ic_zoom_out;
	private static int zoomOutIconNight = R.drawable.ic_zoom_out_night;

	public static boolean isNightTheme() {
		return isNight;
	}

	public static void setNightTheme() {
		isNight = true;
	}

	public static void setDayTheme() {
		isNight = false;
	}

	public static int getPrimaryColor() {
		if (isNight) { return primaryColorNight; } else { return primaryColor; }
	}

	public static int getViewBackground() {
		if (isNight) { return viewBackgroundNight; } else { return viewBackground; }
	}

	public static int getWindowBackground() {
		if (isNight) { return windowBackgroundNight; } else { return windowBackground; }
	}

	public static int getGridColor() {
		if (isNight) { return gridColorNight; } else { return gridColor; }
	}

	public static int getGridBaseLineColor() {
		if (isNight) { return gridBaseLineColorNight; } else { return gridBaseLineColor; }
	}

	public static int getGridTextColor() {
		if (isNight) { return gridTextColorNight; } else { return gridTextColor; }
	}

	public static int getSelectionColor() {
		if (isNight) { return selectionColorNight; } else { return selectionColor; }
	}

	public static int getOverlayColor() {
		if (isNight) { return overlayColorNight; } else { return overlayColor; }
	}

	public static int getTextCheckerColor() {
		if (isNight) { return textCheckerColorNight; } else { return textCheckerColor; }
	}

	public static int getPanelColor() {
		if (isNight) { return panelColorNight; } else { return panelColor; }
	}

	public static int getPanelTextColor() {
		if (isNight) { return panelTextColorNight; } else { return panelTextColor; }
	}

	public static int getScrubblerColor() {
		if (isNight) { return scrubblerColorNight; } else { return scrubblerColor; }
	}

	public static int getShadowColor() {
		if (isNight) { return shadowColorNight; } else { return shadowColor; }
	}

	public static int getTittleColor() {
		if (isNight) { return tittleColorNight; } else { return tittleColor; }
	}

	public static int getBarOverlayColor() {
		if (isNight) { return barOverlayColorNight; } else { return barOverlayColor; }
	}

	public static int getMoonIcon() {
		if (isNight) { return moonNight; } else { return moon; }
	}

	public static int getArrowColor() {
		if (isNight) { return arrowColorNight; } else { return arrowColor; }
	}

	public static int getZoomOutColor() {
		if (isNight) { return zoomOutColorNight; } else { return zoomOutColor; }
	}

	public static int getZoomOutIcon() {
		if (isNight) { return zoomOutIcon; } else { return zoomOutIconNight; }
	}

	public static boolean isAvailable() {
		return isAvailable;
	}

//	<attr name="primaryColor" format="color" />
//	<attr name="viewBackground" format="color" />
//	<attr name="gridColor" format="color" />
//	<attr name="gridBaseLineColor" format="color" />
//	<attr name="gridTextColor" format="color" />
//	<attr name="selectionColor" format="color" />
//	<attr name="overlayColor" format="color" />
//	<attr name="textCheckerColor" format="color" />
//	<attr name="panelColor" format="color" />
//	<attr name="panelTextColor" format="color" />
//	<attr name="scrubblerColor" format="color" />
//	<attr name="shadowColor" format="color" />
//	<attr name="tittleColor" format="color" />
//	<attr name="barOverlayColor" format="color" />
//	<attr name="arrowColor" format="color" />
}
